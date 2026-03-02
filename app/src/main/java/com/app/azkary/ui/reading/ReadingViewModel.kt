package com.app.azkary.ui.reading

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.domain.IslamicDateProvider
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val localeManager: LocaleManager,
    private val islamicDateProvider: IslamicDateProvider,
    userPreferencesRepository: UserPreferencesRepository,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val categoryId: String? = savedStateHandle["categoryId"]

    private val todayFlow: Flow<String> = flow {
        emit(islamicDateProvider.getCurrentDate().toString())
    }

    val holdToComplete: StateFlow<Boolean> = userPreferencesRepository.holdToComplete
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    val items: Flow<List<AzkarItemUi>> = localeManager.currentLangTagFlow.flatMapLatest { lang ->
        if (categoryId == null) {
            flowOf(emptyList())
        } else {
            todayFlow.flatMapLatest { today ->
                repository.observeItemsForCategory(
                    categoryId,
                    langTag = lang,
                    today
                )
            }
        }
    }

    val weightedProgress: Flow<Float> = localeManager.currentLangTagFlow.flatMapLatest { lang ->
        if (categoryId == null) {
            flowOf(0f)
        } else {
            todayFlow.flatMapLatest { today ->
                repository.getWeightedProgress(
                    categoryId, today, langTag = lang
                )
            }
        }
    }

    fun incrementRepeat(itemId: String) {
        viewModelScope.launch {
            val id = categoryId ?: return@launch
            val today = islamicDateProvider.getCurrentDate().toString()
            repository.incrementRepeat(id, itemId, today)
        }
    }

    fun markItemComplete(itemId: String) {
        viewModelScope.launch {
            val id = categoryId ?: return@launch
            val today = islamicDateProvider.getCurrentDate().toString()
            repository.markItemComplete(id, itemId, today)
        }
    }
}
