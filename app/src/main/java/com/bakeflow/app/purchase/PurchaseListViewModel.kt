package com.bakeflow.app.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.PurchaseException
import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.domain.repository.PurchaseRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseListViewModel(
    private val purchaseRepository: PurchaseRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseListUiState())
    val uiState: StateFlow<PurchaseListUiState> = _uiState.asStateFlow()

    private var allPurchases: List<Purchase> = emptyList()
    private var observeJob: Job? = null

    init {
        startObserving()
    }

    private fun startObserving() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            purchaseRepository.observePurchases()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            purchasesState = UiState.Error(
                                (error as? PurchaseException)?.message
                                    ?: "Failed to load purchases."
                            )
                        )
                    }
                }
                .collect { purchases ->
                    allPurchases = purchases
                    val ingredientFilters = purchases.map { it.ingredientName }
                        .distinct()
                        .sortedBy { it.lowercase() }
                    val supplierFilters = purchases.map { it.supplierName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .sortedBy { it.lowercase() }
                    _uiState.update {
                        it.copy(
                            availableIngredientFilters = ingredientFilters,
                            availableSupplierFilters = supplierFilters
                        )
                    }
                    applyFilters(_uiState.value)
                }
        }
        viewModelScope.launch {
            purchaseRepository.observePurchaseSummary()
                .catch { /* summary errors handled by purchases flow */ }
                .collect { summary ->
                    _uiState.update { it.copy(summary = summary) }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applyFilters(_uiState.value.copy(searchQuery = query))
    }

    fun onIngredientFilterChange(filter: String?) {
        _uiState.update { it.copy(ingredientFilter = filter) }
        applyFilters(_uiState.value.copy(ingredientFilter = filter))
    }

    fun onSupplierFilterChange(filter: String?) {
        _uiState.update { it.copy(supplierFilter = filter) }
        applyFilters(_uiState.value.copy(supplierFilter = filter))
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun showSnackbar(message: String) {
        _uiState.update { it.copy(snackbarMessage = message) }
    }

    fun retryLoad() {
        _uiState.update { it.copy(purchasesState = UiState.Loading) }
        startObserving()
    }

    private fun applyFilters(state: PurchaseListUiState) {
        var filtered = allPurchases
        if (state.ingredientFilter != null) {
            filtered = filtered.filter { it.ingredientName == state.ingredientFilter }
        }
        if (state.supplierFilter != null) {
            filtered = filtered.filter { it.supplierName == state.supplierFilter }
        }
        if (state.searchQuery.isNotBlank()) {
            val normalized = state.searchQuery.trim().lowercase()
            filtered = filtered.filter {
                it.ingredientName.lowercase().contains(normalized) ||
                    it.supplierName.lowercase().contains(normalized) ||
                    it.invoiceNumber.lowercase().contains(normalized)
            }
        }
        _uiState.update {
            it.copy(
                purchasesState = if (allPurchases.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredPurchases = filtered
            )
        }
    }
}

class PurchaseListViewModelFactory(
    private val purchaseRepository: PurchaseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseListViewModel::class.java)) {
            return PurchaseListViewModel(purchaseRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
