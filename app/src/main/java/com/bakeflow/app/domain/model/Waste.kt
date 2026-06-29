package com.bakeflow.app.domain.model

data class Waste(
    val wasteId: String,
    val ownerId: String,
    val productId: String,
    val productName: String,
    val quantity: Double,
    val reason: WasteReason,
    val notes: String,
    val estimatedLoss: Double,
    val wasteDate: Long,
    val createdAt: Long,
    val createdBy: String
)

enum class WasteReason(val displayName: String) {
    BURNED("Burned"),
    EXPIRED("Expired"),
    DAMAGED("Damaged"),
    RETURNED("Returned"),
    STAFF_CONSUMPTION("Staff Consumption"),
    SAMPLE("Sample"),
    OTHER("Other");

    companion object {
        fun fromString(value: String?): WasteReason =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: OTHER
    }
}

data class WasteSummary(
    val todayCount: Int = 0,
    val todayQuantity: Double = 0.0,
    val todayEstimatedLoss: Double = 0.0,
    val recentWaste: List<Waste> = emptyList()
)
