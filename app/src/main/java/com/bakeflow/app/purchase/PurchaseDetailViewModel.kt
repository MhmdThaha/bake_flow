package com.bakeflow.app.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.PurchaseException
import com.bakeflow.app.domain.repository.PurchaseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseDetailViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseDetailUiState())
    val uiState: StateFlow<PurchaseDetailUiState> = _uiState.asStateFlow()

    init {
        loadPurchase()
    }

    private fun loadPurchase() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            purchaseRepository.getPurchase(purchaseId)
                .onSuccess { purchase ->
                    _uiState.update { it.copy(isLoading = false, purchase = purchase) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = (error as? PurchaseException)?.message
                                ?: error.message
                                ?: "Something went wrong. Please try again."
                        )
                    }
                }
        }
    }

    fun retryLoad() = loadPurchase()
}

class PurchaseDetailViewModelFactory(
    private val purchaseRepository: PurchaseRepository,
    private val purchaseId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseDetailViewModel::class.java)) {
            return PurchaseDetailViewModel(purchaseRepository, purchaseId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
