package com.example.phyphoxemulator.domain

import java.time.Instant
import java.util.UUID

enum class LogLevel { INFO, WARN, ERROR, BLE }

data class LogEntry(
    val timestamp: Instant = Instant.now(),
    val level: LogLevel,
    val message: String
)

enum class ConnectionState {
    DISCONNECTED,
    ADVERTISING,
    CONNECTED
}

enum class DataType {
    FLOAT32_LE,
    UINT8,
    UTF8
}

data class CharacteristicSpec(
    val uuid: UUID,
    val name: String,
    val dataType: DataType,
    val readable: Boolean,
    val writable: Boolean,
    val notifiable: Boolean,
    val indicative: Boolean
)

data class ServiceSpec(
    val uuid: UUID,
    val name: String,
    val characteristics: List<CharacteristicSpec>
)

data class DeviceProfile(
    val id: String,
    val displayName: String,
    val advertisedName: String,
    val services: List<ServiceSpec>
)

data class SimulationState(
    val analogSlider: Float = 0.5f,
    val digitalToggle: Boolean = false,
    val triggerCounter: Int = 0,
    val lastWriteText: String = ""
)
