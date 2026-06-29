package com.bakeflow.app.domain.model

enum class ProductStatus(val displayName: String) {
    ACTIVE("Active"),
    INACTIVE("Inactive");

    companion object {
        fun fromString(value: String): ProductStatus =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: ACTIVE
    }
}
