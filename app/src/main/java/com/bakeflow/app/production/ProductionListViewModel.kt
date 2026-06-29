package com.bakeflow.app.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.ProductionException
import com.bakeflow.app.domain.repository.ProductionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductionListViewModel(
    private val productionRepository: ProductionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionListUiState())
    val uiState: StateFlow<ProductionListUiState> = _uiState.asStateFlow()

    init {
        observeStats()
    }

    private fun observeStats() {
        viewModelScope.launch {
            productionRepository.observeDashboardStats()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
                .collect { stats ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            stats = ProductionDashboardStatsUi(
                                todayBatchCount = stats.todayBatchCount,
                                todayCompleted = stats.todayCompleted,
                                todayPending = stats.todayPending
                            )
                        )
                    }
                }
        }
    }

    fun retryLoad() {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        observeStats()
    }

    private fun Throwable.toUserMessage(): String =
        (this as? ProductionException)?.message ?: message ?: "Something went wrong. Please try again."
}

class ProductionListViewModelFactory(
    private val productionRepository: ProductionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductionListViewModel::class.java)) {
            return ProductionListViewModel(productionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
