package com.bakeflow.app.domain.repository

import com.bakeflow.app.domain.model.User
import com.bakeflow.app.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: Flow<User?>

    suspend fun signInWithEmail(email: String, password: String): Result<User>

    suspend fun register(
        name: String,
        email: String,
        password: String,
        role: UserRole
    ): Result<User>

    suspend fun sendPasswordResetEmail(email: String): Result<Unit>

    suspend fun signInWithGoogle(idToken: String, displayName: String?): Result<User>

    suspend fun signOut()

    suspend fun refreshCurrentUser(): User?
}
