package com.edgeai.wearable.sms

import com.edgeai.wearable.data.AiOutput
import com.edgeai.wearable.data.Features
import com.edgeai.wearable.data.HealthPacket
import com.edgeai.wearable.data.RawData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencySmsFormatterTest {
    @Test
    fun detectsCardiacAndHighRiskEmergencies() {
        val normal = HealthPacket(
            raw = RawData(spo2 = 98.0, temp = 36.8),
            ai = AiOutput(ecgClass = 0, stressClass = 0, status = 0),
        )
        assertNull(EmergencySmsFormatter.detectEmergencyReason(normal))

        val cardiac = HealthPacket(
            raw = RawData(spo2 = 97.5, temp = 36.9),
            ai = AiOutput(ecgClass = 1, stressClass = 0, status = 2),
        )
        assertEquals("Cardiac Alert", EmergencySmsFormatter.detectEmergencyReason(cardiac))

        val highRisk = HealthPacket(
            raw = RawData(spo2 = 96.0, temp = 37.0),
            ai = AiOutput(ecgClass = 1, stressClass = 1, status = 3),
        )
        assertEquals("High Risk (Cardiac + Stress)", EmergencySmsFormatter.detectEmergencyReason(highRisk))
    }

    @Test
    fun detectsCriticalSpo2AndFever() {
        val lowSpo2 = HealthPacket(
            raw = RawData(spo2 = 87.5, temp = 36.8),
            ai = AiOutput(status = 0),
        )
        assertNotNull(EmergencySmsFormatter.detectEmergencyReason(lowSpo2))

        val fever = HealthPacket(
            raw = RawData(spo2 = 98.0, temp = 39.2),
            ai = AiOutput(status = 0),
        )
        assertNotNull(EmergencySmsFormatter.detectEmergencyReason(fever))
    }

    @Test
    fun formatsEmergencySmsWithVitalsAndPatientName() {
        val config = SmsAlertConfig(
            enabled = true,
            phoneNumber = "+919876543210",
            patientName = "Alex",
            customNote = "Room 204",
        )
        val packet = HealthPacket(
            raw = RawData(spo2 = 95.0, temp = 37.2),
            features = Features(hr = 132.0),
            ai = AiOutput(ecgClass = 1, stressClass = 1, status = 3),
        )
        val msg = EmergencySmsFormatter.buildEmergencyMessage(config, "High Risk", packet)
        assertTrue(msg.contains("EMERGENCY ALERT (Alex): High Risk"))
        assertTrue(msg.contains("HR:132bpm"))
        assertTrue(msg.contains("ECG:PVC"))
        assertTrue(msg.contains("Room 204"))
    }
}
