package com.example.phyphoxemulator.presentation

import androidx.lifecycle.ViewModel
import com.example.phyphoxemulator.ble.BlePeripheralManager
import com.example.phyphoxemulator.data.LogRepository
import com.example.phyphoxemulator.simulation.SimulationEngine
import kotlinx.coroutines.flow.StateFlow

class MainViewModel(
    private val blePeripheralManager: BlePeripheralManager,
    val simulationEngine: SimulationEngine,
    val logRepository: LogRepository
) : ViewModel() {
    val connectionState = blePeripheralManager.connectionState
    val simulationState: StateFlow<com.example.phyphoxemulator.domain.SimulationState> = simulationEngine.state
    val logs = logRepository.entries

    fun startAdvertising() = blePeripheralManager.start()

    fun stopAdvertising() = blePeripheralManager.stop()

    fun setAnalog(value: Float) {
        simulationEngine.setAnalog(value)
        blePeripheralManager.notifySimulationChanged()
    }

    fun setDigital(enabled: Boolean) {
        simulationEngine.setDigital(enabled)
        blePeripheralManager.notifySimulationChanged()
    }

    fun trigger() {
        simulationEngine.trigger()
        blePeripheralManager.notifySimulationChanged()
    }

    fun clearLogs() = logRepository.clear()
}
