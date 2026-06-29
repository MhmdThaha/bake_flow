package com.bakeflow.app.auth

import com.bakeflow.app.domain.model.UserRole

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val name: String = "",
    val role: UserRole = UserRole.STAFF,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isOffline: Boolean = false,
    val authSucceeded: Boolean = false
)
