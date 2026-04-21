package com.mboalink.ui

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mboalink.R
import com.mboalink.models.AppLanguage
import com.mboalink.models.Worker
import com.mboalink.models.WorkerStatus
import com.mboalink.utils.PhoneVerificationService
import com.mboalink.viewmodels.WorkerViewModel
import java.util.UUID

/**
 * MboaLink — Worker Registration Activity
 *
 * 3-step wizard:
 *  Step 0: Identity (name, phone, CNI — alphanumeric, location, photo)
 *  Step 1: Profession (trade, bio, skills, rate)
 *  Step 2: Phone verification + Confirmation
 */
class WorkerRegistrationActivity : AppCompatActivity() {

    private lateinit var viewModel: WorkerViewModel
    private var currentStep = 0
    private var selectedPhotoUri: Uri? = null
    private var verificationCode: String? = null
    private val selectedSkills = mutableSetOf<String>()

    // ── Photo picker ────────────────────────────────────────────────────────
    private val photoPicker = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            selectedPhotoUri = it
            // Display immediately in the ImageView
            val profileImageView = findViewById<ImageView>(R.id.ivWorkerPhoto)
            profileImageView.setImageURI(it)
            profileImageView.visibility = View.VISIBLE
            // Hide the placeholder emoji
            findViewById<TextView>(R.id.tvPhotoPlaceholder).visibility = View.GONE
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_worker_registration)
        viewModel = ViewModelProvider(this)[WorkerViewModel::class.java]
        setupClickListeners()
        showStep(0)
    }

    private fun setupClickListeners() {
        // Photo selection — shows image immediately after pick
        findViewById<View>(R.id.photoPickerArea).setOnClickListener {
            photoPicker.launch("image/*")
        }

        // Skills chips
        val skillIds = listOf(
            R.id.chipCablage, R.id.chipPlomberie, R.id.chipPeinture,
            R.id.chipMenuiserie, R.id.chipNettoyage, R.id.chipCarrelage,
            R.id.chipClimatisation, R.id.chipConstruction
        )
        skillIds.forEach { chipId ->
            findViewById<Chip>(chipId)?.setOnCheckedChangeListener { chip, isChecked ->
                if (isChecked) selectedSkills.add(chip.text.toString())
                else selectedSkills.remove(chip.text.toString())
            }
        }

        // Navigation buttons
        findViewById<Button>(R.id.btnNext).setOnClickListener { onNextClicked() }
        findViewById<Button>(R.id.btnBack).setOnClickListener { onBackClicked() }

        // Send verification code
        findViewById<Button>(R.id.btnSendCode).setOnClickListener {
            val phone = findViewById<EditText>(R.id.etPhone).text.toString()
            sendVerificationCode(phone)
        }

        // Final submit
        findViewById<Button>(R.id.btnSubmit).setOnClickListener { submitRegistration() }
    }

    private fun onNextClicked() {
        if (validateCurrentStep()) showStep(currentStep + 1)
    }

    private fun onBackClicked() {
        if (currentStep > 0) showStep(currentStep - 1)
        else finish()
    }

    private fun showStep(step: Int) {
        currentStep = step
        // Hide all step panels, show the current one
        listOf(R.id.stepPanel0, R.id.stepPanel1, R.id.stepPanel2).forEachIndexed { i, panelId ->
            findViewById<View>(panelId).visibility = if (i == step) View.VISIBLE else View.GONE
        }
        // Update progress dots
        updateProgressDots()
        // Update buttons
        findViewById<Button>(R.id.btnNext).visibility = if (step < 2) View.VISIBLE else View.GONE
        findViewById<Button>(R.id.btnSubmit).visibility = if (step == 2) View.VISIBLE else View.GONE
    }

    private fun updateProgressDots() {
        listOf(R.id.dot0, R.id.dot1, R.id.dot2).forEachIndexed { i, dotId ->
            val dot = findViewById<View>(dotId)
            dot.setBackgroundResource(
                if (i <= currentStep) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun validateCurrentStep(): Boolean {
        return when (currentStep) {
            0 -> {
                val name = findViewById<EditText>(R.id.etName).text.toString().trim()
                val phone = findViewById<EditText>(R.id.etPhone).text.toString().trim()
                val cni = findViewById<EditText>(R.id.etCNI).text.toString().trim()
                val location = findViewById<EditText>(R.id.etLocation).text.toString().trim()

                when {
                    name.isEmpty() -> { showError("Le nom est obligatoire"); false }
                    !PhoneVerificationService.isValidCameroonPhone(phone) -> {
                        showError("Numéro invalide. Format: +237 6XX XXX XXX"); false
                    }
                    !PhoneVerificationService.isValidCNI(cni) -> {
                        // CNI is alphanumeric — letters AND digits are accepted
                        showError("CNI invalide. Ex: AB123456789 (lettres et chiffres acceptés)"); false
                    }
                    location.isEmpty() -> { showError("Le quartier est obligatoire"); false }
                    else -> true
                }
            }
            1 -> {
                val profession = findViewById<Spinner>(R.id.spinnerProfession).selectedItem?.toString() ?: ""
                val bio = findViewById<EditText>(R.id.etBio).text.toString().trim()
                when {
                    profession.isEmpty() || profession == "— Sélectionner —" -> {
                        showError("Choisissez votre métier"); false
                    }
                    bio.isEmpty() -> { showError("La description est obligatoire"); false }
                    else -> true
                }
            }
            else -> true
        }
    }

    // ── Phone verification ───────────────────────────────────────────────────
    private fun sendVerificationCode(phone: String) {
        if (!PhoneVerificationService.isValidCameroonPhone(phone)) {
            showError("Numéro invalide"); return
        }
        val result = PhoneVerificationService.sendCode(phone, demoMode = true)
        verificationCode = result.demoCode

        if (result.demoCode != null) {
            // Show the demo code on screen (remove in production)
            val demoCodeView = findViewById<TextView>(R.id.tvDemoCode)
            demoCodeView.text = "Code de vérification (démo): ${result.demoCode}"
            demoCodeView.visibility = View.VISIBLE
        }
        Toast.makeText(this, result.message, Toast.LENGTH_SHORT).show()
    }

    // ── Submit final registration ────────────────────────────────────────────
    private fun submitRegistration() {
        // Verify phone code first
        val phone = findViewById<EditText>(R.id.etPhone).text.toString()
        val enteredCode = findViewById<EditText>(R.id.etVerificationCode).text.toString()
        val agreedToTerms = findViewById<CheckBox>(R.id.cbTerms).isChecked

        if (verificationCode == null) { showError("Envoyez d'abord le code de vérification"); return }
        if (!agreedToTerms) { showError("Acceptez les conditions d'utilisation"); return }

        val verifyResult = PhoneVerificationService.verifyCode(phone, enteredCode)
        if (!verifyResult.success) { showError(verifyResult.message); return }

        val professionPROF_EMOJIS = mapOf(
            "Electricien" to "⚡", "Plombier" to "🔧", "Menuisier" to "🪚",
            "Aide-Ménagère" to "🧹", "Peintre" to "🎨", "Technicien Clim" to "❄️",
            "Maçon" to "🧱", "Mécanicien" to "🔩"
        )
        val profession = findViewById<Spinner>(R.id.spinnerProfession).selectedItem.toString()

        val worker = Worker(
            id = "w_" + UUID.randomUUID().toString().take(8),
            name = findViewById<EditText>(R.id.etName).text.toString().trim(),
            phone = phone,
            profession = profession,
            location = "${findViewById<EditText>(R.id.etLocation).text.toString().trim()}, Yaoundé",
            bio = findViewById<EditText>(R.id.etBio).text.toString().trim(),
            skills = selectedSkills.toList(),
            languages = getSelectedLanguages(),
            photoUrl = selectedPhotoUri?.toString(),
            nationalId = findViewById<EditText>(R.id.etCNI).text.toString().trim().uppercase(),
            isVerified = true,  // Phone verified ✅
            rating = 0.0, reviewCount = 0, jobsCompleted = 0,
            hourlyRate = findViewById<EditText>(R.id.etRate).text.toString().toIntOrNull() ?: 2000,
            status = WorkerStatus.AVAILABLE
        )

        viewModel.addWorker(worker)
        Toast.makeText(this, "Profil créé avec succès! ✅", Toast.LENGTH_LONG).show()
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun getSelectedLanguages(): List<String> {
        val langs = mutableListOf<String>()
        if (findViewById<CheckBox>(R.id.cbLangFR).isChecked) langs.add("Français")
        if (findViewById<CheckBox>(R.id.cbLangEN).isChecked) langs.add("English")
        if (findViewById<CheckBox>(R.id.cbLangPID).isChecked) langs.add("Pidgin")
        return langs
    }

    private fun showError(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
