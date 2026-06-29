package com.bakeflow.app.purchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.data.repository.PurchaseException
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.PurchaseRepository
import com.bakeflow.app.domain.repository.RecordPurchaseRequest
import com.bakeflow.app.purchase.validation.PurchaseValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PurchaseFormViewModel(
    private val purchaseRepository: PurchaseRepository,
    private val ingredientRepository: IngredientRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedIngredientId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(PurchaseFormUiState())
    val uiState: StateFlow<PurchaseFormUiState> = _uiState.asStateFlow()

    init {
        _uiState.update {
            it.copy(
                supplierName = preferences.getLastSupplier(),
                costPerUnitText = preferences.getLastPurchaseCost().ifBlank { it.costPerUnitText },
                recentSuppliers = preferences.getRecentSuppliers()
            )
        }
        observeIngredients()
    }

    private fun observeIngredients() {
        viewModelScope.launch {
            ingredientRepository.observeIngredients()
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to load ingredients.")
                    }
                }
                .collect { ingredients ->
                    val recentIds = preferences.getRecentIngredientIds()
                    val sorted = preferences.sortByRecent(
                        ingredients.sortedBy { it.name.lowercase() },
                        { it.id },
                        recentIds
                    )
                    val selectedId = _uiState.value.selectedIngredientId
                        ?: preselectedIngredientId
                        ?: sorted.firstOrNull()?.id
                    val selected = sorted.find { it.id == selectedId }
                    _uiState.update {
                        it.copy(
                            ingredients = sorted,
                            selectedIngredientId = selected?.id,
                            unit = selected?.unit?.displayName.orEmpty(),
                            costPerUnitText = when {
                                it.costPerUnitText.isNotBlank() -> it.costPerUnitText
                                preferences.getLastPurchaseCost().isNotBlank() -> preferences.getLastPurchaseCost()
                                selected != null -> formatCost(selected.costPerUnit)
                                else -> ""
                            },
                            recentSuppliers = preferences.getRecentSuppliers()
                        )
                    }
                }
        }
    }

    fun onIngredientSelected(ingredientId: String) {
        val ingredient = _uiState.value.ingredients.find { it.id == ingredientId }
        _uiState.update {
            it.copy(
                selectedIngredientId = ingredientId,
                ingredientError = null,
                unit = ingredient?.unit?.displayName.orEmpty(),
                costPerUnitText = ingredient?.let { ing -> formatCost(ing.costPerUnit) }
                    ?: it.costPerUnitText
            )
        }
    }

    fun onQuantityChange(value: String) {
        _uiState.update { it.copy(quantityText = value, quantityError = null) }
    }

    fun onCostPerUnitChange(value: String) {
        _uiState.update { it.copy(costPerUnitText = value, costError = null) }
    }

    fun onSupplierChange(value: String) {
        _uiState.update { it.copy(supplierName = value) }
    }

    fun onInvoiceChange(value: String) {
        _uiState.update { it.copy(invoiceNumber = value) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun savePurchase() {
        val state = _uiState.value
        val ingredientError = PurchaseValidator.validateIngredient(state.selectedIngredientId)
        val quantityError = PurchaseValidator.validateQuantity(state.quantityText)
        val costError = PurchaseValidator.validateCostPerUnit(state.costPerUnitText)
        if (ingredientError != null || quantityError != null || costError != null) {
            _uiState.update {
                it.copy(
                    ingredientError = ingredientError,
                    quantityError = quantityError,
                    costError = costError
                )
            }
            return
        }

        val user = firebaseAuth.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save a purchase.") }
            return
        }

        val quantity = state.quantityText.toDoubleOrNull()!!
        val costPerUnit = state.costPerUnitText.toDoubleOrNull()!!
        val createdBy = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: user.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            purchaseRepository.recordPurchase(
                RecordPurchaseRequest(
                    ingredientId = state.selectedIngredientId!!,
                    quantity = quantity,
                    costPerUnit = costPerUnit,
                    supplierName = state.supplierName,
                    invoiceNumber = state.invoiceNumber,
                    notes = state.notes,
                    purchaseDateMillis = System.currentTimeMillis(),
                    createdBy = createdBy
                )
            ).onSuccess {
                preferences.setLastSupplier(state.supplierName)
                preferences.setLastPurchaseCost(state.costPerUnitText)
                state.selectedIngredientId?.let { preferences.recordRecentIngredient(it) }
                if (state.supplierName.isNotBlank()) {
                    preferences.recordRecentSupplier(state.supplierName)
                }
                _uiState.update { it.copy(isSaving = false, saveSucceeded = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = (error as? PurchaseException)?.message
                            ?: error.message
                            ?: "Could not save purchase."
                    )
                }
            }
        }
    }

    fun clearSaveSucceeded() {
        _uiState.update { it.copy(saveSucceeded = false) }
    }

    private fun formatCost(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)
}

class PurchaseFormViewModelFactory(
    private val purchaseRepository: PurchaseRepository,
    private val ingredientRepository: IngredientRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedIngredientId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PurchaseFormViewModel::class.java)) {
            return PurchaseFormViewModel(
                purchaseRepository,
                ingredientRepository,
                firebaseAuth,
                preferences,
                preselectedIngredientId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
