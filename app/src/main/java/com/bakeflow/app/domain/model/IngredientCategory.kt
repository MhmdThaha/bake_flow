package com.bakeflow.app.domain.model

enum class IngredientCategory(val displayName: String) {
    FLOUR("Flour"),
    SWEETENER("Sweetener"),
    OIL("Oil"),
    DAIRY("Dairy"),
    SEASONING("Seasoning"),
    PACKAGING("Packaging"),
    ADDITIVE("Additive"),
    OTHER("Other");

    companion object {
        fun fromString(value: String): IngredientCategory =
            entries.find { it.name.equals(value, ignoreCase = true) }
                ?: entries.find { it.displayName.equals(value, ignoreCase = true) }
                ?: OTHER
    }
}
