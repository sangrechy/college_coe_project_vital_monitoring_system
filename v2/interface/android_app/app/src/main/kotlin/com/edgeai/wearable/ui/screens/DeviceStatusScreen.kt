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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edgeai.wearable.ble.BleConnectionState
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.DeviceUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.KeyValueRow
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.AccentTealBorder
import com.edgeai.wearable.ui.theme.AccentTealLight
import com.edgeai.wearable.ui.theme.BgSoft
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun DeviceStatusScreen(
    device: DeviceUiState,
    packetRate: Double,
    signalQuality: String,
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
            title = { Text("Clear Local History?", fontFamily = ManropeFontFamily, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will delete all ${device.dbRecordCount} records stored in the local SQLite database.",
                    fontFamily = ManropeFontFamily,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showClearDialog = false
                        onClearDatabase()
                    }
                ) { Text("Clear All", color = DangerColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) { Text("Cancel") }
            },
        )
    }

    val connBadge = when {
        device.isConnected -> BadgeLevel.OK
        device.bleState == BleConnectionState.CONNECTING || device.bleState == BleConnectionState.SCANNING -> BadgeLevel.WARN
        else -> BadgeLevel.DANGER
    }
    val connText = when {
        device.demoMode -> "Demo Active"
        device.bleState == BleConnectionState.CONNECTED -> "Active"
        device.bleState == BleConnectionState.CONNECTING -> "Connecting"
        device.bleState == BleConnectionState.SCANNING -> "Scanning"
        else -> "Inactive"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        permissionDeniedMsg?.let {
            AlertBanner(text = it, level = BadgeLevel.DANGER, icon = Icons.Rounded.Warning)
        }
        device.bleMessage?.let {
            AlertBanner(
                text = it,
                level = if (device.isConnected) BadgeLevel.OK else BadgeLevel.INFO,
                icon = Icons.Rounded.Info,
            )
        }

        // BLE Connection Card
        ClinicalCard {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("BLE Connection", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                StatusBadge(text = connText, level = connBadge, showDot = true)
            }
            Spacer(modifier = Modifier.height(8.dp))
            KeyValueRow("Status", if (device.isConnected) "Connected" else device.bleState.name)
            KeyValueRow("Device Name", device.deviceName ?: "—", mono = true)
            KeyValueRow("MAC Address", device.deviceAddress ?: "—", mono = true)
            KeyValueRow("Service UUID", "…789001", mono = true)
            KeyValueRow("Characteristic UUID", "…789002", mono = true)
            KeyValueRow("Update Rate", "~100 ms (10 Hz)", mono = true, showDivider = false)

            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (!device.isConnected) {
                    Button(
                        onClick = { runWithBlePermission { onConnect(null) } },
                        enabled = device.bleState != BleConnectionState.CONNECTING,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            if (device.bleState == BleConnectionState.CONNECTING) "Connecting…" else "Connect Target",
                            fontFamily = ManropeFontFamily,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    OutlinedButton(
                        onClick = { runWithBlePermission { onConnectAutoScan() } },
                        enabled = device.bleState != BleConnectionState.CONNECTING,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Auto-Find", fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = onDisconnect,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Disconnect", fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // System Diagnostics Card
        ClinicalCard {
            Text("System Diagnostics", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            KeyValueRow("Transport", if (device.demoMode) "Synthetic Simulator" else "Direct Android BLE", mono = true)
            KeyValueRow("Packet Rate", "$packetRate Hz", mono = true)
            KeyValueRow("Signal Quality", signalQuality)
            KeyValueRow("App Uptime", HealthLabels.formatUptime(device.uptimeSeconds), mono = true)
            KeyValueRow("Records in DB", device.dbRecordCount.toString(), mono = true)
            KeyValueRow("Storage", "SQLite WAL (on-device)", mono = true, showDivider = false)
        }

        // BLE Device Scanner
        ClinicalCard {
            Text("BLE Device Scanner", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Scan nearby BLE peripherals and tap to connect", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = {
                    if (device.isScanning) onStopScan() else runWithBlePermission { onScan() }
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                shape = RoundedCornerShape(10.dp),
            ) {
                Text(
                    if (device.isScanning) "Stop Scan" else "Scan for Devices",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            if (device.scannedDevices.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "${device.scannedDevices.size} device(s) found — tap to connect:",
                    fontFamily = ManropeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    device.scannedDevices.forEach { d ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (d.hasTargetService) AccentTealLight else BgSoft)
                                .border(
                                    1.dp,
                                    if (d.hasTargetService) AccentTealBorder else BorderLight,
                                    RoundedCornerShape(12.dp),
                                )
                                .clickable {
                                    runWithBlePermission { onConnect(d.address) }
                                }
                                .padding(horizontal = 14.dp, vertical = 11.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = if (d.hasTargetService) "${d.name} ★" else d.name,
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = d.address,
                                    fontSize = 11.sp,
                                    fontFamily = IbmPlexMonoFontFamily,
                                    color = TextMuted,
                                )
                            }
                            if (d.rssi != null) {
                                Text(
                                    text = "${d.rssi} dBm",
                                    fontSize = 11.sp,
                                    fontFamily = IbmPlexMonoFontFamily,
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
            Text("Settings & Tools", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = device.targetAddress,
                onValueChange = onSetTargetAddress,
                label = { Text("Default ESP32 MAC Address", fontFamily = ManropeFontFamily) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Auto-Reconnect BLE", fontFamily = ManropeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Retry connection every 6s when disconnected", fontFamily = ManropeFontFamily, fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = device.autoReconnect,
                    onCheckedChange = { enabled ->
                        if (enabled) runWithBlePermission { onSetAutoReconnect(true) }
                        else onSetAutoReconnect(false)
                    },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentTeal),
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Demo Simulation Mode", fontFamily = ManropeFontFamily, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Generate live 10 Hz synthetic ECG & vitals without hardware", fontFamily = ManropeFontFamily, fontSize = 11.sp, color = TextMuted)
                }
                Switch(
                    checked = device.demoMode,
                    onCheckedChange = onSetDemoMode,
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentTeal),
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
            Button(
                onClick = { showClearDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = DangerColor),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    "Clear SQLite History (${device.dbRecordCount} rows)",
                    fontFamily = ManropeFontFamily,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        // ESP32 Hardware Reference
        ClinicalCard {
            Text("ESP32 Configuration Reference", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
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
        Spacer(modifier = Modifier.height(10.dp))
    }
}
