package com.edgeai.wearable.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.EcgWaveSnapshot
import com.edgeai.wearable.ui.VitalsUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ChartReferenceLine
import com.edgeai.wearable.ui.components.LiveEcgAreaChartCard
import com.edgeai.wearable.ui.components.StatTile
import com.edgeai.wearable.ui.theme.BorderStrong
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.EcgFiltColor
import com.edgeai.wearable.ui.theme.EcgRawColor
import com.edgeai.wearable.ui.theme.OkColor
import com.edgeai.wearable.ui.theme.WarnColor
import kotlinx.coroutines.flow.StateFlow

private const val ECG_THRESHOLD = 1800f

@Composable
fun EcgMonitorScreen(
    vitals: VitalsUiState,
    ecgStream: StateFlow<EcgWaveSnapshot>,
) {
    val raw = vitals.latestPacket?.raw
    val feat = vitals.latestPacket?.features
    val ai = vitals.latestPacket?.ai
    val ecgResult = HealthLabels.getEcgClass(ai?.ecgClass)

    val ecgAccent = when (ecgResult.badge) {
        BadgeLevel.OK -> OkColor
        BadgeLevel.WARN -> WarnColor
        BadgeLevel.DANGER -> DangerColor
        else -> BorderStrong
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "ecg_stats_1") {
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
        }

        item(key = "ecg_stats_2") {
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
        }

        item(key = "ecg_raw_chart") {
            LiveEcgAreaChartCard(
                title = "Raw ECG Signal",
                subtitle = "AD8232 analog output · 10 Hz live stream",
                seriesLabel = "Raw ADC",
                ecgStream = ecgStream,
                useFiltered = false,
                color = EcgRawColor,
                referenceLine = ChartReferenceLine(
                    value = ECG_THRESHOLD,
                    color = DangerColor,
                    label = "Threshold (1800)",
                ),
                chartHeight = 200.dp,
                emptyMessage = "Waiting for live AD8232 ECG samples…",
            )
        }

        item(key = "ecg_filt_chart") {
            LiveEcgAreaChartCard(
                title = "Filtered ECG Signal",
                subtitle = "After 8-point moving average filter (firmware-side)",
                seriesLabel = "Filtered",
                ecgStream = ecgStream,
                useFiltered = true,
                color = EcgFiltColor,
                chartHeight = 200.dp,
                emptyMessage = "Waiting for filtered ECG samples…",
            )
        }

        item(key = "ecg_info") {
            AlertBanner(
                text = "Firmware DSP uses an 8-point moving average (ECG_FILTER_SIZE=8) and detects peaks above threshold 1800 with a 300 ms refractory period. Valid RR: 300–2000 ms.",
                level = BadgeLevel.INFO,
                icon = Icons.Rounded.Info,
            )
        }
    }
}
