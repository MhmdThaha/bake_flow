package com.bakeflow.app.domain.model

enum class ProductionStatus(val displayName: String) {
    PENDING("Pending"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled");

    companion object {
        fun fromString(value: String): ProductionStatus =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: COMPLETED
    }
}
