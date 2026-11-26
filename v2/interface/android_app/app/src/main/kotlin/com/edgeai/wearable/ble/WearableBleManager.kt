package com.edgeai.wearable.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Build
import android.os.ParcelUuid
import android.util.Log
import com.edgeai.wearable.data.HealthPacket
import com.edgeai.wearable.data.ScannedBleDevice
import com.edgeai.wearable.data.parseHealthPacket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
}

/**
 * Zero-UI-blocking Android BLE central manager for ESP32-S3 EdgeAI_Wearable.
 * - Executes ALL Bluetooth/GATT IPC on a dedicated single-threaded bleDispatcher (never blocks Main Thread!)
 * - Requests CONNECTION_PRIORITY_HIGH for snappy handshake & low-jitter 10 Hz notifications
 * - Negotiates MTU 247 with an 800ms fallback so discovery never stalls
 * - Offloads JSON parsing off the Binder thread onto Dispatchers.Default
 * - Reassembles fragmented JSON notifications if a phone's BT stack keeps MTU=23
 */
@OptIn(ExperimentalCoroutinesApi::class)
class WearableBleManager(
    private val context: Context,
    private val scope: CoroutineScope,
    private val onPacketReceived: (HealthPacket, String) -> Unit,
) {
    companion object {
        private const val TAG = "WearableBleManager"
        val SERVICE_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789001")
        val CHARACTERISTIC_UUID: UUID = UUID.fromString("12345678-1234-1234-1234-123456789002")
        val CCCD_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val DEFAULT_DEVICE_NAME = "EdgeAI_Wearable"
        const val DEFAULT_DEVICE_ADDRESS = "44:B1:76:CE:D9:ED"
        const val REQUESTED_MTU = 247
        const val SCAN_DURATION_MS = 10_000L
        private const val MTU_FALLBACK_MS = 800L
        private const val SCAN_THROTTLE_MS = 450L
        private const val MAX_JSON_BUFFER = 2048
    }

    // Dedicated serialized dispatcher for all BluetoothGatt / Scanner IPC
    private val bleDispatcher = Dispatchers.IO.limitedParallelism(1)

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    @Volatile
    private var gatt: BluetoothGatt? = null
    @Volatile
    private var servicesDiscoveryStarted = false
    @Volatile
    private var autoConnectOnScan = false
    @Volatile
    private var manualDisconnect = false

    private var scanTimeoutJob: Job? = null
    private var scanThrottleJob: Job? = null
    private var mtuFallbackJob: Job? = null
    private var connectJob: Job? = null

    private val jsonBuffer = StringBuilder(512)
    private val discoveredMap = ConcurrentHashMap<String, ScannedBleDevice>()

    private val _connectionState = MutableStateFlow(BleConnectionState.DISCONNECTED)
    val connectionState: StateFlow<BleConnectionState> = _connectionState.asStateFlow()

    private val _deviceName = MutableStateFlow<String?>(null)
    val deviceName: StateFlow<String?> = _deviceName.asStateFlow()

    private val _deviceAddress = MutableStateFlow<String?>(null)
    val deviceAddress: StateFlow<String?> = _deviceAddress.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<ScannedBleDevice>>(emptyList())
    val scannedDevices: StateFlow<List<ScannedBleDevice>> = _scannedDevices.asStateFlow()

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(autoConnectTarget: Boolean = false) {
        scope.launch(bleDispatcher) {
            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                _statusMessage.value = "Bluetooth is turned off. Please enable Bluetooth."
                return@launch
            }
            val scanner = adapter.bluetoothLeScanner
            if (scanner == null) {
                _statusMessage.value = "BLE scanner unavailable on this device."
                return@launch
            }

            stopScanInternal()
            autoConnectOnScan = autoConnectTarget
            discoveredMap.clear()
            _scannedDevices.value = emptyList()
            _isScanning.value = true
            if (_connectionState.value == BleConnectionState.DISCONNECTED) {
                _connectionState.value = BleConnectionState.SCANNING
            }
            _statusMessage.value = if (autoConnectTarget) {
                "Scanning for $DEFAULT_DEVICE_NAME…"
            } else {
                "Scanning for nearby BLE devices (10s)…"
            }

            val settings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                scanner.startScan(null, settings, scanCallback)
            } catch (_: SecurityException) {
                _isScanning.value = false
                _connectionState.value = BleConnectionState.DISCONNECTED
                _statusMessage.value = "Missing Bluetooth scan permission."
                return@launch
            }

            scanTimeoutJob = scope.launch(bleDispatcher) {
                delay(SCAN_DURATION_MS)
                stopScanInternal()
                if (autoConnectTarget && _connectionState.value != BleConnectionState.CONNECTED &&
                    _connectionState.value != BleConnectionState.CONNECTING
                ) {
                    _statusMessage.value = "No $DEFAULT_DEVICE_NAME found nearby."
                } else if (!autoConnectTarget && discoveredMap.isEmpty()) {
                    _statusMessage.value = "No BLE devices found nearby."
                }
            }
        }
    }

    fun stopScan() {
        scope.launch(bleDispatcher) { stopScanInternal() }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanInternal() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        scanThrottleJob?.cancel()
        scanThrottleJob = null
        if (_isScanning.value) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {
            }
            _isScanning.value = false
            publishScannedDevices()
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String) {
        scope.launch(bleDispatcher) {
            val adapter = bluetoothAdapter
            if (adapter == null || !adapter.isEnabled) {
                _statusMessage.value = "Bluetooth is turned off. Please enable Bluetooth."
                return@launch
            }
            stopScanInternal()
            manualDisconnect = false
            val normalized = address.trim().uppercase()
            val device = try {
                adapter.getRemoteDevice(normalized)
            } catch (_: IllegalArgumentException) {
                _statusMessage.value = "Invalid MAC address: $address"
                return@launch
            }
            connectToDeviceInternal(device)
        }
    }

    fun connectAuto(preferredAddress: String? = null) {
        if (!preferredAddress.isNullOrBlank()) {
            connectByAddress(preferredAddress)
        } else {
            startScan(autoConnectTarget = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDeviceInternal(device: BluetoothDevice) {
        connectJob?.cancel()
        val hadPreviousGatt = gatt != null
        closeGattInternal()
        synchronized(jsonBuffer) { jsonBuffer.setLength(0) }
        servicesDiscoveryStarted = false

        _connectionState.value = BleConnectionState.CONNECTING
        _deviceAddress.value = device.address
        _deviceName.value = try {
            device.name ?: DEFAULT_DEVICE_NAME
        } catch (_: SecurityException) {
            DEFAULT_DEVICE_NAME
        }
        _statusMessage.value = "Connecting to ${_deviceName.value} [${device.address}]…"

        connectJob = scope.launch(bleDispatcher) {
            if (hadPreviousGatt) {
                delay(120L) // Brief settle only when tearing down an existing GATT handle
            }
            try {
                gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
            } catch (_: SecurityException) {
                _connectionState.value = BleConnectionState.DISCONNECTED
                _statusMessage.value = "Missing Bluetooth connect permission."
            }
        }
    }

    fun disconnect() {
        manualDisconnect = true
        connectJob?.cancel()
        mtuFallbackJob?.cancel()
        _connectionState.value = BleConnectionState.DISCONNECTED
        _deviceName.value = null
        _statusMessage.value = "Disconnected."
        scope.launch(bleDispatcher) {
            stopScanInternal()
            closeGattInternal()
            synchronized(jsonBuffer) { jsonBuffer.setLength(0) }
        }
    }

    @SuppressLint("MissingPermission")
    private fun closeGattInternal() {
        mtuFallbackJob?.cancel()
        mtuFallbackJob = null
        val g = gatt
        gatt = null
        try {
            g?.disconnect()
            g?.close()
        } catch (_: Exception) {
        }
    }

    private fun scheduleScanPublish() {
        if (scanThrottleJob?.isActive == true) return
        scanThrottleJob = scope.launch(Dispatchers.Default) {
            delay(SCAN_THROTTLE_MS)
            publishScannedDevices()
        }
    }

    private fun publishScannedDevices() {
        _scannedDevices.value = discoveredMap.values
            .sortedWith(
                compareByDescending<ScannedBleDevice> { it.hasTargetService }
                    .thenByDescending { it.rssi ?: Int.MIN_VALUE }
            )
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            val device = result.device ?: return
            val address = device.address ?: return
            val name = result.scanRecord?.deviceName
                ?: try {
                    device.name
                } catch (_: SecurityException) {
                    null
                }
                ?: "Unknown"

            val serviceUuids = result.scanRecord?.serviceUuids.orEmpty()
            val hasTargetService = serviceUuids.any { it == ParcelUuid(SERVICE_UUID) } ||
                name.equals(DEFAULT_DEVICE_NAME, ignoreCase = true)

            discoveredMap[address] = ScannedBleDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                hasTargetService = hasTargetService,
            )
            scheduleScanPublish()

            if (autoConnectOnScan && hasTargetService) {
                autoConnectOnScan = false
                scope.launch(bleDispatcher) {
                    stopScanInternal()
                    connectToDeviceInternal(device)
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            _isScanning.value = false
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
            _statusMessage.value = "BLE scan failed (code $errorCode)."
        }
    }

    @SuppressLint("MissingPermission")
    private fun triggerDiscoverServices(targetGatt: BluetoothGatt) {
        if (servicesDiscoveryStarted) return
        servicesDiscoveryStarted = true
        mtuFallbackJob?.cancel()
        scope.launch(bleDispatcher) {
            if (gatt === targetGatt) {
                targetGatt.discoverServices()
            }
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                servicesDiscoveryStarted = false
                _statusMessage.value = "Connected. Optimizing BLE link…"
                scope.launch(bleDispatcher) {
                    // Request high-priority low-latency connection interval (11.25–15ms)
                    try {
                        gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                    } catch (_: Exception) {
                    }
                    val requested = gatt.requestMtu(REQUESTED_MTU)
                    if (!requested) {
                        triggerDiscoverServices(gatt)
                    } else {
                        mtuFallbackJob?.cancel()
                        mtuFallbackJob = scope.launch(bleDispatcher) {
                            delay(MTU_FALLBACK_MS)
                            if (!servicesDiscoveryStarted && this@WearableBleManager.gatt === gatt) {
                                Log.w(TAG, "MTU callback timed out; discovering services")
                                triggerDiscoverServices(gatt)
                            }
                        }
                    }
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                scope.launch(bleDispatcher) { closeGattInternal() }
                _connectionState.value = BleConnectionState.DISCONNECTED
                if (!manualDisconnect) {
                    _statusMessage.value = "BLE disconnected (status $status)."
                }
            }
        }

        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU negotiated: $mtu (status=$status)")
            triggerDiscoverServices(gatt)
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _statusMessage.value = "Service discovery failed (status $status)."
                disconnect()
                return
            }

            scope.launch(bleDispatcher) {
                val service = gatt.getService(SERVICE_UUID)
                val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
                if (service == null || characteristic == null) {
                    _statusMessage.value = "Target EdgeAI health service/characteristic not found."
                    disconnect()
                    return@launch
                }

                if (!gatt.setCharacteristicNotification(characteristic, true)) {
                    _statusMessage.value = "Failed to enable local GATT notifications."
                    disconnect()
                    return@launch
                }

                val cccd = characteristic.getDescriptor(CCCD_UUID)
                if (cccd != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    } else {
                        @Suppress("DEPRECATION")
                        cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                        @Suppress("DEPRECATION")
                        gatt.writeDescriptor(cccd)
                    }
                } else {
                    _connectionState.value = BleConnectionState.CONNECTED
                    _statusMessage.value = "Connected and monitoring notifications."
                }
            }
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt,
            descriptor: BluetoothGattDescriptor,
            status: Int,
        ) {
            if (descriptor.uuid == CCCD_UUID && status == BluetoothGatt.GATT_SUCCESS) {
                _connectionState.value = BleConnectionState.CONNECTED
                _statusMessage.value = "Connected to ${_deviceName.value ?: DEFAULT_DEVICE_NAME} (10 Hz stream active)."
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                _statusMessage.value = "Failed to subscribe to BLE notifications (status $status)."
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                val copy = value.copyOf()
                scope.launch(Dispatchers.Default) { handlePayload(copy) }
            }
        }

        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                val copy = characteristic.value?.copyOf() ?: return
                scope.launch(Dispatchers.Default) { handlePayload(copy) }
            }
        }

        private fun handlePayload(bytes: ByteArray) {
            val chunk = bytes.toString(Charsets.UTF_8)
            if (chunk.isBlank()) return

            val trimmed = chunk.trim()
            if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
                val packet = parseHealthPacket(trimmed)
                if (packet != null) {
                    synchronized(jsonBuffer) { jsonBuffer.setLength(0) }
                    onPacketReceived(packet, trimmed)
                    return
                }
            }

            synchronized(jsonBuffer) {
                if (jsonBuffer.length + chunk.length > MAX_JSON_BUFFER) {
                    jsonBuffer.setLength(0)
                }
                jsonBuffer.append(chunk)
                val start = jsonBuffer.indexOf("{")
                val end = jsonBuffer.lastIndexOf("}")
                if (start >= 0 && end > start) {
                    val candidate = jsonBuffer.substring(start, end + 1)
                    val packet = parseHealthPacket(candidate)
                    if (packet != null) {
                        jsonBuffer.delete(0, end + 1)
                        onPacketReceived(packet, candidate)
                    }
                }
            }
        }
    }
}
