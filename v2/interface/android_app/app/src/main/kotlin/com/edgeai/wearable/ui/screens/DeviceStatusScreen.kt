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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.edgeai.wearable.ble.BleConnectionState
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.sms.SmsAlertConfig
import com.edgeai.wearable.ui.DeviceUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.KeyValueRow
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.AccentTealBorder
import com.edgeai.wearable.ui.theme.AccentTealDark
import com.edgeai.wearable.ui.theme.AccentTealLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgSoft
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

private val COOLDOWN_OPTIONS = listOf(1, 3, 5, 10)

@OptIn(ExperimentalMaterial3Api::class)
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
    onSaveSmsConfig: (SmsAlertConfig) -> Unit,
    onSendTestSms: (SmsAlertConfig) -> Unit,
) {
    val context = LocalContext.current
    var showClearDialog by remember { mutableStateOf(false) }
    var showSmsSheet by remember { mutableStateOf(false) }
    var pendingBleAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var pendingSmsAction by remember { mutableStateOf<(() -> Unit)?>(null) }
    var permissionDeniedMsg by remember { mutableStateOf<String?>(null) }

    val requiredBlePermissions = remember {
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

    val blePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val allGranted = grants.values.all { it }
        if (allGranted) {
            permissionDeniedMsg = null
            pendingBleAction?.invoke()
        } else {
            permissionDeniedMsg = "Bluetooth / Location permissions are required to scan and connect via BLE."
        }
        pendingBleAction = null
    }

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedMsg = null
            pendingSmsAction?.invoke()
        } else {
            permissionDeniedMsg = "SMS permission denied. On Android 13+, open App Info → ⋮ → Allow restricted settings, then grant SMS."
        }
        pendingSmsAction = null
    }

    fun runWithBlePermission(action: () -> Unit) {
        val missing = requiredBlePermissions.any { perm ->
            ContextCompat.checkSelfPermission(context, perm) != PackageManager.PERMISSION_GRANTED
        }
        if (missing) {
            pendingBleAction = action
            blePermissionLauncher.launch(requiredBlePermissions)
        } else {
            permissionDeniedMsg = null
            action()
        }
    }

    fun runWithSmsPermission(action: () -> Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED
        if (!granted) {
            pendingSmsAction = action
            smsPermissionLauncher.launch(Manifest.permission.SEND_SMS)
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

    if (showSmsSheet) {
        EmergencySmsBottomSheet(
            initialConfig = device.smsConfig,
            onDismiss = { showSmsSheet = false },
            onSave = { updated ->
                if (updated.enabled) {
                    runWithSmsPermission {
                        onSaveSmsConfig(updated)
                        showSmsSheet = false
                    }
                } else {
                    onSaveSmsConfig(updated)
                    showSmsSheet = false
                }
            },
            onSendTest = { updated ->
                runWithSmsPermission {
                    onSendTestSms(updated)
                }
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

    val smsCfg = device.smsConfig
    val smsActive = smsCfg.enabled && smsCfg.phoneNumber.isNotBlank()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        permissionDeniedMsg?.let { msg ->
            item(key = "perm_msg") {
                AlertBanner(text = msg, level = BadgeLevel.DANGER, icon = Icons.Rounded.Warning)
            }
        }
        device.bleMessage?.let { msg ->
            item(key = "ble_msg") {
                AlertBanner(
                    text = msg,
                    level = if (device.isConnected) BadgeLevel.OK else BadgeLevel.INFO,
                    icon = Icons.Rounded.Info,
                )
            }
        }

        // BLE Connection Card
        item(key = "ble_conn") {
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
        }

        // System Diagnostics Card
        item(key = "sys_diag") {
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
        }

        // BLE Device Scanner
        item(key = "ble_scanner") {
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
        }

        // Settings & Tools Card (includes clickable Emergency SMS Alerts row)
        item(key = "settings_tools") {
            ClinicalCard {
                Text("Settings & Tools", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(12.dp))

                // Clickable Emergency SMS Alerts setting row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(if (smsActive) AccentTealLight else BgSoft)
                        .border(
                            1.dp,
                            if (smsActive) AccentTealBorder else BorderLight,
                            RoundedCornerShape(14.dp),
                        )
                        .clickable { showSmsSheet = true }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentTeal.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Sms,
                            contentDescription = "Emergency SMS Alerts",
                            tint = AccentTealDark,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Emergency SMS Alerts",
                            fontFamily = ManropeFontFamily,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                        )
                        Text(
                            text = if (smsActive) {
                                "Active · ${smsCfg.phoneNumber} (${smsCfg.cooldownMinutes}m cooldown)"
                            } else {
                                "Tap to configure emergency phone number"
                            },
                            fontFamily = ManropeFontFamily,
                            fontSize = 11.sp,
                            color = TextSecondary,
                            maxLines = 1,
                        )
                    }
                    StatusBadge(
                        text = if (smsActive) "On" else "Off",
                        level = if (smsActive) BadgeLevel.OK else BadgeLevel.NEUTRAL,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = "Open",
                        tint = TextMuted,
                        modifier = Modifier.size(20.dp),
                    )
                }

                if (smsCfg.lastSentInfo != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = smsCfg.lastSentInfo,
                        fontFamily = IbmPlexMonoFontFamily,
                        fontSize = 10.sp,
                        color = TextMuted,
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

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
        }

        // ESP32 Hardware Reference
        item(key = "esp32_ref") {
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
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmergencySmsBottomSheet(
    initialConfig: SmsAlertConfig,
    onDismiss: () -> Unit,
    onSave: (SmsAlertConfig) -> Unit,
    onSendTest: (SmsAlertConfig) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var enabled by remember { mutableStateOf(initialConfig.enabled) }
    var phone by remember { mutableStateOf(initialConfig.phoneNumber) }
    var patientName by remember { mutableStateOf(initialConfig.patientName) }
    var customNote by remember { mutableStateOf(initialConfig.customNote) }
    var cooldownMinutes by remember { mutableIntStateOf(initialConfig.cooldownMinutes) }

    fun currentDraft() = initialConfig.copy(
        enabled = enabled,
        phoneNumber = phone.trim(),
        patientName = patientName.trim(),
        customNote = customNote.trim(),
        cooldownMinutes = cooldownMinutes,
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = BgCard,
        shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Emergency SMS Alerts",
                fontFamily = ManropeFontFamily,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Text(
                text = "Automatically texts your emergency contact when Cardiac Alert, High Risk, Critical SpO₂ (<90%), or High Fever (≥38.6°C) persists for 5 seconds.",
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgSoft)
                    .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Enable Auto-Send SMS",
                        fontFamily = ManropeFontFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                    )
                    Text(
                        "Works offline over cellular network",
                        fontFamily = ManropeFontFamily,
                        fontSize = 11.sp,
                        color = TextMuted,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = { enabled = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = AccentTeal),
                )
            }

            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Emergency Phone Number", fontFamily = ManropeFontFamily) },
                placeholder = { Text("+91 98765 43210", fontFamily = IbmPlexMonoFontFamily) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = patientName,
                onValueChange = { patientName = it },
                label = { Text("Patient Name (optional)", fontFamily = ManropeFontFamily) },
                placeholder = { Text("e.g. John Doe", fontFamily = ManropeFontFamily) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = customNote,
                onValueChange = { customNote = it },
                label = { Text("Custom Note / Location (optional)", fontFamily = ManropeFontFamily) },
                placeholder = { Text("e.g. Hostel Block B, Room 204", fontFamily = ManropeFontFamily) },
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text(
                    "Alert Cooldown (prevents repeated SMS)",
                    fontFamily = ManropeFontFamily,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary,
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    COOLDOWN_OPTIONS.forEach { mins ->
                        FilterChip(
                            selected = cooldownMinutes == mins,
                            onClick = { cooldownMinutes = mins },
                            label = {
                                Text(
                                    "${mins} min",
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = BgSoft,
                                labelColor = TextSecondary,
                                selectedContainerColor = AccentTealLight,
                                selectedLabelColor = AccentTealDark,
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = cooldownMinutes == mins,
                                borderColor = BorderLight,
                                selectedBorderColor = AccentTeal,
                            ),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedButton(
                    onClick = { onSendTest(currentDraft()) },
                    enabled = phone.isNotBlank(),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Send Test SMS", fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold)
                }
                Button(
                    onClick = { onSave(currentDraft()) },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save Settings", fontFamily = ManropeFontFamily, fontWeight = FontWeight.SemiBold)
                }
            }

            Text(
                text = "Note: On Android 13+ sideloaded APKs, if SMS permission is blocked, open App Info → ⋮ → Allow restricted settings once.",
                fontFamily = ManropeFontFamily,
                fontSize = 11.sp,
                color = TextMuted,
                lineHeight = 15.sp,
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
