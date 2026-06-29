package com.bakeflow.app.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.data.repository.WasteException
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.model.WasteReason
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecordWasteRequest
import com.bakeflow.app.domain.repository.WasteRepository
import com.bakeflow.app.waste.validation.WasteValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WasteFormViewModel(
    private val wasteRepository: WasteRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedProductId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteFormUiState())
    val uiState: StateFlow<WasteFormUiState> = _uiState.asStateFlow()

    init {
        observeProducts()
    }

    private fun observeProducts() {
        viewModelScope.launch {
            productRepository.observeProducts()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to load products.")
                    }
                }
                .collect { products ->
                    val withStock = products.filter {
                        it.status == ProductStatus.ACTIVE && it.finishedStock > 0
                    }
                    val recentIds = preferences.getRecentProductIds()
                    val sorted = preferences.sortByRecent(withStock, { it.id }, recentIds)
                    val selectedId = _uiState.value.selectedProductId
                        ?: preselectedProductId
                        ?: sorted.firstOrNull()?.id
                    _uiState.update {
                        it.copy(
                            products = sorted,
                            selectedProductId = selectedId
                        )
                    }
                }
        }
    }

    fun onProductSelected(productId: String) {
        _uiState.update {
            it.copy(
                selectedProductId = productId,
                productError = null,
                quantityError = null
            )
        }
    }

    fun onQuantityChange(value: String) {
        _uiState.update { it.copy(quantityText = value, quantityError = null) }
    }

    fun onReasonChange(reason: WasteReason) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveWaste() {
        val state = _uiState.value
        val productError = WasteValidator.validateProduct(state.selectedProductId)
        val quantityError = WasteValidator.validateQuantity(state.quantityText, state.availableStock)
        if (productError != null || quantityError != null) {
            _uiState.update {
                it.copy(productError = productError, quantityError = quantityError)
            }
            return
        }

        val user = firebaseAuth.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to record waste.") }
            return
        }

        val quantity = state.quantityText.toDoubleOrNull()!!
        val createdBy = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: user.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            wasteRepository.recordWaste(
                RecordWasteRequest(
                    productId = state.selectedProductId!!,
                    quantity = quantity,
                    reason = state.reason,
                    notes = state.notes,
                    wasteDateMillis = System.currentTimeMillis(),
                    createdBy = createdBy
                )
            ).onSuccess {
                preferences.recordRecentProduct(state.selectedProductId!!)
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSucceeded = true,
                        quantityText = "1",
                        notes = "",
                        productError = null,
                        quantityError = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = (error as? WasteException)?.message
                            ?: error.message
                            ?: "Could not record waste."
                    )
                }
            }
        }
    }

    fun clearSaveSucceeded() {
        _uiState.update { it.copy(saveSucceeded = false) }
    }

    fun resetForm() {
        _uiState.update {
            WasteFormUiState(
                products = it.products,
                selectedProductId = it.products.firstOrNull()?.id,
                reason = WasteReason.DAMAGED
            )
        }
    }
}

class WasteFormViewModelFactory(
    private val wasteRepository: WasteRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedProductId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WasteFormViewModel::class.java)) {
            return WasteFormViewModel(
                wasteRepository,
                productRepository,
                firebaseAuth,
                preferences,
                preselectedProductId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
