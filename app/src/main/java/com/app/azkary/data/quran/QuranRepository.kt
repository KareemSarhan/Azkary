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
                // Extract bismillah from the first ayah for surahs other than Al-Fatihah (1) and At-Tawbah (9)
                // The SDK prepends "بسم الله الرحمن الرحيم" to the first ayah text.
                // Split on الرحيم to separate bismillah from the actual ayah content.
                val bismillahMarker = "\u0627\u0644\u0631\u0651\u064E\u062D\u0650\u064A\u0645\u0650" // الرحيم
                var bismillahText: String? = null
                val ayahList = rawAyahs.mapIndexed { index, text ->
                    AyahUi(ayahNumber = index + 1, text = text)
                }.toMutableList()

                if (surahNumber != 1 && surahNumber != 9 && ayahList.isNotEmpty()) {
                    val firstAyah = ayahList[0]
                    val splitIndex = firstAyah.text.indexOf(bismillahMarker)
                    if (splitIndex >= 0) {
                        val afterMarker = splitIndex + bismillahMarker.length
                        bismillahText = firstAyah.text.substring(0, afterMarker).trim()
                        val remaining = firstAyah.text.substring(afterMarker).trim()
                        if (remaining.isNotEmpty()) {
                            ayahList[0] = firstAyah.copy(text = remaining)
                        } else {
                            ayahList.removeAt(0)
                        }
                    }
                }

                QuranSurahUi(
                    surahNumber = surahNumber,
                    surahName = name,
                    bismillah = bismillahText,
                    ayahs = ayahList
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
