package com.edgeai.wearable.data

import org.json.JSONObject

data class RawData(
    val ecg: Int? = null,
    val temp: Double? = null,
    val ir: Long? = null,
    val red: Long? = null,
    val spo2: Double? = null,
    val ax: Int? = null,
    val ay: Int? = null,
    val az: Int? = null,
)

data class Features(
    val ecgFilt: Int? = null,
    val rr: Double? = null,
    val hr: Double? = null,
    val hrv: Double? = null,
    val motion: Double? = null,
)

data class AiOutput(
    val ecgClass: Int? = null,
    val stressClass: Int? = null,
    val status: Int? = null,
)

data class HealthPacket(
    val raw: RawData? = null,
    val features: Features? = null,
    val ai: AiOutput? = null,
)

data class TrendPoint(
    val tsIso: String,
    val value: Double,
)

data class VitalsTrendRow(
    val tsIso: String,
    val hr: Double?,
    val hrv: Double?,
    val spo2: Double?,
    val temp: Double?,
    val motion: Double?,
    val ecgClass: Int?,
    val stressClass: Int?,
    val status: Int?,
)

data class ScannedBleDevice(
    val name: String,
    val address: String,
    val rssi: Int?,
    val hasTargetService: Boolean = false,
)

fun calcSignalQuality(packet: HealthPacket?): String {
    val ir = packet?.raw?.ir ?: return "Unknown"
    return when {
        ir > 50_000L -> "Good"
        ir > 20_000L -> "Fair"
        else -> "Poor"
    }
}

private fun JSONObject.optIntOrNull(key: String): Int? =
    if (has(key) && !isNull(key)) optInt(key) else null

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (has(key) && !isNull(key)) optLong(key) else null

private fun JSONObject.optDoubleOrNull(key: String): Double? {
    if (!has(key) || isNull(key)) return null
    val d = optDouble(key, Double.NaN)
    return if (d.isNaN()) null else d
}

fun parseHealthPacket(jsonString: String): HealthPacket? {
    return try {
        val root = JSONObject(jsonString.trim())
        val rawObj = root.optJSONObject("raw")
        val featObj = root.optJSONObject("features")
        val aiObj = root.optJSONObject("ai")

        val raw = rawObj?.let {
            RawData(
                ecg = it.optIntOrNull("ecg"),
                temp = it.optDoubleOrNull("temp"),
                ir = it.optLongOrNull("ir"),
                red = it.optLongOrNull("red"),
                spo2 = it.optDoubleOrNull("spo2"),
                ax = it.optIntOrNull("ax"),
                ay = it.optIntOrNull("ay"),
                az = it.optIntOrNull("az"),
            )
        }
        val features = featObj?.let {
            Features(
                ecgFilt = it.optIntOrNull("ecg_filt"),
                rr = it.optDoubleOrNull("rr"),
                hr = it.optDoubleOrNull("hr"),
                hrv = it.optDoubleOrNull("hrv"),
                motion = it.optDoubleOrNull("motion"),
            )
        }
        val ai = aiObj?.let {
            AiOutput(
                ecgClass = it.optIntOrNull("ecg_class"),
                stressClass = it.optIntOrNull("stress_class"),
                status = it.optIntOrNull("status"),
            )
        }
        HealthPacket(raw = raw, features = features, ai = ai)
    } catch (_: Exception) {
        null
    }
}

fun HealthPacket.toJsonString(): String {
    val root = JSONObject()
    raw?.let { r ->
        root.put(
            "raw",
            JSONObject().apply {
                r.ecg?.let { put("ecg", it) }
                r.temp?.let { put("temp", it) }
                r.ir?.let { put("ir", it) }
                r.red?.let { put("red", it) }
                r.spo2?.let { put("spo2", it) }
                r.ax?.let { put("ax", it) }
                r.ay?.let { put("ay", it) }
                r.az?.let { put("az", it) }
            }
        )
    }
    features?.let { f ->
        root.put(
            "features",
            JSONObject().apply {
                f.ecgFilt?.let { put("ecg_filt", it) }
                f.rr?.let { put("rr", it) }
                f.hr?.let { put("hr", it) }
                f.hrv?.let { put("hrv", it) }
                f.motion?.let { put("motion", it) }
            }
        )
    }
    ai?.let { a ->
        root.put(
            "ai",
            JSONObject().apply {
                a.ecgClass?.let { put("ecg_class", it) }
                a.stressClass?.let { put("stress_class", it) }
                a.status?.let { put("status", it) }
            }
        )
    }
    return root.toString()
}
