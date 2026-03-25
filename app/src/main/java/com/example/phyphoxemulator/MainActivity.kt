package com.example.phyphoxemulator

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.phyphoxemulator.ble.BlePeripheralManager
import com.example.phyphoxemulator.ble.PhyphoxExperimentTransfer
import com.example.phyphoxemulator.data.LogRepository
import com.example.phyphoxemulator.domain.LogLevel
import com.example.phyphoxemulator.presentation.MainScreen
import com.example.phyphoxemulator.presentation.MainViewModel
import com.example.phyphoxemulator.simulation.SimulationEngine

class MainActivity : ComponentActivity() {
    private val logRepository = LogRepository()
    private val simulationEngine = SimulationEngine()

    private lateinit var viewModel: MainViewModel

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            if (result.values.all { it }) {
                logRepository.add(LogLevel.INFO, "All required BLE permissions granted")
            } else {
                logRepository.add(LogLevel.WARN, "Missing BLE permissions; app cannot advertise/connect reliably")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ensurePermissions()

        val blePeripheralManager = BlePeripheralManager(
            context = applicationContext,
            logRepository = logRepository,
            simulationEngine = simulationEngine,
            experimentTransfer = PhyphoxExperimentTransfer()
        )
        viewModel = MainViewModel(blePeripheralManager, simulationEngine, logRepository)

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        viewModel.stopAdvertising()
    }

    private fun ensurePermissions() {
        val required = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            listOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }

        val missing = required.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        }
    }
}
