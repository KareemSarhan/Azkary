package com.app.azkary.ui.reading

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.AzkarItemUi
import com.app.azkary.data.repository.AzkarRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ReadingViewModel @Inject constructor(
    private val repository: AzkarRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    val categoryId: String = checkNotNull(savedStateHandle["categoryId"])
    private val today = LocalDate.now().toString()
    
    // In a real app, these would come from UserPreferences
    private val langTag = "en"
    private val fallbackTags = listOf("ar")

    val items: Flow<List<AzkarItemUi>> = repository.observeItemsForCategory(
        categoryId, langTag, fallbackTags, today
    )

    val weightedProgress: Flow<Float> = repository.getWeightedProgress(
        categoryId, today, langTag
    )

    fun incrementRepeat(itemId: String) {
        viewModelScope.launch {
            repository.incrementRepeat(categoryId, itemId, today)
        }
    }
}
