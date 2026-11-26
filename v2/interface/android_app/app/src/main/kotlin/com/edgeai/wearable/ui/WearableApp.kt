package com.edgeai.wearable.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ShowChart
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
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
import com.edgeai.wearable.ui.theme.AccentBlue
import com.edgeai.wearable.ui.theme.AccentBlueLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgNavy
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.TextInvert
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextSidebar

enum class WearableTab(
    val label: String,
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
) {
    OVERVIEW("Home", "System Overview", "AD8232 · MAX30102 · TMP117 · MPU6500", Icons.Rounded.Dashboard),
    ECG("ECG", "ECG Monitor", "Live raw & filtered AD8232 waveform", Icons.Rounded.MonitorHeart),
    VITALS("Vitals", "Vital Signs", "HR, SpO₂, HRV, Temp & Motion", Icons.Rounded.Favorite),
    AI("AI", "AI Results", "ESP32-S3 on-device inference", Icons.Rounded.Psychology),
    TRENDS("Trends", "Historical Trends", "Local SQLite time-series", Icons.AutoMirrored.Rounded.ShowChart),
    DEVICE("Device", "Device & Settings", "BLE scanner, diagnostics & storage", Icons.Rounded.Bluetooth),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WearableApp(
    viewModel: WearableViewModel = viewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var currentTab by rememberSaveable { mutableStateOf(WearableTab.OVERVIEW) }

    Scaffold(
        containerColor = BgPage,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BgNavy,
                    titleContentColor = TextInvert,
                ),
                title = {
                    Column {
                        Text(
                            text = currentTab.title,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextInvert,
                        )
                        Text(
                            text = currentTab.subtitle,
                            fontSize = 11.sp,
                            color = TextSidebar,
                        )
                    }
                },
                actions = {
                    Row(
                        modifier = Modifier.padding(end = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "${state.packetRate} Hz",
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSidebar,
                        )
                        StatusBadge(
                            text = when {
                                state.demoMode -> "● Demo"
                                state.isConnected -> "● BLE"
                                else -> "○ Offline"
                            },
                            level = when {
                                state.demoMode -> BadgeLevel.INFO
                                state.isConnected -> BadgeLevel.OK
                                else -> BadgeLevel.DANGER
                            },
                        )
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = BgCard,
                tonalElevation = 2.dp,
            ) {
                WearableTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = currentTab == tab,
                        onClick = { currentTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label, fontSize = 10.sp, maxLines = 1) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AccentBlue,
                            selectedTextColor = AccentBlue,
                            indicatorColor = AccentBlueLight,
                            unselectedIconColor = TextMuted,
                            unselectedTextColor = TextMuted,
                        ),
                    )
                }
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(BgPage)
                .padding(innerPadding),
        ) {
            when (currentTab) {
                WearableTab.OVERVIEW -> OverviewScreen(
                    state = state,
                    onNavigateToEcg = { currentTab = WearableTab.ECG },
                    onNavigateToVitals = { currentTab = WearableTab.VITALS },
                    onNavigateToAi = { currentTab = WearableTab.AI },
                    onNavigateToDevice = { currentTab = WearableTab.DEVICE },
                )

                WearableTab.ECG -> EcgMonitorScreen(state = state)

                WearableTab.VITALS -> VitalSignsScreen(state = state)

                WearableTab.AI -> AiResultsScreen(state = state)

                WearableTab.TRENDS -> HistoricalTrendsScreen(
                    state = state,
                    onSelectWindow = viewModel::setHistoryWindow,
                    onRefresh = viewModel::refreshHistory,
                )

                WearableTab.DEVICE -> DeviceStatusScreen(
                    state = state,
                    onScan = viewModel::startScan,
                    onStopScan = viewModel::stopScan,
                    onConnect = viewModel::connect,
                    onConnectAutoScan = viewModel::connectAutoScan,
                    onDisconnect = viewModel::disconnect,
                    onSetAutoReconnect = viewModel::setAutoReconnect,
                    onSetTargetAddress = viewModel::setTargetAddress,
                    onSetDemoMode = viewModel::setDemoMode,
                    onClearDatabase = viewModel::clearDatabase,
                )
            }
        }
    }
}
