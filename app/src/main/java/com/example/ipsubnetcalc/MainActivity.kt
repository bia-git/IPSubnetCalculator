package com.example.ipsubnetcalc

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.ipsubnetcalc.ui.CalculatorScreen
import com.example.ipsubnetcalc.ui.SubnetTableScreen
import com.example.ipsubnetcalc.ui.theme.IPSubnetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            IPSubnetTheme {
                App()
            }
        }
    }
}

private enum class Tab(val label: String, val icon: ImageVector) {
    CALCULATOR("คำนวณ", Icons.Outlined.Calculate),
    TABLE("ตารางซับเน็ต", Icons.Outlined.TableChart)
}

@Composable
private fun App() {
    var tab by rememberSaveable { mutableStateOf(Tab.CALCULATOR) }
    // When a CIDR is tapped in the table, this carries the value to the calculator.
    var pendingInput by rememberSaveable { mutableStateOf("") }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                Tab.entries.forEach { entry ->
                    NavigationBarItem(
                        selected = tab == entry,
                        onClick = { tab = entry },
                        icon = { Icon(entry.icon, contentDescription = entry.label) },
                        label = { Text(entry.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
            }
        }
    ) { inner ->
        when (tab) {
            Tab.CALCULATOR -> CalculatorScreen(
                initialInput = pendingInput,
                onInputConsumed = { pendingInput = "" },
                modifier = Modifier.padding(inner)
            )
            Tab.TABLE -> SubnetTableScreen(
                onPickCidr = { value ->
                    pendingInput = value
                    tab = Tab.CALCULATOR
                },
                modifier = Modifier.padding(inner)
            )
        }
    }
}
