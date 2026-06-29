package com.bakeflow.app.recipes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.bakeflow.app.data.repository.RecipeException
import com.bakeflow.app.domain.model.IngredientUnit
import com.bakeflow.app.domain.model.Recipe
import com.bakeflow.app.domain.model.RecipeItem
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.domain.repository.RecipeRepository
import com.bakeflow.app.recipes.validation.RecipeValidator
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class RecipeWizardViewModel(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeId: String?
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecipeWizardUiState(recipeId = recipeId))
    val uiState: StateFlow<RecipeWizardUiState> = _uiState.asStateFlow()

    init {
        observeReferenceData()
        if (recipeId != null) {
            loadRecipe(recipeId)
        }
    }

    private fun observeReferenceData() {
        viewModelScope.launch {
            combine(
                productRepository.observeProducts(),
                ingredientRepository.observeIngredients(),
                recipeRepository.observeRecipes()
            ) { products, ingredients, recipes ->
                val currentProductId = recipes.find { it.id == recipeId }?.productId
                val blockedProductIds = recipes
                    .map { it.productId }
                    .toSet()
                    .minus(currentProductId ?: "")
                Triple(products, ingredients, blockedProductIds)
            }.collect { (products, ingredients, blockedProductIds) ->
                _uiState.update {
                    it.copy(
                        products = products,
                        ingredients = ingredients,
                        usedProductIds = blockedProductIds
                    )
                }
            }
        }
    }

    fun onProductSearchChange(query: String) {
        _uiState.update { it.copy(productSearchQuery = query, productError = null) }
    }

    fun selectProduct(productId: String) {
        _uiState.update {
            it.copy(selectedProductId = productId, productError = null, productSearchQuery = "")
        }
    }

    fun addIngredient(ingredientId: String) {
        val ingredient = _uiState.value.ingredients.find { it.id == ingredientId } ?: return
        if (_uiState.value.draftItems.any { it.ingredientId == ingredientId }) return
        val draftItem = DraftRecipeItem(
            localId = UUID.randomUUID().toString(),
            ingredientId = ingredientId,
            unit = ingredient.unit.displayName
        )
        _uiState.update {
            it.copy(
                draftItems = it.draftItems + draftItem,
                ingredientsError = null
            )
        }
    }

    fun removeIngredient(localId: String) {
        _uiState.update {
            it.copy(draftItems = it.draftItems.filter { item -> item.localId != localId })
        }
    }

    fun onQuantityChange(localId: String, quantity: String) {
        _uiState.update { state ->
            state.copy(
                draftItems = state.draftItems.map { item ->
                    if (item.localId == localId) {
                        item.copy(quantity = quantity, quantityError = null)
                    } else {
                        item
                    }
                }
            )
        }
    }

    fun onUnitChange(localId: String, unit: String) {
        _uiState.update { state ->
            state.copy(
                draftItems = state.draftItems.map { item ->
                    if (item.localId == localId) item.copy(unit = unit) else item
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun nextStep() {
        when (_uiState.value.currentStep) {
            RecipeWizardStep.CHOOSE_PRODUCT -> validateAndAdvanceFromProduct()
            RecipeWizardStep.ADD_INGREDIENTS -> validateAndAdvanceFromIngredients()
            RecipeWizardStep.ENTER_QUANTITIES -> validateAndAdvanceFromQuantities()
            RecipeWizardStep.SAVE_RECIPE -> saveRecipe()
        }
    }

    fun previousStep() {
        val previous = when (_uiState.value.currentStep) {
            RecipeWizardStep.CHOOSE_PRODUCT -> return
            RecipeWizardStep.ADD_INGREDIENTS -> RecipeWizardStep.CHOOSE_PRODUCT
            RecipeWizardStep.ENTER_QUANTITIES -> RecipeWizardStep.ADD_INGREDIENTS
            RecipeWizardStep.SAVE_RECIPE -> RecipeWizardStep.ENTER_QUANTITIES
        }
        _uiState.update { it.copy(currentStep = previous, errorMessage = null) }
    }

    fun saveRecipe() {
        val state = _uiState.value
        val productError = RecipeValidator.validateProduct(state.selectedProductId)
        val ingredientsError = RecipeValidator.validateHasIngredients(state.draftItems)
        val duplicateError = RecipeValidator.validateNoDuplicateIngredients(state.draftItems)
        val validatedItems = RecipeValidator.validateAllQuantities(state.draftItems)

        if (productError != null || ingredientsError != null || duplicateError != null ||
            RecipeValidator.hasQuantityErrors(validatedItems)
        ) {
            _uiState.update {
                it.copy(
                    productError = productError,
                    ingredientsError = ingredientsError ?: duplicateError,
                    draftItems = validatedItems,
                    errorMessage = duplicateError
                )
            }
            return
        }

        val ownerId = FirebaseAuth.getInstance().currentUser?.uid
        if (ownerId == null) {
            _uiState.update { it.copy(errorMessage = "You must be signed in to save recipes.") }
            return
        }

        val recipe = Recipe(
            id = state.recipeId.orEmpty(),
            ownerId = ownerId,
            productId = state.selectedProductId!!
        )
        val items = validatedItems.map { draft ->
            val (_, qtyError) = RecipeValidator.validateQuantity(draft.quantity)
            require(qtyError == null)
            RecipeItem(
                id = "",
                recipeId = state.recipeId.orEmpty(),
                ingredientId = draft.ingredientId,
                quantity = draft.quantity.toDouble(),
                unit = draft.unit
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            recipeRepository.saveRecipe(recipe, items)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false, saveSucceeded = true) }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    private fun validateAndAdvanceFromProduct() {
        val error = RecipeValidator.validateProduct(_uiState.value.selectedProductId)
        if (error != null) {
            _uiState.update { it.copy(productError = error) }
            return
        }
        val productId = _uiState.value.selectedProductId!!
        if (_uiState.value.usedProductIds.contains(productId)) {
            _uiState.update {
                it.copy(productError = "This product already has a recipe.")
            }
            return
        }
        _uiState.update {
            it.copy(currentStep = RecipeWizardStep.ADD_INGREDIENTS, productError = null)
        }
    }

    private fun validateAndAdvanceFromIngredients() {
        val error = RecipeValidator.validateHasIngredients(_uiState.value.draftItems)
        if (error != null) {
            _uiState.update { it.copy(ingredientsError = error) }
            return
        }
        _uiState.update {
            it.copy(currentStep = RecipeWizardStep.ENTER_QUANTITIES, ingredientsError = null)
        }
    }

    private fun validateAndAdvanceFromQuantities() {
        val duplicateError = RecipeValidator.validateNoDuplicateIngredients(_uiState.value.draftItems)
        if (duplicateError != null) {
            _uiState.update { it.copy(ingredientsError = duplicateError) }
            return
        }
        val validated = RecipeValidator.validateAllQuantities(_uiState.value.draftItems)
        if (RecipeValidator.hasQuantityErrors(validated)) {
            _uiState.update { it.copy(draftItems = validated) }
            return
        }
        _uiState.update {
            it.copy(
                currentStep = RecipeWizardStep.SAVE_RECIPE,
                draftItems = validated,
                ingredientsError = null
            )
        }
    }

    private fun loadRecipe(recipeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitialLoading = true) }
            recipeRepository.getRecipeWithItems(recipeId)
                .onSuccess { recipeWithItems ->
                    _uiState.update {
                        it.copy(
                            recipeId = recipeWithItems.recipe.id,
                            selectedProductId = recipeWithItems.recipe.productId,
                            draftItems = recipeWithItems.items.map { item ->
                                DraftRecipeItem(
                                    localId = UUID.randomUUID().toString(),
                                    ingredientId = item.ingredientId,
                                    quantity = formatQuantity(item.quantity),
                                    unit = item.unit
                                )
                            },
                            isInitialLoading = false
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(isInitialLoading = false, errorMessage = error.toUserMessage())
                    }
                }
        }
    }

    private fun formatQuantity(value: Double): String =
        if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()

    fun filteredProducts(): List<com.bakeflow.app.domain.model.Product> {
        val state = _uiState.value
        val query = state.productSearchQuery.trim().lowercase()
        return state.products.filter { product ->
            val matchesQuery = query.isBlank() || product.name.lowercase().contains(query)
            val available = !state.usedProductIds.contains(product.id) ||
                product.id == state.selectedProductId
            matchesQuery && available
        }
    }

    fun availableIngredients(): List<com.bakeflow.app.domain.model.Ingredient> {
        val addedIds = _uiState.value.draftItems.map { it.ingredientId }.toSet()
        return _uiState.value.ingredients.filter { it.id !in addedIds }
    }

    fun ingredientName(ingredientId: String): String =
        _uiState.value.ingredients.find { it.id == ingredientId }?.name ?: "Unknown"

    fun selectedProductName(): String =
        _uiState.value.products.find { it.id == _uiState.value.selectedProductId }?.name ?: ""

    fun unitOptions(): List<String> = IngredientUnit.entries.map { it.displayName }

    private fun Throwable.toUserMessage(): String =
        (this as? RecipeException)?.message ?: message ?: "Something went wrong. Please try again."
}

class RecipeWizardViewModelFactory(
    private val recipeRepository: RecipeRepository,
    private val productRepository: ProductRepository,
    private val ingredientRepository: IngredientRepository,
    private val recipeId: String?
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecipeWizardViewModel::class.java)) {
            return RecipeWizardViewModel(
                recipeRepository,
                productRepository,
                ingredientRepository,
                recipeId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
