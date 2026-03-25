package com.example.phyphoxemulator.profile

import com.example.phyphoxemulator.domain.CharacteristicSpec
import com.example.phyphoxemulator.domain.DataType
import com.example.phyphoxemulator.domain.DeviceProfile
import com.example.phyphoxemulator.domain.ServiceSpec
import java.util.UUID

object DemoProfiles {
    val telemetryServiceUuid: UUID = UUID.fromString("3f55a001-65ff-4f4c-b3d1-8d8f9f8f0001")
    val analogCharacteristicUuid: UUID = UUID.fromString("3f55a001-65ff-4f4c-b3d1-8d8f9f8f1001")
    val digitalCharacteristicUuid: UUID = UUID.fromString("3f55a001-65ff-4f4c-b3d1-8d8f9f8f1002")
    val writeCharacteristicUuid: UUID = UUID.fromString("3f55a001-65ff-4f4c-b3d1-8d8f9f8f1003")
    val eventCharacteristicUuid: UUID = UUID.fromString("3f55a001-65ff-4f4c-b3d1-8d8f9f8f1004")

    val phyphoxServiceUuid: UUID = UUID.fromString("cddf0001-30f7-4671-8b43-5e40ba53514a")
    val phyphoxExperimentCharacteristicUuid: UUID = UUID.fromString("cddf0002-30f7-4671-8b43-5e40ba53514a")
    val phyphoxTransferControlCharacteristicUuid: UUID = UUID.fromString("cddf0003-30f7-4671-8b43-5e40ba53514a")
    val phyphoxEventCharacteristicUuid: UUID = UUID.fromString("cddf0004-30f7-4671-8b43-5e40ba53514a")

    val defaultProfile = DeviceProfile(
        id = "demo-profile-v1",
        displayName = "Demo phyphox BLE Emulator",
        advertisedName = "phyphox-emu",
        services = listOf(
            ServiceSpec(
                uuid = telemetryServiceUuid,
                name = "Telemetry Service",
                characteristics = listOf(
                    CharacteristicSpec(
                        uuid = analogCharacteristicUuid,
                        name = "Analog Float",
                        dataType = DataType.FLOAT32_LE,
                        readable = true,
                        writable = false,
                        notifiable = true,
                        indicative = false
                    ),
                    CharacteristicSpec(
                        uuid = digitalCharacteristicUuid,
                        name = "Digital uInt8",
                        dataType = DataType.UINT8,
                        readable = true,
                        writable = false,
                        notifiable = true,
                        indicative = false
                    ),
                    CharacteristicSpec(
                        uuid = writeCharacteristicUuid,
                        name = "Write Input",
                        dataType = DataType.UTF8,
                        readable = true,
                        writable = true,
                        notifiable = false,
                        indicative = false
                    ),
                    CharacteristicSpec(
                        uuid = eventCharacteristicUuid,
                        name = "Trigger Event",
                        dataType = DataType.UINT8,
                        readable = true,
                        writable = false,
                        notifiable = true,
                        indicative = false
                    )
                )
            ),
            ServiceSpec(
                uuid = phyphoxServiceUuid,
                name = "phyphox Experiment Transfer",
                characteristics = listOf(
                    CharacteristicSpec(
                        uuid = phyphoxExperimentCharacteristicUuid,
                        name = "Experiment Data",
                        dataType = DataType.UTF8,
                        readable = true,
                        writable = false,
                        notifiable = false,
                        indicative = false
                    ),
                    CharacteristicSpec(
                        uuid = phyphoxTransferControlCharacteristicUuid,
                        name = "Transfer Control",
                        dataType = DataType.UTF8,
                        readable = true,
                        writable = true,
                        notifiable = false,
                        indicative = false
                    ),
                    CharacteristicSpec(
                        uuid = phyphoxEventCharacteristicUuid,
                        name = "phyphox Events",
                        dataType = DataType.UTF8,
                        readable = true,
                        writable = true,
                        notifiable = true,
                        indicative = false
                    )
                )
            )
        )
    )
}
