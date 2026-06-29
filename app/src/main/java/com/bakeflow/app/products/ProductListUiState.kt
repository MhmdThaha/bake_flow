package com.bakeflow.app.products

import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Product

data class ProductListUiState(
    val productsState: UiState<List<Product>> = UiState.Loading,
    val searchQuery: String = "",
    val filteredProducts: List<Product> = emptyList(),
    val deleteInProgressId: String? = null,
    val snackbarMessage: String? = null
)
