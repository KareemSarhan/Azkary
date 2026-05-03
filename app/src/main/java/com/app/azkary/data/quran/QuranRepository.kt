package com.app.azkary.data.quran

import android.content.Context
import android.os.Build
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

    private fun isSdkSupported(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O

    suspend fun openDatabaseIfNeeded() {
        if (!isSdkSupported()) return
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
                val ayahs = db.getAyahsInSurah(surahNumber)
                val metadata = db.getMetadataForSection(SectionType.SURAH, surahNumber)
                QuranSurahUi(
                    surahNumber = surahNumber,
                    surahName = name,
                    ayahs = ayahs.mapIndexed { index, text ->
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
