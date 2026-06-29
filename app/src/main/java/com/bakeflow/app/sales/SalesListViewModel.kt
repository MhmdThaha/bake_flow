package com.bakeflow.app.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.SaleException
import com.bakeflow.app.domain.model.Sale
import com.bakeflow.app.domain.repository.SaleRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SalesListViewModel(
    private val saleRepository: SaleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SalesListUiState())
    val uiState: StateFlow<SalesListUiState> = _uiState.asStateFlow()

    private var allSales: List<Sale> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            saleRepository.observeSales()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            salesState = UiState.Error(
                                (error as? SaleException)?.message ?: "Failed to load sales."
                            )
                        )
                    }
                }
                .collect { sales ->
                    allSales = sales
                    applySearch(_uiState.value.searchQuery, sales)
                }
        }
        viewModelScope.launch {
            saleRepository.observeSalesSummary()
                .catch { /* handled by sales flow */ }
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allSales)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(salesState = UiState.Loading) }
        startObserving()
    }

    private fun applySearch(query: String, sales: List<Sale>) {
        val filtered = if (query.isBlank()) {
            sales
        } else {
            val normalized = query.trim().lowercase()
            sales.filter {
                it.productName.lowercase().contains(normalized) ||
                    it.customerName.lowercase().contains(normalized) ||
                    it.paymentMethod.displayName.lowercase().contains(normalized)
            }
        }
        _uiState.update {
            it.copy(
                salesState = if (sales.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredSales = filtered
            )
        }
    }
}

class SalesListViewModelFactory(
    private val saleRepository: SaleRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SalesListViewModel::class.java)) {
            return SalesListViewModel(saleRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
