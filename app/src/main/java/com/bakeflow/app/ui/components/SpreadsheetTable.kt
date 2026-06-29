package com.bakeflow.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

data class SpreadsheetColumn(
    val title: String,
    val weight: Float = 1f,
    val minWidth: Dp = 96.dp
)

@Composable
fun <T> SpreadsheetTable(
    columns: List<SpreadsheetColumn>,
    rows: List<T>,
    modifier: Modifier = Modifier,
    rowKey: (T) -> Any,
    rowBackground: (T) -> Color = { Color.Transparent },
    onRowClick: ((T) -> Unit)? = null,
    cellContent: @Composable (columnIndex: Int, row: T) -> Unit
) {
    val horizontalScroll = rememberScrollState()
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(horizontalScroll)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEach { column ->
                Box(
                    modifier = Modifier
                        .width(column.minWidth * column.weight)
                        .padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = column.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        HorizontalDivider()
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(rows, key = rowKey) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .then(
                            if (onRowClick != null) {
                                Modifier.clickable { onRowClick(row) }
                            } else {
                                Modifier
                            }
                        )
                        .horizontalScroll(horizontalScroll)
                        .background(rowBackground(row))
                        .padding(vertical = 4.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start
                    ) {
                        columns.forEachIndexed { index, column ->
                            Box(
                                modifier = Modifier
                                    .width(column.minWidth * column.weight)
                                    .fillMaxHeight()
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                cellContent(index, row)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
fun SpreadsheetCellText(
    text: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = text,
        modifier = modifier,
        style = if (emphasized) MaterialTheme.typography.bodyLarge else MaterialTheme.typography.bodyMedium,
        fontWeight = if (emphasized) FontWeight.SemiBold else FontWeight.Normal,
        color = color,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis
    )
}
