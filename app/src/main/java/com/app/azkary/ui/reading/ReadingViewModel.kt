package com.app.azkary.ui.reading

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val localeManager: LocaleManager,
    userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val categoryId: String = checkNotNull(savedStateHandle["categoryId"])

    private val today = LocalDate.now().toString()

    // Get current language tag directly from LocaleManager
    private val currentLangTag: String
        get() = localeManager.getCurrentLanguageTag(context)

    val holdToComplete: StateFlow<Boolean> = userPreferencesRepository.holdToComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val items: Flow<List<AzkarItemUi>> = flowOf(currentLangTag).flatMapLatest { lang ->
        repository.observeItemsForCategory(
            categoryId,
            langTag = lang,
            today
        )
    }

    val weightedProgress: Flow<Float> = flowOf(currentLangTag).flatMapLatest { lang ->
        repository.getWeightedProgress(
            categoryId, today, langTag = lang
        )
    }

    fun incrementRepeat(itemId: String) {
        viewModelScope.launch {
            repository.incrementRepeat(categoryId, itemId, today)
        }
    }

    fun markItemComplete(itemId: String) {
        viewModelScope.launch {
            repository.markItemComplete(categoryId, itemId, today)
        }
    }
}
