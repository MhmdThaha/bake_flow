package com.bakeflow.app.inventory.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.rememberCoroutineScope
import com.bakeflow.app.domain.model.Ingredient
import kotlinx.coroutines.launch
import com.bakeflow.app.domain.model.IngredientCategory
import com.bakeflow.app.domain.model.IngredientStatus
import com.bakeflow.app.domain.model.IngredientUnit
import com.bakeflow.app.domain.repository.IngredientRepository
import com.bakeflow.app.inventory.IngredientFormViewModel
import com.bakeflow.app.inventory.IngredientFormViewModelFactory
import com.bakeflow.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IngredientEditBottomSheet(
    ingredientId: String?,
    ingredientRepository: IngredientRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formKey = ingredientId ?: "new"
    val formViewModel: IngredientFormViewModel = viewModel(
        key = "ingredient_sheet_$formKey",
        factory = IngredientFormViewModelFactory(ingredientRepository, ingredientId)
    )
    val formState by formViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(formState.saveSucceeded) {
        if (formState.saveSucceeded) {
            onSaved()
            onDismiss()
        }
    }
    LaunchedEffect(formState.deleteSucceeded) {
        if (formState.deleteSucceeded) {
            onDeleted()
            onDismiss()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (ingredientId == null) "Add Ingredient" else "Edit Ingredient",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (formState.isInitialLoading) {
                LoadingState(message = "Loading…")
            } else {
                SheetField(
                    value = formState.name,
                    onValueChange = formViewModel::onNameChange,
                    label = "Ingredient",
                    error = formState.nameError
                )
                EnumDropdown(
                    label = "Category",
                    value = formState.category.displayName,
                    options = IngredientCategory.entries.map { it.displayName },
                    onSelect = { label ->
                        IngredientCategory.entries.find { it.displayName == label }
                            ?.let(formViewModel::onCategoryChange)
                    }
                )
                EnumDropdown(
                    label = "Unit",
                    value = formState.unit.displayName,
                    options = IngredientUnit.entries.map { it.displayName },
                    onSelect = { label ->
                        IngredientUnit.entries.find { it.displayName == label }
                            ?.let(formViewModel::onUnitChange)
                    }
                )
                SheetField(
                    value = formState.costPerUnit,
                    onValueChange = formViewModel::onCostPerUnitChange,
                    label = "Cost per unit",
                    error = formState.costPerUnitError,
                    keyboardType = KeyboardType.Decimal
                )
                SheetField(
                    value = formState.currentStock,
                    onValueChange = formViewModel::onCurrentStockChange,
                    label = "Current stock",
                    error = formState.currentStockError,
                    keyboardType = KeyboardType.Decimal
                )
                SheetField(
                    value = formState.reorderLevel,
                    onValueChange = formViewModel::onReorderLevelChange,
                    label = "Low stock level",
                    error = formState.reorderLevelError,
                    keyboardType = KeyboardType.Decimal
                )
                EnumDropdown(
                    label = "Status",
                    value = formState.status.displayName,
                    options = IngredientStatus.entries.map { it.displayName },
                    onSelect = { label ->
                        IngredientStatus.entries.find { it.displayName == label }
                            ?.let(formViewModel::onStatusChange)
                    }
                )
                formState.errorMessage?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = formViewModel::saveIngredient,
                    enabled = !formState.isLoading,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (formState.isLoading) CircularProgressIndicator() else Text("Save")
                }
                if (ingredientId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = formViewModel::deleteIngredient,
                        enabled = !formState.isLoading,
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiveStockBottomSheet(
    ingredients: List<Ingredient>,
    ingredientRepository: IngredientRepository,
    onDismiss: () -> Unit,
    onReceived: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var selectedId by remember { mutableStateOf(ingredients.firstOrNull()?.id.orEmpty()) }
    var quantity by remember { mutableStateOf("") }
    var supplier by remember { mutableStateOf("") }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = "Receive Stock",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
            EnumDropdown(
                label = "Ingredient",
                value = ingredients.find { it.id == selectedId }?.name ?: "Select",
                options = ingredients.map { it.name },
                onSelect = { name ->
                    selectedId = ingredients.find { it.name == name }?.id.orEmpty()
                }
            )
            SheetField(value = supplier, onValueChange = { supplier = it }, label = "Supplier (optional)")
            SheetField(
                value = quantity,
                onValueChange = { quantity = it },
                label = "Quantity received",
                keyboardType = KeyboardType.Decimal
            )
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val qty = quantity.toDoubleOrNull()
                    val ingredient = ingredients.find { it.id == selectedId }
                    if (ingredient == null || qty == null || qty <= 0) {
                        error = "Enter a valid quantity"
                        return@Button
                    }
                    isSaving = true
                    error = null
                    scope.launch {
                        ingredientRepository.updateIngredient(
                            ingredient.copy(currentStock = ingredient.currentStock + qty)
                        ).onSuccess {
                            isSaving = false
                            onReceived()
                            onDismiss()
                        }.onFailure {
                            isSaving = false
                            error = it.message ?: "Could not update stock"
                        }
                    }
                },
                enabled = !isSaving && ingredients.isNotEmpty(),
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                if (isSaving) CircularProgressIndicator() else Text("Receive")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SheetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    Spacer(modifier = Modifier.height(8.dp))
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EnumDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Spacer(modifier = Modifier.height(8.dp))
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
