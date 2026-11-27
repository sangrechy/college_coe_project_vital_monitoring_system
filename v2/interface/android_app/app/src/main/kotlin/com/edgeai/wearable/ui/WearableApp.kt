package com.edgeai.wearable.ui

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.screens.AiResultsScreen
import com.edgeai.wearable.ui.screens.DeviceStatusScreen
import com.edgeai.wearable.ui.screens.EcgMonitorScreen
import com.edgeai.wearable.ui.screens.HistoricalTrendsScreen
import com.edgeai.wearable.ui.screens.OverviewScreen
import com.edgeai.wearable.ui.screens.VitalSignsScreen
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgNavActive
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextNav
import com.edgeai.wearable.ui.theme.TextNavActive
import com.edgeai.wearable.ui.theme.TextPrimary

enum class WearableTab(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    OVERVIEW("Home", "System Overview", "AD8232 · MAX30102 · TMP117 · MPU6500", Icons.Rounded.Dashboard),
    ECG("ECG", "ECG Monitor", "Live raw & filtered AD8232 signal", Icons.Rounded.MonitorHeart),
    VITALS("Vitals", "Vital Signs", "HR, SpO₂, HRV, Temp & Motion", Icons.Rounded.Favorite),
    AI("AI", "AI Results", "ESP32-S3 on-device inference", Icons.Rounded.Memory),
    TRENDS("Trends", "Historical Trends", "Local SQLite time-series", Icons.AutoMirrored.Rounded.ShowChart),
    DEVICE("Device", "Device Status", "BLE link, diagnostics & settings", Icons.Rounded.Bluetooth),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearableApp(
    viewModel: WearableViewModel = viewModel(),
) {
    val deviceState by viewModel.deviceState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(WearableTab.OVERVIEW) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            Column {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BgCard,
                        titleContentColor = TextPrimary,
                    ),
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(11.dp),
                        ) {
                            // Deep teal rounded brand-mark matching Sidebar.js
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentTeal),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.MonitorHeart,
                                    contentDescription = "Health Monitor",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            Column {
                                Text(
                                    text = currentTab.title,
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = (-0.3).sp,
                                    color = TextPrimary,
                                )
                                Text(
                                    text = currentTab.subtitle,
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextMuted,
                                    maxLines = 1,
                                )
                            }
                        }
                    },
                    actions = {
                        TopBarStatusActions(
                            packetRate = deviceState.packetRate,
                            isConnected = deviceState.isConnected,
                            demoMode = deviceState.demoMode,
                        )
                    },
                )
                HorizontalDivider(color = BorderLight, thickness = 0.75.dp)
            }
        },
        bottomBar = {
            Column {
                HorizontalDivider(color = BorderLight, thickness = 0.75.dp)
                NavigationBar(
                    containerColor = BgCard,
                    tonalElevation = 0.dp,
                ) {
                    WearableTab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = currentTab == tab,
                            onClick = { currentTab = tab },
                            icon = { Icon(tab.icon, contentDescription = tab.label, modifier = Modifier.size(21.dp)) },
                            label = {
                                Text(
                                    tab.label,
                                    fontFamily = ManropeFontFamily,
                                    fontSize = 10.sp,
                                    fontWeight = if (currentTab == tab) FontWeight.Bold else FontWeight.SemiBold,
                                    maxLines = 1,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = TextNavActive,
                                selectedTextColor = TextNavActive,
                                indicatorColor = BgNavActive,
                                unselectedIconColor = TextNav,
                                unselectedTextColor = TextNav,
                            ),
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        // Warm stone canvas with subtle radial teal glow at top-right (matching body in global.css)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(innerPadding),
        ) {
            Crossfade(targetState = currentTab, animationSpec = tween(160), label = "tab") { tab ->
            when (tab) {
                WearableTab.OVERVIEW -> {
                    val vitals by viewModel.vitalsState.collectAsStateWithLifecycle()
                    OverviewScreen(
                        vitals = vitals,
                        ecgStream = viewModel.ecgStream,
                        isConnected = deviceState.isConnected,
                        onNavigateToEcg = { currentTab = WearableTab.ECG },
                        onNavigateToVitals = { currentTab = WearableTab.VITALS },
                        onNavigateToAi = { currentTab = WearableTab.AI },
                        onNavigateToDevice = { currentTab = WearableTab.DEVICE },
                    )
                }

                WearableTab.ECG -> {
                    val vitals by viewModel.vitalsState.collectAsStateWithLifecycle()
                    EcgMonitorScreen(
                        vitals = vitals,
                        ecgStream = viewModel.ecgStream,
                    )
                }

                WearableTab.VITALS -> {
                    val vitals by viewModel.vitalsState.collectAsStateWithLifecycle()
                    val history by viewModel.historyState.collectAsStateWithLifecycle()
                    VitalSignsScreen(
                        vitals = vitals,
                        history = history,
                        isConnected = deviceState.isConnected,
                    )
                }

                WearableTab.AI -> {
                    val vitals by viewModel.vitalsState.collectAsStateWithLifecycle()
                    AiResultsScreen(vitals = vitals)
                }

                WearableTab.TRENDS -> {
                    val history by viewModel.historyState.collectAsStateWithLifecycle()
                    HistoricalTrendsScreen(
                        history = history,
                        dbRecordCount = deviceState.dbRecordCount,
                        onSelectWindow = viewModel::setHistoryWindow,
                        onRefresh = viewModel::refreshHistory,
                    )
                }

                WearableTab.DEVICE -> {
                    DeviceStatusScreen(
                        device = deviceState,
                        packetRate = deviceState.packetRate,
                        signalQuality = deviceState.signalQuality,
                        onScan = viewModel::startScan,
                        onStopScan = viewModel::stopScan,
                        onConnect = viewModel::connect,
                        onConnectAutoScan = viewModel::connectAutoScan,
                        onDisconnect = viewModel::disconnect,
                        onSetAutoReconnect = viewModel::setAutoReconnect,
                        onSetTargetAddress = viewModel::setTargetAddress,
                        onSetDemoMode = viewModel::setDemoMode,
                        onClearDatabase = viewModel::clearDatabase,
                        onSaveSmsConfig = viewModel::saveSmsConfig,
                        onSendTestSms = viewModel::sendTestSms,
                    )
                }
            }
            }
        }
    }
}

@Composable
private fun TopBarStatusActions(
    packetRate: Double,
    isConnected: Boolean,
    demoMode: Boolean,
) {
    Row(
        modifier = Modifier.padding(end = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(BgPage)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "$packetRate Hz",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = IbmPlexMonoFontFamily,
                color = TextPrimary,
            )
        }
        StatusBadge(
            text = when {
                demoMode -> "Demo"
                isConnected -> "BLE"
                else -> "Offline"
            },
            level = when {
                demoMode -> BadgeLevel.INFO
                isConnected -> BadgeLevel.OK
                else -> BadgeLevel.DANGER
            },
            showDot = true,
        )
    }
}
