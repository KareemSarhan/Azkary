package com.app.azkary.ui.summary

import androidx.lifecycle.ViewModel
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val effectiveLang = userPreferencesRepository.appLanguage.map { 
        if (it == AppLanguage.SYSTEM) Locale.getDefault().language else it.tag
    }
    private val fallbackTags = listOf("ar", "en")

    val categories: Flow<List<CategoryUi>> = effectiveLang.flatMapLatest { lang ->
        repository.observeCategoriesWithDisplayName(
            langTag = lang,
            fallbackTags = fallbackTags
        )
    }
    
    val currentSession: Flow<CategoryUi?> = categories.map { list ->
        list.find { it.systemKey == SystemCategoryKey.MORNING }
            ?: list.find { it.systemKey == SystemCategoryKey.NIGHT }
            ?: list.firstOrNull()
    }
}
