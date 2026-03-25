package com.example.phyphoxemulator.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.os.ParcelUuid
import com.example.phyphoxemulator.data.LogRepository
import com.example.phyphoxemulator.domain.ConnectionState
import com.example.phyphoxemulator.domain.LogLevel
import com.example.phyphoxemulator.profile.DemoProfiles
import com.example.phyphoxemulator.simulation.SimulationEngine
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

@SuppressLint("MissingPermission")
class BlePeripheralManager(
    private val context: Context,
    private val logRepository: LogRepository,
    private val simulationEngine: SimulationEngine,
    private val experimentTransfer: PhyphoxExperimentTransfer
) {
    private val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter
    private val advertiser: BluetoothLeAdvertiser? = bluetoothAdapter?.bluetoothLeAdvertiser
    private var gattServer: BluetoothGattServer? = null

    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val subscribedDevices = linkedSetOf<BluetoothDevice>()

    private val cccdUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings?) {
            logRepository.add(LogLevel.BLE, "Advertising started")
            _connectionState.value = ConnectionState.ADVERTISING
        }

        override fun onStartFailure(errorCode: Int) {
            logRepository.add(LogLevel.ERROR, "Advertising start failed: $errorCode")
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }

    private val callback = object : BluetoothGattServerCallback() {
        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothGatt.STATE_CONNECTED) {
                _connectionState.value = ConnectionState.CONNECTED
                logRepository.add(LogLevel.BLE, "Connected: ${device.address}")
            } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
                subscribedDevices.remove(device)
                _connectionState.value = ConnectionState.ADVERTISING
                logRepository.add(LogLevel.BLE, "Disconnected: ${device.address}")
            }
        }

        override fun onCharacteristicReadRequest(
            device: BluetoothDevice,
            requestId: Int,
            offset: Int,
            characteristic: BluetoothGattCharacteristic
        ) {
            val value = when (characteristic.uuid) {
                DemoProfiles.analogCharacteristicUuid -> simulationEngine.analogAsFloatLe()
                DemoProfiles.digitalCharacteristicUuid -> simulationEngine.digitalAsUInt8()
                DemoProfiles.writeCharacteristicUuid -> simulationEngine.writeEchoAsUtf8()
                DemoProfiles.eventCharacteristicUuid -> simulationEngine.triggerAsUInt8()
                DemoProfiles.phyphoxExperimentCharacteristicUuid -> experimentTransfer.readExperimentChunk(offset)
                DemoProfiles.phyphoxTransferControlCharacteristicUuid -> "READY".toByteArray(Charsets.UTF_8)
                DemoProfiles.phyphoxEventCharacteristicUuid -> "IDLE".toByteArray(Charsets.UTF_8)
                else -> byteArrayOf()
            }
            val payload = if (offset in 1 until value.size) value.copyOfRange(offset, value.size) else value
            gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, payload)
            logRepository.add(LogLevel.BLE, "Read ${characteristic.uuid} (${payload.size} bytes)")
        }

        override fun onCharacteristicWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            characteristic: BluetoothGattCharacteristic,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            when (characteristic.uuid) {
                DemoProfiles.writeCharacteristicUuid -> {
                    val text = value.toString(Charsets.UTF_8)
                    simulationEngine.setLastWrite(text)
                    logRepository.add(LogLevel.BLE, "Write input: $text")
                }

                DemoProfiles.phyphoxTransferControlCharacteristicUuid -> {
                    val result = experimentTransfer.handleControlWrite(value)
                    logRepository.add(LogLevel.BLE, "Transfer control: $result")
                }

                DemoProfiles.phyphoxEventCharacteristicUuid -> {
                    val event = value.toString(Charsets.UTF_8)
                    logRepository.add(LogLevel.BLE, "phyphox event: $event")
                }
            }

            characteristic.value = value
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }

        override fun onDescriptorWriteRequest(
            device: BluetoothDevice,
            requestId: Int,
            descriptor: BluetoothGattDescriptor,
            preparedWrite: Boolean,
            responseNeeded: Boolean,
            offset: Int,
            value: ByteArray
        ) {
            if (descriptor.uuid == cccdUuid) {
                if (value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE) ||
                    value.contentEquals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)
                ) {
                    subscribedDevices.add(device)
                    logRepository.add(LogLevel.BLE, "Notify enabled: ${device.address}")
                } else if (value.contentEquals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                    subscribedDevices.remove(device)
                    logRepository.add(LogLevel.BLE, "Notify disabled: ${device.address}")
                }
            }
            descriptor.value = value
            if (responseNeeded) {
                gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
            }
        }
    }

    fun start(profileName: String = DemoProfiles.defaultProfile.advertisedName) {
        if (bluetoothAdapter == null || advertiser == null) {
            logRepository.add(LogLevel.ERROR, "BLE not supported on this device")
            return
        }
        bluetoothAdapter.name = profileName
        startGattServer()
        startAdvertising(profileName)
    }

    fun stop() {
        advertiser?.stopAdvertising(advertiseCallback)
        gattServer?.close()
        gattServer = null
        subscribedDevices.clear()
        _connectionState.value = ConnectionState.DISCONNECTED
        logRepository.add(LogLevel.BLE, "Advertising stopped and GATT server closed")
    }

    fun notifySimulationChanged() {
        val server = gattServer ?: return
        subscribedDevices.forEach { device ->
            notifyCharacteristic(server, device, DemoProfiles.analogCharacteristicUuid, simulationEngine.analogAsFloatLe())
            notifyCharacteristic(server, device, DemoProfiles.digitalCharacteristicUuid, simulationEngine.digitalAsUInt8())
            notifyCharacteristic(server, device, DemoProfiles.eventCharacteristicUuid, simulationEngine.triggerAsUInt8())
        }
    }

    private fun notifyCharacteristic(
        server: BluetoothGattServer,
        device: BluetoothDevice,
        characteristicUuid: UUID,
        value: ByteArray
    ) {
        val characteristic = findCharacteristic(characteristicUuid) ?: return
        characteristic.value = value
        server.notifyCharacteristicChanged(device, characteristic, false)
        logRepository.add(LogLevel.BLE, "Notify ${characteristic.uuid}: ${value.toHexString()}")
    }

    private fun startGattServer() {
        gattServer?.close()
        gattServer = bluetoothManager?.openGattServer(context, callback)
        val profile = DemoProfiles.defaultProfile
        profile.services.forEach { serviceSpec ->
            val service = BluetoothGattService(serviceSpec.uuid, BluetoothGattService.SERVICE_TYPE_PRIMARY)
            serviceSpec.characteristics.forEach { charSpec ->
                var properties = 0
                var permissions = 0
                if (charSpec.readable) {
                    properties = properties or BluetoothGattCharacteristic.PROPERTY_READ
                    permissions = permissions or BluetoothGattCharacteristic.PERMISSION_READ
                }
                if (charSpec.writable) {
                    properties = properties or BluetoothGattCharacteristic.PROPERTY_WRITE
                    permissions = permissions or BluetoothGattCharacteristic.PERMISSION_WRITE
                }
                if (charSpec.notifiable) {
                    properties = properties or BluetoothGattCharacteristic.PROPERTY_NOTIFY
                }
                if (charSpec.indicative) {
                    properties = properties or BluetoothGattCharacteristic.PROPERTY_INDICATE
                }

                val characteristic = BluetoothGattCharacteristic(charSpec.uuid, properties, permissions)
                if (charSpec.notifiable || charSpec.indicative) {
                    val descriptor = BluetoothGattDescriptor(
                        cccdUuid,
                        BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
                    )
                    characteristic.addDescriptor(descriptor)
                }
                service.addCharacteristic(characteristic)
            }
            gattServer?.addService(service)
        }
        logRepository.add(LogLevel.BLE, "GATT server initialized with ${profile.services.size} services")
    }

    private fun startAdvertising(deviceName: String) {
        val settings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
            .setConnectable(true)
            .build()

        val data = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(ParcelUuid(DemoProfiles.telemetryServiceUuid))
            .addServiceUuid(ParcelUuid(DemoProfiles.phyphoxServiceUuid))
            .build()

        logRepository.add(LogLevel.BLE, "Starting advertising as $deviceName")
        advertiser?.startAdvertising(settings, data, advertiseCallback)
    }

    private fun findCharacteristic(uuid: UUID): BluetoothGattCharacteristic? {
        gattServer?.services?.forEach { service ->
            service.characteristics.firstOrNull { it.uuid == uuid }?.let { return it }
        }
        return null
    }
}

private fun ByteArray.toHexString(): String = joinToString(" ") { "%02X".format(it) }
