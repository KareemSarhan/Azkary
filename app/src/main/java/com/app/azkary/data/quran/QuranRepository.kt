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
    private companion object {
        const val CANONICAL_BISMILLAH = "بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ"
        const val BISMILLAH_TARGET = "بسماللهالرحمنالرحيم"
    }

    @Volatile
    private var quranDatabase: QuranDatabase? = null
    private val dbMutex = Mutex()

    suspend fun openDatabaseIfNeeded() {
        if (quranDatabase != null) return
        dbMutex.withLock {
            if (quranDatabase == null) {
                try {
                    withContext(Dispatchers.IO) {
                        val db = QuranDatabase(applicationContext)
                        db.openDatabase()
                        quranDatabase = db
                    }
                } catch (_: Exception) {
                    quranDatabase = null
                }
            }
        }
    }

    private fun requireDb(): QuranDatabase? = quranDatabase

    suspend fun getSurah(surahNumber: Int): QuranSurahUi? {
        if (surahNumber < 1 || surahNumber > 114) return null
        openDatabaseIfNeeded()
        val db = requireDb() ?: return null
        return try {
            withContext(Dispatchers.IO) {
                val name = db.getNameOfSurah(surahNumber)
                val rawAyahs = db.getAyahsInSurah(surahNumber)
                val (bismillahText, ayahList) = splitLeadingBismillah(surahNumber, rawAyahs)

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

    private fun splitLeadingBismillah(
        surahNumber: Int,
        rawAyahs: List<String>
    ): Pair<String?, List<AyahUi>> {
        val ayahList = rawAyahs.mapIndexed { index, text ->
            AyahUi(ayahNumber = index + 1, text = text)
        }.toMutableList()

        if (surahNumber == 1 || surahNumber == 9 || ayahList.isEmpty()) {
            return null to ayahList
        }

        val firstAyah = ayahList[0]
        val bismillahEndIndex = firstAyah.text.leadingBismillahEndIndex()
            ?: return CANONICAL_BISMILLAH to ayahList

        val bismillahText = firstAyah.text.substring(0, bismillahEndIndex).trim()
        val remaining = firstAyah.text.substring(bismillahEndIndex).trim()

        if (remaining.isNotEmpty()) {
            ayahList[0] = firstAyah.copy(text = remaining)
        } else {
            ayahList.removeAt(0)
        }

        return bismillahText to ayahList
    }

    private fun String.leadingBismillahEndIndex(): Int? {
        val normalized = StringBuilder()

        for (index in indices) {
            val normalizedChar = this[index].normalizedForBismillah() ?: continue

            if (normalized.isEmpty() && normalizedChar != BISMILLAH_TARGET.first()) {
                return null
            }

            normalized.append(normalizedChar)
            val current = normalized.toString()

            if (!BISMILLAH_TARGET.startsWith(current)) {
                return null
            }

            if (current == BISMILLAH_TARGET) {
                return index + 1
            }
        }

        return null
    }

    private fun Char.normalizedForBismillah(): Char? {
        return when (this) {
            '\u0622', '\u0623', '\u0625', '\u0671' -> '\u0627'
            '\u0640', '\u0670' -> null
            in '\u064B'..'\u065F' -> null
            in '\u06D6'..'\u06ED' -> null
            else -> if (isWhitespace()) null else this
        }
    }

    suspend fun getVerseOfDay(islamicDate: String): VerseOfDayUi? {
        openDatabaseIfNeeded()
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
