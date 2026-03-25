package com.example.phyphoxemulator.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.phyphoxemulator.domain.ConnectionState
import com.example.phyphoxemulator.domain.LogEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val state by viewModel.simulationState.collectAsState()
    val connectionState by viewModel.connectionState.collectAsState()
    val logs by viewModel.logs.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Phyphox BLE Emulator", style = MaterialTheme.typography.headlineSmall)
        Text("Status: ${connectionState.label}")

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::startAdvertising) { Text("Start Advertising") }
            OutlinedButton(onClick = viewModel::stopAdvertising) { Text("Stop") }
            OutlinedButton(onClick = viewModel::clearLogs) { Text("Logs löschen") }
        }

        Text("Analog Float32 LE: ${"%.3f".format(state.analogSlider)}")
        Slider(
            value = state.analogSlider,
            onValueChange = viewModel::setAnalog,
            valueRange = -10f..10f
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Digital uInt8")
            Switch(checked = state.digitalToggle, onCheckedChange = viewModel::setDigital)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = viewModel::trigger) { Text("Trigger Event") }
            Text("Counter: ${state.triggerCounter}")
        }

        Text("Last Write: ${state.lastWriteText}")
        HorizontalDivider()
        Text("Live-Logs", style = MaterialTheme.typography.titleMedium)
        LogList(logs = logs, modifier = Modifier.weight(1f))
    }
}

private val ConnectionState.label: String
    get() = when (this) {
        ConnectionState.DISCONNECTED -> "Disconnected"
        ConnectionState.ADVERTISING -> "Advertising"
        ConnectionState.CONNECTED -> "Connected"
    }

@Composable
private fun LogList(logs: List<LogEntry>, modifier: Modifier = Modifier) {
    val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
    LazyColumn(modifier = modifier.fillMaxWidth()) {
        items(logs.reversed()) { entry ->
            val ts = formatter.format(entry.timestamp.atZone(ZoneId.systemDefault()))
            Text("[$ts] [${entry.level}] ${entry.message}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
