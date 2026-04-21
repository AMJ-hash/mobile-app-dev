package com.mboalink.ui

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mboalink.R
import com.mboalink.models.Worker
import com.mboalink.models.WorkerStatus
import com.mboalink.viewmodels.BookingViewModel
import com.mboalink.viewmodels.WorkerViewModel

/**
 * MboaLink — MainActivity
 * Entry point. Hosts the worker list, search, navigation.
 */
class MainActivity : AppCompatActivity() {

    private lateinit var workerViewModel: WorkerViewModel
    private lateinit var bookingViewModel: BookingViewModel
    private lateinit var adapter: WorkerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        workerViewModel = ViewModelProvider(this)[WorkerViewModel::class.java]
        bookingViewModel = ViewModelProvider(this)[BookingViewModel::class.java]

        setupRecyclerView()
        setupObservers()
        setupNavigation()

        workerViewModel.loadWorkers()
    }

    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvWorkers)
        rv.layoutManager = GridLayoutManager(this, 2)
        adapter = WorkerAdapter(
            onWorkerClick = { worker -> openWorkerDetail(worker) },
            onHireClick   = { worker -> openBookingDirectly(worker) }  // Goes straight to payment
        )
        rv.adapter = adapter
    }

    private fun setupObservers() {
        workerViewModel.filteredWorkers.observe(this) { workers ->
            adapter.submitList(workers)
            updateStatsDisplay(workers)
        }
        workerViewModel.isLoading.observe(this) { loading ->
            findViewById<View>(R.id.progressBar).visibility = if (loading) View.VISIBLE else View.GONE
        }
    }

    /**
     * "Hire Worker" button → opens BookingActivity directly at payment step.
     * Does NOT go to "Post a Job" screen.
     */
    private fun openBookingDirectly(worker: Worker) {
        if (worker.status != WorkerStatus.AVAILABLE) {
            val msg = when (worker.status) {
                WorkerStatus.BUSY -> "Ce prestataire est actuellement occupé"
                WorkerStatus.OFFLINE -> "Ce prestataire est hors-ligne"
                else -> "Non disponible"
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
            return
        }
        val intent = Intent(this, BookingActivity::class.java).apply {
            putExtra("WORKER_ID", worker.id)
            putExtra("START_AT_PAYMENT", true)  // Skip directly to payment step
        }
        startActivity(intent)
    }

    private fun openWorkerDetail(worker: Worker) {
        val intent = Intent(this, WorkerDetailActivity::class.java).apply {
            putExtra("WORKER_ID", worker.id)
        }
        startActivity(intent)
    }

    private fun updateStatsDisplay(workers: List<Worker>) {
        // Update stat counters in the UI
    }

    private fun setupNavigation() {
        // Bottom navigation / sidebar setup
    }
}
