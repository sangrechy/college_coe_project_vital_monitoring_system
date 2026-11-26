package com.edgeai.wearable.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.ui.theme.AccentBlue
import com.edgeai.wearable.ui.theme.AccentBlueDark
import com.edgeai.wearable.ui.theme.AccentBlueLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.DangerBg
import com.edgeai.wearable.ui.theme.DangerBorder
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.InfoBorder
import com.edgeai.wearable.ui.theme.NeutralBg
import com.edgeai.wearable.ui.theme.OkBg
import com.edgeai.wearable.ui.theme.OkBorder
import com.edgeai.wearable.ui.theme.OkColor
import com.edgeai.wearable.ui.theme.TextMuted
import com.edgeai.wearable.ui.theme.TextPrimary
import com.edgeai.wearable.ui.theme.TextSecondary
import com.edgeai.wearable.ui.theme.WarnBg
import com.edgeai.wearable.ui.theme.WarnBorder
import com.edgeai.wearable.ui.theme.WarnColor
import java.util.Locale

data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Float?>,
)

data class ChartReferenceLine(
    val value: Float,
    val color: Color,
    val label: String? = null,
)

@Composable
fun badgeColors(level: BadgeLevel): Triple<Color, Color, Color> = when (level) {
    BadgeLevel.OK -> Triple(OkBg, OkColor, OkBorder)
    BadgeLevel.WARN -> Triple(WarnBg, WarnColor, WarnBorder)
    BadgeLevel.DANGER -> Triple(DangerBg, DangerColor, DangerBorder)
    BadgeLevel.INFO -> Triple(AccentBlueLight, AccentBlueDark, InfoBorder)
    BadgeLevel.NEUTRAL -> Triple(NeutralBg, TextSecondary, BorderLight)
}

@Composable
fun StatusBadge(
    text: String,
    level: BadgeLevel,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = badgeColors(level)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(999.dp))
            .padding(horizontal = 10.dp, vertical = 3.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ClinicalCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val cardMod = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardMod.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            content = content,
        )
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.US),
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.8.sp,
        modifier = modifier.padding(top = 8.dp, bottom = 6.dp),
    )
}

@Composable
fun AlertBanner(
    text: String,
    level: BadgeLevel,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = badgeColors(level)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = text,
            color = fg,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
fun StatTile(
    label: String,
    value: String,
    unit: String = "",
    sub: String? = null,
    subBadge: BadgeLevel? = null,
    accentColor: Color = AccentBlue,
    livePulse: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pulseAlpha = if (livePulse) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(700),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
        a
    } else 1f

    val cardMod = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardMod,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(1.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                ) {
                    if (livePulse) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .alpha(pulseAlpha)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                    }
                    Text(
                        text = label,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = unit,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(bottom = 2.dp),
                        )
                    }
                }
                if (sub != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    if (subBadge != null) {
                        StatusBadge(text = sub, level = subBadge)
                    } else {
                        Text(
                            text = sub,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun GaugeBar(
    label: String,
    value: Double?,
    min: Double,
    max: Double,
    unit: String,
    color: Color,
    digits: Int = 1,
) {
    val pct = if (value == null || value.isNaN()) {
        0f
    } else {
        (((value - min) / (max - min)).coerceIn(0.0, 1.0)).toFloat()
    }
    val formattedVal = if (value == null || value.isNaN()) "--" else String.format(Locale.US, "%.${digits}f", value)

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 12.sp, color = TextSecondary)
            Text(
                text = "$formattedVal $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = FontFamily.Monospace,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(BgPage)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(min.toInt().toString(), fontSize = 10.sp, color = TextMuted)
            Text("${max.toInt()} $unit", fontSize = 10.sp, color = TextMuted)
        }
    }
}

@Composable
fun KeyValueRow(
    label: String,
    value: String,
    mono: Boolean = false,
    showDivider: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontSize = 13.sp,
                color = TextSecondary,
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = if (mono) FontFamily.Monospace else FontFamily.Default,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = BorderLight, thickness = 0.7.dp)
        }
    }
}

@Composable
fun SparklineChart(
    values: List<Float>,
    color: Color,
    height: Dp = 76.dp,
    modifier: Modifier = Modifier,
) {
    if (values.size < 2) {
        Box(
            modifier = modifier.fillMaxWidth().height(height),
            contentAlignment = Alignment.Center,
        ) {
            Text("Waiting for live signal…", fontSize = 12.sp, color = TextMuted)
        }
        return
    }

    Canvas(modifier = modifier.fillMaxWidth().height(height)) {
        val minV = values.minOrNull() ?: 0f
        val maxV = values.maxOrNull() ?: 1f
        val range = (maxV - minV).let { if (it < 1f) 1f else it }
        val pad = 6f
        val usableH = (size.height - pad * 2).coerceAtLeast(1f)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        values.forEachIndexed { idx, v ->
            val x = idx * stepX
            val y = size.height - pad - ((v - minV) / range) * usableH
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChartCard(
    title: String,
    subtitle: String? = null,
    series: List<ChartSeries>,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    chartHeight: Dp = 180.dp,
    emptyMessage: String = "Collecting data… Keep device connected.",
) {
    val allValues = series.flatMap { s -> s.values.filterNotNull() } + referenceLines.map { it.value }
    val hasEnoughData = series.any { s -> s.values.count { it != null } >= 2 }

    ClinicalCard {
        Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
        if (subtitle != null) {
            Text(subtitle, fontSize = 12.sp, color = TextMuted)
        }
        Spacer(modifier = Modifier.height(10.dp))

        if (!hasEnoughData || allValues.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().height(chartHeight),
                contentAlignment = Alignment.Center,
            ) {
                Text(emptyMessage, fontSize = 12.sp, color = TextMuted)
            }
        } else {
            val rawMin = allValues.minOrNull() ?: 0f
            val rawMax = allValues.maxOrNull() ?: 1f
            val padRange = ((rawMax - rawMin) * 0.08f).let { if (it < 0.5f) 1f else it }
            val minV = rawMin - padRange
            val maxV = rawMax + padRange
            val range = (maxV - minV).coerceAtLeast(1f)

            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Y-axis min/mid/max labels
                Column(
                    modifier = Modifier.width(44.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        String.format(Locale.US, "%.0f", maxV),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                    )
                    Text(
                        String.format(Locale.US, "%.0f", (maxV + minV) / 2f),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                    )
                    Text(
                        String.format(Locale.US, "%.0f", minV),
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = TextMuted,
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))

                Canvas(modifier = Modifier.weight(1f).fillMaxSize()) {
                    val gridEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = size.height * i / gridSteps
                        drawLine(
                            color = BorderLight,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.5f,
                            pathEffect = gridEffect,
                        )
                    }

                    // Reference lines
                    referenceLines.forEach { ref ->
                        val y = size.height - ((ref.value - minV) / range) * size.height
                        if (y in 0f..size.height) {
                            drawLine(
                                color = ref.color.copy(alpha = 0.65f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.5f,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f), 0f),
                            )
                        }
                    }

                    // Data series
                    series.forEach { s ->
                        val count = s.values.size
                        if (count < 2) return@forEach
                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                        val path = Path()
                        var started = false
                        s.values.forEachIndexed { idx, v ->
                            if (v == null || v.isNaN()) {
                                started = false
                            } else {
                                val x = idx * stepX
                                val y = size.height - ((v - minV) / range) * size.height
                                if (!started) {
                                    path.moveTo(x, y)
                                    started = true
                                } else {
                                    path.lineTo(x, y)
                                }
                            }
                        }
                        drawPath(
                            path = path,
                            color = s.color,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round),
                        )
                    }
                }
            }
        }

        // Legend
        if (series.isNotEmpty()) {
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                series.forEach { s ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(s.color)
                        )
                        Text(s.name, fontSize = 11.sp, color = TextSecondary)
                    }
                }
                referenceLines.filter { it.label != null }.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(r.color.copy(alpha = 0.7f))
                        )
                        Text(r.label!!, fontSize = 11.sp, color = r.color)
                    }
                }
            }
        }
    }
}
