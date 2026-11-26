package com.edgeai.wearable.data

import java.util.Locale

enum class BadgeLevel {
    OK, WARN, DANGER, INFO, NEUTRAL
}

data class LabelInfo(
    val label: String,
    val badge: BadgeLevel,
    val desc: String = "",
)

object HealthLabels {
    val ecgClassMap: Map<Int, LabelInfo> = mapOf(
        -1 to LabelInfo("No Signal", BadgeLevel.NEUTRAL, "Attach AD8232 ECG leads to classify rhythm"),
        0 to LabelInfo("Normal", BadgeLevel.OK, "Regular sinus cardiac rhythm detected"),
        1 to LabelInfo("PVC", BadgeLevel.DANGER, "Premature Ventricular Contraction detected"),
        2 to LabelInfo("PAC", BadgeLevel.WARN, "Premature Atrial Contraction detected"),
        3 to LabelInfo("LBBB", BadgeLevel.DANGER, "Left Bundle Branch Block pattern detected"),
        4 to LabelInfo("RBBB", BadgeLevel.DANGER, "Right Bundle Branch Block pattern detected"),
        5 to LabelInfo("Other Abnormal", BadgeLevel.DANGER, "Irregular rhythm anomaly detected"),
    )

    val stressClassMap: Map<Int, LabelInfo> = mapOf(
        -1 to LabelInfo("No Signal", BadgeLevel.NEUTRAL, "Place finger on MAX30102 for stress analysis"),
        0 to LabelInfo("Baseline", BadgeLevel.OK, "Physiological indicators calm & normal"),
        1 to LabelInfo("Stress", BadgeLevel.DANGER, "Elevated physiological stress detected"),
        2 to LabelInfo("Amusement", BadgeLevel.INFO, "Positive arousal / elevated engagement"),
        3 to LabelInfo("Meditation", BadgeLevel.OK, "Deep relaxation & steady autonomic tone"),
    )

    val statusMap: Map<Int, LabelInfo> = mapOf(
        -1 to LabelInfo("Waiting for Signal", BadgeLevel.NEUTRAL, "Attach ECG leads or place finger on MAX30102"),
        0 to LabelInfo("Healthy", BadgeLevel.OK, "All active physiological indicators within normal range"),
        1 to LabelInfo("Stressed / Monitor", BadgeLevel.WARN, "Elevated physiological stress detected"),
        2 to LabelInfo("Cardiac Alert", BadgeLevel.DANGER, "Irregular cardiac rhythm detected by Edge AI"),
        3 to LabelInfo("High Risk", BadgeLevel.DANGER, "Combined cardiac anomaly and acute stress detected"),
    )

    fun getEcgClass(code: Int?): LabelInfo = ecgClassMap[code] ?: ecgClassMap[-1]!!
    fun getStressClass(code: Int?): LabelInfo = stressClassMap[code] ?: stressClassMap[-1]!!
    fun getStatus(code: Int?): LabelInfo = statusMap[code] ?: statusMap[-1]!!

    fun hrZone(hr: Double?): LabelInfo = when {
        hr == null || hr < 1.0 -> LabelInfo("No Signal", BadgeLevel.NEUTRAL)
        hr < 60.0 -> LabelInfo("Low", BadgeLevel.WARN)
        hr <= 100.0 -> LabelInfo("Normal", BadgeLevel.OK)
        hr <= 140.0 -> LabelInfo("Elevated", BadgeLevel.WARN)
        else -> LabelInfo("High", BadgeLevel.DANGER)
    }

    fun spo2Zone(spo2: Double?): LabelInfo = when {
        spo2 == null || spo2 < 1.0 -> LabelInfo("No Signal", BadgeLevel.NEUTRAL)
        spo2 >= 95.0 -> LabelInfo("Normal", BadgeLevel.OK)
        spo2 >= 90.0 -> LabelInfo("Low", BadgeLevel.WARN)
        else -> LabelInfo("Critical", BadgeLevel.DANGER)
    }

    fun tempZone(temp: Double?): LabelInfo = when {
        temp == null || temp < 1.0 -> LabelInfo("No Signal", BadgeLevel.NEUTRAL)
        temp < 35.0 -> LabelInfo("Low", BadgeLevel.WARN)
        temp <= 37.5 -> LabelInfo("Normal", BadgeLevel.OK)
        temp <= 38.5 -> LabelInfo("Elevated", BadgeLevel.WARN)
        else -> LabelInfo("Fever", BadgeLevel.DANGER)
    }

    fun formatUptime(secs: Long): String {
        if (secs <= 0L) return "0s"
        val h = secs / 3600
        val m = (secs % 3600) / 60
        val s = secs % 60
        return when {
            h > 0 -> "${h}h ${m}m ${s}s"
            m > 0 -> "${m}m ${s}s"
            else -> "${s}s"
        }
    }

    fun round(v: Double?, digits: Int = 1): String {
        if (v == null || v.isNaN() || v.isInfinite()) return "--"
        return String.format(Locale.US, "%.${digits}f", v)
    }
}
