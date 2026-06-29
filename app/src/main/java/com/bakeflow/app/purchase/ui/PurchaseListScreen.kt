package com.bakeflow.app.purchase.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.domain.model.PurchaseSummary
import com.bakeflow.app.purchase.PurchaseListViewModel
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

private val purchaseColumns = listOf(
    SpreadsheetColumn("Ingredient", weight = 1.2f, minWidth = 100.dp),
    SpreadsheetColumn("Supplier", weight = 1f, minWidth = 90.dp),
    SpreadsheetColumn("Qty", weight = 0.6f, minWidth = 56.dp),
    SpreadsheetColumn("Total", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Date", weight = 0.7f, minWidth = 64.dp)
)

@Composable
fun PurchaseListScreen(
    viewModel: PurchaseListViewModel,
    onReceiveStock: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    embedded: Boolean = false
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .then(if (embedded) Modifier else Modifier.padding(horizontal = 12.dp))
    ) {
        if (!embedded) {
            Text(
                text = "Purchases",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        PurchaseListContent(
            uiState = uiState,
            viewModel = viewModel,
            onPurchaseClick = onPurchaseClick,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun PurchaseListContent(
    uiState: com.bakeflow.app.purchase.PurchaseListUiState,
    viewModel: PurchaseListViewModel,
    onPurchaseClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        PurchaseSummaryRow(summary = uiState.summary)
        BakeFlowSearchBar(
            value = uiState.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            placeholder = "Search purchases…",
            modifier = Modifier.padding(vertical = 8.dp)
        )
        PurchaseFilterRow(
            ingredientFilter = uiState.ingredientFilter,
            supplierFilter = uiState.supplierFilter,
            ingredientOptions = uiState.availableIngredientFilters,
            supplierOptions = uiState.availableSupplierFilters,
            onIngredientFilterChange = viewModel::onIngredientFilterChange,
            onSupplierFilterChange = viewModel::onSupplierFilterChange
        )
        when (val state = uiState.purchasesState) {
            UiState.Loading -> LoadingState(message = "Loading purchases…")
            is UiState.Error -> Column {
                ErrorState(message = state.message)
                TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
            }
            UiState.Empty -> EmptyState(
                title = "No purchases logged",
                message = "Tap the truck icon to receive stock — stock updates automatically.",
                modifier = Modifier.fillMaxSize()
            )
            is UiState.Success -> {
                if (uiState.filteredPurchases.isEmpty()) {
                    EmptyState(
                        title = "No matches",
                        message = "Try another search or filter.",
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    SpreadsheetTable(
                        columns = purchaseColumns,
                        rows = uiState.filteredPurchases,
                        rowKey = { it.purchaseId },
                        onRowClick = { onPurchaseClick(it.purchaseId) },
                        modifier = Modifier.fillMaxSize()
                    ) { columnIndex, purchase ->
                        PurchaseCell(columnIndex = columnIndex, purchase = purchase)
                    }
                }
            }
        }
    }
}

@Composable
private fun PurchaseSummaryRow(summary: PurchaseSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Today's Purchases",
            value = summary.todayCount.toString(),
            subtitle = formatMoney(summary.todayTotalCost),
            icon = Icons.Default.LocalShipping,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseFilterRow(
    ingredientFilter: String?,
    supplierFilter: String?,
    ingredientOptions: List<String>,
    supplierOptions: List<String>,
    onIngredientFilterChange: (String?) -> Unit,
    onSupplierFilterChange: (String?) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterDropdown(
            label = "Ingredient",
            value = ingredientFilter ?: "All",
            options = listOf("All") + ingredientOptions,
            onSelect = { selected ->
                onIngredientFilterChange(if (selected == "All") null else selected)
            },
            modifier = Modifier.weight(1f)
        )
        FilterDropdown(
            label = "Supplier",
            value = supplierFilter ?: "All",
            options = listOf("All") + supplierOptions,
            onSelect = { selected ->
                onSupplierFilterChange(if (selected == "All") null else selected)
            },
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FilterDropdown(
    label: String,
    value: String,
    options: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            singleLine = true
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

@Composable
private fun PurchaseCell(columnIndex: Int, purchase: Purchase) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(purchase.ingredientName, emphasized = true)
        1 -> SpreadsheetCellText(purchase.supplierName.ifBlank { "—" })
        2 -> SpreadsheetCellText(formatQty(purchase.quantity, purchase.unit))
        3 -> SpreadsheetCellText(formatMoney(purchase.totalCost))
        else -> SpreadsheetCellText(formatDate(purchase.purchaseDate))
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

private fun formatQty(quantity: Double, unit: String): String {
    val qty = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else String.format("%.1f", quantity)
    return if (unit.isBlank()) qty else "$qty $unit"
}

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
