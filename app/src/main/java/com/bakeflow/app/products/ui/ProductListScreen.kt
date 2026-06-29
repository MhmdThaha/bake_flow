package com.bakeflow.app.products.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.Product
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.products.ProductListViewModel
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.SpreadsheetCellText
import com.bakeflow.app.ui.components.SpreadsheetColumn
import com.bakeflow.app.ui.components.SpreadsheetTable
import java.util.Locale

private val productColumns = listOf(
    SpreadsheetColumn("Product", weight = 1.2f, minWidth = 110.dp),
    SpreadsheetColumn("Category", weight = 0.9f, minWidth = 90.dp),
    SpreadsheetColumn("Price", weight = 0.7f, minWidth = 72.dp),
    SpreadsheetColumn("Ready", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Status", weight = 0.7f, minWidth = 72.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: ProductListViewModel,
    productRepository: ProductRepository,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var sheetProductId by remember { mutableStateOf<String?>(null) }
    var showAddSheet by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSnackbar()
        }
    }

    if (showAddSheet) {
        ProductEditBottomSheet(
            productId = null,
            productRepository = productRepository,
            onDismiss = { showAddSheet = false },
            onSaved = { showAddSheet = false },
            onDeleted = { showAddSheet = false }
        )
    }
    sheetProductId?.let { productId ->
        ProductEditBottomSheet(
            productId = productId,
            productRepository = productRepository,
            onDismiss = { sheetProductId = null },
            onSaved = { sheetProductId = null },
            onDeleted = { sheetProductId = null }
        )
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddSheet = true },
                shape = MaterialTheme.shapes.large
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add product")
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
                text = "Products",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 12.dp)
            )
            BakeFlowSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = "Search products…"
            )
            when (val state = uiState.productsState) {
                UiState.Loading -> LoadingState(message = "Loading products…")
                is UiState.Error -> Column(modifier = Modifier.fillMaxSize()) {
                    ErrorState(message = state.message, modifier = Modifier.weight(1f))
                    TextButton(
                        onClick = viewModel::retryLoad,
                        modifier = Modifier.fillMaxSize()
                    ) { Text("Try Again") }
                }
                UiState.Empty -> EmptyState(
                    title = "No products yet",
                    message = "Tap + to add your first item — like adding a row in a sheet.",
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Success -> {
                    if (uiState.filteredProducts.isEmpty()) {
                        EmptyState(
                            title = "No matches",
                            message = "Try a different search.",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        SpreadsheetTable(
                            columns = productColumns,
                            rows = uiState.filteredProducts,
                            rowKey = { it.id },
                            onRowClick = { sheetProductId = it.id },
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 12.dp)
                        ) { columnIndex, product ->
                            ProductSpreadsheetCell(columnIndex, product)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductSpreadsheetCell(columnIndex: Int, product: Product) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(text = product.name, emphasized = true)
        1 -> SpreadsheetCellText(text = product.category.ifBlank { "—" })
        2 -> SpreadsheetCellText(
            text = String.format(Locale.getDefault(), "$%.2f", product.sellingPrice)
        )
        3 -> SpreadsheetCellText(text = formatQty(product.finishedStock))
        else -> SpreadsheetCellText(text = product.status.displayName)
    }
}

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)
