package com.mboalink.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mboalink.models.AppLanguage
import com.mboalink.models.Worker
import com.mboalink.models.WorkerStatus
import com.mboalink.network.MboaLinkRepository
import kotlinx.coroutines.launch

/**
 * MboaLink — WorkerViewModel
 * Manages worker list state, search, filtering, and booking actions
 */
class WorkerViewModel(
    private val repository: MboaLinkRepository
) : ViewModel() {

    private val _workers = MutableLiveData<List<Worker>>(emptyList())
    val workers: LiveData<List<Worker>> = _workers

    private val _filteredWorkers = MutableLiveData<List<Worker>>(emptyList())
    val filteredWorkers: LiveData<List<Worker>> = _filteredWorkers

    private val _isLoading = MutableLiveData(false)
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>(null)
    val error: LiveData<String?> = _error

    private var currentQuery = ""
    private var currentProfession = ""
    private var currentStatus = ""

    fun loadWorkers() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val result = repository.getWorkers()
                _workers.value = result
                applyFilters()
            } catch (e: Exception) {
                _error.value = "Erreur de chargement: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun search(query: String) {
        currentQuery = query.lowercase()
        applyFilters()
    }

    fun filterByProfession(profession: String) {
        currentProfession = profession
        applyFilters()
    }

    fun filterByStatus(status: String) {
        currentStatus = status
        applyFilters()
    }

    private fun applyFilters() {
        val base = _workers.value ?: return
        _filteredWorkers.value = base.filter { w ->
            val matchQ = currentQuery.isEmpty() ||
                w.name.lowercase().contains(currentQuery) ||
                w.profession.lowercase().contains(currentQuery) ||
                w.location.lowercase().contains(currentQuery) ||
                w.skills.any { it.lowercase().contains(currentQuery) }
            val matchP = currentProfession.isEmpty() || w.profession == currentProfession
            val matchS = currentStatus.isEmpty() || w.status.name.lowercase() == currentStatus.lowercase()
            matchQ && matchP && matchS
        }
    }

    fun getAvailableWorkersByProfession(profession: String): List<Worker> {
        return _workers.value
            ?.filter { it.status == WorkerStatus.AVAILABLE && (profession.isEmpty() || it.profession == profession) }
            ?.sortedByDescending { it.rating }
            ?: emptyList()
    }

    fun addWorker(worker: Worker) {
        val current = _workers.value?.toMutableList() ?: mutableListOf()
        current.add(worker)
        _workers.value = current
        applyFilters()
        viewModelScope.launch {
            try { repository.saveWorker(worker) }
            catch (e: Exception) { _error.value = "Erreur sauvegarde: ${e.message}" }
        }
    }

    fun updateWorkerStatus(workerId: String, status: WorkerStatus) {
        val current = _workers.value?.toMutableList() ?: return
        val idx = current.indexOfFirst { it.id == workerId }
        if (idx >= 0) {
            current[idx] = current[idx].copy(status = status)
            _workers.value = current
            applyFilters()
        }
    }

    fun getAiRecommendation(query: String = ""): Worker? {
        return (_filteredWorkers.value ?: _workers.value)
            ?.filter { it.status == WorkerStatus.AVAILABLE }
            ?.maxByOrNull { it.rating * 0.6 + (it.jobsCompleted * 0.001) + (it.reviewCount * 0.01) }
    }

    fun getStats(): Map<String, Int> {
        val all = _workers.value ?: emptyList()
        return mapOf(
            "total" to all.size,
            "available" to all.count { it.status == WorkerStatus.AVAILABLE },
            "busy" to all.count { it.status == WorkerStatus.BUSY },
            "verified" to all.count { it.isVerified }
        )
    }
}
