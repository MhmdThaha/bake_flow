package com.bakeflow.app.inventory.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit
import com.bakeflow.app.inventory.IngredientFormViewModel
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientFormScreen(
    viewModel: IngredientFormViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var unitMenuExpanded by remember { mutableStateOf(false) }
    var statusMenuExpanded by remember { mutableStateOf(false) }
    val isEditing = uiState.ingredientId != null

    LaunchedEffect(uiState.saveSucceeded, uiState.deleteSucceeded) {
        if (uiState.saveSucceeded || uiState.deleteSucceeded) {
            onNavigateBack()
        }
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
                title = { Text(if (isEditing) "Edit Ingredient" else "Add Ingredient") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isInitialLoading -> LoadingState(
                message = "Loading ingredient…",
                modifier = Modifier.padding(innerPadding)
            )

            uiState.errorMessage != null && uiState.name.isBlank() && isEditing -> ErrorState(
                message = uiState.errorMessage.orEmpty(),
                modifier = Modifier.padding(innerPadding)
            )

            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = uiState.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Ingredient Name") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.nameError != null,
                    supportingText = uiState.nameError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                DropdownField(
                    label = "Category",
                    value = uiState.category.displayName,
                    expanded = categoryMenuExpanded,
                    onExpandedChange = { categoryMenuExpanded = it },
                    options = IngredientCategory.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        IngredientCategory.entries
                            .find { it.displayName == displayName }
                            ?.let(viewModel::onCategoryChange)
                    }
                )

                DropdownField(
                    label = "Unit",
                    value = uiState.unit.displayName,
                    expanded = unitMenuExpanded,
                    onExpandedChange = { unitMenuExpanded = it },
                    isError = uiState.unitError != null,
                    errorMessage = uiState.unitError,
                    options = IngredientUnit.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        IngredientUnit.entries
                            .find { it.displayName == displayName }
                            ?.let(viewModel::onUnitChange)
                    }
                )

                OutlinedTextField(
                    value = uiState.costPerUnit,
                    onValueChange = viewModel::onCostPerUnitChange,
                    label = { Text("Cost Per Unit") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.costPerUnitError != null,
                    supportingText = uiState.costPerUnitError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = uiState.currentStock,
                    onValueChange = viewModel::onCurrentStockChange,
                    label = { Text("Current Stock") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.currentStockError != null,
                    supportingText = uiState.currentStockError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                OutlinedTextField(
                    value = uiState.reorderLevel,
                    onValueChange = viewModel::onReorderLevelChange,
                    label = { Text("Reorder Level") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.reorderLevelError != null,
                    supportingText = uiState.reorderLevelError?.let { error ->
                        { Text(error, color = MaterialTheme.colorScheme.error) }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )

                DropdownField(
                    label = "Status",
                    value = uiState.status.displayName,
                    expanded = statusMenuExpanded,
                    onExpandedChange = { statusMenuExpanded = it },
                    options = IngredientStatus.entries.map { it.displayName },
                    onOptionSelected = { displayName ->
                        IngredientStatus.entries
                            .find { it.displayName == displayName }
                            ?.let(viewModel::onStatusChange)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = viewModel::saveIngredient,
                    enabled = !uiState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text(
                            text = if (isEditing) "Save Changes" else "Add Ingredient",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                if (isEditing) {
                    Button(
                        onClick = viewModel::deleteIngredient,
                        enabled = !uiState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Text(
                            text = "Delete Ingredient",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownField(
    label: String,
    value: String,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    isError: Boolean = false,
    errorMessage: String? = null
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            isError = isError,
            supportingText = errorMessage?.let { error ->
                { Text(error, color = MaterialTheme.colorScheme.error) }
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = MaterialTheme.shapes.large
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        onExpandedChange(false)
                    }
                )
            }
        }
    }
}
