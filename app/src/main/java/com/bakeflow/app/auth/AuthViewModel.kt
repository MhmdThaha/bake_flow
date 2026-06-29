package com.bakeflow.app.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.R
import com.bakeflow.app.auth.util.GoogleAuthHelper
import com.bakeflow.app.auth.util.GoogleSignInCancelledException
import com.bakeflow.app.auth.validation.AuthValidator
import com.bakeflow.app.data.network.NetworkMonitor
import com.bakeflow.app.data.repository.AuthException
import com.bakeflow.app.domain.model.UserRole
import com.bakeflow.app.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            networkMonitor.isOnline.collect { online ->
                _uiState.update { it.copy(isOffline = !online) }
            }
        }
    }

    fun onEmailChange(email: String) {
        _uiState.update {
            it.copy(email = email, emailError = null, errorMessage = null, successMessage = null)
        }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(password = password, passwordError = null, errorMessage = null, successMessage = null)
        }
    }

    fun onConfirmPasswordChange(confirmPassword: String) {
        _uiState.update {
            it.copy(
                confirmPassword = confirmPassword,
                confirmPasswordError = null,
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onNameChange(name: String) {
        _uiState.update {
            it.copy(name = name, nameError = null, errorMessage = null, successMessage = null)
        }
    }

    fun onRoleChange(role: UserRole) {
        _uiState.update { it.copy(role = role, errorMessage = null, successMessage = null) }
    }

    fun showError(message: String) {
        _uiState.update { it.copy(errorMessage = message, isLoading = false) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun signIn() {
        val state = _uiState.value
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = if (state.password.isBlank()) "Password is required" else null

        if (emailError != null || passwordError != null) {
            _uiState.update {
                it.copy(emailError = emailError, passwordError = passwordError)
            }
            return
        }

        if (state.isOffline) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Check your network and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            authRepository.signInWithEmail(state.email.trim(), state.password)
                .onSuccess {
                    _uiState.update {
                        it.copy(isLoading = false, authSucceeded = true, successMessage = "Welcome back!")
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage(),
                            authSucceeded = false
                        )
                    }
                }
        }
    }

    fun register() {
        val state = _uiState.value
        val emailError = AuthValidator.validateEmail(state.email)
        val passwordError = AuthValidator.validatePassword(state.password)
        val confirmPasswordError = AuthValidator.validateConfirmPassword(state.password, state.confirmPassword)
        val nameError = AuthValidator.validateName(state.name)

        if (emailError != null || passwordError != null || confirmPasswordError != null || nameError != null) {
            _uiState.update {
                it.copy(
                    emailError = emailError,
                    passwordError = passwordError,
                    confirmPasswordError = confirmPasswordError,
                    nameError = nameError
                )
            }
            return
        }

        if (state.isOffline) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Check your network and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            authRepository.register(
                name = state.name.trim(),
                email = state.email.trim(),
                password = state.password,
                role = state.role
            )
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            authSucceeded = true,
                            successMessage = "Account created successfully!"
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage(),
                            authSucceeded = false
                        )
                    }
                }
        }
    }

    fun sendPasswordReset() {
        val state = _uiState.value
        val emailError = AuthValidator.validateEmail(state.email)

        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        if (state.isOffline) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Check your network and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            authRepository.sendPasswordResetEmail(state.email.trim())
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Password reset email sent. Check your inbox."
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage()
                        )
                    }
                }
        }
    }

    fun signInWithGoogle(context: Context) {
        if (_uiState.value.isOffline) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Check your network and try again.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val serverClientId = context.getString(R.string.default_web_client_id)
            GoogleAuthHelper.signIn(context, serverClientId)
                .onSuccess { googleResult ->
                    Log.d(TAG, "GoogleAuthHelper.signIn succeeded, calling authRepository.signInWithGoogle")
                    authRepository.signInWithGoogle(googleResult.idToken, googleResult.displayName)
                        .onSuccess { user ->
                            Log.d(TAG, "authRepository.signInWithGoogle succeeded: uid=${user.uid}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    authSucceeded = true,
                                    successMessage = "Signed in with Google!"
                                )
                            }
                        }
                        .onFailure { error ->
                            Log.d(TAG, "authRepository.signInWithGoogle failed: ${error.message}")
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    errorMessage = error.toUserMessage(),
                                    authSucceeded = false
                                )
                            }
                        }
                }
                .onFailure { error ->
                    Log.d(TAG, "GoogleAuthHelper.signIn failed: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = if (error is GoogleSignInCancelledException) {
                                null
                            } else {
                                error.toUserMessage()
                            },
                            authSucceeded = false
                        )
                    }
                }
        }
    }

    fun signOut(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            authRepository.signOut()
            _uiState.update {
                AuthUiState(isOffline = !networkMonitor.isCurrentlyOnline())
            }
            onComplete()
        }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? AuthException)?.message ?: message ?: "Something went wrong. Please try again."
}

class AuthViewModelFactory(
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            return AuthViewModel(authRepository, networkMonitor) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private const val TAG = "BakeFlowNav"
