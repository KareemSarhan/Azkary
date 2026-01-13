package com.app.azkary.ui.summary

import androidx.lifecycle.ViewModel
import com.app.azkary.data.model.CategoryUi
import com.app.azkary.data.model.SystemCategoryKey
import com.app.azkary.data.repository.AzkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class SummaryViewModel @Inject constructor(
    private val repository: AzkarRepository
) : ViewModel() {

    // In a real app, these would come from a UserPreferencesRepository
    private val langTag = "en"
    private val fallbackTags = listOf("ar")

    val categories: Flow<List<CategoryUi>> = repository.observeCategoriesWithDisplayName(
        langTag = langTag,
        fallbackTags = fallbackTags
    )
    
    val currentSession: Flow<CategoryUi?> = categories.map { list ->
        // Priority logic: Morning -> Night -> Sleep
        list.find { it.systemKey == SystemCategoryKey.MORNING }
            ?: list.find { it.systemKey == SystemCategoryKey.NIGHT }
            ?: list.firstOrNull()
    }
}
