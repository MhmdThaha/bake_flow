package com.bakeflow.app.sales.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PointOfSale
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.PaymentMethod
import com.bakeflow.app.domain.model.Sale
import com.bakeflow.app.domain.model.SalesSummary
import com.bakeflow.app.ui.components.QuantityStepper
import com.bakeflow.app.ui.components.RecentItemsRow
import com.bakeflow.app.sales.SaleFormViewModel
import com.bakeflow.app.sales.SalesListViewModel
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

private val salesColumns = listOf(
    SpreadsheetColumn("Product", weight = 1.2f, minWidth = 100.dp),
    SpreadsheetColumn("Qty", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Total", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Payment", weight = 0.8f, minWidth = 72.dp),
    SpreadsheetColumn("Date", weight = 0.7f, minWidth = 64.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    listViewModel: SalesListViewModel,
    formViewModel: SaleFormViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by listViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var showSaleSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            listViewModel.clearSnackbar()
        }
    }

    if (showSaleSheet) {
        SaleFormBottomSheet(
            viewModel = formViewModel,
            onDismiss = { showSaleSheet = false },
            onSaved = {
                listViewModel.showSnackbar("Sale saved — stock updated")
                showSaleSheet = false
            }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showSaleSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.PointOfSale, contentDescription = "New sale")
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
                text = "Sales",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            SalesSummaryRow(summary = uiState.summary)
            BakeFlowSearchBar(
                value = uiState.searchQuery,
                onValueChange = listViewModel::onSearchQueryChange,
                placeholder = "Search sales…",
                modifier = Modifier.padding(vertical = 8.dp)
            )
            when (val state = uiState.salesState) {
                UiState.Loading -> LoadingState(message = "Loading sales…")
                is UiState.Error -> Column {
                    ErrorState(message = state.message)
                    TextButton(onClick = listViewModel::retryLoad) { Text("Try Again") }
                }
                UiState.Empty -> EmptyState(
                    title = "No sales yet",
                    message = "Tap + to record a sale — price and total are calculated for you.",
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Success -> {
                    if (uiState.filteredSales.isEmpty()) {
                        EmptyState(
                            title = "No matches",
                            message = "Try another search.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpreadsheetTable(
                            columns = salesColumns,
                            rows = uiState.filteredSales,
                            rowKey = { it.saleId },
                            modifier = Modifier.fillMaxSize()
                        ) { columnIndex, sale ->
                            SalesCell(columnIndex = columnIndex, sale = sale)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SalesSummaryRow(summary: SalesSummary) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        MetricCard(
            title = "Today's Sales",
            value = summary.todayCount.toString(),
            icon = Icons.Default.PointOfSale,
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            title = "Today's Revenue",
            value = formatMoney(summary.todayRevenue),
            subtitle = "Best: ${summary.bestSellingProductName}",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SalesCell(columnIndex: Int, sale: Sale) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(sale.productName, emphasized = true)
        1 -> SpreadsheetCellText(formatQty(sale.quantity))
        2 -> SpreadsheetCellText(formatMoney(sale.totalAmount))
        3 -> SpreadsheetCellText(sale.paymentMethod.displayName)
        else -> SpreadsheetCellText(formatDate(sale.saleDate))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SaleFormBottomSheet(
    viewModel: SaleFormViewModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    LaunchedEffect(uiState.saveSucceeded) {
        if (uiState.saveSucceeded) {
            viewModel.clearSaveSucceeded()
            onSaved()
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        SaleFormContent(
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
fun SaleFormContent(
    uiState: com.bakeflow.app.sales.SaleFormUiState,
    viewModel: SaleFormViewModel,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "New Sale",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary
        )

        ProductDropdown(
            products = uiState.products,
            selectedId = uiState.selectedProductId,
            error = uiState.productError,
            onSelect = viewModel::onProductSelected
        )

        RecentItemsRow(
            title = "Recent products",
            items = uiState.products.take(3).map { it.id },
            onItemClick = viewModel::onProductSelected,
            itemLabel = { id -> uiState.products.find { it.id == id }?.name ?: id },
            modifier = Modifier.padding(top = 8.dp)
        )

        StockSummaryRow(
            available = uiState.availableStock,
            sold = uiState.soldQuantity,
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

        OutlinedTextField(
            value = uiState.unitPriceText,
            onValueChange = {},
            readOnly = true,
            label = { Text("Unit price (auto)") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 8.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )

        Text(
            text = "Total: ${formatMoney(uiState.totalAmount)}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(top = 12.dp)
        )

        PaymentMethodDropdown(
            selected = uiState.paymentMethod,
            onSelect = viewModel::onPaymentMethodChange
        )

        OptionalField(value = uiState.customerName, onValueChange = viewModel::onCustomerNameChange, label = "Customer (optional)")
        OptionalField(value = uiState.customerPhone, onValueChange = viewModel::onCustomerPhoneChange, label = "Phone (optional)")
        OptionalField(value = uiState.notes, onValueChange = viewModel::onNotesChange, label = "Notes (optional)")

        uiState.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }

        Button(
            onClick = viewModel::saveSale,
            enabled = !uiState.isSaving &&
                uiState.products.isNotEmpty() &&
                !uiState.hasInsufficientStock,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .padding(top = 16.dp),
            shape = MaterialTheme.shapes.large
        ) {
            if (uiState.isSaving) CircularProgressIndicator() else Text("Save Sale")
        }
    }
}

@Composable
private fun StockSummaryRow(
    available: Double,
    sold: Double,
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
        StockChip("Sold", formatQty(sold))
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
private fun PaymentMethodDropdown(
    selected: PaymentMethod,
    onSelect: (PaymentMethod) -> Unit
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
            label = { Text("Payment") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            PaymentMethod.entries.forEach { method ->
                DropdownMenuItem(
                    text = { Text(method.displayName) },
                    onClick = {
                        onSelect(method)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
private fun OptionalField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(top = 8.dp),
        shape = MaterialTheme.shapes.large,
        singleLine = true
    )
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
