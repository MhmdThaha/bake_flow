package com.bakeflow.app.auth

import com.bakeflow.app.domain.model.User
import com.bakeflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

sealed class AuthSessionState {
    data object Loading : AuthSessionState()
    data object Unauthenticated : AuthSessionState()
    data class Authenticated(val user: User) : AuthSessionState()
}

/**
 * Observes Firebase auth state and exposes session status for navigation guards.
 */
class SessionManager(
    private val authRepository: AuthRepository
) {
    private val scope = CoroutineScope(SupervisorJob())

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    init {
        scope.launch {
            authRepository.currentUser.collectLatest { user ->
                _sessionState.value = if (user != null) {
                    AuthSessionState.Authenticated(user)
                } else {
                    AuthSessionState.Unauthenticated
                }
            }
        }
    }

    val isAuthenticated: Boolean
        get() = _sessionState.value is AuthSessionState.Authenticated
}
