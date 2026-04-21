package com.mboalink.utils

import kotlin.random.Random

/**
 * MboaLink — Phone Verification Service
 *
 * Production: Integrates with Twilio / Africa's Talking SMS API
 * Demo mode: Generates a 6-digit code and displays it on screen
 *
 * To use real SMS (Twilio):
 *   1. Add dependency: implementation("com.twilio.sdk:twilio:9.x.x")
 *   2. Set TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN, TWILIO_FROM_NUMBER in env
 *   3. Uncomment the sendRealSms() method below
 */
object PhoneVerificationService {

    private val pendingCodes = mutableMapOf<String, VerificationEntry>()
    private const val CODE_EXPIRY_MS = 5 * 60 * 1000L // 5 minutes

    data class VerificationEntry(
        val code: String,
        val phone: String,
        val createdAt: Long = System.currentTimeMillis(),
        var attempts: Int = 0
    )

    data class VerificationResult(
        val success: Boolean,
        val message: String,
        val demoCode: String? = null   // Only set in demo mode, never in production
    )

    /**
     * Send a 6-digit verification code to the given phone number.
     * In demo mode: returns the code in the result for display.
     * In production: sends a real SMS and returns success/failure only.
     */
    fun sendCode(phone: String, demoMode: Boolean = true): VerificationResult {
        if (!isValidCameroonPhone(phone)) {
            return VerificationResult(false, "Numéro invalide. Format: +237 6XX XXX XXX")
        }

        val code = generateCode()
        pendingCodes[phone] = VerificationEntry(code = code, phone = phone)

        return if (demoMode) {
            // DEMO: show the code (remove in production)
            VerificationResult(
                success = true,
                message = "Code envoyé (mode démo)",
                demoCode = code
            )
        } else {
            // PRODUCTION: send real SMS
            sendRealSms(phone, code)
        }
    }

    /**
     * Verify the code entered by the user
     */
    fun verifyCode(phone: String, enteredCode: String): VerificationResult {
        val entry = pendingCodes[phone]
            ?: return VerificationResult(false, "Aucun code envoyé pour ce numéro")

        if (System.currentTimeMillis() - entry.createdAt > CODE_EXPIRY_MS) {
            pendingCodes.remove(phone)
            return VerificationResult(false, "Code expiré. Demandez un nouveau code.")
        }

        entry.attempts++
        if (entry.attempts > 5) {
            pendingCodes.remove(phone)
            return VerificationResult(false, "Trop de tentatives. Recommencez.")
        }

        return if (enteredCode.trim() == entry.code) {
            pendingCodes.remove(phone)
            VerificationResult(true, "Numéro vérifié avec succès ✅")
        } else {
            VerificationResult(false, "Code incorrect. ${5 - entry.attempts} tentatives restantes.")
        }
    }

    private fun generateCode(): String = Random.nextInt(100000, 999999).toString()

    fun isValidCameroonPhone(phone: String): Boolean {
        // Accepts: +237 6XXXXXXXX, 237 6XXXXXXXX, 06XXXXXXXX, 6XXXXXXXX
        val cleaned = phone.replace(Regex("[\\s\\-()]"), "")
        return cleaned.matches(Regex("^(\\+?237)?[26][0-9]{8}$"))
    }

    /**
     * CNI (Carte Nationale d'Identité) validation
     * Cameroonian CNI: alphanumeric, typically 9–15 chars
     * Format examples: 123456789, AB1234567, 12AB56789
     */
    fun isValidCNI(cni: String): Boolean {
        val cleaned = cni.trim().uppercase()
        // Alphanumeric, 6–15 characters, must contain at least one digit
        return cleaned.length in 6..15 &&
            cleaned.matches(Regex("^[A-Z0-9]+$")) &&
            cleaned.any { it.isDigit() }
    }

    // ── PRODUCTION SMS (Twilio) ─────────────────────────────────────────────
    // Uncomment and configure to send real SMS:
    //
    // private fun sendRealSms(phone: String, code: String): VerificationResult {
    //     return try {
    //         val accountSid = System.getenv("TWILIO_ACCOUNT_SID")
    //         val authToken  = System.getenv("TWILIO_AUTH_TOKEN")
    //         val fromNumber = System.getenv("TWILIO_FROM_NUMBER")
    //         Twilio.init(accountSid, authToken)
    //         Message.creator(
    //             PhoneNumber(phone),
    //             PhoneNumber(fromNumber),
    //             "MboaLink: votre code de vérification est $code. Valable 5 minutes."
    //         ).create()
    //         VerificationResult(true, "SMS envoyé à $phone")
    //     } catch (e: Exception) {
    //         VerificationResult(false, "Erreur SMS: ${e.message}")
    //     }
    // }

    private fun sendRealSms(phone: String, code: String): VerificationResult {
        // Placeholder until Twilio credentials are configured
        return VerificationResult(true, "SMS envoyé à $phone (configurer Twilio pour production)")
    }
}
