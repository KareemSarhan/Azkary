package com.app.azkary.ui.category

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.AvailableZikr
import com.app.azkary.data.model.CategoryItemConfig
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.UUID


@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class CategoryCreationViewModel @Inject constructor(
    private val repository: AzkarRepository,
    private val localeManager: LocaleManager,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    
    val categoryId: String? = savedStateHandle["categoryId"]
    
    private val _uiState = MutableStateFlow(CategoryCreationUiState())
    val uiState: StateFlow<CategoryCreationUiState> = _uiState.asStateFlow()
    
    val availableItems: Flow<List<AvailableZikr>> = localeManager.currentLangTagFlow.flatMapLatest { lang ->
        repository.observeAvailableItems(lang)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )
    
    fun onCategoryNameChange(name: String) {
        _uiState.value = _uiState.value.copy(categoryName = name)
    }
    
    fun onSearchQueryChange(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
    }
    
    fun onItemSelect(item: AvailableZikr) {
        val selectedItems = _uiState.value.selectedItems.toMutableList()
        if (!selectedItems.any { it.itemId == item.id }) {
            selectedItems.add(
                CategoryItemConfig(
                    itemId = item.id,
                    requiredRepeats = item.requiredRepeats,
                    isInfinite = false
                )
            )
            _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
        }
    }
    
    fun onItemRemove(itemId: String) {
        val selectedItems = _uiState.value.selectedItems.filter { it.itemId != itemId }
        _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
    }
    
    fun onItemCountChange(itemId: String, count: Int) {
        val selectedItems = _uiState.value.selectedItems.map { config ->
            if (config.itemId == itemId) {
                config.copy(requiredRepeats = count.coerceAtLeast(1))
            } else {
                config
            }
        }
        _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
    }
    
    fun onItemInfiniteToggle(itemId: String) {
        val selectedItems = _uiState.value.selectedItems.map { config ->
            if (config.itemId == itemId) {
                config.copy(isInfinite = !config.isInfinite)
            } else {
                config
            }
        }
        _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun saveCategory(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val state = _uiState.value
        
        when {
            state.categoryName.isBlank() -> {
                _uiState.value = state.copy(error = "Please enter a category name")
                onError("Please enter a category name")
            }
            state.selectedItems.isEmpty() -> {
                _uiState.value = state.copy(error = "Please select at least one zikr")
                onError("Please select at least one zikr")
            }
            else -> {
                viewModelScope.launch {
                    try {
                        repository.createCustomCategory(
                            name = state.categoryName.trim(),
                            langTag = localeManager.getCurrentLanguageTag(context),
                            itemConfigs = state.selectedItems
                        )
                        _uiState.value = CategoryCreationUiState()
                        onSuccess()
                    } catch (e: Exception) {
                        val errorMsg = "Failed to save category: ${e.message}"
                        _uiState.value = _uiState.value.copy(error = errorMsg)
                        onError(errorMsg)
                    }
                }
            }
        }
    }
    
    fun onAddCustomZikr(
        arabicText: String,
        transliteration: String,
        translation: String,
        reference: String,
        requiredRepeats: Int,
        isInfinite: Boolean
    ) {
        viewModelScope.launch {
            try {
                val itemId = repository.createCustomZikr(
                    arabicText = arabicText,
                    transliteration = transliteration,
                    translation = translation,
                    reference = reference,
                    requiredRepeats = requiredRepeats,
                    isInfinite = isInfinite,
                    langTag = localeManager.getCurrentLanguageTag(context)
                )
                
                val selectedItems = _uiState.value.selectedItems.toMutableList()
                selectedItems.add(
                    CategoryItemConfig(
                        itemId = itemId,
                        requiredRepeats = if (isInfinite) 0 else requiredRepeats,
                        isInfinite = isInfinite
                    )
                )
                _uiState.value = _uiState.value.copy(selectedItems = selectedItems)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = "Failed to create zikr: ${e.message}")
            }
        }
    }
}

data class CategoryCreationUiState(
    val categoryName: String = "",
    val searchQuery: String = "",
    val selectedItems: List<CategoryItemConfig> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)
