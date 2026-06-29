package com.bakeflow.app.stockadjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.StockAdjustmentException
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StockAdjustmentDetailViewModel(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val adjustmentId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAdjustmentDetailUiState())
    val uiState: StateFlow<StockAdjustmentDetailUiState> = _uiState.asStateFlow()

    init {
        loadAdjustment()
    }

    private fun loadAdjustment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            stockAdjustmentRepository.getAdjustment(adjustmentId)
                .onSuccess { adjustment ->
                    _uiState.update { it.copy(isLoading = false, adjustment = adjustment) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = (error as? StockAdjustmentException)?.message
                                ?: error.message
                                ?: "Something went wrong. Please try again."
                        )
                    }
                }
        }
    }

    fun retryLoad() = loadAdjustment()
}

class StockAdjustmentDetailViewModelFactory(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val adjustmentId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAdjustmentDetailViewModel::class.java)) {
            return StockAdjustmentDetailViewModel(stockAdjustmentRepository, adjustmentId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
