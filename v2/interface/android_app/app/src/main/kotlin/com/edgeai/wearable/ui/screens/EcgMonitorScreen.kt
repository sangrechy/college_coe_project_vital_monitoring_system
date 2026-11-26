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
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.LiveTelemetryState
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
fun EcgMonitorScreen(live: LiveTelemetryState) {
    val raw = live.latestPacket?.raw
    val feat = live.latestPacket?.features
    val ai = live.latestPacket?.ai
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
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Raw ECG",
                value = raw?.ecg?.toString() ?: "--",
                sub = "ADC counts",
                accentColor = EcgRawColor,
                monoValue = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Filtered ECG",
                value = feat?.ecgFilt?.toString() ?: "--",
                sub = "8-pt moving avg",
                accentColor = EcgFiltColor,
                monoValue = true,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "RR Interval",
                value = HealthLabels.round(feat?.rr, 0),
                unit = "ms",
                sub = "HR: ${HealthLabels.round(feat?.hr, 0)} bpm",
                accentColor = ChartHrvColor,
                monoValue = true,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "ECG Class",
                value = ecgResult.label,
                sub = ecgResult.label,
                subBadge = ecgResult.badge,
                accentColor = ecgAccent,
                modifier = Modifier.weight(1f),
            )
        }

        // Raw ECG Area Chart (Deep Teal)
        LineChartCard(
            title = "Raw ECG Signal",
            subtitle = "AD8232 analog output · last ${live.ecgRawBuffer.size} samples",
            series = listOf(
                ChartSeries(
                    name = "Raw ADC",
                    color = EcgRawColor,
                    values = live.ecgRawBuffer,
                    fillArea = true,
                )
            ),
            referenceLines = listOf(
                ChartReferenceLine(
                    value = ECG_THRESHOLD,
                    color = DangerColor,
                    label = "Threshold (1800)",
                )
            ),
            chartHeight = 200.dp,
            emptyMessage = "Waiting for live AD8232 ECG samples…",
        )

        // Filtered ECG Area Chart (Warm Terracotta)
        LineChartCard(
            title = "Filtered ECG Signal",
            subtitle = "After 8-point moving average filter (firmware-side)",
            series = listOf(
                ChartSeries(
                    name = "Filtered",
                    color = EcgFiltColor,
                    values = live.ecgFiltBuffer,
                    fillArea = true,
                )
            ),
            chartHeight = 200.dp,
            emptyMessage = "Waiting for filtered ECG samples…",
        )

        AlertBanner(
            text = "Firmware DSP uses an 8-point moving average (ECG_FILTER_SIZE=8) and detects peaks above threshold 1800 with a 300 ms refractory period. Valid RR: 300–2000 ms.",
            level = BadgeLevel.INFO,
            icon = Icons.Rounded.Info,
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}
