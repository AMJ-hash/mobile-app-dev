package com.mboalink.models

import java.util.Date

/**
 * MboaLink — Review & Rating Model
 */
data class Review(
    val id: String,
    val workerId: String,
    val authorName: String,
    val rating: Int,          // 1–5 stars
    val comment: String,
    val date: Date = Date()
)

/**
 * UserProfile — Client profile stored locally
 */
data class UserProfile(
    val id: String,
    val name: String,
    val phone: String,
    val neighbourhood: String,
    val email: String?,
    val photoPath: String? = null,
    val isPhoneVerified: Boolean = false,
    val preferredLanguage: AppLanguage = AppLanguage.FR
)
