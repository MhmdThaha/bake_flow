package com.bakeflow.app.products.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.bakeflow.app.domain.model.ProductStatus
import com.bakeflow.app.domain.repository.ProductRepository
import com.bakeflow.app.products.ProductFormViewModel
import com.bakeflow.app.products.ProductFormViewModelFactory
import com.bakeflow.app.ui.components.LoadingState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductEditBottomSheet(
    productId: String?,
    productRepository: ProductRepository,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    onDeleted: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val formKey = productId ?: "new"
    val formViewModel: ProductFormViewModel = viewModel(
        key = "product_sheet_$formKey",
        factory = ProductFormViewModelFactory(productRepository, productId)
    )
    val formState by formViewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(formState.saveSucceeded) {
        if (formState.saveSucceeded) {
            onSaved()
            onDismiss()
        }
    }
    LaunchedEffect(formState.deleteSucceeded) {
        if (formState.deleteSucceeded) {
            onDeleted()
            onDismiss()
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = if (productId == null) "Add Product" else "Edit Product",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (formState.isInitialLoading) {
                LoadingState(message = "Loading…")
            } else {
                OutlinedTextField(
                    value = formState.name,
                    onValueChange = formViewModel::onNameChange,
                    label = { Text("Name") },
                    isError = formState.nameError != null,
                    supportingText = formState.nameError?.let { { Text(it) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = formState.category,
                    onValueChange = formViewModel::onCategoryChange,
                    label = { Text("Category") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = formState.sellingPrice,
                    onValueChange = formViewModel::onSellingPriceChange,
                    label = { Text("Price") },
                    isError = formState.priceError != null,
                    supportingText = formState.priceError?.let { { Text(it) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    singleLine = true,
                    shape = MaterialTheme.shapes.large
                )
                Spacer(modifier = Modifier.height(12.dp))
                ProductStatusDropdown(
                    status = formState.status,
                    onStatusChange = formViewModel::onStatusChange
                )
                formState.errorMessage?.let { message ->
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = formViewModel::saveProduct,
                    enabled = !formState.isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.large
                ) {
                    if (formState.isLoading) {
                        CircularProgressIndicator()
                    } else {
                        Text("Save", style = MaterialTheme.typography.titleMedium)
                    }
                }
                if (productId != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = formViewModel::deleteProduct,
                        enabled = !formState.isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 56.dp),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text("Delete", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProductStatusDropdown(
    status: ProductStatus,
    onStatusChange: (ProductStatus) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = status.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Status") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            ProductStatus.entries.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onStatusChange(option)
                        expanded = false
                    }
                )
            }
        }
    }
}
