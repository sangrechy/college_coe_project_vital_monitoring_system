package com.edgeai.wearable.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.TrackChanges
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.WearableUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.SectionTitle
import com.edgeai.wearable.ui.components.SparklineChart
import com.edgeai.wearable.ui.components.StatTile
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.components.badgeColors
import com.edgeai.wearable.ui.theme.AccentBlueDark
import com.edgeai.wearable.ui.theme.AccentBlueLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BorderStrong
import com.edgeai.wearable.ui.theme.ChartHrColor
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.ChartMotionColor
import com.edgeai.wearable.ui.theme.ChartSpo2Color
import com.edgeai.wearable.ui.theme.ChartTempColor
import com.edgeai.wearable.ui.theme.EcgRawColor
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun OverviewScreen(
    state: WearableUiState,
    onNavigateToEcg: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToDevice: () -> Unit,
) {
    val raw = state.latestPacket?.raw
    val feat = state.latestPacket?.features
    val ai = state.latestPacket?.ai

    val ecgResult = HealthLabels.getEcgClass(ai?.ecgClass)
    val stressResult = HealthLabels.getStressClass(ai?.stressClass)
    val statusResult = HealthLabels.getStatus(ai?.status)
    val hrInfo = HealthLabels.hrZone(feat?.hr)
    val spo2Info = HealthLabels.spo2Zone(raw?.spo2)
    val tempInfo = HealthLabels.tempZone(raw?.temp)

    val statusCode = ai?.status ?: -1
    val (bannerBg, bannerFg, bannerBorder) = if (statusCode < 0) {
        Triple(BgCard, TextPrimary, BorderStrong)
    } else {
        badgeColors(statusResult.badge)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (!state.isConnected) {
            AlertBanner(
                text = "BLE device not connected. Open the Device tab to scan, connect, or enable Demo Mode.",
                level = BadgeLevel.WARN,
            )
        }

        // Overall Health Status Banner
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(bannerBg)
                .border(1.dp, bannerBorder, RoundedCornerShape(12.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "OVERALL HEALTH STATUS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = bannerFg.copy(alpha = 0.75f),
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = statusResult.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = statusResult.desc,
                    fontSize = 12.sp,
                    color = TextSecondary,
                )
            }
            val icon = when {
                statusCode < 0 -> Icons.Rounded.RadioButtonUnchecked
                statusCode >= 2 -> Icons.Rounded.Warning
                statusCode == 1 -> Icons.Rounded.TrackChanges
                else -> Icons.Rounded.CheckCircle
            }
            Icon(
                imageVector = icon,
                contentDescription = statusResult.label,
                tint = bannerFg,
                modifier = Modifier.size(34.dp),
            )
        }

        // Vital Signs Grid (2 + 2 + 1)
        SectionTitle("Vital Signs")
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "Heart Rate",
                value = HealthLabels.round(feat?.hr, 0),
                unit = "bpm",
                sub = hrInfo.label,
                accentColor = ChartHrColor,
                livePulse = state.isConnected && (feat?.hr ?: 0.0) > 0.0,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "SpO₂",
                value = HealthLabels.round(raw?.spo2, 1),
                unit = "%",
                sub = spo2Info.label,
                accentColor = ChartSpo2Color,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "HRV",
                value = HealthLabels.round(feat?.hrv, 1),
                unit = "ms",
                sub = "RR variability",
                accentColor = ChartHrvColor,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Temp (TMP117)",
                value = HealthLabels.round(raw?.temp, 2),
                unit = "°C",
                sub = tempInfo.label,
                accentColor = ChartTempColor,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatTile(
                label = "Motion (MPU6500)",
                value = HealthLabels.round(feat?.motion, 2),
                unit = "g",
                sub = "Resultant G",
                accentColor = ChartMotionColor,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Signal Quality",
                value = state.signalQuality,
                unit = "",
                sub = "${state.packetRate} Hz · Tap for BLE",
                accentColor = if (state.isConnected) Color(0xFF16A34A) else TextMuted,
                onClick = onNavigateToDevice,
                modifier = Modifier.weight(1f),
            )
        }

        // ECG Sparkline Card
        ClinicalCard(onClick = onNavigateToEcg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("ECG Signal", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                    Text("Raw ADC · tap to open full view", fontSize = 11.sp, color = TextMuted)
                }
                StatusBadge(text = ecgResult.label, level = ecgResult.badge)
            }
            Spacer(modifier = Modifier.height(8.dp))
            SparklineChart(
                values = state.ecgRawBuffer.takeLast(60),
                color = EcgRawColor,
                height = 76.dp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    "RR: ${HealthLabels.round(feat?.rr, 0)} ms",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                )
                Text(
                    "Raw: ${raw?.ecg ?: "--"}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                )
                Text(
                    "Filt: ${feat?.ecgFilt ?: "--"}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = TextSecondary,
                )
            }
        }

        // AI Classifications Summary
        ClinicalCard(onClick = onNavigateToAi) {
            Text("AI Classifications", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("On-device ESP32-S3 inference · tap for details", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(10.dp))

            AiSummaryRow(
                iconText = "〜",
                iconBg = AccentBlueLight,
                iconFg = AccentBlueDark,
                title = "ECG Classification",
                value = ecgResult.label,
                desc = ecgResult.desc,
                badge = ecgResult.badge,
            )
            Spacer(modifier = Modifier.height(8.dp))
            AiSummaryRow(
                iconText = "◈",
                iconBg = Color(0xFFEDE9FE),
                iconFg = Color(0xFF6D28D9),
                title = "Stress Level",
                value = stressResult.label,
                desc = stressResult.desc,
                badge = stressResult.badge,
            )
        }

        // Raw Sensor Readings (4 x 2 grid)
        SectionTitle("Raw Sensor Readings")
        ClinicalCard {
            val items = listOf(
                Triple("ECG ADC", raw?.ecg?.toString() ?: "--", ""),
                Triple("IR", raw?.ir?.toString() ?: "--", ""),
                Triple("RED", raw?.red?.toString() ?: "--", ""),
                Triple("SpO₂", HealthLabels.round(raw?.spo2, 1), "%"),
                Triple("Accel X", raw?.ax?.toString() ?: "--", "LSB"),
                Triple("Accel Y", raw?.ay?.toString() ?: "--", "LSB"),
                Triple("Accel Z", raw?.az?.toString() ?: "--", "LSB"),
                Triple("TMP117", HealthLabels.round(raw?.temp, 2), "°C"),
            )
            items.chunked(4).forEachIndexed { rowIdx, rowItems ->
                if (rowIdx > 0) Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    rowItems.forEach { (label, v, unit) ->
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(label.uppercase(), fontSize = 10.sp, color = TextMuted)
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (unit.isNotEmpty() && v != "--") "$v$unit" else v,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AiSummaryRow(
    iconText: String,
    iconBg: Color,
    iconFg: Color,
    title: String,
    value: String,
    desc: String,
    badge: BadgeLevel,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF8FAFC))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Text(iconText, color = iconFg, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 11.sp, color = TextMuted)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(desc, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
        }
        StatusBadge(text = value, level = badge)
    }
}
