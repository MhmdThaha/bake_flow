package com.bakeflow.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp

@Composable
fun QuantityStepper(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    error: String? = null,
    step: Double = 1.0,
    minValue: Double = 1.0
) {
    val current = value.toDoubleOrNull() ?: minValue
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        IconButton(
            onClick = {
                val next = (current - step).coerceAtLeast(minValue)
                onValueChange(formatStepperValue(next))
            }
        ) {
            Icon(Icons.Default.Remove, contentDescription = "Decrease")
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(label) },
            isError = error != null,
            supportingText = error?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp),
            shape = MaterialTheme.shapes.large,
            singleLine = true
        )
        IconButton(
            onClick = {
                onValueChange(formatStepperValue(current + step))
            }
        ) {
            Icon(Icons.Default.Add, contentDescription = "Increase")
        }
    }
}

private fun formatStepperValue(value: Double): String =
    if (value % 1.0 == 0.0) value.toLong().toString() else String.format("%.1f", value)
