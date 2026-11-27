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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.edgeai.wearable.data.BadgeLevel
import com.edgeai.wearable.ui.EcgWaveSnapshot
import kotlinx.coroutines.flow.StateFlow
import com.edgeai.wearable.ui.theme.AccentTeal
import com.edgeai.wearable.ui.theme.AccentTealBorder
import com.edgeai.wearable.ui.theme.AccentTealDark
import com.edgeai.wearable.ui.theme.AccentTealLight
import com.edgeai.wearable.ui.theme.BgCard
import com.edgeai.wearable.ui.theme.BgPage
import com.edgeai.wearable.ui.theme.BgSoft
import com.edgeai.wearable.ui.theme.BorderLight
import com.edgeai.wearable.ui.theme.DangerBg
import com.edgeai.wearable.ui.theme.DangerBorder
import com.edgeai.wearable.ui.theme.DangerColor
import com.edgeai.wearable.ui.theme.GridColor
import com.edgeai.wearable.ui.theme.IbmPlexMonoFontFamily
import com.edgeai.wearable.ui.theme.ManropeFontFamily
import com.edgeai.wearable.ui.theme.NeutralBg
import com.edgeai.wearable.ui.theme.NeutralBorder
import com.edgeai.wearable.ui.theme.NeutralColor
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

@Immutable
data class ChartSeries(
    val name: String,
    val color: Color,
    val values: List<Float?>,
    val fillArea: Boolean = false,
)

@Immutable
data class ChartReferenceLine(
    val value: Float,
    val color: Color,
    val label: String? = null,
)

fun badgeColors(level: BadgeLevel): Triple<Color, Color, Color> = when (level) {
    BadgeLevel.OK -> Triple(OkBg, OkColor, OkBorder)
    BadgeLevel.WARN -> Triple(WarnBg, WarnColor, WarnBorder)
    BadgeLevel.DANGER -> Triple(DangerBg, DangerColor, DangerBorder)
    BadgeLevel.INFO -> Triple(AccentTealLight, AccentTealDark, AccentTealBorder)
    BadgeLevel.NEUTRAL -> Triple(NeutralBg, NeutralColor, NeutralBorder)
}

@Composable
fun StatusBadge(
    text: String,
    level: BadgeLevel,
    showDot: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = badgeColors(level)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(fg)
            )
        }
        Text(
            text = text,
            color = fg,
            fontFamily = ManropeFontFamily,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.75.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content,
        )
    }
}

@Composable
fun StatusHeroCard(
    eyebrow: String,
    title: String,
    description: String,
    level: BadgeLevel,
    icon: ImageVector,
    modifier: Modifier = Modifier,
) {
    val (bg, tone, border) = badgeColors(level)
    val gradient = remember(bg) {
        Brush.horizontalGradient(
            colorStops = arrayOf(
                0.0f to bg,
                0.85f to BgCard,
            )
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bg)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Circular tone icon with soft 14% halo ring (exact .status-hero-icon)
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(tone.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(CircleShape)
                    .background(tone),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = eyebrow.uppercase(Locale.US),
                fontFamily = ManropeFontFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.1.sp,
                color = tone,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                fontFamily = ManropeFontFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = TextPrimary,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 17.sp,
            )
        }
    }
}

@Composable
fun SectionTitle(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text.uppercase(Locale.US),
        fontFamily = ManropeFontFamily,
        color = TextMuted,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.9.sp,
        modifier = modifier.padding(top = 10.dp, bottom = 2.dp, start = 4.dp),
    )
}

@Composable
fun AlertBanner(
    text: String,
    level: BadgeLevel,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val (bg, fg, border) = badgeColors(level)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = fg,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = text,
            fontFamily = ManropeFontFamily,
            color = fg,
            fontSize = 12.sp,
            lineHeight = 17.sp,
            fontWeight = FontWeight.SemiBold,
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
    accentColor: Color = AccentTeal,
    icon: ImageVector? = null,
    livePulse: Boolean = false,
    monoValue: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val pulseAlpha = if (livePulse) {
        val transition = rememberInfiniteTransition(label = "pulse")
        val a by transition.animateFloat(
            initialValue = 1f,
            targetValue = 0.25f,
            animationSpec = infiniteRepeatable(
                animation = tween(750),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseAlpha",
        )
        a
    } else 1f

    val cardMod = if (onClick != null) modifier.clickable { onClick() } else modifier
    Card(
        modifier = cardMod,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BgCard),
        border = BorderStroke(0.75.dp, BorderLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (livePulse) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .graphicsLayer { alpha = pulseAlpha }
                                    .clip(CircleShape)
                                    .background(accentColor)
                            )
                        }
                        Text(
                            text = label,
                            fontFamily = ManropeFontFamily,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (icon != null) {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(accentColor.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = label,
                                tint = accentColor,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = value,
                        color = TextPrimary,
                        fontSize = if (monoValue) 22.sp else 26.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = if (monoValue) IbmPlexMonoFontFamily else ManropeFontFamily,
                        letterSpacing = (-0.6).sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (unit.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = unit,
                            fontFamily = ManropeFontFamily,
                            color = TextMuted,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 3.dp),
                        )
                    }
                }
                if (sub != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    if (subBadge != null) {
                        StatusBadge(text = sub, level = subBadge)
                    } else {
                        Text(
                            text = sub,
                            fontFamily = ManropeFontFamily,
                            color = TextMuted,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RawSensorCell(
    label: String,
    value: String,
    unit: String = "",
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(BgSoft)
            .padding(vertical = 10.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = label.uppercase(Locale.US),
            fontFamily = ManropeFontFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            color = TextMuted,
            maxLines = 1,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = value,
                fontFamily = IbmPlexMonoFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary,
                maxLines = 1,
            )
            if (unit.isNotEmpty() && value != "--") {
                Spacer(modifier = Modifier.width(2.dp))
                Text(
                    text = unit,
                    fontFamily = ManropeFontFamily,
                    fontSize = 10.sp,
                    color = TextMuted,
                )
            }
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

    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontFamily = ManropeFontFamily, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextSecondary)
            Text(
                text = "$formattedVal $unit",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = IbmPlexMonoFontFamily,
                color = TextPrimary,
            )
        }
        Spacer(modifier = Modifier.height(5.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(BgPage)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(pct)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(color)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(min.toInt().toString(), fontFamily = IbmPlexMonoFontFamily, fontSize = 10.sp, color = TextMuted)
            Text("${max.toInt()} $unit", fontFamily = IbmPlexMonoFontFamily, fontSize = 10.sp, color = TextMuted)
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
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                fontFamily = ManropeFontFamily,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = TextSecondary,
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = if (mono) IbmPlexMonoFontFamily else ManropeFontFamily,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (showDivider) {
            HorizontalDivider(color = BorderLight, thickness = 0.8.dp)
        }
    }
}

/**
 * Draw-phase-only live ECG sparkline matching Overview.js (.ecg-well + AreaChart gradient).
 * Reads EcgWaveSnapshot ONLY inside DrawScope with cached Brush/Path/Stroke — zero allocations!
 */
@Composable
fun LiveSparklineAreaChart(
    ecgStream: StateFlow<EcgWaveSnapshot>,
    color: Color,
    maxPoints: Int = 60,
    height: Dp = 110.dp,
    modifier: Modifier = Modifier,
) {
    val waveState = ecgStream.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp))
            .background(BgSoft)
            .padding(horizontal = 4.dp, vertical = 6.dp)
            .drawWithCache {
                val linePath = Path()
                val fillPath = Path()
                val strokeStyle = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                val fillBrush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.26f), color.copy(alpha = 0.0f)),
                    startY = 0f,
                    endY = size.height,
                )
                onDrawBehind {
                    val arr = waveState.value.raw
                    val total = arr.size
                    if (total < 2) return@onDrawBehind

                    val startIdx = (total - maxPoints).coerceAtLeast(0)
                    val count = total - startIdx
                    var minV = Float.POSITIVE_INFINITY
                    var maxV = Float.NEGATIVE_INFINITY
                    for (i in startIdx until total) {
                        val v = arr[i]
                        if (v < minV) minV = v
                        if (v > maxV) maxV = v
                    }
                    val range = (maxV - minV).let { if (it < 1f) 1f else it }
                    val pad = 8f
                    val usableH = (size.height - pad * 2).coerceAtLeast(1f)
                    val stepX = size.width / (count - 1).coerceAtLeast(1)

                    linePath.reset()
                    fillPath.reset()

                    for (i in 0 until count) {
                        val x = i * stepX
                        val y = size.height - pad - ((arr[startIdx + i] - minV) / range) * usableH
                        if (i == 0) {
                            linePath.moveTo(x, y)
                            fillPath.moveTo(x, size.height)
                            fillPath.lineTo(x, y)
                        } else {
                            linePath.lineTo(x, y)
                            fillPath.lineTo(x, y)
                        }
                    }
                    fillPath.lineTo(size.width, size.height)
                    fillPath.close()

                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = linePath, color = color, style = strokeStyle)
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        if (waveState.value.raw.size < 2) {
            Text("Waiting for live signal…", fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
        }
    }
}

/**
 * Draw-phase-only live ECG AreaChart card for EcgMonitorScreen.
 * Isolates 10 Hz redraws to GPU draw phase with cached Brush/Path/Stroke/Dash.
 */
@Composable
fun LiveEcgAreaChartCard(
    title: String,
    subtitle: String,
    seriesLabel: String,
    ecgStream: StateFlow<EcgWaveSnapshot>,
    useFiltered: Boolean,
    color: Color,
    referenceLine: ChartReferenceLine? = null,
    chartHeight: Dp = 200.dp,
    emptyMessage: String = "Waiting for live ECG samples…",
) {
    val waveState = ecgStream.collectAsStateWithLifecycle()

    ClinicalCard {
        Text(
            title,
            fontFamily = ManropeFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        Text(
            subtitle,
            fontFamily = ManropeFontFamily,
            fontSize = 12.sp,
            color = TextMuted,
        )
        Spacer(modifier = Modifier.height(12.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(chartHeight)
                .clip(RoundedCornerShape(12.dp))
                .background(BgSoft)
                .border(1.dp, BorderLight, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 8.dp)
                .drawWithCache {
                    val linePath = Path()
                    val fillPath = Path()
                    val strokeStyle = Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                    val refDash = PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f)
                    val fillBrush = Brush.verticalGradient(
                        colors = listOf(color.copy(alpha = 0.22f), color.copy(alpha = 0.0f)),
                        startY = 0f,
                        endY = size.height,
                    )
                    onDrawBehind {
                        val snap = waveState.value
                        val arr = if (useFiltered) snap.filt else snap.raw
                        val count = arr.size
                        if (count < 2) return@onDrawBehind

                        // Horizontal subtle gridlines
                        val gridSteps = 4
                        for (i in 0..gridSteps) {
                            val y = size.height * i / gridSteps
                            drawLine(
                                color = GridColor,
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 1.8f,
                            )
                        }

                        var rawMin = Float.POSITIVE_INFINITY
                        var rawMax = Float.NEGATIVE_INFINITY
                        for (i in 0 until count) {
                            val v = arr[i]
                            if (v < rawMin) rawMin = v
                            if (v > rawMax) rawMax = v
                        }
                        if (referenceLine != null) {
                            if (referenceLine.value < rawMin) rawMin = referenceLine.value
                            if (referenceLine.value > rawMax) rawMax = referenceLine.value
                        }
                        val padRange = ((rawMax - rawMin) * 0.08f).let { if (it < 1f) 1f else it }
                        val minV = rawMin - padRange
                        val maxV = rawMax + padRange
                        val range = (maxV - minV).coerceAtLeast(1f)

                        if (referenceLine != null) {
                            val refY = size.height - ((referenceLine.value - minV) / range) * size.height
                            if (refY in 0f..size.height) {
                                drawLine(
                                    color = referenceLine.color.copy(alpha = 0.6f),
                                    start = Offset(0f, refY),
                                    end = Offset(size.width, refY),
                                    strokeWidth = 2.5f,
                                    pathEffect = refDash,
                                )
                            }
                        }

                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                        linePath.reset()
                        fillPath.reset()
                        for (i in 0 until count) {
                            val x = i * stepX
                            val y = size.height - ((arr[i] - minV) / range) * size.height
                            if (i == 0) {
                                linePath.moveTo(x, y)
                                fillPath.moveTo(x, size.height)
                                fillPath.lineTo(x, y)
                            } else {
                                linePath.lineTo(x, y)
                                fillPath.lineTo(x, y)
                            }
                        }
                        fillPath.lineTo(size.width, size.height)
                        fillPath.close()

                        drawPath(path = fillPath, brush = fillBrush)
                        drawPath(path = linePath, color = color, style = strokeStyle)
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            val hasData = (if (useFiltered) waveState.value.filt.size else waveState.value.raw.size) >= 2
            if (!hasData) {
                Text(emptyMessage, fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Box(
                    modifier = Modifier
                        .width(14.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(color)
                )
                Text(seriesLabel, fontFamily = ManropeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
            }
            if (referenceLine?.label != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(2.dp)
                            .background(referenceLine.color.copy(alpha = 0.7f))
                    )
                    Text(referenceLine.label, fontFamily = ManropeFontFamily, fontSize = 11.sp, color = referenceLine.color)
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LineChartCard(
    title: String,
    subtitle: String? = null,
    series: List<ChartSeries>,
    referenceLines: List<ChartReferenceLine> = emptyList(),
    chartHeight: Dp = 190.dp,
    emptyMessage: String = "Collecting data… Keep device connected.",
) {
    val linePath = remember { Path() }
    val fillPath = remember { Path() }
    val strokeStyle = remember { Stroke(width = 4f, cap = StrokeCap.Round, join = StrokeJoin.Round) }
    val refDash = remember { PathEffect.dashPathEffect(floatArrayOf(10f, 8f), 0f) }

    val hasEnoughData = remember(series) {
        series.any { s -> s.values.count { it != null } >= 2 }
    }

    ClinicalCard {
        Text(
            title,
            fontFamily = ManropeFontFamily,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
        )
        if (subtitle != null) {
            Text(
                subtitle,
                fontFamily = ManropeFontFamily,
                fontSize = 12.sp,
                color = TextMuted,
            )
        }
        Spacer(modifier = Modifier.height(12.dp))

        if (!hasEnoughData) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(chartHeight)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BgSoft),
                contentAlignment = Alignment.Center,
            ) {
                Text(emptyMessage, fontFamily = ManropeFontFamily, fontSize = 12.sp, color = TextMuted)
            }
        } else {
            var rawMin = Float.POSITIVE_INFINITY
            var rawMax = Float.NEGATIVE_INFINITY
            for (s in series) {
                for (v in s.values) {
                    if (v != null && !v.isNaN()) {
                        if (v < rawMin) rawMin = v
                        if (v > rawMax) rawMax = v
                    }
                }
            }
            for (r in referenceLines) {
                if (r.value < rawMin) rawMin = r.value
                if (r.value > rawMax) rawMax = r.value
            }
            if (!rawMin.isFinite() || !rawMax.isFinite()) {
                rawMin = 0f
                rawMax = 1f
            }
            val padRange = ((rawMax - rawMin) * 0.08f).let { if (it < 0.5f) 1f else it }
            val minV = rawMin - padRange
            val maxV = rawMax + padRange
            val range = (maxV - minV).coerceAtLeast(1f)

            Row(
                modifier = Modifier.fillMaxWidth().height(chartHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    modifier = Modifier.width(44.dp).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(String.format(Locale.US, "%.0f", maxV), fontSize = 10.sp, fontFamily = IbmPlexMonoFontFamily, color = TextMuted)
                    Text(String.format(Locale.US, "%.0f", (maxV + minV) / 2f), fontSize = 10.sp, fontFamily = IbmPlexMonoFontFamily, color = TextMuted)
                    Text(String.format(Locale.US, "%.0f", minV), fontSize = 10.sp, fontFamily = IbmPlexMonoFontFamily, color = TextMuted)
                }
                Spacer(modifier = Modifier.width(8.dp))

                Canvas(modifier = Modifier.weight(1f).fillMaxSize()) {
                    // Horizontal-only subtle gridlines (matching global.css --grid)
                    val gridSteps = 4
                    for (i in 0..gridSteps) {
                        val y = size.height * i / gridSteps
                        drawLine(
                            color = GridColor,
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1.8f,
                        )
                    }

                    // Dashed reference lines
                    for (ref in referenceLines) {
                        val y = size.height - ((ref.value - minV) / range) * size.height
                        if (y in 0f..size.height) {
                            drawLine(
                                color = ref.color.copy(alpha = 0.6f),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.5f,
                                pathEffect = refDash,
                            )
                        }
                    }

                    // Series paths
                    for (s in series) {
                        val count = s.values.size
                        if (count < 2) continue
                        val stepX = size.width / (count - 1).coerceAtLeast(1)
                        linePath.reset()
                        fillPath.reset()
                        var started = false
                        var firstX = 0f
                        var lastX = 0f

                        for (idx in 0 until count) {
                            val v = s.values[idx]
                            if (v == null || v.isNaN()) {
                                started = false
                            } else {
                                val x = idx * stepX
                                val y = size.height - ((v - minV) / range) * size.height
                                if (!started) {
                                    linePath.moveTo(x, y)
                                    if (s.fillArea) {
                                        firstX = x
                                        fillPath.moveTo(x, size.height)
                                        fillPath.lineTo(x, y)
                                    }
                                    started = true
                                } else {
                                    linePath.lineTo(x, y)
                                    if (s.fillArea) fillPath.lineTo(x, y)
                                }
                                lastX = x
                            }
                        }

                        if (s.fillArea && lastX > firstX) {
                            fillPath.lineTo(lastX, size.height)
                            fillPath.close()
                            drawPath(
                                path = fillPath,
                                brush = Brush.verticalGradient(
                                    colors = listOf(s.color.copy(alpha = 0.22f), s.color.copy(alpha = 0.0f)),
                                    startY = 0f,
                                    endY = size.height,
                                ),
                            )
                        }
                        drawPath(path = linePath, color = s.color, style = strokeStyle)
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
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(3.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(s.color)
                        )
                        Text(s.name, fontFamily = ManropeFontFamily, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = TextSecondary)
                    }
                }
                referenceLines.filter { it.label != null }.forEach { r ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(14.dp)
                                .height(2.dp)
                                .background(r.color.copy(alpha = 0.7f))
                        )
                        Text(r.label!!, fontFamily = ManropeFontFamily, fontSize = 11.sp, color = r.color)
                    }
                }
            }
        }
    }
}
