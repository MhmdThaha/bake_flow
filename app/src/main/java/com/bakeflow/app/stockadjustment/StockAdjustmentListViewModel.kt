package com.bakeflow.app.stockadjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.StockAdjustmentException
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.domain.model.StockAdjustmentSummary
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class StockAdjustmentListViewModel(
    private val stockAdjustmentRepository: StockAdjustmentRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAdjustmentListUiState())
    val uiState: StateFlow<StockAdjustmentListUiState> = _uiState.asStateFlow()

    private var allAdjustments: List<StockAdjustment> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            stockAdjustmentRepository.observeAdjustments()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            adjustmentsState = UiState.Error(
                                (error as? StockAdjustmentException)?.message
                                    ?: "Failed to load adjustments."
                            )
                        )
                    }
                }
                .collect { adjustments ->
                    allAdjustments = adjustments
                    val summary = computeSummary(adjustments)
                    _uiState.update { it.copy(summary = summary) }
                    applySearch(_uiState.value.searchQuery, adjustments)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allAdjustments)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(adjustmentsState = UiState.Loading) }
        startObserving()
    }

    private fun applySearch(query: String, adjustments: List<StockAdjustment>) {
        val filtered = if (query.isBlank()) {
            adjustments
        } else {
            val normalized = query.trim().lowercase()
            adjustments.filter {
                it.itemName.lowercase().contains(normalized) ||
                    it.itemType.displayName.lowercase().contains(normalized) ||
                    it.adjustmentReason.displayName.lowercase().contains(normalized) ||
                    it.notes.lowercase().contains(normalized)
            }
        }
        _uiState.update {
            it.copy(
                adjustmentsState = if (adjustments.isEmpty()) {
                    UiState.Empty
                } else {
                    UiState.Success(filtered)
                },
                filteredAdjustments = filtered
            )
        }
    }

    private fun computeSummary(adjustments: List<StockAdjustment>): StockAdjustmentSummary {
        val startOfDay = startOfTodayMillis()
        val today = adjustments.filter { it.adjustmentDate >= startOfDay }
        return StockAdjustmentSummary(
            todayCount = today.size,
            todayDifferenceTotal = today.sumOf { it.difference },
            recentAdjustments = adjustments.take(5)
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

class StockAdjustmentListViewModelFactory(
    private val stockAdjustmentRepository: StockAdjustmentRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAdjustmentListViewModel::class.java)) {
            return StockAdjustmentListViewModel(stockAdjustmentRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
