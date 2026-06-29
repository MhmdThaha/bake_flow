package com.bakeflow.app.recipes.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.recipes.DraftRecipeItem
import com.bakeflow.app.recipes.RecipeWizardStep
import com.bakeflow.app.recipes.RecipeWizardViewModel
import com.bakeflow.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeWizardScreen(
    viewModel: RecipeWizardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val isEditing = uiState.recipeId != null

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) onNavigateBack()
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Recipe" else "Create Recipe") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        if (uiState.isInitialLoading) {
            LoadingState(message = "Loading recipe…", modifier = Modifier.padding(innerPadding))
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
                StepHeader(currentStep = uiState.currentStep)
                Spacer(modifier = Modifier.height(16.dp))

                when (uiState.currentStep) {
                    RecipeWizardStep.CHOOSE_PRODUCT -> ChooseProductStep(
                        searchQuery = uiState.productSearchQuery,
                        onSearchChange = viewModel::onProductSearchChange,
                        products = viewModel.filteredProducts(),
                        selectedProductId = uiState.selectedProductId,
                        onProductSelected = viewModel::selectProduct,
                        productError = uiState.productError,
                        isLocked = isEditing,
                        lockedProductName = viewModel.selectedProductName()
                    )

                    RecipeWizardStep.ADD_INGREDIENTS -> AddIngredientsStep(
                        draftItems = uiState.draftItems,
                        availableIngredients = viewModel.availableIngredients(),
                        onAddIngredient = viewModel::addIngredient,
                        onRemoveIngredient = viewModel::removeIngredient,
                        ingredientName = viewModel::ingredientName,
                        error = uiState.ingredientsError
                    )

                    RecipeWizardStep.ENTER_QUANTITIES -> EnterQuantitiesStep(
                        draftItems = uiState.draftItems,
                        ingredientName = viewModel::ingredientName,
                        unitOptions = viewModel.unitOptions(),
                        onQuantityChange = viewModel::onQuantityChange,
                        onUnitChange = viewModel::onUnitChange,
                        onRemoveIngredient = viewModel::removeIngredient
                    )

                    RecipeWizardStep.SAVE_RECIPE -> ReviewStep(
                        productName = viewModel.selectedProductName(),
                        draftItems = uiState.draftItems,
                        ingredientName = viewModel::ingredientName
                    )
                }

                Spacer(modifier = Modifier.weight(1f))
                WizardNavigationButtons(
                    currentStep = uiState.currentStep,
                    isLoading = uiState.isLoading,
                    onPrevious = viewModel::previousStep,
                    onNext = viewModel::nextStep
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun StepHeader(currentStep: RecipeWizardStep) {
    Column {
        LinearProgressIndicator(
            progress = { currentStep.stepNumber / 4f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Step ${currentStep.stepNumber} of 4",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = currentStep.title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun ChooseProductStep(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    products: List<Product>,
    selectedProductId: String?,
    onProductSelected: (String) -> Unit,
    productError: String?,
    isLocked: Boolean = false,
    lockedProductName: String = ""
) {
    Column {
        if (isLocked) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                tonalElevation = 2.dp,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = lockedProductName,
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "Product cannot be changed when editing a recipe.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search products") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (products.isEmpty()) {
                Text(
                    text = "No products available. Create a product first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 360.dp)
                ) {
                    items(products, key = { it.id }) { product ->
                        val selected = product.id == selectedProductId
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product.id) },
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = if (selected) 4.dp else 1.dp,
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            }
                        ) {
                            Text(
                                text = product.name,
                                modifier = Modifier.padding(16.dp),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }
        productError?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun AddIngredientsStep(
    draftItems: List<DraftRecipeItem>,
    availableIngredients: List<com.bakeflow.app.domain.model.Ingredient>,
    onAddIngredient: (String) -> Unit,
    onRemoveIngredient: (String) -> Unit,
    ingredientName: (String) -> String,
    error: String?
) {
    var showPicker by remember { mutableStateOf(false) }

    Column {
        OutlinedButton(
            onClick = { showPicker = true },
            modifier = Modifier.fillMaxWidth(),
            enabled = availableIngredients.isNotEmpty()
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
            Text("Add Ingredient", modifier = Modifier.padding(start = 8.dp))
        }
        error?.let {
            Text(
                text = it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (draftItems.isEmpty()) {
            Text(
                text = "No ingredients added yet.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            draftItems.forEach { item ->
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    shape = MaterialTheme.shapes.large,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ingredientName(item.ingredientId),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        IconButton(onClick = { onRemoveIngredient(item.localId) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove")
                        }
                    }
                }
            }
        }
    }

    if (showPicker) {
        IngredientPickerDialog(
            ingredients = availableIngredients,
            onDismiss = { showPicker = false },
            onSelect = { id ->
                onAddIngredient(id)
                showPicker = false
            }
        )
    }
}

@Composable
private fun EnterQuantitiesStep(
    draftItems: List<DraftRecipeItem>,
    ingredientName: (String) -> String,
    unitOptions: List<String>,
    onQuantityChange: (String, String) -> Unit,
    onUnitChange: (String, String) -> Unit,
    onRemoveIngredient: (String) -> Unit
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        draftItems.forEach { item ->
            QuantityItemRow(
                item = item,
                name = ingredientName(item.ingredientId),
                unitOptions = unitOptions,
                onQuantityChange = { onQuantityChange(item.localId, it) },
                onUnitChange = { onUnitChange(item.localId, it) },
                onRemove = { onRemoveIngredient(item.localId) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuantityItemRow(
    item: DraftRecipeItem,
    name: String,
    unitOptions: List<String>,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (String) -> Unit,
    onRemove: () -> Unit
) {
    var unitExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "Remove")
                }
            }
            OutlinedTextField(
                value = item.quantity,
                onValueChange = onQuantityChange,
                label = { Text("Quantity") },
                modifier = Modifier.fillMaxWidth(),
                isError = item.quantityError != null,
                supportingText = item.quantityError?.let { err ->
                    { Text(err, color = MaterialTheme.colorScheme.error) }
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )
            ExposedDropdownMenuBox(
                expanded = unitExpanded,
                onExpandedChange = { unitExpanded = it },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                OutlinedTextField(
                    value = item.unit,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Unit") },
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitExpanded)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                    shape = MaterialTheme.shapes.large
                )
                DropdownMenu(
                    expanded = unitExpanded,
                    onDismissRequest = { unitExpanded = false }
                ) {
                    unitOptions.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit) },
                            onClick = {
                                onUnitChange(unit)
                                unitExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewStep(
    productName: String,
    draftItems: List<DraftRecipeItem>,
    ingredientName: (String) -> String
) {
    Column(
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Product",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = productName,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        Text(
            text = "Ingredients",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        draftItems.forEach { item ->
            Text(
                text = "• ${ingredientName(item.ingredientId)} — ${item.quantity} ${item.unit}",
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun WizardNavigationButtons(
    currentStep: RecipeWizardStep,
    isLoading: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentStep != RecipeWizardStep.CHOOSE_PRODUCT) {
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                enabled = !isLoading
            ) {
                Text("Back")
            }
        }
        Button(
            onClick = onNext,
            modifier = Modifier.weight(1f).heightIn(min = 56.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else {
                Text(
                    when (currentStep) {
                        RecipeWizardStep.SAVE_RECIPE -> "Save Recipe"
                        else -> "Next"
                    }
                )
            }
        }
    }
}

@Composable
private fun IngredientPickerDialog(
    ingredients: List<com.bakeflow.app.domain.model.Ingredient>,
    onDismiss: () -> Unit,
    onSelect: (String) -> Unit
) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Ingredient") },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 320.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(ingredients, key = { it.id }) { ingredient ->
                    Text(
                        text = ingredient.name,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(ingredient.id) }
                            .padding(12.dp),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
