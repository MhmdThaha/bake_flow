package com.bakeflow.app.domain.model

data class Product(
    val id: String,
    val ownerId: String,
    val name: String,
    val category: String,
    val sellingPrice: Double,
    val finishedStock: Double = 0.0,
    val status: ProductStatus
)
