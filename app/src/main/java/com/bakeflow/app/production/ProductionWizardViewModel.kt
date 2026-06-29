package com.bakeflow.app.production

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.common.BakeFlowPreferences
import com.bakeflow.app.data.repository.ProductionException
import com.bakeflow.app.domain.repository.ExecuteProductionRequest
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.ProductionRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import com.bakeflow.app.production.validation.ProductionValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProductionWizardViewModel(
    private val productionRepository: ProductionRepository,
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val preferences: BakeFlowPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductionWizardUiState())
    val uiState: StateFlow<ProductionWizardUiState> = _uiState.asStateFlow()

    init {
        observeProductsWithRecipes()
    }

    private fun observeProductsWithRecipes() {
        viewModelScope.launch {
            combine(
                productRepository.observeProducts(),
                recipeRepository.observeRecipes()
            ) { products, recipes ->
                val recipeByProduct = recipes.associateBy { it.productId }
                val recentIds = preferences.getRecentProductionProductIds()
                val list = products.mapNotNull { product ->
                    recipeByProduct[product.id]?.let { recipe ->
                        ProductWithRecipe(product = product, recipe = recipe)
                    }
                }
                preferences.sortByRecent(list, { it.product.id }, recentIds)
            }.collect { productsWithRecipes ->
                _uiState.update { it.copy(productsWithRecipes = productsWithRecipes) }
            }
        }
    }

    fun onProductSearchChange(query: String) {
        _uiState.update { it.copy(productSearchQuery = query, productError = null) }
    }

    fun selectProduct(productId: String) {
        val entry = _uiState.value.productsWithRecipes.find { it.product.id == productId }
        _uiState.update {
            it.copy(
                selectedProductId = productId,
                selectedRecipeId = entry?.recipe?.id,
                productError = ProductionValidator.validateHasRecipe(entry?.recipe?.id),
                productSearchQuery = ""
            )
        }
    }

    fun onQuantityChange(value: String) {
        _uiState.update {
            it.copy(
                quantityText = value.filter { char -> char.isDigit() || char == '.' },
                quantityError = null
            )
        }
    }

    fun filteredProducts(): List<ProductWithRecipe> {
        val query = _uiState.value.productSearchQuery.trim().lowercase()
        if (query.isBlank()) return _uiState.value.productsWithRecipes
        return _uiState.value.productsWithRecipes.filter {
            it.product.name.lowercase().contains(query) ||
                it.product.category.lowercase().contains(query)
        }
    }

    fun selectedProductName(): String =
        _uiState.value.productsWithRecipes
            .find { it.product.id == _uiState.value.selectedProductId }
            ?.product?.name.orEmpty()

    fun nextStep() {
        val state = _uiState.value
        when (state.currentStep) {
            ProductionWizardStep.CHOOSE_PRODUCT -> {
                val recipeError = ProductionValidator.validateHasRecipe(state.selectedRecipeId)
                if (recipeError != null) {
                    _uiState.update { it.copy(productError = recipeError) }
                    return
                }
                _uiState.update {
                    it.copy(currentStep = ProductionWizardStep.ENTER_QUANTITY, productError = null)
                }
            }
            ProductionWizardStep.ENTER_QUANTITY -> {
                val qtyError = ProductionValidator.validateQuantity(state.quantityText)
                if (qtyError != null) {
                    _uiState.update { it.copy(quantityError = qtyError) }
                    return
                }
                calculateRequirements()
            }
            ProductionWizardStep.REVIEW_REQUIREMENTS -> {
                val shortageError = ProductionValidator.validateNoShortages(state.requirementLines)
                if (shortageError != null) {
                    _uiState.update { it.copy(errorMessage = shortageError) }
                    return
                }
                _uiState.update {
                    it.copy(currentStep = ProductionWizardStep.CONFIRM, errorMessage = null)
                }
            }
            ProductionWizardStep.CONFIRM -> Unit
        }
    }

    fun previousStep() {
        val previous = when (_uiState.value.currentStep) {
            ProductionWizardStep.CHOOSE_PRODUCT -> ProductionWizardStep.CHOOSE_PRODUCT
            ProductionWizardStep.ENTER_QUANTITY -> ProductionWizardStep.CHOOSE_PRODUCT
            ProductionWizardStep.REVIEW_REQUIREMENTS -> ProductionWizardStep.ENTER_QUANTITY
            ProductionWizardStep.CONFIRM -> ProductionWizardStep.REVIEW_REQUIREMENTS
        }
        _uiState.update { it.copy(currentStep = previous, errorMessage = null) }
    }

    fun calculateRequirements() {
        val state = _uiState.value
        val recipeId = state.selectedRecipeId ?: return
        val quantity = state.quantityText.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true, errorMessage = null) }
            recipeRepository.getRecipeWithItems(recipeId)
                .onSuccess { recipeWithItems ->
                    val ingredients = ingredientRepository.observeIngredients().first()
                    val lines = ProductionCalculator.calculateRequirements(
                        recipeItems = recipeWithItems.items,
                        ingredients = ingredients,
                        quantityProduced = quantity
                    )
                    _uiState.update {
                        it.copy(
                            isCalculating = false,
                            requirementLines = lines,
                            currentStep = ProductionWizardStep.REVIEW_REQUIREMENTS,
                            quantityError = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isCalculating = false,
                            errorMessage = error.message ?: "Unable to calculate requirements."
                        )
                    }
                }
        }
    }

    fun confirmProduction() {
        val state = _uiState.value
        val quantityError = ProductionValidator.validateQuantity(state.quantityText)
        val recipeError = ProductionValidator.validateHasRecipe(state.selectedRecipeId)
        val shortageError = ProductionValidator.validateNoShortages(state.requirementLines)
        if (quantityError != null || recipeError != null || shortageError != null) {
            _uiState.update {
                it.copy(
                    quantityError = quantityError,
                    productError = recipeError,
                    errorMessage = shortageError
                )
            }
            return
        }

        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to start production.") }
            return
        }

        val productName = selectedProductName()
        val productId = state.selectedProductId ?: return
        val recipeId = state.selectedRecipeId ?: return
        val quantity = state.quantityText.toDoubleOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }
            productionRepository.executeProduction(
                ExecuteProductionRequest(
                    productId = productId,
                    productName = productName,
                    recipeId = recipeId,
                    quantityProduced = quantity,
                    createdBy = user.uid
                )
            ).onSuccess {
                preferences.recordRecentProductionProduct(productId)
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        saveSucceeded = true,
                        successMessage = "Production completed! Inventory updated."
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        errorMessage = error.toUserMessage()
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun Throwable.toUserMessage(): String =
        (this as? ProductionException)?.message ?: message ?: "Something went wrong. Please try again."
}

class ProductionWizardViewModelFactory(
    private val productionRepository: ProductionRepository,
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val preferences: BakeFlowPreferences
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProductionWizardViewModel::class.java)) {
            return ProductionWizardViewModel(
                productionRepository,
                recipeRepository,
                productRepository,
                ingredientRepository,
                preferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
