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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.data.LabelInfo
import com.edgeai.wearable.ui.WearableUiState
import com.edgeai.wearable.ui.components.AlertBanner
import com.edgeai.wearable.ui.components.ClinicalCard
import com.edgeai.wearable.ui.components.KeyValueRow
import com.edgeai.wearable.ui.components.StatusBadge
import com.edgeai.wearable.ui.components.badgeColors
import com.edgeai.wearable.ui.theme.AccentBlue
import com.edgeai.wearable.ui.theme.AccentBlueDark
import com.edgeai.wearable.ui.theme.AccentBlueLight
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.BorderStrong
import com.edgeai.wearable.ui.theme.InfoBorder
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary

@Composable
fun AiResultsScreen(state: WearableUiState) {
    val ai = state.latestPacket?.ai
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
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AlertBanner(
            text = "All inference runs on-device on the ESP32-S3. Three embedded Random Forest / fusion models run locally: ecg_predict(), stress_predict(), and fusion_predict().",
            level = BadgeLevel.INFO,
        )

        AiClassCard(
            iconText = "〜",
            iconBg = AccentBlueLight,
            iconFg = AccentBlueDark,
            label = "ECG CLASSIFICATION · ecg_predict()",
            result = ecgResult,
            code = ecgCode,
        )
        AiClassCard(
            iconText = "◈",
            iconBg = Color(0xFFEDE9FE),
            iconFg = Color(0xFF6D28D9),
            label = "STRESS LEVEL · stress_predict()",
            result = stressResult,
            code = stressCode,
        )
        AiClassCard(
            iconText = "⊕",
            iconBg = Color(0xFFFEF3C7),
            iconFg = Color(0xFFD97706),
            label = "FUSION STATUS · fusion_predict()",
            result = statusResult,
            code = statusCode,
        )

        // Model Inputs Reference
        ClinicalCard {
            Text("ECG Model Inputs", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("ecg_predict(rr, peak_amp, beat_std, beat_energy)", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("RR Interval", "features.rr", mono = true)
            KeyValueRow("Peak Amplitude", "peak_amp (rolling max)", mono = true)
            KeyValueRow("Beat Std Dev", "beat_std (EMA)", mono = true)
            KeyValueRow("Beat Energy", "beat_energy (EMA)", mono = true, showDivider = false)
        }

        ClinicalCard {
            Text("Stress Model Inputs", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Text("stress_predict(hr, hrv, temp, motion)", fontSize = 11.sp, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            KeyValueRow("Heart Rate", "features.hr", mono = true)
            KeyValueRow("HRV", "features.hrv", mono = true)
            KeyValueRow("Body Temperature", "raw.temp (TMP117)", mono = true)
            KeyValueRow("Motion Level", "features.motion (MPU6500)", mono = true, showDivider = false)
        }

        // ECG Class Legend (MIT-BIH 6-class)
        ClinicalCard {
            Text("ECG Class Legend (MIT-BIH)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HealthLabels.ecgClassMap.filterKeys { it >= 0 }.forEach { (code, info) ->
                    LegendItem(
                        label = "$code → ${info.label} — ${info.desc}",
                        active = ecgCode == code,
                    )
                }
            }
        }

        // Stress Class Legend (WESAD 4-class)
        ClinicalCard {
            Text("Stress Class Legend (WESAD)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                HealthLabels.stressClassMap.filterKeys { it >= 0 }.forEach { (code, info) ->
                    LegendItem(
                        label = "$code → ${info.label} — ${info.desc}",
                        active = stressCode == code,
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun AiClassCard(
    iconText: String,
    iconBg: Color,
    iconFg: Color,
    label: String,
    result: LabelInfo,
    code: Int,
) {
    val (bg, fg, border) = badgeColors(result.badge)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(12.dp))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(iconBg),
                contentAlignment = Alignment.Center,
            ) {
                Text(iconText, color = iconFg, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = fg.copy(alpha = 0.85f),
                )
                Text(
                    text = result.label,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                )
            }
            StatusBadge(text = "Code $code", level = result.badge)
        }
        Text(
            text = result.desc,
            fontSize = 13.sp,
            color = TextSecondary,
            lineHeight = 18.sp,
        )
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
            .clip(RoundedCornerShape(8.dp))
            .background(if (active) AccentBlueLight else BgPage)
            .border(1.dp, if (active) InfoBorder else BorderLight, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (active) AccentBlue else BorderStrong)
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (active) AccentBlueDark else TextSecondary,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}
