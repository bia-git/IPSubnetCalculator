package com.example.ipsubnetcalc.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ipsubnetcalc.core.SubnetTable
import com.example.ipsubnetcalc.ui.theme.MonoFamily

private enum class TableFilter(val label: String) {
    ALL("ทั้งหมด (/0–/32)"),
    COMMON("ที่ใช้บ่อย (/8–/30)")
}

// Column widths in dp — sized to fit the longest content tightly so the
// table fits most phone screens (~390dp) without scrolling. Only very
// narrow screens (older / small devices) will need horizontal scroll.
private val CIDR_WIDTH = 56.dp
private val MASK_WIDTH = 124.dp   // fits "255.255.255.255" at 13sp
private val HOSTS_WIDTH = 96.dp    // fits "16,777,214" (the typical max in /8–/30)
private val TOTAL_WIDTH = 96.dp
private val CLASS_WIDTH = 52.dp

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
            text = "แตะแถวเพื่อนำ CIDR ไปคำนวณ • ปัดซ้าย-ขวาเพื่อดูคอลัมน์",
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

        // Scrollable table: header + body share one horizontal scroll state
        val scrollState = rememberScrollState()
        Column(modifier = Modifier.fillMaxSize()) {
            HeaderRow(scrollState)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                items(rows, key = { it.cidr }) { row ->
                    DataRow(row, scrollState, onPickCidr)
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(scrollState: androidx.compose.foundation.ScrollState) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = RoundedCornerShape(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 4.dp)
            .horizontalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderCell("CIDR", CIDR_WIDTH)
            HeaderCell("Netmask", MASK_WIDTH)
            HeaderCell("Hosts", HOSTS_WIDTH, TextAlign.End)
            HeaderCell("Total IPs", TOTAL_WIDTH, TextAlign.End)
            HeaderCell("Class", CLASS_WIDTH, TextAlign.Center)
        }
    }
}

@Composable
private fun DataRow(
    row: SubnetTable.Row,
    scrollState: androidx.compose.foundation.ScrollState,
    onPickCidr: (String) -> Unit
) {
    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .horizontalScroll(scrollState)
    ) {
        Row(
            modifier = Modifier
                .clickable { onPickCidr("0.0.0.0/${row.cidr}") }
                .padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DataCell(
                text = "/${row.cidr}",
                width = CIDR_WIDTH,
                weight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            DataCell(
                text = row.netmask,
                width = MASK_WIDTH,
                color = MaterialTheme.colorScheme.onSurface
            )
            DataCell(
                text = SubnetTable.formatCount(row.usableHosts),
                width = HOSTS_WIDTH,
                align = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurface
            )
            DataCell(
                text = SubnetTable.formatCount(row.totalIps),
                width = TOTAL_WIDTH,
                align = TextAlign.End,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            DataCell(
                text = row.ipClass,
                width = CLASS_WIDTH,
                align = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HeaderCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onPrimaryContainer,
        textAlign = align,
        fontSize = 13.sp,
        modifier = Modifier.width(width)
    )
}

@Composable
private fun DataCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    weight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    align: TextAlign = TextAlign.Start
) {
    Text(
        text = text,
        fontFamily = MonoFamily,
        fontWeight = weight,
        color = color,
        textAlign = align,
        fontSize = 13.sp,
        modifier = Modifier.width(width)
    )
}
