package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.Product
import kotlinx.coroutines.flow.Flow

interface ProductRepository {
    fun observeProducts(): Flow<List<Product>>

    suspend fun getProduct(productId: String): Result<Product>

    suspend fun addProduct(product: Product): Result<Product>

    suspend fun updateProduct(product: Product): Result<Product>

    suspend fun deleteProduct(productId: String): Result<Unit>
}
