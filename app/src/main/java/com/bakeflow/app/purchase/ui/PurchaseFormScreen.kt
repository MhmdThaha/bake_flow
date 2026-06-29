package com.bakeflow.app.purchase.ui

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
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.purchase.PurchaseFormViewModel
import com.bakeflow.app.ui.components.QuantityStepper
import com.bakeflow.app.ui.components.RecentItemsRow
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormScreen(
    viewModel: PurchaseFormViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) {
            snackbarHostState.showSnackbar("Purchase saved — stock updated")
            viewModel.clearSaveSucceeded()
            onNavigateBack()
        }
    }

    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { snackbarHostState.showSnackbar(it) }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Receive Stock") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        PurchaseFormContent(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseFormContent(
    uiState: com.bakeflow.app.purchase.PurchaseFormUiState,
    viewModel: PurchaseFormViewModel,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true
) {
    Column(modifier = modifier) {
        if (showTitle) {
            Text(
                text = "Receive Stock",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        IngredientDropdown(
            ingredients = uiState.ingredients,
            selectedId = uiState.selectedIngredientId,
            error = uiState.ingredientError,
            onSelect = viewModel::onIngredientSelected
        )

        RecentItemsRow(
            title = "Recent ingredients",
            items = uiState.ingredients.take(3).map { it.id },
            onItemClick = viewModel::onIngredientSelected,
            itemLabel = { id -> uiState.ingredients.find { it.id == id }?.name ?: id },
            modifier = Modifier.padding(top = 8.dp)
        )

        RecentItemsRow(
            title = "Recent suppliers",
            items = uiState.recentSuppliers,
            onItemClick = viewModel::onSupplierChange,
            modifier = Modifier.padding(top = 8.dp)
        )

        QuantityStepper(
            value = uiState.quantityText.ifBlank { "1" },
            onValueChange = viewModel::onQuantityChange,
            label = "Quantity purchased",
            error = uiState.quantityError,
            modifier = Modifier.padding(top = 8.dp)
        )

        OutlinedTextField(
            value = uiState.unit,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        FormField(
            value = uiState.costPerUnitText,
            onValueChange = viewModel::onCostPerUnitChange,
            label = "Cost per unit",
            error = uiState.costError,
            keyboardType = KeyboardType.Decimal
        )

        FormField(
            value = uiState.supplierName,
            onValueChange = viewModel::onSupplierChange,
            label = "Supplier (optional)"
        )

        FormField(
            value = uiState.invoiceNumber,
            onValueChange = viewModel::onInvoiceChange,
            label = "Invoice number (optional)"
        )

        FormField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = "Notes (optional)"
        )

        Text(
            text = "Total cost: ${formatMoney(uiState.totalCost)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = viewModel::savePurchase,
            enabled = !uiState.isSaving && uiState.ingredients.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) {
                CircularProgressIndicator()
            } else {
                Text("Save Purchase")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IngredientDropdown(
    ingredients: List<com.bakeflow.app.domain.model.Ingredient>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = ingredients.find { it.id == selectedId }?.name ?: "Select ingredient"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier.fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Ingredient") },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            ingredients.forEach { ingredient ->
                DropdownMenuItem(
                    text = { Text(ingredient.name) },
                    onClick = {
                        onSelect(ingredient.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.large,
        singleLine = true
    )
}

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
