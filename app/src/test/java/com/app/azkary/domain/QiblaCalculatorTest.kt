package com.app.azkary.domain

import com.app.azkary.data.model.LatLng
import org.junit.Assert.assertEquals
import org.junit.Test

class QiblaCalculatorTest {

    @Test
    fun `bearingToKaaba returns expected bearings for known cities`() {
        assertEquals(136.1, QiblaCalculator.bearingToKaaba(LatLng(30.0444, 31.2357)), 0.2)
        assertEquals(243.8, QiblaCalculator.bearingToKaaba(LatLng(24.7136, 46.6753)), 0.2)
        assertEquals(119.0, QiblaCalculator.bearingToKaaba(LatLng(51.5074, -0.1278)), 0.2)
        assertEquals(58.5, QiblaCalculator.bearingToKaaba(LatLng(40.7128, -74.0060)), 0.2)
    }

    @Test
    fun `normalizeDegrees wraps negative and overflowing values`() {
        assertEquals(350.0, QiblaCalculator.normalizeDegrees(-10.0), 0.0)
        assertEquals(10.0, QiblaCalculator.normalizeDegrees(370.0), 0.0)
        assertEquals(0.0, QiblaCalculator.normalizeDegrees(720.0), 0.0)
    }

    @Test
    fun `smallestTurnDegrees returns signed shortest turn`() {
        assertEquals(20.0, QiblaCalculator.smallestTurnDegrees(10.0, 350.0), 0.0)
        assertEquals(-20.0, QiblaCalculator.smallestTurnDegrees(350.0, 10.0), 0.0)
        assertEquals(180.0, QiblaCalculator.smallestTurnDegrees(180.0, 0.0), 0.0)
    }
}
