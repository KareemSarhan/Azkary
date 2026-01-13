package com.app.azkary.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.prefs.LayoutDirection
import com.app.azkary.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {
    val layoutDirection = userPreferencesRepository.layoutDirection

    fun setLayoutDirection(direction: LayoutDirection) {
        viewModelScope.launch {
            userPreferencesRepository.setLayoutDirection(direction)
        }
    }
}
