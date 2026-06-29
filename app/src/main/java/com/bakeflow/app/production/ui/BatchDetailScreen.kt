package com.bakeflow.app.production.ui

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
import androidx.compose.material3.HorizontalDivider
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
import com.bakeflow.app.production.ProductionDetailViewModel
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatchDetailScreen(
    viewModel: ProductionDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Batch Details") },
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
                message = "Loading batch…",
                modifier = Modifier.padding(innerPadding)
            )
            uiState.errorMessage != null -> Column(modifier = Modifier.padding(innerPadding)) {
                ErrorState(message = uiState.errorMessage!!)
                TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
            }
            uiState.batch != null -> BatchDetailContent(
                batch = uiState.batch!!,
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
private fun BatchDetailContent(
    batch: com.bakeflow.app.domain.model.ProductionBatch,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Product", style = MaterialTheme.typography.labelLarge)
                Text(batch.productName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text("Quantity produced: ${formatQty(batch.quantityProduced)}")
                Text("Status: ${batch.status.displayName}")
                Text("Total cost: ${formatMoney(batch.estimatedCost)}", fontWeight = FontWeight.SemiBold)
                Text("Created: ${formatDate(batch.createdAt)}")
                batch.completedAt?.let { Text("Completed: ${formatDate(it)}") }
                Text("Created by: ${batch.createdBy}")
            }
        }
        Text("Ingredients consumed", style = MaterialTheme.typography.titleLarge)
        batch.ingredientUsage.forEach { usage ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(usage.ingredientName, fontWeight = FontWeight.SemiBold)
                Text("${formatQty(usage.requiredQuantity)} ${usage.unit}")
                Text("Cost: ${formatMoney(usage.totalCost)}")
            }
            HorizontalDivider()
        }
    }
}

private fun formatDate(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(millis))

private fun formatQty(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format(Locale.getDefault(), "%.2f", value)

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
