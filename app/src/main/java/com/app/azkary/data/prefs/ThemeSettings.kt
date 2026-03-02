package com.app.azkary.data.prefs

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useTrueBlack: Boolean = true
)