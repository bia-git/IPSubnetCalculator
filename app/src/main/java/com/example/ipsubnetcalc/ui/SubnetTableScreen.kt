package com.example.ipsubnetcalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ipsubnetcalc.core.SubnetTable
import com.example.ipsubnetcalc.ui.theme.MonoFamily

private enum class TableFilter(val label: String) {
    ALL("ทั้งหมด (/0–/32)"),
    COMMON("ที่ใช้บ่อย (/8–/30)")
}

@Composable
fun SubnetTableScreen(
    onPickCidr: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var filter by rememberSaveable { mutableStateOf(TableFilter.COMMON) }
    var query by rememberSaveable { mutableStateOf("") }

    val rows = remember(filter, query) {
        val base = when (filter) {
            TableFilter.ALL -> SubnetTable.all
            TableFilter.COMMON -> SubnetTable.common
        }
        val q = query.trim()
        if (q.isEmpty()) base
        else base.filter {
            "/${it.cidr}".contains(q) || it.netmask.contains(q) || it.ipClass.contains(q, ignoreCase = true)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = "ตารางซับเน็ตมาตรฐาน IPv4",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "แตะแถวเพื่อนำ CIDR ไปคำนวณ",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text("ค้นหา CIDR หรือ mask…") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Search),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TableFilter.entries.forEach { entry ->
                FilterChip(
                    selected = filter == entry,
                    onClick = { filter = entry },
                    label = { Text(entry.label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        Spacer(Modifier.height(10.dp))

        // Header row
        HeaderRow()

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            items(rows, key = { it.cidr }) { row ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPickCidr("0.0.0.0/${row.cidr}") }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Cell(
                        text = "/${row.cidr}",
                        weight = 0.85f,
                        weightFont = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Cell(
                        text = row.netmask,
                        weight = 2.0f,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Cell(
                        text = SubnetTable.formatCount(row.usableHosts),
                        weight = 1.7f,
                        align = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Cell(
                        text = SubnetTable.formatCount(row.totalIps),
                        weight = 1.7f,
                        align = TextAlign.End,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Cell(
                        text = row.ipClass,
                        weight = 0.75f,
                        align = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderRow() {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Cell("CIDR", 0.85f, FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, size = 11f)
            Cell("Netmask", 2.0f, FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, size = 11f)
            Cell("Hosts", 1.7f, FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, align = TextAlign.End, size = 11f)
            Cell("Total", 1.7f, FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, align = TextAlign.End, size = 11f)
            Cell("Class", 0.75f, FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer, align = TextAlign.Center, size = 11f)
        }
    }
}

/**
 * Single table cell. Uses a smaller font (default 12sp) with [maxLines] = 1 and
 * [TextOverflow.Ellipsis] so big numbers like "16,777,214" never wrap or
 * overflow the row. Callers can override the size for header cells.
 */
@Composable
private fun androidx.compose.foundation.layout.RowScope.Cell(
    text: String,
    weight: Float,
    weightFont: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    mono: Boolean = true,
    align: TextAlign = TextAlign.Start,
    size: Float = 12f
) {
    Text(
        text = text,
        fontFamily = if (mono) MonoFamily else FontFamily.Default,
        fontWeight = weightFont,
        color = color,
        textAlign = align,
        fontSize = size.sp,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .weight(weight)
            .padding(end = 4.dp)
    )
}
