package com.bakeflow.app.sales

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.data.repository.SaleException
import com.bakeflow.app.domain.model.PaymentMethod
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecordSaleRequest
import com.bakeflow.app.domain.repository.SaleRepository
import com.bakeflow.app.sales.validation.SaleValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SaleFormViewModel(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedProductId: String? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(SaleFormUiState())
    val uiState: StateFlow<SaleFormUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(paymentMethod = preferences.getLastPaymentMethod()) }
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
                    val sellable = products.filter { it.status == ProductStatus.ACTIVE }
                    val recentIds = preferences.getRecentProductIds()
                    val sorted = preferences.sortByRecent(sellable, { it.id }, recentIds)
                    val selectedId = _uiState.value.selectedProductId
                        ?: preselectedProductId
                        ?: sellable.firstOrNull()?.id
                    val selected = sellable.find { it.id == selectedId }
                    _uiState.update {
                        it.copy(
                            products = sellable,
                            selectedProductId = selected?.id,
                            unitPriceText = selected?.let { product -> formatPrice(product.sellingPrice) }
                                ?: it.unitPriceText
                        )
                    }
                }
        }
    }

    fun onProductSelected(productId: String) {
        val product = _uiState.value.products.find { it.id == productId }
        _uiState.update {
            it.copy(
                selectedProductId = productId,
                productError = null,
                unitPriceText = product?.let { p -> formatPrice(p.sellingPrice) } ?: it.unitPriceText,
                quantityError = null
            )
        }
    }

    fun onQuantityChange(value: String) {
        _uiState.update { it.copy(quantityText = value, quantityError = null) }
    }

    fun onPaymentMethodChange(method: PaymentMethod) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onCustomerNameChange(value: String) {
        _uiState.update { it.copy(customerName = value) }
    }

    fun onCustomerPhoneChange(value: String) {
        _uiState.update { it.copy(customerPhone = value) }
    }

    fun onNotesChange(value: String) {
        _uiState.update { it.copy(notes = value) }
    }

    fun saveSale() {
        val state = _uiState.value
        val productError = SaleValidator.validateProduct(state.selectedProductId)
        val quantityError = SaleValidator.validateQuantity(state.quantityText, state.availableStock)
        if (productError != null || quantityError != null) {
            _uiState.update {
                it.copy(productError = productError, quantityError = quantityError)
            }
            return
        }

        val user = firebaseAuth.currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save a sale.") }
            return
        }

        val quantity = state.quantityText.toDoubleOrNull()!!
        val unitPrice = state.unitPriceText.toDoubleOrNull() ?: state.selectedProduct?.sellingPrice ?: 0.0
        val createdBy = user.displayName?.takeIf { it.isNotBlank() }
            ?: user.email?.substringBefore('@')
            ?: user.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            saleRepository.recordSale(
                RecordSaleRequest(
                    productId = state.selectedProductId!!,
                    quantity = quantity,
                    unitPrice = unitPrice,
                    paymentMethod = state.paymentMethod,
                    customerName = state.customerName,
                    customerPhone = state.customerPhone,
                    notes = state.notes,
                    saleDateMillis = System.currentTimeMillis(),
                    createdBy = createdBy
                )
            ).onSuccess {
                preferences.recordRecentProduct(state.selectedProductId!!)
                preferences.setLastPaymentMethod(state.paymentMethod)
                _uiState.update { it.copy(isSaving = false, saveSucceeded = true) }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = (error as? SaleException)?.message
                            ?: error.message
                            ?: "Could not save sale."
                    )
                }
            }
        }
    }

    fun clearSaveSucceeded() {
        _uiState.update { it.copy(saveSucceeded = false) }
    }

    private fun formatPrice(price: Double): String =
        if (price % 1.0 == 0.0) price.toLong().toString() else String.format("%.2f", price)
}

class SaleFormViewModelFactory(
    private val saleRepository: SaleRepository,
    private val productRepository: ProductRepository,
    private val firebaseAuth: FirebaseAuth,
    private val preferences: BakeFlowPreferences,
    private val preselectedProductId: String? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SaleFormViewModel::class.java)) {
            return SaleFormViewModel(
                saleRepository,
                productRepository,
                firebaseAuth,
                preferences,
                preselectedProductId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
