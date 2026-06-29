package com.bakeflow.app.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.ProductionException
import com.bakeflow.app.domain.repository.ProductionRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductionHistoryViewModel(
    private val productionRepository: ProductionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionHistoryUiState())
    val uiState: StateFlow<ProductionHistoryUiState> = _uiState.asStateFlow()

    private var allBatches: List<com.bakeflow.app.domain.model.ProductionBatch> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            productionRepository.observeBatches()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            batchesState = UiState.Error(
                                (error as? ProductionException)?.message
                                    ?: "Failed to load production history."
                            )
                        )
                    }
                }
                .collect { batches ->
                    allBatches = batches
                    applySearch(_uiState.value.searchQuery, batches)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allBatches)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(batchesState = UiState.Loading) }
        startObserving()
    }

    private fun applySearch(query: String, batches: List<com.bakeflow.app.domain.model.ProductionBatch>) {
        val filtered = if (query.isBlank()) {
            batches
        } else {
            val normalized = query.trim().lowercase()
            batches.filter {
                it.productName.lowercase().contains(normalized) ||
                    it.status.displayName.lowercase().contains(normalized)
            }
        }
        _uiState.update {
            it.copy(
                batchesState = if (batches.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredBatches = filtered
            )
        }
    }
}

class ProductionHistoryViewModelFactory(
    private val productionRepository: ProductionRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductionHistoryViewModel::class.java)) {
            return ProductionHistoryViewModel(productionRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
