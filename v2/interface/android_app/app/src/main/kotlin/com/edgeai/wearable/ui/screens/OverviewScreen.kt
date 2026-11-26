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
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MonitorHeart
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.ui.LiveTelemetryState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.RawSensorCell
import com.edgeai.wearable.ui.components.SectionTitle
import com.edgeai.wearable.ui.components.SparklineAreaChart
import com.edgeai.wearable.ui.components.StatTile
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.components.StatusHeroCard
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.AccentTealDark
import com.edgeai.wearable.ui.theme.AccentTealLight
import com.edgeai.wearable.ui.theme.BgSoft
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.ChartHrColor
import com.edgeai.wearable.ui.theme.ChartHrvColor
import com.edgeai.wearable.ui.theme.ChartMotionColor
import com.edgeai.wearable.ui.theme.ChartSpo2Color
import com.edgeai.wearable.ui.theme.ChartTempColor
import com.edgeai.wearable.ui.theme.EcgRawColor
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.StressIconBg
import com.edgeai.wearable.ui.theme.StressIconFg
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun OverviewScreen(
    live: LiveTelemetryState,
    isConnected: Boolean,
    onNavigateToEcg: () -> Unit,
    onNavigateToVitals: () -> Unit,
    onNavigateToAi: () -> Unit,
    onNavigateToDevice: () -> Unit,
) {
    val raw = live.latestPacket?.raw
    val feat = live.latestPacket?.features
    val ai = live.latestPacket?.ai

    val ecgResult = HealthLabels.getEcgClass(ai?.ecgClass)
    val stressResult = HealthLabels.getStressClass(ai?.stressClass)
    val statusResult = HealthLabels.getStatus(ai?.status)
    val hrInfo = HealthLabels.hrZone(feat?.hr)
    val spo2Info = HealthLabels.spo2Zone(raw?.spo2)
    val tempInfo = HealthLabels.tempZone(raw?.temp)

    val statusCode = ai?.status ?: -1
    val heroIcon = when {
        statusCode < 0 -> Icons.Rounded.RadioButtonUnchecked
        statusCode >= 2 -> Icons.Rounded.Warning
        statusCode == 1 -> Icons.Rounded.Visibility
        else -> Icons.Rounded.CheckCircle
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        if (!isConnected) {
            AlertBanner(
                text = "BLE device not connected. Open Device Status to connect or enable Demo Mode.",
                level = BadgeLevel.WARN,
                icon = Icons.Rounded.Warning,
            )
        }

        // Overall Health Status Hero Banner
        StatusHeroCard(
            eyebrow = "Overall Health Status",
            title = statusResult.label,
            description = statusResult.desc,
            level = statusResult.badge,
            icon = heroIcon,
        )

        // Vital Signs Grid
        SectionTitle("Vital Signs")
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
                onClick = onNavigateToVitals,
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
                onClick = onNavigateToVitals,
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
                accentColor = ChartHrvColor,
                icon = Icons.Rounded.Air,
                onClick = onNavigateToVitals,
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
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatTile(
                label = "Motion (MPU6500)",
                value = HealthLabels.round(feat?.motion, 2),
                unit = "g",
                sub = "Resultant G",
                accentColor = ChartMotionColor,
                icon = Icons.Rounded.DirectionsRun,
                onClick = onNavigateToVitals,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                label = "Signal Quality",
                value = live.signalQuality,
                unit = "",
                sub = "${live.packetRate} Hz · Tap for BLE",
                accentColor = AccentTeal,
                icon = Icons.Rounded.Sensors,
                onClick = onNavigateToDevice,
                modifier = Modifier.weight(1f),
            )
        }

        // ECG Area Sparkline Card
        ClinicalCard(onClick = onNavigateToEcg) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text("ECG Signal", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Raw ADC · tap to open full view", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
                }
                StatusBadge(text = ecgResult.label, level = ecgResult.badge)
            }
            Spacer(modifier = Modifier.height(12.dp))
            SparklineAreaChart(
                values = live.ecgRawBuffer.takeLast(60),
                color = EcgRawColor,
                height = 115.dp,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                MetaValue("RR", "${HealthLabels.round(feat?.rr, 0)} ms")
                MetaValue("ECG Raw", raw?.ecg?.toString() ?: "--")
                MetaValue("ECG Filt", feat?.ecgFilt?.toString() ?: "--")
            }
        }

        // AI Classifications Summary
        ClinicalCard(onClick = onNavigateToAi) {
            Text("AI Classifications", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("Edge inference results · tap to open", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(12.dp))

            AiSummaryRow(
                icon = Icons.Rounded.MonitorHeart,
                iconBg = AccentTealLight,
                iconFg = AccentTealDark,
                title = "ECG Classification",
                value = ecgResult.label,
                desc = ecgResult.desc,
                badge = ecgResult.badge,
            )
            Spacer(modifier = Modifier.height(10.dp))
            AiSummaryRow(
                icon = Icons.Rounded.Bolt,
                iconBg = StressIconBg,
                iconFg = StressIconFg,
                title = "Stress Level",
                value = stressResult.label,
                desc = stressResult.desc,
                badge = stressResult.badge,
            )
        }

        // Raw Sensor Readings (soft stone cells matching .raw-grid)
        SectionTitle("Raw Sensor Readings")
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
        items.chunked(4).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (label, v, unit) ->
                    RawSensorCell(label = label, value = v, unit = unit, modifier = Modifier.weight(1f))
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun MetaValue(label: String, value: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(label, fontFamily = ManropeFontFamily, fontSize = 11.sp, color = TextMuted)
        Text(value, fontFamily = IbmPlexMonoFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
    }
}

@Composable
private fun AiSummaryRow(
    icon: ImageVector,
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
            .clip(RoundedCornerShape(14.dp))
            .background(BgSoft)
            .border(1.dp, BorderLight, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(iconBg),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = title, tint = iconFg, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontFamily = ManropeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Text(value, fontFamily = ManropeFontFamily, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(desc, fontFamily = ManropeFontFamily, fontSize = 11.sp, color = TextSecondary, maxLines = 2)
        }
        StatusBadge(text = value, level = badge)
    }
}
