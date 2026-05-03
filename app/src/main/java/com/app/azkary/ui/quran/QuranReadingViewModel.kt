package com.app.azkary.ui.quran

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.azkary.data.model.QuranSurahUi
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

    private val _surahUi = MutableStateFlow<QuranSurahUi?>(null)
    val surahUi: StateFlow<QuranSurahUi?> = _surahUi.asStateFlow()

    init {
        viewModelScope.launch {
            val surah = quranRepository.getSurah(surahNumber)
            _surahUi.value = surah
        }
    }
}
