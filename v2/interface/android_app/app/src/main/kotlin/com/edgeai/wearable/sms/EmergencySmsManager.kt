package com.edgeai.wearable.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.compose.runtime.Immutable
import androidx.core.content.ContextCompat
import com.edgeai.wearable.data.HealthLabels
import com.edgeai.wearable.data.HealthPacket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Immutable
data class SmsAlertConfig(
    val enabled: Boolean = false,
    val phoneNumber: String = "",
    val patientName: String = "",
    val customNote: String = "",
    val cooldownMinutes: Int = 5,
    val lastSentInfo: String? = null,
)

object EmergencySmsFormatter {
    fun detectEmergencyReason(packet: HealthPacket): String? {
        val status = packet.ai?.status ?: -1
        if (status == 3) return "High Risk (Cardiac + Stress)"
        if (status == 2) return "Cardiac Alert"

        val spo2 = packet.raw?.spo2
        if (spo2 != null && spo2 >= 1.0 && spo2 < 90.0) {
            return "Critical SpO2 (${HealthLabels.round(spo2, 1)}%)"
        }

        val temp = packet.raw?.temp
        if (temp != null && temp >= 38.6) {
            return "High Fever (${HealthLabels.round(temp, 1)}C)"
        }

        return null
    }

    fun buildEmergencyMessage(
        config: SmsAlertConfig,
        reason: String,
        packet: HealthPacket?,
    ): String {
        val who = config.patientName.trim().takeIf { it.isNotEmpty() }
        val header = if (who != null) "EMERGENCY ALERT ($who): $reason" else "EMERGENCY ALERT: $reason"

        val hr = HealthLabels.round(packet?.features?.hr, 0)
        val spo2 = HealthLabels.round(packet?.raw?.spo2, 1)
        val temp = HealthLabels.round(packet?.raw?.temp, 1)
        val ecg = HealthLabels.getEcgClass(packet?.ai?.ecgClass).label
        val stress = HealthLabels.getStressClass(packet?.ai?.stressClass).label

        val vitals = "HR:${hr}bpm SpO2:${spo2}% Temp:${temp}C ECG:$ecg Stress:$stress"
        val note = config.customNote.trim()
        return if (note.isNotEmpty()) "$header | $vitals | $note" else "$header | $vitals"
    }

    fun buildTestMessage(config: SmsAlertConfig): String {
        val who = config.patientName.trim().takeIf { it.isNotEmpty() }
        val prefix = if (who != null) "EdgeAI Wearable ($who)" else "EdgeAI Wearable"
        val note = config.customNote.trim()
        return if (note.isNotEmpty()) {
            "$prefix: Test emergency SMS alert is working. ($note)"
        } else {
            "$prefix: Test emergency SMS alert is working."
        }
    }
}

class EmergencySmsManager(private val context: Context) {

    companion object {
        private const val PREFS_NAME = "edgeai_sms_prefs"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_PHONE = "phone"
        private const val KEY_PATIENT = "patient"
        private const val KEY_NOTE = "note"
        private const val KEY_COOLDOWN = "cooldown"
        private const val DEBOUNCE_MS = 5_000L
    }

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var emergencyStartMs: Long = 0L
    @Volatile
    private var lastAutoSentMs: Long = 0L

    fun loadConfig(): SmsAlertConfig = SmsAlertConfig(
        enabled = prefs.getBoolean(KEY_ENABLED, false),
        phoneNumber = prefs.getString(KEY_PHONE, "") ?: "",
        patientName = prefs.getString(KEY_PATIENT, "") ?: "",
        customNote = prefs.getString(KEY_NOTE, "") ?: "",
        cooldownMinutes = prefs.getInt(KEY_COOLDOWN, 5).coerceIn(1, 60),
    )

    fun saveConfig(config: SmsAlertConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putString(KEY_PHONE, config.phoneNumber.trim())
            .putString(KEY_PATIENT, config.patientName.trim())
            .putString(KEY_NOTE, config.customNote.trim())
            .putInt(KEY_COOLDOWN, config.cooldownMinutes.coerceIn(1, 60))
            .apply()
    }

    fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.SEND_SMS) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * Evaluates an incoming live packet against emergency rules with 5s debounce + cooldown.
     * Returns the emergency reason string if an SMS should be dispatched right now, else null.
     */
    fun evaluatePacket(packet: HealthPacket, config: SmsAlertConfig, nowMs: Long = System.currentTimeMillis()): String? {
        if (!config.enabled || config.phoneNumber.isBlank()) {
            emergencyStartMs = 0L
            return null
        }

        val reason = EmergencySmsFormatter.detectEmergencyReason(packet)
        if (reason == null) {
            emergencyStartMs = 0L
            return null
        }

        if (emergencyStartMs == 0L) {
            emergencyStartMs = nowMs
            return null
        }

        val sustainedMs = nowMs - emergencyStartMs
        if (sustainedMs < DEBOUNCE_MS) return null

        val cooldownMs = config.cooldownMinutes * 60_000L
        if (lastAutoSentMs > 0L && (nowMs - lastAutoSentMs) < cooldownMs) return null

        lastAutoSentMs = nowMs
        return reason
    }

    /**
     * Sends an SMS off the main thread. Returns Result with timestamped summary or error.
     */
    fun sendSmsSync(phoneNumber: String, message: String): Result<String> {
        val cleanNumber = phoneNumber.trim()
        if (cleanNumber.isEmpty()) {
            return Result.failure(IllegalArgumentException("Please enter an emergency phone number."))
        }
        if (!hasSmsPermission()) {
            return Result.failure(SecurityException("SMS permission not granted. On Android 13+, enable 'Allow restricted settings' in App Info first."))
        }

        return try {
            val smsManager: SmsManager? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                context.getSystemService(SmsManager::class.java)
            } else {
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
            if (smsManager == null) {
                return Result.failure(IllegalStateException("SMS service unavailable on this device."))
            }

            val parts = smsManager.divideMessage(message)
            if (parts.size > 1) {
                smsManager.sendMultipartTextMessage(cleanNumber, null, parts, null, null)
            } else {
                smsManager.sendTextMessage(cleanNumber, null, message, null, null)
            }
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            Result.success("Sent to $cleanNumber at $timeStr")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
