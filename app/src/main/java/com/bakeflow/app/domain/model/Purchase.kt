package com.bakeflow.app.domain.model

data class Purchase(
    val purchaseId: String,
    val ownerId: String,
    val ingredientId: String,
    val ingredientName: String,
    val supplierName: String,
    val quantity: Double,
    val unit: String,
    val costPerUnit: Double,
    val totalCost: Double,
    val purchaseDate: Long,
    val invoiceNumber: String,
    val notes: String,
    val createdAt: Long,
    val createdBy: String
)

data class PurchaseSummary(
    val todayCount: Int = 0,
    val todayTotalCost: Double = 0.0
)
