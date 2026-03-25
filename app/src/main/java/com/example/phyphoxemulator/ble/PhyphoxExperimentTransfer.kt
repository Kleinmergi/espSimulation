package com.example.phyphoxemulator.ble

import com.example.phyphoxemulator.profile.DemoProfiles

class PhyphoxExperimentTransfer {
    private val experimentXml = """
        <?xml version="1.0" encoding="utf-8"?>
        <phyphox version="1.10">
          <title>BLE Emulator Demo</title>
          <category>Tools</category>
          <description>Demo experiment for Android BLE peripheral emulator.</description>
          <input>
            <bluetooth id="bleInput" mode="notification" service="${DemoProfiles.telemetryServiceUuid}" characteristic="${DemoProfiles.analogCharacteristicUuid}" datatype="float32LittleEndian" />
          </input>
        </phyphox>
    """.trimIndent()

    fun readExperimentChunk(offset: Int, mtuPayload: Int = 180): ByteArray {
        val bytes = experimentXml.toByteArray(Charsets.UTF_8)
        if (offset >= bytes.size) return byteArrayOf()
        val end = (offset + mtuPayload).coerceAtMost(bytes.size)
        return bytes.copyOfRange(offset, end)
    }

    fun totalSize(): Int = experimentXml.toByteArray(Charsets.UTF_8).size

    fun handleControlWrite(payload: ByteArray): String {
        val input = payload.toString(Charsets.UTF_8).trim()
        return when {
            input.equals("SIZE", ignoreCase = true) -> "SIZE:${totalSize()}"
            input.startsWith("GET", ignoreCase = true) -> "OK"
            else -> "UNKNOWN:$input"
        }
    }
}
