package com.edgeai.wearable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.ui.WearableUiState
import com.edgeai.wearable.ui.components.ChartSeries
import com.edgeai.wearable.ui.components.LineChartCard
import com.edgeai.wearable.ui.theme.ChartHrColor
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.ChartMotionColor
import com.edgeai.wearable.ui.theme.ChartSpo2Color
import com.edgeai.wearable.ui.theme.ChartTempColor
import com.edgeai.wearable.ui.theme.EcgRawColor
import com.edgeai.wearable.ui.theme.TextMuted

private val TIME_WINDOWS = listOf(2, 5, 10, 30)

@Composable
fun HistoricalTrendsScreen(
    state: WearableUiState,
    onSelectWindow: (Int) -> Unit,
    onRefresh: () -> Unit,
) {
    val window = state.historyWindowMinutes
    val vitals = state.historyVitals
    val ecg = state.historyEcg

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Window selector chips + refresh
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TIME_WINDOWS.forEach { mins ->
                FilterChip(
                    selected = window == mins,
                    onClick = { onSelectWindow(mins) },
                    label = { Text("${mins}m", fontSize = 12.sp) },
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            OutlinedButton(onClick = onRefresh) {
                Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
            }
        }

        // ECG Raw Trend
        LineChartCard(
            title = "ECG Raw Trend",
            subtitle = "Raw ADC values over the last $window minutes (${ecg.size} points)",
            series = listOf(
                ChartSeries("ECG Raw", EcgRawColor, ecg.map { it.value.toFloat() })
            ),
            chartHeight = 165.dp,
        )

        // Heart Rate & HRV Trend
        LineChartCard(
            title = "Heart Rate & HRV Trend",
            subtitle = "bpm and ms variability",
            series = listOf(
                ChartSeries("HR (bpm)", ChartHrColor, vitals.map { it.hr?.toFloat() }),
                ChartSeries("HRV (ms)", ChartHrvColor, vitals.map { it.hrv?.toFloat() }),
            ),
            chartHeight = 180.dp,
        )

        // SpO2 Trend
        LineChartCard(
            title = "SpO₂ Trend",
            subtitle = "Blood oxygen saturation from MAX30102 (%)",
            series = listOf(
                ChartSeries("SpO₂ (%)", ChartSpo2Color, vitals.map { it.spo2?.toFloat() })
            ),
            chartHeight = 155.dp,
        )

        // Temperature Trend
        LineChartCard(
            title = "Temperature Trend",
            subtitle = "Body temperature from TMP117 sensor (°C)",
            series = listOf(
                ChartSeries("Temp (°C)", ChartTempColor, vitals.map { it.temp?.toFloat() })
            ),
            chartHeight = 155.dp,
        )

        // Motion Trend
        LineChartCard(
            title = "Motion Level Trend",
            subtitle = "MPU6500 accelerometer resultant (g)",
            series = listOf(
                ChartSeries("Motion (g)", ChartMotionColor, vitals.map { it.motion?.toFloat() })
            ),
            chartHeight = 155.dp,
        )

        // AI Classification Timeline
        LineChartCard(
            title = "AI Classification Timeline",
            subtitle = "ECG class · Stress class · Overall status",
            series = listOf(
                ChartSeries("ECG Class", EcgRawColor, vitals.map { it.ecgClass?.toFloat() }),
                ChartSeries("Stress Class", ChartHrvColor, vitals.map { it.stressClass?.toFloat() }),
                ChartSeries("Status", ChartHrColor, vitals.map { it.status?.toFloat() }),
            ),
            chartHeight = 160.dp,
        )

        Text(
            text = "Auto-refreshes every 5s · ${vitals.size} vitals · ${ecg.size} ECG · Total DB rows: ${state.dbRecordCount}",
            fontSize = 11.sp,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
