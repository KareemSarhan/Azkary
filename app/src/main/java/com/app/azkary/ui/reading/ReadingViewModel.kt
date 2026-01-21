package com.app.azkary.ui.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val categoryId: String = checkNotNull(savedStateHandle["categoryId"])

    private val today = LocalDate.now().toString()

    private val langState = userPreferencesRepository.appLanguage.map { it.tag }
    private val fallbackTags = listOf("ar", "en")

    val items: Flow<List<AzkarItemUi>> = langState.flatMapLatest { lang ->
        repository.observeItemsForCategory(
            categoryId,
            langTag = if (lang == AppLanguage.SYSTEM.tag) "en" else lang,
            fallbackTags,
            today
        )
    }

    val weightedProgress: Flow<Float> = langState.flatMapLatest { lang ->
        repository.getWeightedProgress(
            categoryId, today, langTag = if (lang == AppLanguage.SYSTEM.tag) "en" else lang
        )
    }

    fun incrementRepeat(itemId: String) {
        viewModelScope.launch {
            repository.incrementRepeat(categoryId, itemId, today)
        }
    }
}
