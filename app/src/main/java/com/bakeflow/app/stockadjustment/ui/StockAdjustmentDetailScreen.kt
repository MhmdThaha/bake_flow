package com.bakeflow.app.stockadjustment.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bakeflow.app.domain.model.StockAdjustment
import com.bakeflow.app.stockadjustment.StockAdjustmentDetailViewModel
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockAdjustmentDetailScreen(
    viewModel: StockAdjustmentDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Adjustment Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingState(
                message = "Loading adjustment…",
                modifier = Modifier.padding(innerPadding)
            )
            uiState.errorMessage != null -> Column(modifier = Modifier.padding(innerPadding)) {
                ErrorState(message = uiState.errorMessage!!)
                TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
            }
            uiState.adjustment != null -> StockAdjustmentDetailContent(
                adjustment = uiState.adjustment!!,
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            )
        }
    }
}

@Composable
private fun StockAdjustmentDetailContent(
    adjustment: StockAdjustment,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Item", adjustment.itemName, emphasized = true)
                DetailRow("Type", adjustment.itemType.displayName)
                DetailRow("Previous stock", formatQty(adjustment.previousQuantity))
                DetailRow("Adjusted to", formatQty(adjustment.adjustedQuantity), emphasized = true)
                DetailRow("Difference", formatSignedQty(adjustment.difference))
                DetailRow("Reason", adjustment.adjustmentReason.displayName)
                DetailRow("Adjustment date", formatDate(adjustment.adjustmentDate))
                DetailRow("Notes", adjustment.notes.ifBlank { "—" })
                DetailRow("Recorded", formatDate(adjustment.createdAt))
                DetailRow("Created by", adjustment.createdBy.ifBlank { "—" })
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    emphasized: Boolean = false
) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            style = if (emphasized) MaterialTheme.typography.titleLarge else MaterialTheme.typography.bodyLarge,
            fontWeight = if (emphasized) FontWeight.Bold else FontWeight.Normal
        )
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

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
