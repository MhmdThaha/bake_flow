package com.bakeflow.app.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.ProductException
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.products.validation.ProductValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductFormViewModel(
    private val productRepository: ProductRepository,
    private val productId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductFormUiState(productId = productId))
    val uiState: StateFlow<ProductFormUiState> = _uiState.asStateFlow()

    init {
        if (productId != null) {
            loadProduct(productId)
        }
    }

    fun onNameChange(name: String) {
        _uiState.update { it.copy(name = name, nameError = null, errorMessage = null) }
    }

    fun onCategoryChange(category: String) {
        _uiState.update { it.copy(category = category, errorMessage = null) }
    }

    fun onSellingPriceChange(price: String) {
        _uiState.update { it.copy(sellingPrice = price, priceError = null, errorMessage = null) }
    }

    fun onStatusChange(status: ProductStatus) {
        _uiState.update { it.copy(status = status, errorMessage = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun saveProduct() {
        val state = _uiState.value
        val nameError = ProductValidator.validateName(state.name)
        val (price, priceError) = ProductValidator.validateSellingPrice(state.sellingPrice)

        if (nameError != null || priceError != null) {
            _uiState.update {
                it.copy(nameError = nameError, priceError = priceError)
            }
            return
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save products.") }
            return
        }

        val product = Product(
            id = state.productId.orEmpty(),
            ownerId = state.ownerId.ifBlank { ownerId },
            name = state.name.trim(),
            category = state.category.trim(),
            sellingPrice = price!!,
            status = state.status
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = if (state.productId == null) {
                productRepository.addProduct(product)
            } else {
                productRepository.updateProduct(product)
            }
            result
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, saveSucceeded = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    fun deleteProduct() {
        val id = _uiState.value.productId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            productRepository.deleteProduct(id)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, deleteSucceeded = true)
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    private fun loadProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true, errorMessage = null) }
            productRepository.getProduct(productId)
                .onSuccess { product ->
                    _uiState.update {
                        it.copy(
                            productId = product.id,
                            ownerId = product.ownerId,
                            name = product.name,
                            category = product.category,
                            sellingPrice = formatPrice(product.sellingPrice),
                            status = product.status,
                            isInitialLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    private fun formatPrice(price: Double): String =
        if (price % 1.0 == 0.0) price.toLong().toString() else price.toString()

    private fun Throwable.toUserMessage(): String =
        (this as? ProductException)?.message ?: message ?: "Something went wrong. Please try again."
}

class ProductFormViewModelFactory(
    private val productRepository: ProductRepository,
    private val productId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductFormViewModel::class.java)) {
            return ProductFormViewModel(productRepository, productId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
