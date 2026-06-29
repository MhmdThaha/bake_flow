package com.bakeflow.app.production.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.common.UiState
import com.bakeflow.app.domain.model.ProductionBatch
import com.bakeflow.app.production.ProductionHistoryViewModel
import com.bakeflow.app.ui.components.BakeFlowSearchBar
import com.bakeflow.app.ui.components.EmptyState
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import com.bakeflow.app.ui.components.SpreadsheetCellText
import com.bakeflow.app.ui.components.SpreadsheetColumn
import com.bakeflow.app.ui.components.SpreadsheetTable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val historyColumns = listOf(
    SpreadsheetColumn("Product", weight = 1.2f, minWidth = 100.dp),
    SpreadsheetColumn("Qty", weight = 0.5f, minWidth = 48.dp),
    SpreadsheetColumn("Cost", weight = 0.7f, minWidth = 64.dp),
    SpreadsheetColumn("Status", weight = 0.7f, minWidth = 72.dp),
    SpreadsheetColumn("Date", weight = 0.7f, minWidth = 64.dp)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductionHistoryScreen(
    viewModel: ProductionHistoryViewModel,
    onNavigateBack: () -> Unit,
    onBatchClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearSnackbar()
        }
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Production History") },
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
                .padding(horizontal = 12.dp)
        ) {
            BakeFlowSearchBar(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                placeholder = "Search batches…",
                modifier = Modifier.padding(vertical = 12.dp)
            )
            when (val state = uiState.batchesState) {
                UiState.Loading -> LoadingState(message = "Loading history…")
                is UiState.Error -> Column {
                    ErrorState(message = state.message)
                    TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
                }
                UiState.Empty -> EmptyState(
                    title = "No production yet",
                    message = "Start production to see batch history here.",
                    modifier = Modifier.fillMaxSize()
                )
                is UiState.Success -> {
                    if (uiState.filteredBatches.isEmpty()) {
                        EmptyState(title = "No matches", message = "Try another search.")
                    } else {
                        SpreadsheetTable(
                            columns = historyColumns,
                            rows = uiState.filteredBatches,
                            rowKey = { it.batchId },
                            onRowClick = { onBatchClick(it.batchId) },
                            modifier = Modifier.fillMaxSize()
                        ) { columnIndex, batch ->
                            HistoryCell(columnIndex, batch)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryCell(columnIndex: Int, batch: ProductionBatch) {
    when (columnIndex) {
        0 -> SpreadsheetCellText(batch.productName, emphasized = true)
        1 -> SpreadsheetCellText(formatQty(batch.quantityProduced))
        2 -> SpreadsheetCellText(formatMoney(batch.estimatedCost))
        3 -> SpreadsheetCellText(batch.status.displayName)
        else -> SpreadsheetCellText(formatDate(batch.createdAt))
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(millis))

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.1f", value)

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
