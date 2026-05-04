package com.app.azkary.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.QuranUiState
import com.app.azkary.data.quran.QuranRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class QuranReadingViewModel @Inject constructor(
    private val quranRepository: QuranRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val surahNumber: Int = savedStateHandle["surahNumber"] ?: 1

    private val _uiState = MutableStateFlow<QuranUiState>(QuranUiState.Loading)
    val uiState: StateFlow<QuranUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            try {
                val surah = quranRepository.getSurah(surahNumber)
                _uiState.value = if (surah != null) QuranUiState.Success(surah) else QuranUiState.Error
            } catch (_: Exception) {
                _uiState.value = QuranUiState.Error
            }
        }
    }
}
