package com.mboalink.network

import com.mboalink.models.Booking
import com.mboalink.models.Review
import com.mboalink.models.Worker

/**
 * MboaLink — Repository
 * Abstracts local JSON storage and optional remote API calls.
 * In production, replace the stub implementations with Retrofit/OkHttp calls.
 */
interface MboaLinkRepository {
    suspend fun getWorkers(): List<Worker>
    suspend fun saveWorker(worker: Worker): Boolean
    suspend fun getBookings(): List<Booking>
    suspend fun saveBooking(booking: Booking): Boolean
    suspend fun getReviews(): List<Review>
    suspend fun saveReview(review: Review): Boolean
}

/**
 * Local implementation — reads/writes JSON files via Python backend bridge
 */
class LocalRepository : MboaLinkRepository {
    override suspend fun getWorkers(): List<Worker> = emptyList()   // Bridge fills this
    override suspend fun saveWorker(worker: Worker) = true
    override suspend fun getBookings(): List<Booking> = emptyList()
    override suspend fun saveBooking(booking: Booking) = true
    override suspend fun getReviews(): List<Review> = emptyList()
    override suspend fun saveReview(review: Review) = true
}

/**
 * API endpoints — for future REST backend
 */
object ApiEndpoints {
    const val BASE_URL = "https://api.mboalink.cm/v1/"
    const val WORKERS  = "workers"
    const val BOOKINGS = "bookings"
    const val REVIEWS  = "reviews"
    const val VERIFY   = "auth/verify-phone"
    const val REGISTER = "auth/register"
}
