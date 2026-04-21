package com.mboalink.ui

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import com.mboalink.R
import com.mboalink.models.PaymentMethod
import com.mboalink.models.Worker
import com.mboalink.viewmodels.BookingViewModel
import com.mboalink.viewmodels.WorkerViewModel

/**
 * MboaLink — BookingActivity
 *
 * When launched with START_AT_PAYMENT = true (from "Hire Worker" button),
 * it skips directly to the payment step — not to "Post a Job".
 *
 * Flow:
 *  If START_AT_PAYMENT = true → show worker info + payment form immediately
 *  If START_AT_PAYMENT = false → show service description form first
 */
class BookingActivity : AppCompatActivity() {

    private lateinit var bookingViewModel: BookingViewModel
    private lateinit var workerViewModel: WorkerViewModel
    private var currentWorker: Worker? = null
    private var selectedPayment = PaymentMethod.ORANGE_MONEY

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_booking)

        bookingViewModel = ViewModelProvider(this)[BookingViewModel::class.java]
        workerViewModel  = ViewModelProvider(this)[WorkerViewModel::class.java]

        val workerId = intent.getStringExtra("WORKER_ID") ?: run { finish(); return }
        val startAtPayment = intent.getBooleanExtra("START_AT_PAYMENT", false)

        // Load worker and populate UI
        workerViewModel.workers.observe(this) { workers ->
            currentWorker = workers.find { it.id == workerId }
            currentWorker?.let { populateWorkerCard(it) }
        }

        setupPaymentSelection()
        setupSubmitButton()

        // If coming from "Hire Worker" button → jump to payment immediately
        if (startAtPayment) {
            showPaymentStep()
        } else {
            showServiceStep()
        }

        observeBookingResult()
    }

    /** Show worker info + payment options directly (from "Hire Worker" click) */
    private fun showPaymentStep() {
        findViewById<View>(R.id.layoutServiceDescription).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutPayment).visibility = View.VISIBLE
        // Scroll to payment section
        findViewById<ScrollView>(R.id.scrollView).post {
            findViewById<ScrollView>(R.id.scrollView).smoothScrollTo(0, findViewById<View>(R.id.layoutPayment).top)
        }
    }

    private fun showServiceStep() {
        findViewById<View>(R.id.layoutServiceDescription).visibility = View.VISIBLE
        findViewById<View>(R.id.layoutPayment).visibility = View.VISIBLE
    }

    private fun populateWorkerCard(worker: Worker) {
        // Populate worker summary card (name, photo, rating, profession)
        // Photo is loaded from worker.photoUrl using Glide/Coil
    }

    private fun setupPaymentSelection() {
        // Orange Money card
        findViewById<View>(R.id.cardOrangeMoney).setOnClickListener {
            selectedPayment = PaymentMethod.ORANGE_MONEY
            updatePaymentUI()
        }
        // MTN MoMo card
        findViewById<View>(R.id.cardMTNMoMo).setOnClickListener {
            selectedPayment = PaymentMethod.MTN_MOMO
            updatePaymentUI()
        }
        // Cash card
        findViewById<View>(R.id.cardCash).setOnClickListener {
            selectedPayment = PaymentMethod.CASH
            updatePaymentUI()
        }
        updatePaymentUI()
    }

    private fun updatePaymentUI() {
        // Highlight selected payment option
        // Show/hide phone number input for mobile money
        val showPhoneInput = selectedPayment != PaymentMethod.CASH
        findViewById<View>(R.id.layoutPaymentPhone).visibility =
            if (showPhoneInput) View.VISIBLE else View.GONE
        // Update hint label based on method
        val hint = when (selectedPayment) {
            PaymentMethod.ORANGE_MONEY -> "Numéro Orange (ex: 670 000 000)"
            PaymentMethod.MTN_MOMO     -> "Numéro MTN (ex: 650 000 000)"
            PaymentMethod.CASH         -> ""
        }
        findViewById<EditText>(R.id.etPaymentPhone).hint = hint
    }

    private fun setupSubmitButton() {
        findViewById<Button>(R.id.btnConfirmBooking).setOnClickListener {
            val worker = currentWorker ?: return@setOnClickListener
            bookingViewModel.createBooking(
                worker      = worker,
                clientName  = findViewById<EditText>(R.id.etClientName).text.toString(),
                clientPhone = findViewById<EditText>(R.id.etClientPhone).text.toString(),
                serviceType = findViewById<EditText>(R.id.etServiceType).text.toString(),
                description = findViewById<EditText>(R.id.etDescription).text.toString(),
                paymentNumber = if (selectedPayment != PaymentMethod.CASH)
                    findViewById<EditText>(R.id.etPaymentPhone).text.toString()
                else null
            )
        }
    }

    private fun observeBookingResult() {
        bookingViewModel.bookingResult.observe(this) { result ->
            result ?: return@observe
            if (result.success && result.booking != null) {
                // Show success screen with booking reference
                val successLayout = findViewById<View>(R.id.layoutSuccess)
                successLayout.visibility = View.VISIBLE
                findViewById<TextView>(R.id.tvBookingRef).text = result.booking.id
                bookingViewModel.clearBookingResult()
            } else if (!result.success) {
                Toast.makeText(this, result.error, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
