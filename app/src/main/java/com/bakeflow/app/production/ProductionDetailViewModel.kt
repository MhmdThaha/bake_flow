package com.bakeflow.app.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.ProductionException
import com.bakeflow.app.domain.repository.ProductionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductionDetailViewModel(
    private val productionRepository: ProductionRepository,
    private val batchId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionDetailUiState())
    val uiState: StateFlow<ProductionDetailUiState> = _uiState.asStateFlow()

    init {
        loadBatch()
    }

    private fun loadBatch() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            productionRepository.getBatch(batchId)
                .onSuccess { batch ->
                    _uiState.update { it.copy(isLoading = false, batch = batch) }
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

    fun retryLoad() = loadBatch()

    private fun Throwable.toUserMessage(): String =
        (this as? ProductionException)?.message ?: message ?: "Something went wrong. Please try again."
}

class ProductionDetailViewModelFactory(
    private val productionRepository: ProductionRepository,
    private val batchId: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductionDetailViewModel::class.java)) {
            return ProductionDetailViewModel(productionRepository, batchId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
