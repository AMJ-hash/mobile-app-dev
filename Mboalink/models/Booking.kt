package com.mboalink.models

import java.util.Date

/**
 * MboaLink — Booking (Réservation) Model
 */
data class Booking(
    val id: String,
    val workerId: String,
    val workerName: String,
    val workerProfession: String,
    val clientName: String,
    val clientPhone: String,
    val serviceType: String,
    val description: String,
    val paymentMethod: PaymentMethod,
    val paymentNumber: String? = null,  // Orange/MTN number if applicable
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val createdAt: Date = Date(),
    val estimatedArrival: String = "15–30 min",
    val totalAmount: Int? = null        // in FCFA
)

enum class BookingStatus {
    PENDING, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED
}

enum class PaymentMethod {
    ORANGE_MONEY, MTN_MOMO, CASH;

    fun displayName(): String = when (this) {
        ORANGE_MONEY -> "Orange Money"
        MTN_MOMO -> "MTN MoMo"
        CASH -> "Espèces"
    }

    fun emoji(): String = when (this) {
        ORANGE_MONEY -> "🍊"
        MTN_MOMO -> "📱"
        CASH -> "💵"
    }
}
