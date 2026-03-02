package com.app.azkary.data.prefs

data class ThemeSettings(
    val themeMode: ThemeMode = ThemeMode.DARK,
    val useTrueBlack: Boolean = true
)