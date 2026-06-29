package com.bakeflow.app.waste.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Waste
import com.bakeflow.app.domain.model.WasteReason
import com.bakeflow.app.domain.model.WasteSummary
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.MetricCard
import com.bakeflow.app.ui.components.QuantityStepper
import com.bakeflow.app.ui.components.RecentItemsRow
import com.bakeflow.app.ui.components.SpreadsheetCellText
import com.bakeflow.app.ui.components.SpreadsheetColumn
import com.bakeflow.app.ui.components.SpreadsheetTable
import com.bakeflow.app.waste.WasteFormUiState
import com.bakeflow.app.waste.WasteFormViewModel
import com.bakeflow.app.waste.WasteListViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val wasteColumns = listOf(
    SpreadsheetColumn("Product", weight = 1.2f, minWidth = 100.dp),
    SpreadsheetColumn("Qty", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Reason", weight = 0.9f, minWidth = 72.dp),
    SpreadsheetColumn("Loss", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Date", weight = 0.7f, minWidth = 64.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteScreen(
    listViewModel: WasteListViewModel,
    formViewModel: WasteFormViewModel,
    onWasteClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showWasteSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            listViewModel.clearSnackbar()
        }
    }

    if (showWasteSheet) {
        WasteFormBottomSheet(
            viewModel = formViewModel,
            onDismiss = { showWasteSheet = false },
            onSaved = {
                listViewModel.showSnackbar("Waste recorded — stock updated")
                showWasteSheet = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showWasteSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Record waste")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = "Waste",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            WasteSummaryRow(summary = uiState.summary)
            BakeFlowSearchBar(
                value = uiState.searchQuery,
                onValueChange = listViewModel::onSearchQueryChange,
                placeholder = "Search waste…",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            when (val state = uiState.wasteState) {
                UiState.Loading -> LoadingState(message = "Loading waste history…")
                is UiState.Error -> Column {
                    ErrorState(message = state.message)
                    TextButton(onClick = listViewModel::retryLoad) { Text("Try Again") }
                }
                UiState.Empty -> EmptyState(
                    title = "No waste recorded",
                    message = "Tap + to log spoiled, expired, or damaged products.",
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Success -> {
                    if (uiState.filteredWaste.isEmpty()) {
                        EmptyState(
                            title = "No matches",
                            message = "Try another search.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpreadsheetTable(
                            columns = wasteColumns,
                            rows = uiState.filteredWaste,
                            rowKey = { it.wasteId },
                            onRowClick = { onWasteClick(it.wasteId) },
                            modifier = Modifier.fillMaxSize()
                        ) { columnIndex, waste ->
                            WasteCell(columnIndex = columnIndex, waste = waste)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WasteSummaryRow(summary: WasteSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Today's Waste",
            value = formatQty(summary.todayQuantity),
            subtitle = "${summary.todayCount} entries",
            icon = Icons.Default.Delete,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Estimated Loss",
            value = formatMoney(summary.todayEstimatedLoss),
            subtitle = "Today",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun WasteCell(columnIndex: Int, waste: Waste) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(waste.productName, emphasized = true)
        1 -> SpreadsheetCellText(formatQty(waste.quantity))
        2 -> SpreadsheetCellText(waste.reason.displayName)
        3 -> SpreadsheetCellText(formatMoney(waste.estimatedLoss))
        else -> SpreadsheetCellText(formatDate(waste.wasteDate))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteFormBottomSheet(
    viewModel: WasteFormViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) {
            viewModel.clearSaveSucceeded()
            viewModel.resetForm()
            onSaved()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        WasteFormContent(
            uiState = uiState,
            viewModel = viewModel,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WasteFormContent(
    uiState: WasteFormUiState,
    viewModel: WasteFormViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Record Waste",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        ProductDropdown(
            products = uiState.products,
            selectedId = uiState.selectedProductId,
            error = uiState.productError,
            onSelect = viewModel::onProductSelected
        )

        if (uiState.products.isEmpty()) {
            Text(
                text = "No products with finished stock. Produce items first.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        RecentItemsRow(
            title = "Recent products",
            items = uiState.products.take(3).map { it.id },
            onItemClick = viewModel::onProductSelected,
            itemLabel = { id -> uiState.products.find { it.id == id }?.name ?: id },
            modifier = Modifier.padding(top = 8.dp)
        )

        StockSummaryRow(
            available = uiState.availableStock,
            wasted = uiState.wasteQuantity,
            remaining = uiState.remainingStock,
            hasInsufficientStock = uiState.hasInsufficientStock
        )

        QuantityStepper(
            value = uiState.quantityText,
            onValueChange = viewModel::onQuantityChange,
            label = "Quantity",
            error = uiState.quantityError,
            modifier = Modifier.padding(top = 8.dp)
        )

        ReasonDropdown(
            selected = uiState.reason,
            onSelect = viewModel::onReasonChange
        )

        Text(
            text = "Estimated loss: ${formatMoney(uiState.estimatedLoss)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(top = 12.dp)
        )

        OutlinedTextField(
            value = uiState.notes,
            onValueChange = viewModel::onNotesChange,
            label = { Text("Notes (optional)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = false,
            maxLines = 3
        )

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = viewModel::saveWaste,
            enabled = !uiState.isSaving &&
                uiState.products.isNotEmpty() &&
                !uiState.hasInsufficientStock,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) CircularProgressIndicator() else Text("Save Waste")
        }
    }
}

@Composable
private fun StockSummaryRow(
    available: Double,
    wasted: Double,
    remaining: Double,
    hasInsufficientStock: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        StockChip("Available", formatQty(available))
        StockChip("Waste", formatQty(wasted))
        StockChip(
            label = "Remaining",
            value = formatQty(remaining),
            emphasized = hasInsufficientStock
        )
    }
}

@Composable
private fun StockChip(label: String, value: String, emphasized: Boolean = false) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall)
        Text(
            value,
            fontWeight = FontWeight.SemiBold,
            color = if (emphasized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductDropdown(
    products: List<com.bakeflow.app.domain.model.Product>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = products.find { it.id == selectedId }
    val label = selected?.let { "${it.name} (${formatQty(it.finishedStock)} ready)" } ?: "Select product"

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Product") },
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
            products.forEach { product ->
                DropdownMenuItem(
                    text = {
                        Text("${product.name} — ${formatQty(product.finishedStock)} in stock")
                    },
                    onClick = {
                        onSelect(product.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonDropdown(
    selected: WasteReason,
    onSelect: (WasteReason) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        OutlinedTextField(
            value = selected.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Reason") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            WasteReason.entries.forEach { reason ->
                DropdownMenuItem(
                    text = { Text(reason.displayName) },
                    onClick = {
                        onSelect(reason)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
