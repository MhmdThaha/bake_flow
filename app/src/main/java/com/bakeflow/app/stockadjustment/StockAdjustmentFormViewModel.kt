package com.bakeflow.app.stockadjustment

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.StockAdjustmentException
import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.AdjustmentReason
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecordStockAdjustmentRequest
import com.bakeflow.app.domain.repository.StockAdjustmentRepository
import com.bakeflow.app.stockadjustment.validation.StockAdjustmentValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class StockAdjustmentFormViewModel(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val ingredientRepository: IngredientRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockAdjustmentFormUiState())
    val uiState: StateFlow<StockAdjustmentFormUiState> = _uiState.asStateFlow()

    init {
        observeItems()
    }

    private fun observeItems() {
        viewModelScope.launch {
            combine(
                ingredientRepository.observeIngredients(),
                productRepository.observeProducts()
            ) { ingredients, products ->
                val activeIngredients = ingredients.filter { it.status == IngredientStatus.ACTIVE }
                val activeProducts = products.filter { it.status == ProductStatus.ACTIVE }
                Pair(activeIngredients, activeProducts)
            }
                .catch { error ->
                    _uiState.update {
                        it.copy(errorMessage = error.message ?: "Failed to load items.")
                    }
                }
                .collect { (ingredients, products) ->
                    val state = _uiState.value
                    val selectedId = when (state.itemType) {
                        AdjustmentItemType.INGREDIENT -> {
                            state.selectedItemId?.takeIf { id -> ingredients.any { it.id == id } }
                                ?: ingredients.firstOrNull()?.id
                        }
                        AdjustmentItemType.PRODUCT -> {
                            state.selectedItemId?.takeIf { id -> products.any { it.id == id } }
                                ?: products.firstOrNull()?.id
                        }
                    }
                    val currentStock = when (state.itemType) {
                        AdjustmentItemType.INGREDIENT ->
                            ingredients.find { it.id == selectedId }?.currentStock
                        AdjustmentItemType.PRODUCT ->
                            products.find { it.id == selectedId }?.finishedStock
                    }
                    _uiState.update {
                        it.copy(
                            ingredients = ingredients,
                            products = products,
                            selectedItemId = selectedId,
                            actualStockText = if (it.actualStockText.isBlank() && currentStock != null) {
                                formatQty(currentStock)
                            } else {
                                it.actualStockText
                            }
                        )
                    }
                }
        }
    }

    fun onItemTypeChange(itemType: AdjustmentItemType) {
        val state = _uiState.value
        val selectedId = when (itemType) {
            AdjustmentItemType.INGREDIENT -> state.ingredients.firstOrNull()?.id
            AdjustmentItemType.PRODUCT -> state.products.firstOrNull()?.id
        }
        val stock = when (itemType) {
            AdjustmentItemType.INGREDIENT ->
                state.ingredients.find { it.id == selectedId }?.currentStock
            AdjustmentItemType.PRODUCT ->
                state.products.find { it.id == selectedId }?.finishedStock
        }
        _uiState.update {
            it.copy(
                itemType = itemType,
                selectedItemId = selectedId,
                actualStockText = stock?.let { qty -> formatQty(qty) } ?: "",
                itemError = null,
                quantityError = null
            )
        }
    }

    fun onItemSelected(itemId: String) {
        val state = _uiState.value
        val stock = when (state.itemType) {
            AdjustmentItemType.INGREDIENT ->
                state.ingredients.find { it.id == itemId }?.currentStock
            AdjustmentItemType.PRODUCT ->
                state.products.find { it.id == itemId }?.finishedStock
        }
        _uiState.update {
            it.copy(
                selectedItemId = itemId,
                actualStockText = stock?.let { qty -> formatQty(qty) } ?: it.actualStockText,
                itemError = null,
                quantityError = null
            )
        }
    }

    fun onActualStockChange(value: String) {
        _uiState.update { it.copy(actualStockText = value, quantityError = null) }
    }

    fun onReasonChange(reason: AdjustmentReason) {
        _uiState.update { it.copy(reason = reason) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveAdjustment() {
        val state = _uiState.value
        val itemError = StockAdjustmentValidator.validateItem(state.selectedItemId)
        val quantityError = StockAdjustmentValidator.validateActualStock(state.actualStockText)
        if (itemError != null || quantityError != null) {
            _uiState.update {
                it.copy(itemError = itemError, quantityError = quantityError)
            }
            return
        }

        val user = firebaseAuth.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save an adjustment.") }
            return
        }

        val adjustedQuantity = state.actualStockText.toDoubleOrNull()!!
        val createdBy = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: user.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            stockAdjustmentRepository.recordAdjustment(
                RecordStockAdjustmentRequest(
                    itemType = state.itemType,
                    itemId = state.selectedItemId!!,
                    adjustedQuantity = adjustedQuantity,
                    reason = state.reason,
                    notes = state.notes,
                    adjustmentDateMillis = System.currentTimeMillis(),
                    createdBy = createdBy
                )
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSucceeded = true,
                        notes = "",
                        itemError = null,
                        quantityError = null
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = (error as? StockAdjustmentException)?.message
                            ?: error.message
                            ?: "Could not save adjustment."
                    )
                }
            }
        }
    }

    fun clearSaveSucceeded() {
        _uiState.update { it.copy(saveSucceeded = false) }
    }

    fun resetForm() {
        val state = _uiState.value
        val stock = state.currentStock
        _uiState.update {
            it.copy(
                actualStockText = formatQty(stock),
                notes = "",
                reason = AdjustmentReason.PHYSICAL_COUNT,
                itemError = null,
                quantityError = null,
                errorMessage = null
            )
        }
    }

    private fun formatQty(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.2f", value)
}

class StockAdjustmentFormViewModelFactory(
    private val stockAdjustmentRepository: StockAdjustmentRepository,
    private val ingredientRepository: IngredientRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StockAdjustmentFormViewModel::class.java)) {
            return StockAdjustmentFormViewModel(
                stockAdjustmentRepository,
                ingredientRepository,
                productRepository,
                firebaseAuth
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
