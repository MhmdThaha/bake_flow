package com.bakeflow.app.domain.model

enum class UserRole(val displayName: String) {
    OWNER("Bakery Owner"),
    MANAGER("Manager"),
    STAFF("Staff");

    companion object {
        fun fromString(value: String): UserRole =
            entries.find { it.name.equals(value, ignoreCase = true) } ?: STAFF
    }
}
