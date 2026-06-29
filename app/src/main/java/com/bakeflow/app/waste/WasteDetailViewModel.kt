package com.bakeflow.app.waste

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.WasteException
import com.bakeflow.app.domain.repository.WasteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WasteDetailViewModel(
    private val wasteRepository: WasteRepository,
    private val wasteId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(WasteDetailUiState())
    val uiState: StateFlow<WasteDetailUiState> = _uiState.asStateFlow()

    init {
        loadWaste()
    }

    private fun loadWaste() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            wasteRepository.getWaste(wasteId)
                .onSuccess { waste ->
                    _uiState.update { it.copy(isLoading = false, waste = waste) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = (error as? WasteException)?.message
                                ?: error.message
                                ?: "Something went wrong. Please try again."
                        )
                    }
                }
        }
    }

    fun retryLoad() = loadWaste()
}

class WasteDetailViewModelFactory(
    private val wasteRepository: WasteRepository,
    private val wasteId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WasteDetailViewModel::class.java)) {
            return WasteDetailViewModel(wasteRepository, wasteId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
