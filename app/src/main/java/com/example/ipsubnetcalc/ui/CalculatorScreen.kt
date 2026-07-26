package com.example.ipsubnetcalc.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.ipsubnetcalc.core.SubnetCalculator
import com.example.ipsubnetcalc.ui.components.ResultCard

private val EXAMPLES = listOf(
    "192.168.1.10/24",
    "10.0.0.5/8",
    "172.16.50.20/20",
    "203.0.113.7/28",
    "192.168.10.0/255.255.255.192"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CalculatorScreen(
    initialInput: String,
    onInputConsumed: () -> Unit,
    modifier: Modifier = Modifier
) {
    var input by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var result by remember { mutableStateOf<SubnetCalculator.Result?>(null) }

    fun doCalc(text: String) {
        try {
            result = SubnetCalculator.calculate(text)
            error = null
        } catch (e: SubnetCalculator.ParseException) {
            error = e.message ?: "รูปแบบไม่ถูกต้อง"
            result = null
        } catch (e: Exception) {
            error = "รูปแบบไม่ถูกต้อง"
            result = null
        }
    }

    // When a CIDR is picked from the subnet table, apply it automatically.
    LaunchedEffect(initialInput) {
        if (initialInput.isNotEmpty()) {
            input = initialInput
            doCalc(initialInput)
            onInputConsumed()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "กรอก IP address พร้อม CIDR (เช่น 192.168.1.10/24) หรือ IP + subnet mask",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = input,
            onValueChange = { input = it; error = null },
            label = { Text("IP / CIDR") },
            placeholder = { Text("192.168.1.10/24") },
            singleLine = true,
            isError = error != null,
            supportingText = {
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { input.takeIf { it.isNotBlank() }?.let(::doCalc) }),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { input.takeIf { it.isNotBlank() }?.let(::doCalc) },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) { Text("คำนวณ", style = MaterialTheme.typography.labelLarge) }

            OutlinedButton(
                onClick = {
                    input = ""
                    result = null
                    error = null
                },
                modifier = Modifier.weight(1f)
            ) { Text("ล้าง", style = MaterialTheme.typography.labelLarge) }
        }

        Text(
            text = "ตัวอย่างด่วน",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            EXAMPLES.forEach { example ->
                AssistChip(
                    onClick = {
                        input = example
                        doCalc(example)
                    },
                    label = { Text(example, style = MaterialTheme.typography.bodyMedium) },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }

        result?.let {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "ผลลัพธ์",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            ResultCard(it)
        }

        Spacer(Modifier.height(24.dp))
    }
}
