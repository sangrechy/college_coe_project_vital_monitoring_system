package com.edgeai.wearable.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.edgeai.wearable.ble.BleConnectionState
import com.edgeai.wearable.ble.WearableBleManager
import com.edgeai.wearable.data.AiOutput
import com.edgeai.wearable.data.Features
import com.edgeai.wearable.data.HealthDatabase
import com.edgeai.wearable.data.HealthPacket
import com.edgeai.wearable.data.RawData
import com.edgeai.wearable.data.ScannedBleDevice
import com.edgeai.wearable.data.TrendPoint
import com.edgeai.wearable.data.VitalsTrendRow
import com.edgeai.wearable.data.calcSignalQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

data class WearableUiState(
    val latestPacket: HealthPacket? = null,
    val ecgRawBuffer: List<Float> = emptyList(),
    val ecgFiltBuffer: List<Float> = emptyList(),
    val packetRate: Double = 0.0,
    val signalQuality: String = "Unknown",
    val bleState: BleConnectionState = BleConnectionState.DISCONNECTED,
    val deviceName: String? = null,
    val deviceAddress: String? = null,
    val isScanning: Boolean = false,
    val scannedDevices: List<ScannedBleDevice> = emptyList(),
    val bleMessage: String? = null,
    val uptimeSeconds: Long = 0L,
    val dbRecordCount: Int = 0,
    val vitalsTrend10m: List<VitalsTrendRow> = emptyList(),
    val historyWindowMinutes: Int = 10,
    val historyEcg: List<TrendPoint> = emptyList(),
    val historyVitals: List<VitalsTrendRow> = emptyList(),
    val isHistoryLoading: Boolean = false,
    val autoReconnect: Boolean = false,
    val targetAddress: String = WearableBleManager.DEFAULT_DEVICE_ADDRESS,
    val demoMode: Boolean = false,
) {
    val isConnected: Boolean
        get() = bleState == BleConnectionState.CONNECTED || demoMode
}

class WearableViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        private const val ECG_BUFFER_SIZE = 120
    }

    private val db = HealthDatabase(application)
    private val bleManager = WearableBleManager(
        context = application,
        scope = viewModelScope,
        onPacketReceived = { packet, rawJson -> handleIncomingPacket(packet, rawJson) },
    )

    private val _uiState = MutableStateFlow(WearableUiState())
    val uiState: StateFlow<WearableUiState> = _uiState.asStateFlow()

    private val startTimeMs = System.currentTimeMillis()
    private val ecgRawRing = ArrayDeque<Float>(ECG_BUFFER_SIZE)
    private val ecgFiltRing = ArrayDeque<Float>(ECG_BUFFER_SIZE)

    private var packetsThisWindow = 0
    private var lastRateTimestampMs = System.currentTimeMillis()
    private var demoJob: Job? = null

    init {
        observeBleManager()
        startTimers()
        refreshHistory()
    }

    private fun observeBleManager() {
        viewModelScope.launch {
            combine(
                bleManager.connectionState,
                bleManager.deviceName,
                bleManager.deviceAddress,
                bleManager.isScanning,
                bleManager.scannedDevices,
            ) { state, name, address, scanning, devices ->
                BleSnapshot(state, name, address, scanning, devices)
            }.collect { snap ->
                _uiState.update { current ->
                    current.copy(
                        bleState = snap.state,
                        deviceName = if (current.demoMode) "EdgeAI_Wearable (Demo)" else snap.name,
                        deviceAddress = if (current.demoMode) "SIM:00:11:22:33:44" else snap.address,
                        isScanning = snap.scanning,
                        scannedDevices = snap.devices,
                    )
                }
            }
        }
        viewModelScope.launch {
            bleManager.statusMessage.collect { msg ->
                _uiState.update { it.copy(bleMessage = msg) }
            }
        }
    }

    private data class BleSnapshot(
        val state: BleConnectionState,
        val name: String?,
        val address: String?,
        val scanning: Boolean,
        val devices: List<ScannedBleDevice>,
    )

    private fun startTimers() {
        // 1-second uptime & packet-rate decay ticker
        viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                val now = System.currentTimeMillis()
                val elapsedSec = (now - lastRateTimestampMs) / 1000.0
                var newRate = _uiState.value.packetRate
                if (elapsedSec >= 1.0) {
                    newRate = ((packetsThisWindow / elapsedSec) * 10.0).roundToInt() / 10.0
                    packetsThisWindow = 0
                    lastRateTimestampMs = now
                }
                _uiState.update {
                    it.copy(
                        uptimeSeconds = (now - startTimeMs) / 1000L,
                        packetRate = newRate,
                    )
                }
            }
        }

        // 5-second SQLite trend & count refresh
        viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val count = db.getCount()
                val vitals10m = db.getVitalsTrend(minutes = 10)
                val window = _uiState.value.historyWindowMinutes
                val histEcg = db.getEcgTrend(minutes = window)
                val histVitals = db.getVitalsTrend(minutes = window)
                _uiState.update {
                    it.copy(
                        dbRecordCount = count,
                        vitalsTrend10m = vitals10m,
                        historyEcg = histEcg,
                        historyVitals = histVitals,
                    )
                }
                delay(5000L)
            }
        }

        // Auto-reconnect loop (every 6 seconds if enabled and disconnected)
        viewModelScope.launch {
            while (isActive) {
                delay(6000L)
                val s = _uiState.value
                if (s.autoReconnect && !s.demoMode &&
                    s.bleState == BleConnectionState.DISCONNECTED &&
                    bleManager.isBluetoothEnabled()
                ) {
                    bleManager.connectAuto(s.targetAddress.takeIf { it.isNotBlank() })
                }
            }
        }
    }

    private fun handleIncomingPacket(packet: HealthPacket, rawJson: String?) {
        packetsThisWindow++

        // Persist asynchronously to SQLite
        viewModelScope.launch(Dispatchers.IO) {
            db.insertPacket(packet, rawJson)
        }

        val ecgRaw = packet.raw?.ecg?.toFloat()
        val ecgFilt = packet.features?.ecgFilt?.toFloat()

        synchronized(ecgRawRing) {
            if (ecgRaw != null) {
                if (ecgRawRing.size >= ECG_BUFFER_SIZE) ecgRawRing.removeFirst()
                ecgRawRing.addLast(ecgRaw)
            }
            if (ecgFilt != null) {
                if (ecgFiltRing.size >= ECG_BUFFER_SIZE) ecgFiltRing.removeFirst()
                ecgFiltRing.addLast(ecgFilt)
            }
            val rawCopy = ecgRawRing.toList()
            val filtCopy = ecgFiltRing.toList()
            val quality = calcSignalQuality(packet)

            _uiState.update {
                it.copy(
                    latestPacket = packet,
                    ecgRawBuffer = rawCopy,
                    ecgFiltBuffer = filtCopy,
                    signalQuality = quality,
                )
            }
        }
    }

    fun startScan() {
        if (_uiState.value.demoMode) setDemoMode(false)
        bleManager.startScan(autoConnectTarget = false)
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connect(address: String? = null) {
        if (_uiState.value.demoMode) setDemoMode(false)
        val addr = address?.takeIf { it.isNotBlank() }
        if (addr != null) {
            bleManager.connectByAddress(addr)
        } else {
            bleManager.connectAuto(_uiState.value.targetAddress.takeIf { it.isNotBlank() })
        }
    }

    fun connectAutoScan() {
        if (_uiState.value.demoMode) setDemoMode(false)
        bleManager.startScan(autoConnectTarget = true)
    }

    fun disconnect() {
        if (_uiState.value.demoMode) {
            setDemoMode(false)
        }
        bleManager.disconnect()
    }

    fun setAutoReconnect(enabled: Boolean) {
        _uiState.update { it.copy(autoReconnect = enabled) }
    }

    fun setTargetAddress(address: String) {
        _uiState.update { it.copy(targetAddress = address) }
    }

    fun setHistoryWindow(minutes: Int) {
        _uiState.update { it.copy(historyWindowMinutes = minutes) }
        refreshHistory()
    }

    fun refreshHistory() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isHistoryLoading = true) }
            val window = _uiState.value.historyWindowMinutes
            val ecg = db.getEcgTrend(minutes = window)
            val vitals = db.getVitalsTrend(minutes = window)
            val vitals10m = db.getVitalsTrend(minutes = 10)
            val count = db.getCount()
            _uiState.update {
                it.copy(
                    historyEcg = ecg,
                    historyVitals = vitals,
                    vitalsTrend10m = vitals10m,
                    dbRecordCount = count,
                    isHistoryLoading = false,
                )
            }
        }
    }

    fun clearDatabase() {
        viewModelScope.launch(Dispatchers.IO) {
            db.clear()
            _uiState.update {
                it.copy(
                    dbRecordCount = 0,
                    vitalsTrend10m = emptyList(),
                    historyEcg = emptyList(),
                    historyVitals = emptyList(),
                    bleMessage = "Local SQLite history cleared.",
                )
            }
        }
    }

    fun setDemoMode(enabled: Boolean) {
        if (enabled == _uiState.value.demoMode) return
        if (enabled) {
            bleManager.disconnect()
            _uiState.update {
                it.copy(
                    demoMode = true,
                    deviceName = "EdgeAI_Wearable (Demo)",
                    deviceAddress = "SIM:00:11:22:33:44",
                    bleMessage = "Demo Simulation Mode active (10 Hz synthetic telemetry).",
                )
            }
            startDemoGenerator()
        } else {
            demoJob?.cancel()
            demoJob = null
            _uiState.update {
                it.copy(
                    demoMode = false,
                    deviceName = null,
                    deviceAddress = null,
                    bleMessage = "Demo Simulation Mode stopped.",
                )
            }
        }
    }

    private fun startDemoGenerator() {
        demoJob?.cancel()
        demoJob = viewModelScope.launch(Dispatchers.Default) {
            var tick = 0
            while (isActive) {
                val t = tick * 0.1
                // Synthetic P-QRS-T ECG wave around baseline 1500, R-peak ~2150
                val phase = (t % 0.85) / 0.85
                val rPeak = 680.0 * exp(-((phase - 0.25) * 28.0).let { it * it })
                val pWave = 110.0 * exp(-((phase - 0.12) * 18.0).let { it * it })
                val tWave = 180.0 * exp(-((phase - 0.50) * 11.0).let { it * it })
                val qDip = -120.0 * exp(-((phase - 0.21) * 35.0).let { it * it })
                val sDip = -160.0 * exp(-((phase - 0.29) * 35.0).let { it * it })
                val noise = Random.nextDouble(-25.0, 25.0)
                val ecgFiltVal = (1500.0 + pWave + qDip + rPeak + sDip + tWave).roundToInt()
                val ecgRawVal = (ecgFiltVal + noise).roundToInt()

                val hr = 72.0 + 4.5 * sin(t * 0.12) + Random.nextDouble(-0.8, 0.8)
                val rr = 60_000.0 / hr
                val hrv = 36.0 + 6.0 * sin(t * 0.08) + Random.nextDouble(-1.2, 1.2)
                val spo2 = (98.2 + 0.5 * sin(t * 0.05)).coerceIn(96.0, 99.8)
                val temp = 36.75 + 0.15 * sin(t * 0.03)
                val motion = 1.02 + abs(0.08 * sin(t * 0.5))

                val packet = HealthPacket(
                    raw = RawData(
                        ecg = ecgRawVal,
                        temp = (temp * 100.0).roundToInt() / 100.0,
                        ir = 72_400L + Random.nextLong(-600, 600),
                        red = 66_800L + Random.nextLong(-500, 500),
                        spo2 = (spo2 * 10.0).roundToInt() / 10.0,
                        ax = 120 + Random.nextInt(-30, 30),
                        ay = -45 + Random.nextInt(-30, 30),
                        az = 16_320 + Random.nextInt(-60, 60),
                    ),
                    features = Features(
                        ecgFilt = ecgFiltVal,
                        rr = rr.roundToInt().toDouble(),
                        hr = (hr * 10.0).roundToInt() / 10.0,
                        hrv = (hrv * 10.0).roundToInt() / 10.0,
                        motion = (motion * 100.0).roundToInt() / 100.0,
                    ),
                    ai = AiOutput(
                        ecgClass = 0,
                        stressClass = if ((tick / 150) % 2 == 0) 0 else 3,
                        status = 0,
                    ),
                )
                handleIncomingPacket(packet, null)
                tick++
                delay(100L) // 10 Hz
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        demoJob?.cancel()
        bleManager.disconnect()
        db.close()
    }
}
