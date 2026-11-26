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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MonitorHeart
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
import com.edgeai.wearable.data.LabelInfo
import com.edgeai.wearable.ui.VitalsUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.KeyValueRow
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.components.badgeColors
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.AccentTealBorder
import com.edgeai.wearable.ui.theme.AccentTealDark
import com.edgeai.wearable.ui.theme.AccentTealLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgSoft
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.BorderStrong
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun AiResultsScreen(vitals: VitalsUiState) {
    val ai = vitals.latestPacket?.ai
    val ecgCode = ai?.ecgClass ?: -1
    val stressCode = ai?.stressClass ?: -1
    val statusCode = ai?.status ?: -1

    val ecgResult = HealthLabels.getEcgClass(ecgCode)
    val stressResult = HealthLabels.getStressClass(stressCode)
    val statusResult = HealthLabels.getStatus(statusCode)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        AlertBanner(
            text = "All inference runs entirely on the ESP32-S3. Three models run on-device: ecg_predict(), stress_predict(), and fusion_predict().",
            level = BadgeLevel.INFO,
            icon = Icons.Rounded.Info,
        )

        AiClassCard(
            icon = Icons.Rounded.MonitorHeart,
            label = "ECG CLASSIFICATION · ecg_predict()",
            result = ecgResult,
            code = ecgCode,
        )
        AiClassCard(
            icon = Icons.Rounded.Bolt,
            label = "STRESS LEVEL · stress_predict()",
            result = stressResult,
            code = stressCode,
        )
        AiClassCard(
            icon = Icons.Rounded.Memory,
            label = "FUSION STATUS · fusion_predict()",
            result = statusResult,
            code = statusCode,
        )

        ClinicalCard {
            Text("ECG Model Inputs", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("ecg_predict(rr, peak_amp, beat_std, beat_energy)", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            KeyValueRow("RR Interval", "features.rr", mono = true)
            KeyValueRow("Peak Amplitude", "peak_amp (rolling max)", mono = true)
            KeyValueRow("Beat Std Dev", "beat_std (EMA)", mono = true)
            KeyValueRow("Beat Energy", "beat_energy (EMA)", mono = true, showDivider = false)
        }

        ClinicalCard {
            Text("Stress Model Inputs", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text("stress_predict(hr, hrv, temp, motion)", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(8.dp))
            KeyValueRow("Heart Rate", "features.hr", mono = true)
            KeyValueRow("HRV", "features.hrv", mono = true)
            KeyValueRow("Body Temperature", "raw.temp (TMP117)", mono = true)
            KeyValueRow("Motion Level", "features.motion (MPU6500)", mono = true, showDivider = false)
        }

        ClinicalCard {
            Text("ECG Class Legend (MIT-BIH)", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                HealthLabels.ecgClassMap.filterKeys { it >= 0 }.forEach { (code, info) ->
                    LegendItem(
                        label = "$code → ${info.label} — ${info.desc}",
                        active = ecgCode == code,
                    )
                }
            }
        }

        ClinicalCard {
            Text("Stress Class Legend (WESAD)", fontFamily = ManropeFontFamily, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                HealthLabels.stressClassMap.filterKeys { it >= 0 }.forEach { (code, info) ->
                    LegendItem(
                        label = "$code → ${info.label} — ${info.desc}",
                        active = stressCode == code,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
    }
}

@Composable
private fun AiClassCard(
    icon: ImageVector,
    label: String,
    result: LabelInfo,
    code: Int,
) {
    val (bg, fg, border) = badgeColors(result.badge)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BgCard.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = label, tint = fg, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = label,
                    fontFamily = ManropeFontFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.7.sp,
                    color = fg,
                    modifier = Modifier.weight(1f),
                )
                StatusBadge(text = "Code $code", level = result.badge)
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = result.label,
                fontFamily = ManropeFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = result.desc,
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    active: Boolean,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (active) AccentTealLight else BgSoft)
            .border(1.dp, if (active) AccentTealBorder else BorderLight, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) AccentTeal else BorderStrong)
        )
        Text(
            text = label,
            fontFamily = ManropeFontFamily,
            fontSize = 12.sp,
            color = if (active) AccentTealDark else TextSecondary,
            fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
