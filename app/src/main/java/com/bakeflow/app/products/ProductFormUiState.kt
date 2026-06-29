package com.bakeflow.app.products

import com.bakeflow.app.domain.model.ProductStatus

data class ProductFormUiState(
    val productId: String? = null,
    val ownerId: String = "",
    val name: String = "",
    val category: String = "",
    val sellingPrice: String = "",
    val status: ProductStatus = ProductStatus.ACTIVE,
    val nameError: String? = null,
    val priceError: String? = null,
    val isLoading: Boolean = false,
    val isInitialLoading: Boolean = false,
    val errorMessage: String? = null,
    val saveSucceeded: Boolean = false,
    val deleteSucceeded: Boolean = false
)
