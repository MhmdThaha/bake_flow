package com.bakeflow.app.products

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.UiState
import com.bakeflow.app.data.repository.ProductException
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.repository.ProductRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductListViewModel(
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState: StateFlow<ProductListUiState> = _uiState.asStateFlow()

    private var allProducts: List<Product> = emptyList()
    private var observeJob: Job? = null

    init {
        startObservingProducts()
    }

    private fun startObservingProducts() {
        observeJob?.cancel()
        observeJob = viewModelScope.launch {
            productRepository.observeProducts()
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            productsState = UiState.Error(
                                (error as? ProductException)?.message
                                    ?: "Failed to load products. Please try again."
                            )
                        )
                    }
                }
                .collect { products ->
                    allProducts = products
                    applySearch(_uiState.value.searchQuery, products)
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearch(query, allProducts)
    }

    fun clearSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(deleteInProgressId = productId) }
            productRepository.deleteProduct(productId)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            deleteInProgressId = null,
                            snackbarMessage = "Product deleted"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            deleteInProgressId = null,
                            snackbarMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    fun retryLoad() {
        _uiState.update { it.copy(productsState = UiState.Loading) }
        startObservingProducts()
    }

    private fun applySearch(query: String, products: List<Product>) {
        val filtered = if (query.isBlank()) {
            products
        } else {
            val normalizedQuery = query.trim().lowercase()
            products.filter { product ->
                product.name.lowercase().contains(normalizedQuery) ||
                    product.category.lowercase().contains(normalizedQuery)
            }
        }
        _uiState.update {
            it.copy(
                productsState = if (products.isEmpty()) UiState.Empty else UiState.Success(filtered),
                filteredProducts = filtered
            )
        }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? ProductException)?.message ?: message ?: "Something went wrong. Please try again."
}

class ProductListViewModelFactory(
    private val productRepository: ProductRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductListViewModel::class.java)) {
            return ProductListViewModel(productRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
