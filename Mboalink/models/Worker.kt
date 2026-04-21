package com.mboalink.models

import java.util.Date

/**
 * MboaLink — Worker (Prestataire) Model
 * Represents a verified service worker on the platform
 */
data class Worker(
    val id: String,
    val name: String,
    val phone: String,
    val profession: String,
    val location: String,
    val bio: String,
    val skills: List<String>,
    val languages: List<String>,
    val photoUrl: String? = null,
    val nationalId: String,           // CNI — alphanumeric (letters + digits)
    val isVerified: Boolean = false,
    val isCertified: Boolean = false,
    val isInsured: Boolean = false,
    val rating: Double = 0.0,
    val reviewCount: Int = 0,
    val jobsCompleted: Int = 0,
    val hourlyRate: Int,              // in FCFA
    val status: WorkerStatus = WorkerStatus.AVAILABLE,
    val badge: WorkerBadge? = null,
    val responseTime: String = "< 1h",
    val joinedDate: Date = Date()
)

enum class WorkerStatus {
    AVAILABLE, BUSY, OFFLINE;

    fun displayLabel(lang: AppLanguage): String = when (this) {
        AVAILABLE -> when (lang) {
            AppLanguage.FR -> "Disponible"
            AppLanguage.EN -> "Available"
            AppLanguage.PID -> "Free"
        }
        BUSY -> when (lang) {
            AppLanguage.FR -> "Occupé"
            AppLanguage.EN -> "Busy"
            AppLanguage.PID -> "Busy"
        }
        OFFLINE -> when (lang) {
            AppLanguage.FR -> "Hors-ligne"
            AppLanguage.EN -> "Offline"
            AppLanguage.PID -> "No dey"
        }
    }
}

enum class WorkerBadge(val labelKey: String) {
    TOP_RATED("badge_top_rated"),
    MOST_RELIABLE("badge_most_reliable"),
    FAST_RESPONDER("badge_fast_responder");
}

enum class AppLanguage { FR, EN, PID }
