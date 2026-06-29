package com.bakeflow.app.stockadjustment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.AdjustmentItemType
import com.bakeflow.app.domain.model.AdjustmentReason
import com.bakeflow.app.domain.model.Ingredient
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.domain.model.StockAdjustmentSummary
import com.bakeflow.app.stockadjustment.StockAdjustmentFormUiState
import com.bakeflow.app.stockadjustment.StockAdjustmentFormViewModel
import com.bakeflow.app.stockadjustment.StockAdjustmentListViewModel
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.MetricCard
import com.bakeflow.app.ui.components.SpreadsheetCellText
import com.bakeflow.app.ui.components.SpreadsheetColumn
import com.bakeflow.app.ui.components.SpreadsheetTable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val adjustmentColumns = listOf(
    SpreadsheetColumn("Item", weight = 1.2f, minWidth = 100.dp),
    SpreadsheetColumn("Type", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Was", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Now", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Diff", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Reason", weight = 0.9f, minWidth = 72.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentScreen(
    listViewModel: StockAdjustmentListViewModel,
    formViewModel: StockAdjustmentFormViewModel,
    onAdjustmentClick: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showAdjustSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            listViewModel.clearSnackbar()
        }
    }

    if (showAdjustSheet) {
        StockAdjustmentFormBottomSheet(
            viewModel = formViewModel,
            onDismiss = { showAdjustSheet = false },
            onSaved = {
                listViewModel.showSnackbar("Stock adjusted — inventory updated")
                showAdjustSheet = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Stock Adjustments") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAdjustSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Tune, contentDescription = "Adjust stock")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp)
        ) {
            AdjustmentSummaryRow(summary = uiState.summary)
            BakeFlowSearchBar(
                value = uiState.searchQuery,
                onValueChange = listViewModel::onSearchQueryChange,
                placeholder = "Search adjustments…",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            when (val state = uiState.adjustmentsState) {
                UiState.Loading -> LoadingState(message = "Loading adjustments…")
                is UiState.Error -> Column {
                    ErrorState(message = state.message)
                    TextButton(onClick = listViewModel::retryLoad) { Text("Try Again") }
                }
                UiState.Empty -> EmptyState(
                    title = "No adjustments yet",
                    message = "Tap + after a physical count to correct ingredient or product stock.",
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Success -> {
                    if (uiState.filteredAdjustments.isEmpty()) {
                        EmptyState(
                            title = "No matches",
                            message = "Try another search.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpreadsheetTable(
                            columns = adjustmentColumns,
                            rows = uiState.filteredAdjustments,
                            rowKey = { it.adjustmentId },
                            onRowClick = { onAdjustmentClick(it.adjustmentId) },
                            modifier = Modifier.fillMaxSize()
                        ) { columnIndex, adjustment ->
                            AdjustmentCell(columnIndex = columnIndex, adjustment = adjustment)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustmentSummaryRow(summary: StockAdjustmentSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Today's Adjustments",
            value = summary.todayCount.toString(),
            subtitle = "entries",
            icon = Icons.Default.Tune,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Net change today",
            value = formatSignedQty(summary.todayDifferenceTotal),
            subtitle = "sum of differences",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun AdjustmentCell(columnIndex: Int, adjustment: StockAdjustment) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(adjustment.itemName, emphasized = true)
        1 -> SpreadsheetCellText(adjustment.itemType.displayName)
        2 -> SpreadsheetCellText(formatQty(adjustment.previousQuantity))
        3 -> SpreadsheetCellText(formatQty(adjustment.adjustedQuantity))
        4 -> SpreadsheetCellText(formatSignedQty(adjustment.difference))
        else -> SpreadsheetCellText(adjustment.adjustmentReason.displayName)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentFormBottomSheet(
    viewModel: StockAdjustmentFormViewModel,
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
        StockAdjustmentFormContent(
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
fun StockAdjustmentFormContent(
    uiState: StockAdjustmentFormUiState,
    viewModel: StockAdjustmentFormViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "Adjust Stock",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AdjustmentItemType.entries.forEach { type ->
                FilterChip(
                    selected = uiState.itemType == type,
                    onClick = { viewModel.onItemTypeChange(type) },
                    label = { Text(type.displayName) }
                )
            }
        }

        ItemDropdown(
            itemType = uiState.itemType,
            ingredients = uiState.ingredients,
            products = uiState.products,
            selectedId = uiState.selectedItemId,
            error = uiState.itemError,
            onSelect = viewModel::onItemSelected
        )

        OutlinedTextField(
            value = formatQty(uiState.currentStock),
            onValueChange = {},
            readOnly = true,
            label = { Text("Current stock (system)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        OutlinedTextField(
            value = uiState.actualStockText,
            onValueChange = viewModel::onActualStockChange,
            label = { Text("Actual stock (counted)") },
            isError = uiState.quantityError != null || uiState.hasNegativeStock,
            supportingText = uiState.quantityError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        Text(
            text = "Difference: ${formatSignedQty(uiState.difference)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                uiState.difference > 0 -> MaterialTheme.colorScheme.primary
                uiState.difference < 0 -> MaterialTheme.colorScheme.error
                else -> MaterialTheme.colorScheme.onSurface
            },
            modifier = Modifier.padding(top = 12.dp)
        )

        ReasonDropdown(
            selected = uiState.reason,
            onSelect = viewModel::onReasonChange
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
            onClick = viewModel::saveAdjustment,
            enabled = !uiState.isSaving &&
                uiState.selectedItemId != null &&
                !uiState.hasNegativeStock &&
                (uiState.ingredients.isNotEmpty() || uiState.products.isNotEmpty()),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) CircularProgressIndicator() else Text("Save Adjustment")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDropdown(
    itemType: AdjustmentItemType,
    ingredients: List<Ingredient>,
    products: List<Product>,
    selectedId: String?,
    error: String?,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val label = when (itemType) {
        AdjustmentItemType.INGREDIENT -> {
            val item = ingredients.find { it.id == selectedId }
            item?.let { "${it.name} (${formatQty(it.currentStock)} ${it.unit.displayName})" }
                ?: "Select ingredient"
        }
        AdjustmentItemType.PRODUCT -> {
            val item = products.find { it.id == selectedId }
            item?.let { "${it.name} (${formatQty(it.finishedStock)} ready)" }
                ?: "Select product"
        }
    }

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
            label = { Text(if (itemType == AdjustmentItemType.INGREDIENT) "Ingredient" else "Product") },
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
            when (itemType) {
                AdjustmentItemType.INGREDIENT -> ingredients.forEach { ingredient ->
                    DropdownMenuItem(
                        text = {
                            Text("${ingredient.name} — ${formatQty(ingredient.currentStock)} ${ingredient.unit.displayName}")
                        },
                        onClick = {
                            onSelect(ingredient.id)
                            expanded = false
                        }
                    )
                }
                AdjustmentItemType.PRODUCT -> products.forEach { product ->
                    DropdownMenuItem(
                        text = {
                            Text("${product.name} — ${formatQty(product.finishedStock)} ready")
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReasonDropdown(
    selected: AdjustmentReason,
    onSelect: (AdjustmentReason) -> Unit
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
            AdjustmentReason.entries.forEach { reason ->
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

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun formatSignedQty(value: Double): String {
    val formatted = formatQty(kotlin.math.abs(value))
    return when {
        value > 0 -> "+$formatted"
        value < 0 -> "-$formatted"
        else -> formatted
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))
