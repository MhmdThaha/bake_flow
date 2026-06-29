package com.bakeflow.app.domain.model

data class StockAdjustment(
    val adjustmentId: String,
    val ownerId: String,
    val itemType: AdjustmentItemType,
    val itemId: String,
    val itemName: String,
    val previousQuantity: Double,
    val adjustedQuantity: Double,
    val difference: Double,
    val adjustmentReason: AdjustmentReason,
    val notes: String,
    val adjustmentDate: Long,
    val createdAt: Long,
    val createdBy: String
)

enum class AdjustmentItemType(val displayName: String) {
    INGREDIENT("Ingredient"),
    PRODUCT("Product");

    companion object {
        fun fromString(value: String?): AdjustmentItemType =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: INGREDIENT
    }
}

enum class AdjustmentReason(val displayName: String) {
    PHYSICAL_COUNT("Physical Count"),
    SPILL("Spill"),
    DAMAGED("Damaged"),
    THEFT("Theft"),
    ENTRY_CORRECTION("Entry Correction"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): AdjustmentReason =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}

data class StockAdjustmentSummary(
    val todayCount: Int = 0,
    val todayDifferenceTotal: Double = 0.0,
    val recentAdjustments: List<StockAdjustment> = emptyList()
)
