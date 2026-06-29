package com.bakeflow.app.common

/**
 * Shared constants used across feature modules.
 */
object BakeFlowConstants {
    const val APP_NAME = "BakeFlow"
}

/**
 * Generic UI state wrapper for MVVM screens.
 * Used by ViewModels to expose loading, success, empty, and error states.
 */
sealed class UiState<out T> {
    data object Loading : UiState<Nothing>()
    data object Empty : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    data class Error(val message: String) : UiState<Nothing>()
}
