package com.mboalink.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mboalink.models.Booking
import com.mboalink.models.BookingStatus
import com.mboalink.models.PaymentMethod
import com.mboalink.models.Worker
import com.mboalink.network.MboaLinkRepository
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID

/**
 * MboaLink — BookingViewModel
 * Handles booking creation, payment selection, and booking history
 */
class BookingViewModel(
    private val repository: MboaLinkRepository
) : ViewModel() {

    private val _bookings = MutableLiveData<List<Booking>>(emptyList())
    val bookings: LiveData<List<Booking>> = _bookings

    // Currently selected payment method
    private val _selectedPayment = MutableLiveData(PaymentMethod.ORANGE_MONEY)
    val selectedPayment: LiveData<PaymentMethod> = _selectedPayment

    // Booking confirmation state
    private val _bookingResult = MutableLiveData<BookingResult?>(null)
    val bookingResult: LiveData<BookingResult?> = _bookingResult

    data class BookingResult(
        val success: Boolean,
        val booking: Booking? = null,
        val error: String? = null
    )

    fun selectPaymentMethod(method: PaymentMethod) {
        _selectedPayment.value = method
    }

    fun createBooking(
        worker: Worker,
        clientName: String,
        clientPhone: String,
        serviceType: String,
        description: String,
        paymentNumber: String? = null
    ) {
        if (clientName.isBlank() || clientPhone.isBlank() || serviceType.isBlank()) {
            _bookingResult.value = BookingResult(false, error = "Veuillez remplir tous les champs obligatoires")
            return
        }

        val booking = Booking(
            id = "BK" + UUID.randomUUID().toString().take(8).uppercase(),
            workerId = worker.id,
            workerName = worker.name,
            workerProfession = worker.profession,
            clientName = clientName,
            clientPhone = clientPhone,
            serviceType = serviceType,
            description = description,
            paymentMethod = _selectedPayment.value ?: PaymentMethod.CASH,
            paymentNumber = paymentNumber,
            status = BookingStatus.CONFIRMED,
            createdAt = Date(),
            estimatedArrival = "15–30 min",
            totalAmount = estimateAmount(worker, serviceType)
        )

        viewModelScope.launch {
            try {
                repository.saveBooking(booking)
                val current = _bookings.value?.toMutableList() ?: mutableListOf()
                current.add(0, booking)
                _bookings.value = current
                _bookingResult.value = BookingResult(true, booking = booking)
            } catch (e: Exception) {
                _bookingResult.value = BookingResult(false, error = e.message)
            }
        }
    }

    private fun estimateAmount(worker: Worker, serviceType: String): Int {
        // Basic estimate — 3 hours of work minimum
        return worker.hourlyRate * 3
    }

    fun loadBookings() {
        viewModelScope.launch {
            val result = repository.getBookings()
            _bookings.value = result
        }
    }

    fun clearBookingResult() {
        _bookingResult.value = null
    }
}
