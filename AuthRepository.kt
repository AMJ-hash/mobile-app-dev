package com.studypulse.app.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

// ── User profile stored in Firestore
data class StudyPulseUser(
    val uid: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val year: Int = 4,
    val programme: String = "Computer Science",
    val courses: List<String> = defaultYear4Courses(),
    val streakDays: Int = 0,
    val totalHours: Float = 0f,
    val createdAt: Long = System.currentTimeMillis()
)

fun defaultYear4Courses() = listOf(
    "Final Year Project",
    "Artificial Intelligence",
    "Machine Learning",
    "Software Engineering",
    "Computer Networks & Security",
    "Database Administration",
    "Mobile Development",
    "Research Methods"
)

sealed class AuthResult {
    object Success : AuthResult()
    data class Error(val message: String) : AuthResult()
    object Loading : AuthResult()
}

@Singleton
class AuthRepository @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    // Current auth state
    private val _currentUser = MutableStateFlow<FirebaseUser?>(auth.currentUser)
    val currentUser: StateFlow<FirebaseUser?> = _currentUser

    val isLoggedIn get() = auth.currentUser != null

    init {
        // Listen for auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            _currentUser.value = firebaseAuth.currentUser
        }
    }

    // ── Email & Password Login
    suspend fun signIn(email: String, password: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e.message))
        }
    }

    // ── Email & Password Register
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String,
        year: Int,
        programme: String
    ): AuthResult {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("User creation failed")

            // Save profile to Firestore
            val user = StudyPulseUser(
                uid = uid,
                firstName = firstName,
                lastName = lastName,
                email = email,
                year = year,
                programme = programme,
                courses = getCoursesFor(year, programme)
            )
            firestore.collection("users").document(uid).set(user).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e.message))
        }
    }

    // ── Google Sign-In
    suspend fun signInWithGoogle(idToken: String): AuthResult {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val user = result.user ?: throw Exception("Google sign-in failed")

            // Create profile if new user
            if (result.additionalUserInfo?.isNewUser == true) {
                val profile = StudyPulseUser(
                    uid = user.uid,
                    firstName = user.displayName?.split(" ")?.firstOrNull() ?: "",
                    lastName = user.displayName?.split(" ")?.lastOrNull() ?: "",
                    email = user.email ?: ""
                )
                firestore.collection("users").document(user.uid)
                    .set(profile, SetOptions.merge()).await()
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e.message))
        }
    }

    // ── Password Reset
    suspend fun sendPasswordReset(email: String): AuthResult {
        return try {
            auth.sendPasswordResetEmail(email).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(friendlyError(e.message))
        }
    }

    // ── Get User Profile from Firestore
    suspend fun getUserProfile(uid: String): StudyPulseUser? {
        return try {
            firestore.collection("users").document(uid)
                .get().await()
                .toObject(StudyPulseUser::class.java)
        } catch (e: Exception) { null }
    }

    // ── Sign Out
    fun signOut() {
        auth.signOut()
    }

    // ── Courses by year & programme
    private fun getCoursesFor(year: Int, programme: String): List<String> {
        return when {
            year == 4 && programme == "Computer Science" -> defaultYear4Courses()
            year == 3 -> listOf("Operating Systems","Data Structures & Algorithms","Database Systems","Software Design","Web Development","Statistics","Technical Writing")
            year == 2 -> listOf("Object-Oriented Programming","Discrete Mathematics","Computer Architecture","Data Communication","Linear Algebra","Introduction to AI")
            year == 1 -> listOf("Introduction to Programming","Mathematics I","Mathematics II","Introduction to IT","Logic & Critical Thinking","Communication Skills")
            else -> defaultYear4Courses()
        }
    }

    // ── Human-friendly Firebase errors
    private fun friendlyError(msg: String?): String {
        return when {
            msg == null -> "Something went wrong. Please try again."
            "password" in msg.lowercase() -> "Incorrect password. Please try again."
            "email" in msg.lowercase() && "already" in msg.lowercase() -> "This email is already registered."
            "user not found" in msg.lowercase() -> "No account found with this email."
            "network" in msg.lowercase() -> "No internet connection. Please check your network."
            "too many requests" in msg.lowercase() -> "Too many attempts. Please wait a moment."
            "weak password" in msg.lowercase() -> "Password must be at least 6 characters."
            else -> msg
        }
    }
}
