package com.app.azkary.data.quran

import android.content.Context
import com.app.azkary.data.model.AyahUi
import com.app.azkary.data.model.QuranSurahUi
import com.app.azkary.data.model.VerseOfDayUi
import com.tazkiyatech.quran.sdk.database.QuranDatabase
import com.tazkiyatech.quran.sdk.model.SectionType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class QuranRepository @Inject constructor(
    @param:ApplicationContext private val applicationContext: Context
) {
    @Volatile
    private var quranDatabase: QuranDatabase? = null
    private val dbMutex = Mutex()

    suspend fun openDatabaseIfNeeded() {
        if (quranDatabase != null) return
        dbMutex.withLock {
            if (quranDatabase == null) {
                try {
                    val db = QuranDatabase(applicationContext)
                    db.openDatabase()
                    quranDatabase = db
                } catch (_: Exception) {
                    quranDatabase = null
                }
            }
        }
    }

    private fun requireDb(): QuranDatabase? = quranDatabase

    suspend fun getSurah(surahNumber: Int): QuranSurahUi? {
        if (surahNumber < 1 || surahNumber > 114) return null
        val db = requireDb() ?: return null
        return try {
            withContext(Dispatchers.IO) {
                val name = db.getNameOfSurah(surahNumber)
                val rawAyahs = db.getAyahsInSurah(surahNumber)
                // Bismillah is shown before all surahs except Al-Fatihah (1) and At-Tawbah (9)
                val bismillahText = if (surahNumber != 1 && surahNumber != 9) {
                    "\u0628\u0650\u0633\u0652\u0645\u0650 \u0627\u0644\u0644\u0651\u064E\u0647\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0652\u0645\u064E\u0670\u0646\u0650 \u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650"
                } else null
                QuranSurahUi(
                    surahNumber = surahNumber,
                    surahName = name,
                    bismillah = bismillahText,
                    ayahs = rawAyahs.mapIndexed { index, text ->
                        AyahUi(ayahNumber = index + 1, text = text)
                    }
                )
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun getVerseOfDay(islamicDate: String): VerseOfDayUi? {
        val db = requireDb() ?: return null
        return try {
            withContext(Dispatchers.IO) {
                val seed = islamicDate.hashCode().toLong()
                val surahNumber = (abs(seed) % 114 + 1).toInt()
                val ayahs = db.getAyahsInSurah(surahNumber)
                if (ayahs.isEmpty()) return@withContext null
                val ayahIndex = (abs(seed / 114) % ayahs.size).toInt()
                val surahName = db.getNameOfSurah(surahNumber)
                VerseOfDayUi(
                    surahName = surahName,
                    ayahText = ayahs[ayahIndex],
                    surahNumber = surahNumber,
                    ayahNumber = ayahIndex + 1
                )
            }
        } catch (_: Exception) {
            null
        }
    }
}
