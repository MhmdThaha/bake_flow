package com.bakeflow.app.domain.model

data class Sale(
    val saleId: String,
    val ownerId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val unitPrice: Double,
    val totalAmount: Double,
    val paymentMethod: PaymentMethod,
    val customerName: String,
    val customerPhone: String,
    val notes: String,
    val saleDate: Long,
    val createdAt: Long,
    val createdBy: String
)

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    CARD("Card"),
    MOBILE("Mobile"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): PaymentMethod =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: CASH
    }
}

data class SalesSummary(
    val todayCount: Int = 0,
    val todayRevenue: Double = 0.0,
    val bestSellingProductName: String = "—",
    val bestSellingQuantity: Double = 0.0,
    val productsSoldToday: List<ProductSoldToday> = emptyList()
)

data class ProductSoldToday(
    val productName: String,
    val quantitySold: Double,
    val revenue: Double
)
