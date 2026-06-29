package com.bakeflow.app.domain.model

enum class IngredientStatus(val displayName: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    companion object {
        fun fromString(value: String): IngredientStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}
