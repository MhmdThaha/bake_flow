package com.bakeflow.app.domain.model

data class User(
    val uid: String,
    val name: String,
    val email: String,
    val role: UserRole,
    val createdAt: Long?,
    val lastLogin: Long?
)
