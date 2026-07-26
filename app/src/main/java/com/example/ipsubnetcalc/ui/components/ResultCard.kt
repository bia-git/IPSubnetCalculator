package com.example.ipsubnetcalc.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ipsubnetcalc.core.SubnetCalculator

/**
 * Big highlighted header showing the network/CIDR plus class & type badges.
 */
@Composable
fun ResultHeader(result: SubnetCalculator.Result, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = result.cidrNotation,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            InfoBadge(text = "Class ${result.ipClass}", container = MaterialTheme.colorScheme.primaryContainer, content = MaterialTheme.colorScheme.onPrimaryContainer)
            InfoBadge(
                text = if (result.isLoopback) "Loopback" else if (result.isPrivate) "Private" else "Public",
                container = if (result.isPrivate) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                content = if (result.isPrivate) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoBadge(text: String, container: androidx.compose.ui.graphics.Color, content: androidx.compose.ui.graphics.Color) {
    Surface(
        color = container,
        contentColor = content,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
        )
    }
}

/**
 * Full result card containing the header plus all detail rows.
 */
@Composable
fun ResultCard(result: SubnetCalculator.Result, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            ResultHeader(result)

            Spacer(Modifier.height(14.dp))
            SectionDivider("รายละเอียดแอดเดรส")

            CopyableRow("Network Address", result.networkAddress)
            CopyableRow("Broadcast Address", result.broadcastAddress)
            CopyableRow("First Host", result.firstHost)
            CopyableRow("Last Host", result.lastHost)
            CopyableRow("Subnet Mask", result.netmask)
            CopyableRow("Wildcard Mask", result.wildcard)

            Spacer(Modifier.height(6.dp))
            SectionDivider("ความจุ")

            CopyableRow("จำนวน IP ทั้งหมด", result.totalIps.toString())
            CopyableRow("Host ที่ใช้ได้", result.usableHosts.toString())

            Spacer(Modifier.height(6.dp))
            SectionDivider("ข้อมูลเพิ่มเติม")

            CopyableRow("CIDR Prefix", "/${result.prefix}")
            CopyableRow("Binary Mask", result.binaryMask)
        }
    }
}

@Composable
private fun SectionDivider(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.width(8.dp))
        androidx.compose.material3.HorizontalDivider(
            color = MaterialTheme.colorScheme.surfaceVariant,
            thickness = 1.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
