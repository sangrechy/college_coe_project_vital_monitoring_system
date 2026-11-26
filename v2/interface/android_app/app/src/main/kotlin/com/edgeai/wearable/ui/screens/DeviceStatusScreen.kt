package com.edgeai.wearable.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edgeai.wearable.ble.BleConnectionState
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.WearableUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.KeyValueRow
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.theme.AccentBlueLight
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.InfoBorder
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun DeviceStatusScreen(
    state: WearableUiState,
    onScan: () -> Unit,
    onStopScan: () -> Unit,
    onConnect: (String?) -> Unit,
    onConnectAutoScan: () -> Unit,
    onDisconnect: () -> Unit,
    onSetAutoReconnect: (Boolean) -> Unit,
    onSetTargetAddress: (String) -> Unit,
    onSetDemoMode: (Boolean) -> Unit,
    onClearDatabase: () -> Unit,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            permissionDeniedMsg = null
            pendingAction?.invoke()
        } else {
            permissionDeniedMsg = "Bluetooth / Location permissions are required to scan and connect via BLE."
        }
        pendingAction = null
    }

    fun runWithBlePermission(action: () -> Unit) {
        val missing = requiredPermissions.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            pendingAction = action
            permissionLauncher.launch(requiredPermissions)
        } else {
            permissionDeniedMsg = null
            action()
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text("Clear Local History?") },
            text = { Text("This will delete all ${state.dbRecordCount} records stored in the local SQLite database.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearDatabase()
                    }
                ) { Text("Clear All", color = DangerColor) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    val connBadge = when {
        state.isConnected -> BadgeLevel.OK
        state.bleState == BleConnectionState.CONNECTING || state.bleState == BleConnectionState.SCANNING -> BadgeLevel.WARN
        else -> BadgeLevel.DANGER
    }
    val connText = when {
        state.demoMode -> "Demo Active"
        state.bleState == BleConnectionState.CONNECTED -> "Active"
        state.bleState == BleConnectionState.CONNECTING -> "Connecting"
        state.bleState == BleConnectionState.SCANNING -> "Scanning"
        else -> "Inactive"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        permissionDeniedMsg?.let {
            AlertBanner(text = it, level = BadgeLevel.DANGER)
        }
        state.bleMessage?.let {
            AlertBanner(
                text = it,
                level = if (state.isConnected) BadgeLevel.OK else BadgeLevel.INFO,
            )
        }

        // BLE Connection Card
        ClinicalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BLE Connection", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                StatusBadge(text = connText, level = connBadge)
            }
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("Status", if (state.isConnected) "Connected" else state.bleState.name)
            KeyValueRow("Device Name", state.deviceName ?: "—", mono = true)
            KeyValueRow("MAC Address", state.deviceAddress ?: "—", mono = true)
            KeyValueRow("Service UUID", "…789001", mono = true)
            KeyValueRow("Characteristic UUID", "…789002", mono = true)
            KeyValueRow("Update Rate", "~100 ms (10 Hz)", mono = true, showDivider = false)

            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!state.isConnected) {
                    Button(
                        onClick = { runWithBlePermission { onConnect(null) } },
                        enabled = state.bleState != BleConnectionState.CONNECTING,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(if (state.bleState == BleConnectionState.CONNECTING) "Connecting…" else "Connect Target")
                    }
                    OutlinedButton(
                        onClick = { runWithBlePermission { onConnectAutoScan() } },
                        enabled = state.bleState != BleConnectionState.CONNECTING,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Auto-Find")
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Disconnect")
                    }
                }
            }
        }

        // System Diagnostics Card
        ClinicalCard {
            Text("System Diagnostics", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("Transport", if (state.demoMode) "Synthetic Simulator" else "Direct Android BLE", mono = true)
            KeyValueRow("Packet Rate", "${state.packetRate} Hz", mono = true)
            KeyValueRow("Signal Quality", state.signalQuality)
            KeyValueRow("App Uptime", HealthLabels.formatUptime(state.uptimeSeconds), mono = true)
            KeyValueRow("Records in DB", state.dbRecordCount.toString(), mono = true)
            KeyValueRow("Storage", "SQLite (on-device)", mono = true, showDivider = false)
        }

        // BLE Device Scanner
        ClinicalCard {
            Text("BLE Device Scanner", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("Scan nearby BLE peripherals and tap to connect", fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = {
                        if (state.isScanning) onStopScan() else runWithBlePermission { onScan() }
                    },
                ) {
                    Text(if (state.isScanning) "Stop Scan" else "Scan for Devices")
                }
            }

            if (state.scannedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "${state.scannedDevices.size} device(s) found — tap to connect:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.scannedDevices.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (d.hasTargetService) AccentBlueLight else BgPage)
                                .border(
                                    1.dp,
                                    if (d.hasTargetService) InfoBorder else BorderLight,
                                    RoundedCornerShape(8.dp),
                                )
                                .clickable {
                                    runWithBlePermission { onConnect(d.address) }
                                }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (d.hasTargetService) "${d.name} ★" else d.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = d.address,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextMuted,
                                )
                            }
                            if (d.rssi != null) {
                                Text(
                                    text = "${d.rssi} dBm",
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = TextSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Settings & Tools Card
        ClinicalCard {
            Text("Settings & Tools", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = state.targetAddress,
                onValueChange = onSetTargetAddress,
                label = { Text("Default ESP32 MAC Address") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Reconnect BLE", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Retry connection every 6s when disconnected", fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = state.autoReconnect,
                    onCheckedChange = { enabled ->
                        if (enabled) runWithBlePermission { onSetAutoReconnect(true) }
                        else onSetAutoReconnect(false)
                    },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Demo Simulation Mode", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text("Generate live 10 Hz synthetic ECG & vitals without hardware", fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = state.demoMode,
                    onCheckedChange = onSetDemoMode,
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Clear SQLite History (${state.dbRecordCount} rows)")
            }
        }

        // ESP32 Hardware Reference
        ClinicalCard {
            Text("ESP32 Configuration Reference", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("Device Name", "EdgeAI_Wearable", mono = true)
            KeyValueRow("Service UUID", "12345678-...-789001", mono = true)
            KeyValueRow("Char UUID", "12345678-...-789002", mono = true)
            KeyValueRow("Comm Method", "BLE Notifications", mono = true)
            KeyValueRow("Update Rate", "100 ms (10 Hz)", mono = true)
            KeyValueRow("SDA / SCL Pins", "GPIO 8 / GPIO 9", mono = true)
            KeyValueRow("ECG Pin", "GPIO 4 (ADC)", mono = true)
            KeyValueRow("ECG Filter", "8-pt moving avg", mono = true)
            KeyValueRow("ECG Threshold", "1800 ADC counts", mono = true)
            KeyValueRow("Refractory", "300 ms", mono = true)
            KeyValueRow("Requested MTU", "247 bytes", mono = true, showDivider = false)
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}
