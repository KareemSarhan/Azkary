package com.app.azkary.data.prefs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ThemeSettingsTest {

    @Test
    fun `default ThemeSettings has DARK theme mode`() {
        val settings = ThemeSettings()
        assertEquals(ThemeMode.DARK, settings.themeMode)
    }

    @Test
    fun `default ThemeSettings has useTrueBlack true`() {
        val settings = ThemeSettings()
        assertTrue(settings.useTrueBlack)
    }

    @Test
    fun `ThemeSettings can be created with custom theme mode`() {
        val settings = ThemeSettings(themeMode = ThemeMode.LIGHT)
        assertEquals(ThemeMode.LIGHT, settings.themeMode)
    }

    @Test
    fun `ThemeSettings can be created with custom useTrueBlack`() {
        val settings = ThemeSettings(useTrueBlack = false)
        assertFalse(settings.useTrueBlack)
    }

    @Test
    fun `ThemeSettings can be created with all custom values`() {
        val settings = ThemeSettings(
            themeMode = ThemeMode.SYSTEM,
            useTrueBlack = false
        )
        assertEquals(ThemeMode.SYSTEM, settings.themeMode)
        assertFalse(settings.useTrueBlack)
    }

    @Test
    fun `ThemeSettings copy creates new instance with updated theme mode`() {
        val original = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = true)
        val copied = original.copy(themeMode = ThemeMode.DARK)
        assertEquals(ThemeMode.DARK, copied.themeMode)
        assertTrue(copied.useTrueBlack)
    }

    @Test
    fun `ThemeSettings copy creates new instance with updated useTrueBlack`() {
        val original = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = true)
        val copied = original.copy(useTrueBlack = false)
        assertEquals(ThemeMode.DARK, copied.themeMode)
        assertFalse(copied.useTrueBlack)
    }

    @Test
    fun `ThemeSettings equality returns true for same values`() {
        val settings1 = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = false)
        val settings2 = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = false)
        assertEquals(settings1, settings2)
    }

    @Test
    fun `ThemeSettings equality returns false for different theme mode`() {
        val settings1 = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = true)
        val settings2 = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = true)
        assertNotEquals(settings1, settings2)
    }

    @Test
    fun `ThemeSettings equality returns false for different useTrueBlack`() {
        val settings1 = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = true)
        val settings2 = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = false)
        assertNotEquals(settings1, settings2)
    }

    @Test
    fun `ThemeSettings hashCode is consistent for equal instances`() {
        val settings1 = ThemeSettings(themeMode = ThemeMode.SYSTEM, useTrueBlack = true)
        val settings2 = ThemeSettings(themeMode = ThemeMode.SYSTEM, useTrueBlack = true)
        assertEquals(settings1.hashCode(), settings2.hashCode())
    }

    @Test
    fun `ThemeSettings toString contains theme mode`() {
        val settings = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = true)
        assertTrue(settings.toString().contains("LIGHT"))
    }

    @Test
    fun `ThemeSettings toString contains useTrueBlack`() {
        val settings = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = false)
        assertTrue(settings.toString().contains("useTrueBlack=false"))
    }

    @Test
    fun `ThemeMode SYSTEM exists`() {
        assertEquals("SYSTEM", ThemeMode.SYSTEM.name)
    }

    @Test
    fun `ThemeMode LIGHT exists`() {
        assertEquals("LIGHT", ThemeMode.LIGHT.name)
    }

    @Test
    fun `ThemeMode DARK exists`() {
        assertEquals("DARK", ThemeMode.DARK.name)
    }

    @Test
    fun `ThemeMode has exactly three values`() {
        assertEquals(3, ThemeMode.entries.size)
    }

    @Test
    fun `ThemeMode values are in correct order`() {
        val modes = ThemeMode.entries.toTypedArray()
        assertEquals(ThemeMode.SYSTEM, modes[0])
        assertEquals(ThemeMode.LIGHT, modes[1])
        assertEquals(ThemeMode.DARK, modes[2])
    }

    @Test
    fun `ThemeMode valueOf returns correct enum for SYSTEM`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.valueOf("SYSTEM"))
    }

    @Test
    fun `ThemeMode valueOf returns correct enum for LIGHT`() {
        assertEquals(ThemeMode.LIGHT, ThemeMode.valueOf("LIGHT"))
    }

    @Test
    fun `ThemeMode valueOf returns correct enum for DARK`() {
        assertEquals(ThemeMode.DARK, ThemeMode.valueOf("DARK"))
    }

    @Test
    fun `ThemeMode valueOf throws for invalid name`() {
        try {
            ThemeMode.valueOf("INVALID")
            assertTrue("Should have thrown IllegalArgumentException", false)
        } catch (e: IllegalArgumentException) {
            assertTrue(true)
        }
    }

    @Test
    fun `ThemeMode can be iterated`() {
        val modes = mutableListOf<ThemeMode>()
        for (mode in ThemeMode.entries) {
            modes.add(mode)
        }
        assertEquals(3, modes.size)
        assertTrue(modes.contains(ThemeMode.SYSTEM))
        assertTrue(modes.contains(ThemeMode.LIGHT))
        assertTrue(modes.contains(ThemeMode.DARK))
    }

    @Test
    fun `ThemeSettings with all theme modes`() {
        ThemeMode.entries.forEach { mode ->
            val settings = ThemeSettings(themeMode = mode, useTrueBlack = true)
            assertEquals(mode, settings.themeMode)
        }
    }

    @Test
    fun `ThemeSettings with useTrueBlack true and false`() {
        val settingsTrue = ThemeSettings(useTrueBlack = true)
        val settingsFalse = ThemeSettings(useTrueBlack = false)
        assertTrue(settingsTrue.useTrueBlack)
        assertFalse(settingsFalse.useTrueBlack)
    }

    @Test
    fun `ThemeSettings component1 returns themeMode`() {
        val settings = ThemeSettings(themeMode = ThemeMode.LIGHT, useTrueBlack = false)
        val (themeMode, _) = settings
        assertEquals(ThemeMode.LIGHT, themeMode)
    }

    @Test
    fun `ThemeSettings component2 returns useTrueBlack`() {
        val settings = ThemeSettings(themeMode = ThemeMode.DARK, useTrueBlack = true)
        val (_, useTrueBlack) = settings
        assertTrue(useTrueBlack)
    }
}
