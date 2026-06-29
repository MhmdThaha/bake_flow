package com.bakeflow.app.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.WasteException
import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteSummary
import com.bakeflow.app.domain.repository.WasteRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class WasteListViewModel(
    private val wasteRepository: WasteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteListUiState())
    val uiState: StateFlow<WasteListUiState> = _uiState.asStateFlow()

    private var allWaste: List<Waste> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            wasteRepository.observeWaste()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            wasteState = UiState.Error(
                                (error as? WasteException)?.message ?: "Failed to load waste history."
                            )
                        )
                    }
                }
                .collect { waste ->
                    allWaste = waste
                    val summary = computeSummary(waste)
                    _uiState.update { it.copy(summary = summary) }
                    applySearch(_uiState.value.searchQuery, waste)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allWaste)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(wasteState = UiState.Loading) }
        startObserving()
    }

    private fun applySearch(query: String, waste: List<Waste>) {
        val filtered = if (query.isBlank()) {
            waste
        } else {
            val normalized = query.trim().lowercase()
            waste.filter {
                it.productName.lowercase().contains(normalized) ||
                    it.reason.displayName.lowercase().contains(normalized) ||
                    it.notes.lowercase().contains(normalized)
            }
        }
        _uiState.update {
            it.copy(
                wasteState = if (waste.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredWaste = filtered
            )
        }
    }

    private fun computeSummary(waste: List<Waste>): WasteSummary {
        val startOfDay = startOfTodayMillis()
        val todayWaste = waste.filter { it.wasteDate >= startOfDay }
        return WasteSummary(
            todayCount = todayWaste.size,
            todayQuantity = todayWaste.sumOf { it.quantity },
            todayEstimatedLoss = todayWaste.sumOf { it.estimatedLoss },
            recentWaste = waste.take(5)
        )
    }

    private fun startOfTodayMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}

class WasteListViewModelFactory(
    private val wasteRepository: WasteRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WasteListViewModel::class.java)) {
            return WasteListViewModel(wasteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
