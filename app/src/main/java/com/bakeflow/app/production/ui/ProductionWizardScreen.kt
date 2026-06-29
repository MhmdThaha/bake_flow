package com.bakeflow.app.production.ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.production.ProductionCalculator
import com.bakeflow.app.production.ProductionWizardStep
import com.bakeflow.app.production.ProductionWizardViewModel
import com.bakeflow.app.ui.components.RecentItemsRow
import com.bakeflow.app.ui.components.LoadingState
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionWizardScreen(
    viewModel: ProductionWizardViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = rememberSnackbarHostState()

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) onNavigateBack()
    }
    LaunchedEffect(uiState.errorMessage, uiState.successMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Start Production") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
            ) {
            WizardStepHeader(currentStep = uiState.currentStep)
            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.weight(1f)) {
            when (uiState.currentStep) {
                ProductionWizardStep.CHOOSE_PRODUCT -> ChooseProductStep(
                    searchQuery = uiState.productSearchQuery,
                    onSearchChange = viewModel::onProductSearchChange,
                    products = viewModel.filteredProducts(),
                    selectedProductId = uiState.selectedProductId,
                    onProductSelected = viewModel::selectProduct,
                    productError = uiState.productError
                )
                ProductionWizardStep.ENTER_QUANTITY -> QuantityStep(
                    productName = viewModel.selectedProductName(),
                    quantity = uiState.quantityText,
                    onQuantityChange = viewModel::onQuantityChange,
                    quantityError = uiState.quantityError,
                    isCalculating = uiState.isCalculating
                )
                ProductionWizardStep.REVIEW_REQUIREMENTS -> RequirementsStep(
                    lines = uiState.requirementLines,
                    isCalculating = uiState.isCalculating
                )
                ProductionWizardStep.CONFIRM -> ConfirmStep(
                    productName = viewModel.selectedProductName(),
                    quantity = uiState.quantityText.toDoubleOrNull() ?: 0.0,
                    lines = uiState.requirementLines,
                    isExecuting = uiState.isExecuting
                )
            }
            }

            Spacer(modifier = Modifier.height(16.dp))
            WizardNavigationButtons(
                currentStep = uiState.currentStep,
                onBack = viewModel::previousStep,
                onNext = viewModel::nextStep,
                onConfirm = viewModel::confirmProduction,
                isBusy = uiState.isCalculating || uiState.isExecuting,
                canConfirm = uiState.requirementLines.none { it.hasShortage }
            )
        }
    }
}

@Composable
private fun rememberSnackbarHostState(): SnackbarHostState =
    androidx.compose.runtime.remember { SnackbarHostState() }

@Composable
private fun WizardStepHeader(currentStep: ProductionWizardStep) {
    Column {
        LinearProgressIndicator(
            progress = { currentStep.stepNumber / 4f },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Step ${currentStep.stepNumber} of 4 — ${currentStep.title}",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ChooseProductStep(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    products: List<com.bakeflow.app.production.ProductWithRecipe>,
    selectedProductId: String?,
    onProductSelected: (String) -> Unit,
    productError: String?
) {
    OutlinedTextField(
        value = searchQuery,
        onValueChange = onSearchChange,
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        placeholder = { Text("Search products with recipes") },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        shape = MaterialTheme.shapes.large
    )
    productError?.let {
        Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
    }
    if (products.isNotEmpty()) {
        RecentItemsRow(
            title = "Recently produced",
            items = products.take(3).map { it.product.id },
            onItemClick = onProductSelected,
            itemLabel = { id -> products.find { it.product.id == id }?.product?.name ?: id },
            modifier = Modifier.padding(top = 12.dp)
        )
    }
    if (products.isEmpty()) {
        Text(
            "No products with recipes. Create a recipe first.",
            modifier = Modifier.padding(top = 16.dp)
        )
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 12.dp)
        ) {
            items(products, key = { it.product.id }) { entry ->
                val selected = entry.product.id == selectedProductId
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProductSelected(entry.product.id) },
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(
                        containerColor = if (selected) {
                            MaterialTheme.colorScheme.primaryContainer
                        } else {
                            MaterialTheme.colorScheme.surfaceVariant
                        }
                    )
                ) {
                    Text(
                        text = entry.product.name,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun QuantityStep(
    productName: String,
    quantity: String,
    onQuantityChange: (String) -> Unit,
    quantityError: String?,
    isCalculating: Boolean
) {
    Text(productName, style = MaterialTheme.typography.headlineSmall)
    Spacer(modifier = Modifier.height(12.dp))
    OutlinedTextField(
        value = quantity,
        onValueChange = onQuantityChange,
        label = { Text("Quantity to produce") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = quantityError != null,
        supportingText = quantityError?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = MaterialTheme.shapes.large,
        singleLine = true
    )
    if (isCalculating) {
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun RequirementsStep(
    lines: List<com.bakeflow.app.domain.model.ProductionRequirementLine>,
    isCalculating: Boolean
) {
    if (isCalculating) {
        LoadingState(message = "Calculating ingredients…")
        return
    }
    Text(
        "We calculate everything — you never do math.",
        style = MaterialTheme.typography.bodyLarge,
        modifier = Modifier.padding(bottom = 12.dp)
    )
    lines.forEach { line ->
        RequirementRow(line)
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
private fun RequirementRow(line: com.bakeflow.app.domain.model.ProductionRequirementLine) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(line.ingredientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Required: ${formatQty(line.requiredQuantity)} ${line.unit}")
        Text("Available: ${formatQty(line.availableQuantity)} ${line.unit}")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (line.hasShortage) Icons.Default.Close else Icons.Default.Check,
                contentDescription = null,
                tint = if (line.hasShortage) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
            Text(
                text = if (line.hasShortage) "Shortage" else "OK",
                color = if (line.hasShortage) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
private fun ConfirmStep(
    productName: String,
    quantity: Double,
    lines: List<com.bakeflow.app.domain.model.ProductionRequirementLine>,
    isExecuting: Boolean
) {
    val totalCost = ProductionCalculator.totalCost(lines)
    val costPerUnit = ProductionCalculator.costPerProduct(totalCost, quantity)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Product", style = MaterialTheme.typography.labelLarge)
            Text(productName, style = MaterialTheme.typography.titleLarge)
            Text("Quantity: ${formatQty(quantity)}")
            Text("Ingredient cost: ${formatMoney(totalCost)}")
            Text("Estimated batch cost: ${formatMoney(totalCost)}", fontWeight = FontWeight.Bold)
            Text("Cost per product: ${formatMoney(costPerUnit)}")
        }
    }
    if (isExecuting) {
        CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
private fun WizardNavigationButtons(
    currentStep: ProductionWizardStep,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onConfirm: () -> Unit,
    isBusy: Boolean,
    canConfirm: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (currentStep != ProductionWizardStep.CHOOSE_PRODUCT) {
            OutlinedButton(
                onClick = onBack,
                enabled = !isBusy,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Back") }
        }
        when (currentStep) {
            ProductionWizardStep.CONFIRM -> Button(
                onClick = onConfirm,
                enabled = !isBusy && canConfirm,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large
            ) { Text("Confirm Production") }
            else -> Button(
                onClick = onNext,
                enabled = !isBusy,
                modifier = Modifier.weight(1f).heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Text(
                    when (currentStep) {
                        ProductionWizardStep.ENTER_QUANTITY -> "Calculate"
                        ProductionWizardStep.REVIEW_REQUIREMENTS -> "Review"
                        else -> "Next"
                    }
                )
            }
        }
    }
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.2f", value)

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
