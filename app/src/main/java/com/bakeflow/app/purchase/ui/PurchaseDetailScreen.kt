package com.bakeflow.app.purchase.ui

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
import com.bakeflow.app.domain.model.Purchase
import com.bakeflow.app.purchase.PurchaseDetailViewModel
import com.bakeflow.app.ui.components.ErrorState
import com.bakeflow.app.ui.components.LoadingState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchaseDetailScreen(
    viewModel: PurchaseDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Purchase Details") },
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
                message = "Loading purchase…",
                modifier = Modifier.padding(innerPadding)
            )
            uiState.errorMessage != null -> Column(modifier = Modifier.padding(innerPadding)) {
                ErrorState(message = uiState.errorMessage!!)
                TextButton(onClick = viewModel::retryLoad) { Text("Try Again") }
            }
            uiState.purchase != null -> PurchaseDetailContent(
                purchase = uiState.purchase!!,
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
private fun PurchaseDetailContent(
    purchase: Purchase,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DetailRow("Ingredient", purchase.ingredientName, emphasized = true)
                DetailRow("Supplier", purchase.supplierName.ifBlank { "—" })
                DetailRow("Quantity", formatQty(purchase.quantity, purchase.unit))
                DetailRow("Cost per unit", formatMoney(purchase.costPerUnit))
                DetailRow("Total", formatMoney(purchase.totalCost), emphasized = true)
                DetailRow("Purchase date", formatDate(purchase.purchaseDate))
                DetailRow("Invoice", purchase.invoiceNumber.ifBlank { "—" })
                DetailRow("Notes", purchase.notes.ifBlank { "—" })
                DetailRow("Recorded", formatDate(purchase.createdAt))
                DetailRow("Created by", purchase.createdBy.ifBlank { "—" })
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

private fun formatQty(quantity: Double, unit: String): String {
    val qty = if (quantity % 1.0 == 0.0) quantity.toLong().toString() else String.format("%.2f", quantity)
    return if (unit.isBlank()) qty else "$qty $unit"
}

private fun formatMoney(value: Double): String =
    String.format(Locale.getDefault(), "$%.2f", value)
