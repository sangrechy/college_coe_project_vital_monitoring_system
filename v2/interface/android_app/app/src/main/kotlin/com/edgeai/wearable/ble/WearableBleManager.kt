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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class BleConnectionState {
    DISCONNECTED,
    SCANNING,
    CONNECTING,
    CONNECTED,
}

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
    }

    private val bluetoothManager =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter?
        get() = bluetoothManager?.adapter

    private var gatt: BluetoothGatt? = null
    private var scanTimeoutJob: Job? = null
    private var autoConnectOnScan: Boolean = false
    private var manualDisconnect: Boolean = false

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

    private val discoveredMap = LinkedHashMap<String, ScannedBleDevice>()

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(autoConnectTarget: Boolean = false) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = "Bluetooth is turned off. Please enable Bluetooth."
            return
        }
        val scanner = adapter.bluetoothLeScanner
        if (scanner == null) {
            _statusMessage.value = "BLE scanner unavailable on this device."
            return
        }

        stopScan()
        autoConnectOnScan = autoConnectTarget
        discoveredMap.clear()
        _scannedDevices.value = emptyList()
        _isScanning.value = true
        if (_connectionState.value == BleConnectionState.DISCONNECTED) {
            _connectionState.value = BleConnectionState.SCANNING
        }
        _statusMessage.value = if (autoConnectTarget) {
            "Scanning for $DEFAULT_DEVICE_NAME..."
        } else {
            "Scanning for nearby BLE devices (10s)..."
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()

        try {
            scanner.startScan(null, settings, scanCallback)
        } catch (e: SecurityException) {
            _isScanning.value = false
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Missing Bluetooth scan permission."
            return
        }

        scanTimeoutJob = scope.launch(Dispatchers.Main) {
            delay(SCAN_DURATION_MS)
            stopScan()
            if (autoConnectTarget && _connectionState.value != BleConnectionState.CONNECTED &&
                _connectionState.value != BleConnectionState.CONNECTING
            ) {
                _statusMessage.value = "No $DEFAULT_DEVICE_NAME found nearby."
            } else if (!autoConnectTarget && discoveredMap.isEmpty()) {
                _statusMessage.value = "No BLE devices found nearby."
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        scanTimeoutJob?.cancel()
        scanTimeoutJob = null
        if (_isScanning.value) {
            try {
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
            } catch (_: Exception) {
            }
            _isScanning.value = false
            if (_connectionState.value == BleConnectionState.SCANNING) {
                _connectionState.value = BleConnectionState.DISCONNECTED
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun connectByAddress(address: String) {
        val adapter = bluetoothAdapter
        if (adapter == null || !adapter.isEnabled) {
            _statusMessage.value = "Bluetooth is turned off. Please enable Bluetooth."
            return
        }
        stopScan()
        manualDisconnect = false
        val normalized = address.trim().uppercase()
        val device = try {
            adapter.getRemoteDevice(normalized)
        } catch (e: IllegalArgumentException) {
            _statusMessage.value = "Invalid MAC address: $address"
            return
        }
        connectToDevice(device)
    }

    @SuppressLint("MissingPermission")
    fun connectAuto(preferredAddress: String? = null) {
        if (!preferredAddress.isNullOrBlank()) {
            connectByAddress(preferredAddress)
        } else {
            startScan(autoConnectTarget = true)
        }
    }

    @SuppressLint("MissingPermission")
    private fun connectToDevice(device: BluetoothDevice) {
        closeGatt()
        _connectionState.value = BleConnectionState.CONNECTING
        _deviceAddress.value = device.address
        _deviceName.value = try {
            device.name ?: DEFAULT_DEVICE_NAME
        } catch (_: SecurityException) {
            DEFAULT_DEVICE_NAME
        }
        _statusMessage.value = "Connecting to ${_deviceName.value} [${device.address}]..."

        try {
            gatt = device.connectGatt(context, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        } catch (e: SecurityException) {
            _connectionState.value = BleConnectionState.DISCONNECTED
            _statusMessage.value = "Missing Bluetooth connect permission."
        }
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        manualDisconnect = true
        stopScan()
        closeGatt()
        _connectionState.value = BleConnectionState.DISCONNECTED
        _deviceName.value = null
        _statusMessage.value = "Disconnected."
    }

    fun clearMessage() {
        _statusMessage.value = null
    }

    @SuppressLint("MissingPermission")
    private fun closeGatt() {
        try {
            gatt?.disconnect()
            gatt?.close()
        } catch (_: Exception) {
        }
        gatt = null
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

            val item = ScannedBleDevice(
                name = name,
                address = address,
                rssi = result.rssi,
                hasTargetService = hasTargetService,
            )
            discoveredMap[address] = item
            _scannedDevices.value = discoveredMap.values
                .sortedWith(
                    compareByDescending<ScannedBleDevice> { it.hasTargetService }
                        .thenByDescending { it.rssi ?: Int.MIN_VALUE }
                )

            if (autoConnectOnScan && hasTargetService) {
                autoConnectOnScan = false
                stopScan()
                connectToDevice(device)
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

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED && status == BluetoothGatt.GATT_SUCCESS) {
                Log.i(TAG, "Connected to GATT server, requesting MTU $REQUESTED_MTU")
                _statusMessage.value = "Connected. Negotiating MTU ($REQUESTED_MTU bytes)..."
                val mtuRequested = gatt.requestMtu(REQUESTED_MTU)
                if (!mtuRequested) {
                    gatt.discoverServices()
                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.w(TAG, "Disconnected from GATT server (status=$status)")
                closeGatt()
                _connectionState.value = BleConnectionState.DISCONNECTED
                if (!manualDisconnect) {
                    _statusMessage.value = "BLE device disconnected (status $status)."
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
            Log.i(TAG, "MTU changed to $mtu (status=$status). Discovering services...")
            gatt.discoverServices()
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            if (status != BluetoothGatt.GATT_SUCCESS) {
                _statusMessage.value = "Service discovery failed (status $status)."
                disconnect()
                return
            }

            val service = gatt.getService(SERVICE_UUID)
            val characteristic = service?.getCharacteristic(CHARACTERISTIC_UUID)
            if (service == null || characteristic == null) {
                _statusMessage.value = "Target EdgeAI health service/characteristic not found on device."
                disconnect()
                return
            }

            val notifySet = gatt.setCharacteristicNotification(characteristic, true)
            if (!notifySet) {
                _statusMessage.value = "Failed to enable local GATT notifications."
                disconnect()
                return
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

        // Android 13+ (API 33+)
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                handlePayload(value)
            }
        }

        // Android 8–12 (API 26–32)
        @Deprecated("Deprecated in API 33")
        @Suppress("DEPRECATION")
        override fun onCharacteristicChanged(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
        ) {
            if (characteristic.uuid == CHARACTERISTIC_UUID) {
                characteristic.value?.let { handlePayload(it) }
            }
        }

        private fun handlePayload(bytes: ByteArray) {
            val rawString = bytes.toString(Charsets.UTF_8).trim()
            if (rawString.isEmpty()) return
            val packet = parseHealthPacket(rawString)
            if (packet != null) {
                onPacketReceived(packet, rawString)
            } else {
                Log.w(TAG, "Failed to parse BLE JSON packet: $rawString")
            }
        }
    }
}
