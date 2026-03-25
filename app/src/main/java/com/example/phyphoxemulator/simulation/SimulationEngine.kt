package com.example.phyphoxemulator.simulation

import com.example.phyphoxemulator.domain.SimulationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SimulationEngine {
    private val _state = MutableStateFlow(SimulationState())
    val state: StateFlow<SimulationState> = _state.asStateFlow()

    fun setAnalog(value: Float) {
        _state.update { it.copy(analogSlider = value) }
    }

    fun setDigital(enabled: Boolean) {
        _state.update { it.copy(digitalToggle = enabled) }
    }

    fun trigger() {
        _state.update { it.copy(triggerCounter = (it.triggerCounter + 1) and 0xFF) }
    }

    fun setLastWrite(value: String) {
        _state.update { it.copy(lastWriteText = value) }
    }

    fun analogAsFloatLe(): ByteArray =
        ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN).putFloat(state.value.analogSlider).array()

    fun digitalAsUInt8(): ByteArray = byteArrayOf(if (state.value.digitalToggle) 1 else 0)

    fun triggerAsUInt8(): ByteArray = byteArrayOf((state.value.triggerCounter and 0xFF).toByte())

    fun writeEchoAsUtf8(): ByteArray = state.value.lastWriteText.toByteArray(Charsets.UTF_8)
}
