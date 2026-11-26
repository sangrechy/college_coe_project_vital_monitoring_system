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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.HistoryUiState
import com.edgeai.wearable.ui.LiveTelemetryState
import com.edgeai.wearable.ui.components.ChartReferenceLine
import com.edgeai.wearable.ui.components.ChartSeries
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.GaugeBar
import com.edgeai.wearable.ui.components.LineChartCard
import com.edgeai.wearable.ui.components.StatTile
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.ChartHrColor
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.ChartMotionColor
import com.edgeai.wearable.ui.theme.ChartSpo2Color
import com.edgeai.wearable.ui.theme.ChartTempColor
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary

@Composable
fun VitalSignsScreen(
    live: LiveTelemetryState,
    history: HistoryUiState,
    isConnected: Boolean,
) {
    val raw = live.latestPacket?.raw
    val feat = live.latestPacket?.features

    val hrInfo = HealthLabels.hrZone(feat?.hr)
    val spo2Info = HealthLabels.spo2Zone(raw?.spo2)
    val tempInfo = HealthLabels.tempZone(raw?.temp)

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
                label = "Heart Rate",
                value = HealthLabels.round(feat?.hr, 0),
                unit = "bpm",
                sub = hrInfo.label,
                subBadge = hrInfo.badge,
                accentColor = ChartHrColor,
                icon = Icons.Rounded.Favorite,
                livePulse = isConnected && (feat?.hr ?: 0.0) > 0.0,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "SpO₂",
                value = HealthLabels.round(raw?.spo2, 1),
                unit = "%",
                sub = spo2Info.label,
                subBadge = spo2Info.badge,
                accentColor = ChartSpo2Color,
                icon = Icons.Rounded.WaterDrop,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "HRV",
                value = HealthLabels.round(feat?.hrv, 1),
                unit = "ms",
                sub = "RR variability",
                subBadge = BadgeLevel.INFO,
                accentColor = ChartHrvColor,
                icon = Icons.Rounded.Air,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Temp (TMP117)",
                value = HealthLabels.round(raw?.temp, 2),
                unit = "°C",
                sub = tempInfo.label,
                subBadge = tempInfo.badge,
                accentColor = ChartTempColor,
                icon = Icons.Rounded.DeviceThermostat,
                modifier = Modifier.weight(1f),
            )
        }
        StatTile(
            label = "Motion (MPU6500)",
            value = HealthLabels.round(feat?.motion, 2),
            unit = "g",
            sub = "Resultant acceleration RMS",
            accentColor = ChartMotionColor,
            icon = Icons.Rounded.DirectionsRun,
            modifier = Modifier.fillMaxWidth(),
        )

        ClinicalCard {
            Text(
                "Cardiac & Oxygenation Indicators",
                fontFamily = ManropeFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            GaugeBar("Heart Rate", feat?.hr, 40.0, 180.0, "bpm", ChartHrColor, digits = 0)
            GaugeBar("SpO₂", raw?.spo2, 85.0, 100.0, "%", ChartSpo2Color, digits = 1)
            GaugeBar("HRV", feat?.hrv, 0.0, 100.0, "ms", ChartHrvColor, digits = 1)
            GaugeBar("RR Interval", feat?.rr, 300.0, 1400.0, "ms", AccentTeal, digits = 0)
        }

        ClinicalCard {
            Text(
                "Physical Indicators (TMP117 & MPU6500)",
                fontFamily = ManropeFontFamily,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(10.dp))
            GaugeBar("Body Temp", raw?.temp, 34.0, 41.0, "°C", ChartTempColor, digits = 2)
            GaugeBar("Motion", feat?.motion, 0.0, 5.0, "g", ChartMotionColor, digits = 2)
            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderLight)
            Spacer(modifier = Modifier.height(10.dp))
            Text("Accel raw (X / Y / Z)", fontFamily = ManropeFontFamily, fontSize = 11.sp, color = TextMuted)
            Text(
                text = "${raw?.ax ?: "--"} / ${raw?.ay ?: "--"} / ${raw?.az ?: "--"} LSB",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = IbmPlexMonoFontFamily,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "IR: ${raw?.ir ?: "--"} · RED: ${raw?.red ?: "--"}",
                fontSize = 11.sp,
                fontFamily = IbmPlexMonoFontFamily,
                color = TextMuted,
            )
        }

        val trend = history.vitalsTrend10m
        LineChartCard(
            title = "10-Minute Vitals Trend",
            subtitle = "Sampled from local SQLite every 5 s (${trend.size} points)",
            series = listOf(
                ChartSeries("HR (bpm)", ChartHrColor, trend.map { it.hr?.toFloat() }),
                ChartSeries("HRV (ms)", ChartHrvColor, trend.map { it.hrv?.toFloat() }),
                ChartSeries("Temp (°C)", ChartTempColor, trend.map { it.temp?.toFloat() }),
                ChartSeries("Motion (g)", ChartMotionColor, trend.map { it.motion?.toFloat() }),
            ),
            referenceLines = listOf(
                ChartReferenceLine(60f, ChartHrColor, "HR 60"),
                ChartReferenceLine(100f, ChartHrColor, "HR 100"),
            ),
            chartHeight = 220.dp,
            emptyMessage = "Collecting data… (${trend.size} records so far)",
        )
        Spacer(modifier = Modifier.height(10.dp))
    }
}
