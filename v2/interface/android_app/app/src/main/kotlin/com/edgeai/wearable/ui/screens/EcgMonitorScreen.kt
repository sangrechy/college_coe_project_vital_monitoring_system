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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.WearableUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ChartReferenceLine
import com.edgeai.wearable.ui.components.ChartSeries
import com.edgeai.wearable.ui.components.LineChartCard
import com.edgeai.wearable.ui.components.StatTile
import com.edgeai.wearable.ui.theme.BorderStrong
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.EcgFiltColor
import com.edgeai.wearable.ui.theme.EcgRawColor
import com.edgeai.wearable.ui.theme.OkColor
import com.edgeai.wearable.ui.theme.WarnColor

private const val ECG_THRESHOLD = 1800f

@Composable
fun EcgMonitorScreen(state: WearableUiState) {
    val raw = state.latestPacket?.raw
    val feat = state.latestPacket?.features
    val ai = state.latestPacket?.ai
    val ecgResult = HealthLabels.getEcgClass(ai?.ecgClass)

    val ecgAccent = when (ecgResult.badge) {
        BadgeLevel.OK -> OkColor
        BadgeLevel.WARN -> WarnColor
        BadgeLevel.DANGER -> DangerColor
        else -> BorderStrong
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // 2x2 Stat Strip
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "Raw ECG",
                value = raw?.ecg?.toString() ?: "--",
                unit = "",
                sub = "ADC counts",
                accentColor = EcgRawColor,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Filtered ECG",
                value = feat?.ecgFilt?.toString() ?: "--",
                unit = "",
                sub = "8-pt moving avg",
                accentColor = EcgFiltColor,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "RR Interval",
                value = HealthLabels.round(feat?.rr, 0),
                unit = "ms",
                sub = "HR: ${HealthLabels.round(feat?.hr, 0)} bpm",
                accentColor = ChartHrvColor,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "ECG Class",
                value = ecgResult.label,
                unit = "",
                sub = ecgResult.label,
                subBadge = ecgResult.badge,
                accentColor = ecgAccent,
                modifier = Modifier.weight(1f),
            )
        }

        // Raw ECG Live Chart
        LineChartCard(
            title = "Raw ECG Signal",
            subtitle = "AD8232 analog output · last ${state.ecgRawBuffer.size} samples",
            series = listOf(
                ChartSeries(
                    name = "Raw ADC",
                    color = EcgRawColor,
                    values = state.ecgRawBuffer.map { it },
                )
            ),
            referenceLines = listOf(
                ChartReferenceLine(
                    value = ECG_THRESHOLD,
                    color = DangerColor,
                    label = "Threshold (1800)",
                )
            ),
            chartHeight = 190.dp,
            emptyMessage = "Waiting for live AD8232 ECG samples…",
        )

        // Filtered ECG Live Chart
        LineChartCard(
            title = "Filtered ECG Signal",
            subtitle = "After 8-point moving average filter (firmware-side)",
            series = listOf(
                ChartSeries(
                    name = "Filtered ECG",
                    color = EcgFiltColor,
                    values = state.ecgFiltBuffer.map { it },
                )
            ),
            chartHeight = 190.dp,
            emptyMessage = "Waiting for filtered ECG samples…",
        )

        AlertBanner(
            text = "Firmware DSP: 8-point moving average (ECG_FILTER_SIZE=8), peak detection above threshold 1800 with a 300 ms refractory period. Valid RR intervals: 300–2000 ms.",
            level = BadgeLevel.INFO,
        )
        Spacer(modifier = Modifier.height(8.dp))
    }
}
