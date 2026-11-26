package com.edgeai.wearable.data

import org.junit.Assert.assertEquals
import org.junit.Test

class HealthLabelsTest {
    @Test
    fun ecgClassesMatchFirmwareCodes() {
        assertEquals("No Signal", HealthLabels.getEcgClass(-1).label)
        assertEquals("Normal", HealthLabels.getEcgClass(0).label)
        assertEquals("PVC", HealthLabels.getEcgClass(1).label)
        assertEquals("PAC", HealthLabels.getEcgClass(2).label)
        assertEquals("LBBB", HealthLabels.getEcgClass(3).label)
        assertEquals("RBBB", HealthLabels.getEcgClass(4).label)
        assertEquals("Other Abnormal", HealthLabels.getEcgClass(5).label)
    }

    @Test
    fun stressAndStatusClassesMatchFirmwareCodes() {
        assertEquals("Baseline", HealthLabels.getStressClass(0).label)
        assertEquals("Stress", HealthLabels.getStressClass(1).label)
        assertEquals("Amusement", HealthLabels.getStressClass(2).label)
        assertEquals("Meditation", HealthLabels.getStressClass(3).label)

        assertEquals("Waiting for Signal", HealthLabels.getStatus(-1).label)
        assertEquals("Healthy", HealthLabels.getStatus(0).label)
        assertEquals("Stressed / Monitor", HealthLabels.getStatus(1).label)
        assertEquals("Cardiac Alert", HealthLabels.getStatus(2).label)
        assertEquals("High Risk", HealthLabels.getStatus(3).label)
    }

    @Test
    fun vitalZonesAndUptimeFormatting() {
        assertEquals("Normal", HealthLabels.hrZone(75.0).label)
        assertEquals("Low", HealthLabels.hrZone(52.0).label)
        assertEquals("Normal", HealthLabels.spo2Zone(98.5).label)
        assertEquals("Critical", HealthLabels.spo2Zone(88.0).label)
        assertEquals("Normal", HealthLabels.tempZone(36.8).label)
        assertEquals("Fever", HealthLabels.tempZone(39.1).label)
        assertEquals("2h 15m 30s", HealthLabels.formatUptime(8130L))
    }
}
