package com.studypulse.app.data.subjects

import androidx.compose.ui.graphics.Color
import com.google.firebase.firestore.FirebaseFirestore
import com.studypulse.app.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ─────────────────────────────────────────────
//  Data Model
// ─────────────────────────────────────────────

data class Subject(
    val id: String = "",
    val name: String = "",
    val code: String = "",
    val icon: String = "📚",
    val colorHex: String = "#7c3aed",
    val credits: Int = 3,
    val weeklyGoalHours: Float = 5f,
    val hoursThisWeek: Float = 0f,
    val hoursTotal: Float = 0f,
    val tags: List<String> = emptyList(),
    val hasExam: Boolean = false,
    val weeklyHours: List<Float> = List(7) { 0f }, // Mon–Sun
    val userId: String = ""
) {
    val progress: Float get() = if (weeklyGoalHours > 0) (hoursThisWeek / weeklyGoalHours).coerceIn(0f, 1f) else 0f
    val remainingHours: Float get() = (weeklyGoalHours - hoursThisWeek).coerceAtLeast(0f)
}

// Default Year 4 CS subjects
fun defaultYear4Subjects(userId: String) = listOf(
    Subject(id = "fyp",    name = "Final Year Project",       code = "CS490", icon = "🎓", colorHex = "#7c3aed", credits = 6, weeklyGoalHours = 10f, hoursThisWeek = 8.2f, tags = listOf("High Priority", "Project"), userId = userId),
    Subject(id = "ai",     name = "Artificial Intelligence",  code = "CS410", icon = "🤖", colorHex = "#3b82f6", credits = 4, weeklyGoalHours = 8f,  hoursThisWeek = 5.4f, tags = listOf("Exam Soon"), hasExam = true, userId = userId),
    Subject(id = "ml",     name = "Machine Learning",         code = "CS415", icon = "🧠", colorHex = "#8b5cf6", credits = 4, weeklyGoalHours = 8f,  hoursThisWeek = 4.8f, tags = listOf("Exam Soon"), hasExam = true, userId = userId),
    Subject(id = "se",     name = "Software Engineering",     code = "CS420", icon = "⚙️", colorHex = "#10b981", credits = 3, weeklyGoalHours = 6f,  hoursThisWeek = 3.6f, tags = listOf("Group Work"), userId = userId),
    Subject(id = "netsec", name = "Networks & Security",      code = "CS430", icon = "🔒", colorHex = "#ef4444", credits = 3, weeklyGoalHours = 6f,  hoursThisWeek = 2.1f, tags = listOf("Needs Attention"), hasExam = true, userId = userId),
    Subject(id = "dba",    name = "Database Administration",  code = "CS440", icon = "🗄️", colorHex = "#f59e0b", credits = 3, weeklyGoalHours = 6f,  hoursThisWeek = 3.2f, tags = listOf("Practical"), userId = userId),
    Subject(id = "mob",    name = "Mobile Development",       code = "CS450", icon = "📱", colorHex = "#ec4899", credits = 3, weeklyGoalHours = 5f,  hoursThisWeek = 1.8f, tags = listOf("Needs Attention", "Project"), userId = userId),
    Subject(id = "rm",     name = "Research Methods",         code = "CS460", icon = "📝", colorHex = "#06b6d4", credits = 2, weeklyGoalHours = 4f,  hoursThisWeek = 2.2f, tags = listOf("Paper Due"), userId = userId),
)

// ─────────────────────────────────────────────
//  Repository
// ─────────────────────────────────────────────

@Singleton
class SubjectRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    private val _subjects = MutableStateFlow<List<Subject>>(emptyList())
    val subjects: StateFlow<List<Subject>> = _subjects

    // Load subjects from Firestore for a user
    suspend fun loadSubjects(userId: String) {
        try {
            val snapshot = firestore.collection("users")
                .document(userId)
                .collection("subjects")
                .get().await()

            val loaded = snapshot.documents.mapNotNull { it.toObject(Subject::class.java) }

            // First time — seed with default Year 4 CS subjects
            if (loaded.isEmpty()) {
                val defaults = defaultYear4Subjects(userId)
                seedSubjects(userId, defaults)
                _subjects.value = defaults
            } else {
                _subjects.value = loaded
            }
        } catch (e: Exception) {
            // Fallback to defaults if offline
            _subjects.value = defaultYear4Subjects(userId)
        }
    }

    private suspend fun seedSubjects(userId: String, subjects: List<Subject>) {
        val batch = firestore.batch()
        val col = firestore.collection("users").document(userId).collection("subjects")
        subjects.forEach { subject ->
            batch.set(col.document(subject.id), subject)
        }
        batch.commit().await()
    }

    // Add a new subject
    suspend fun addSubject(userId: String, subject: Subject): Boolean {
        return try {
            val ref = firestore.collection("users").document(userId)
                .collection("subjects").document()
            val newSubject = subject.copy(id = ref.id, userId = userId)
            ref.set(newSubject).await()
            _subjects.value = _subjects.value + newSubject
            true
        } catch (e: Exception) { false }
    }

    // Update existing subject
    suspend fun updateSubject(userId: String, subject: Subject): Boolean {
        return try {
            firestore.collection("users").document(userId)
                .collection("subjects").document(subject.id)
                .set(subject).await()
            _subjects.value = _subjects.value.map { if (it.id == subject.id) subject else it }
            true
        } catch (e: Exception) { false }
    }

    // Delete subject
    suspend fun deleteSubject(userId: String, subjectId: String): Boolean {
        return try {
            firestore.collection("users").document(userId)
                .collection("subjects").document(subjectId)
                .delete().await()
            _subjects.value = _subjects.value.filter { it.id != subjectId }
            true
        } catch (e: Exception) { false }
    }

    // Log study hours to a subject
    suspend fun logStudyHours(userId: String, subjectId: String, hours: Float) {
        val subject = _subjects.value.find { it.id == subjectId } ?: return
        val updated = subject.copy(
            hoursThisWeek = subject.hoursThisWeek + hours,
            hoursTotal = subject.hoursTotal + hours
        )
        updateSubject(userId, updated)
    }
}
