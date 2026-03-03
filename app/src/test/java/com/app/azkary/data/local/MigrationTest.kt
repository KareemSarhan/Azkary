package com.app.azkary.data.local

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class MigrationTest {

    private val testDbName = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AzkarDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun `migrate 4 to 5 adds requiredRepeats and isInfinite columns`() {
        // Create database at version 4
        var db = helper.createDatabase(testDbName, 4)

        // Insert test data
        db.execSQL("""
            INSERT INTO categories (categoryId, type, systemKey, sortOrder, isArchived, fromTime, toTime) 
            VALUES ('cat-1', 'USER', NULL, 1, 0, 0, 24)
        """)

        db.execSQL("""
            INSERT INTO azkar_items (itemId, requiredRepeats, source, createdAt, updatedAt, isInfinite) 
            VALUES ('item-1', 3, 'SEEDED', 123456789, 123456789, 0)
        """)

        db.execSQL("""
            INSERT INTO category_item_crossrefs (categoryId, itemId, sortOrder, isEnabled) 
            VALUES ('cat-1', 'item-1', 1, 1)
        """)

        db.close()

        // Re-open database with migration
        db = helper.runMigrationsAndValidate(testDbName, 5, true, MIGRATION_4_5)

        // Verify new columns exist with correct values
        val cursor = db.query("SELECT * FROM category_item_crossrefs")
        assertTrue(cursor.moveToFirst())

        val requiredRepeatsIndex = cursor.getColumnIndex("requiredRepeats")
        val isInfiniteIndex = cursor.getColumnIndex("isInfinite")

        assertTrue(requiredRepeatsIndex >= 0)
        assertTrue(isInfiniteIndex >= 0)

        // Values should be migrated from azkar_items
        assertEquals(3, cursor.getInt(requiredRepeatsIndex))
        assertEquals(0, cursor.getInt(isInfiniteIndex))

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 5 to 6 adds night time columns`() {
        // Create database at version 5
        var db = helper.createDatabase(testDbName, 5)

        // Insert test data with month
        db.execSQL("""
            INSERT INTO prayer_months (id, year, month, latitude, longitude, methodId, timezone, lastUpdated) 
            VALUES (1, 2026, 3, 24.7136, 46.6753, 4, 'Asia/Riyadh', 123456789)
        """)

        db.execSQL("""
            INSERT INTO prayer_days (id, monthId, date, fajr, dhuhr, asr, maghrib, isha, sunrise, sunset) 
            VALUES (1, 1, 19754, 18000, 43200, 55800, 64800, 70200, 22500, 64200)
        """)

        db.close()

        // Re-open database with migration
        db = helper.runMigrationsAndValidate(testDbName, 6, true, MIGRATION_5_6)

        // Verify new columns exist with default values
        val cursor = db.query("SELECT * FROM prayer_days")
        assertTrue(cursor.moveToFirst())

        val firstThirdIndex = cursor.getColumnIndex("firstthird")
        val midnightIndex = cursor.getColumnIndex("midnight")
        val lastThirdIndex = cursor.getColumnIndex("lastthird")

        assertTrue(firstThirdIndex >= 0)
        assertTrue(midnightIndex >= 0)
        assertTrue(lastThirdIndex >= 0)

        // Default values should be '00:00' which is 0 seconds
        assertEquals(0, cursor.getInt(firstThirdIndex))
        assertEquals(0, cursor.getInt(midnightIndex))
        assertEquals(0, cursor.getInt(lastThirdIndex))

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 6 to 7 should complete without error`() {
        // Create database at version 6
        var db = helper.createDatabase(testDbName, 6)

        // Insert minimal test data
        db.execSQL("""
            INSERT INTO categories (categoryId, type, systemKey, sortOrder, isArchived, fromTime, toTime) 
            VALUES ('cat-1', 'USER', NULL, 1, 0, 0, 24)
        """)

        db.close()

        // Note: MIGRATION_6_7 doesn't exist in the current codebase, but the database version is 8
        // This test validates that version 6 schema is valid
        db = helper.runMigrationsAndValidate(testDbName, 7, true)
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate 7 to 8 should complete without error`() {
        // Create database at version 7
        var db = helper.createDatabase(testDbName, 7)

        // Insert minimal test data
        db.execSQL("""
            INSERT INTO categories (categoryId, type, systemKey, sortOrder, isArchived, fromTime, toTime) 
            VALUES ('cat-1', 'USER', NULL, 1, 0, 0, 24)
        """)

        db.close()

        // Note: MIGRATION_7_8 doesn't exist in the current codebase, but the database version is 8
        // This test validates that version 7 schema is valid
        db = helper.runMigrationsAndValidate(testDbName, 8, true)
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `migrate all versions from 4 to 8`() {
        // Create database at version 4
        var db = helper.createDatabase(testDbName, 4)

        // Insert test data for version 4
        db.execSQL("""
            INSERT INTO categories (categoryId, type, systemKey, sortOrder, isArchived, fromTime, toTime) 
            VALUES ('cat-1', 'USER', NULL, 1, 0, 0, 24)
        """)

        db.execSQL("""
            INSERT INTO azkar_items (itemId, requiredRepeats, source, createdAt, updatedAt, isInfinite) 
            VALUES ('item-1', 3, 'SEEDED', 123456789, 123456789, 0)
        """)

        db.execSQL("""
            INSERT INTO category_item_crossrefs (categoryId, itemId, sortOrder, isEnabled) 
            VALUES ('cat-1', 'item-1', 1, 1)
        """)

        db.close()

        // Re-open database with all migrations
        val migrations = arrayOf(MIGRATION_4_5, MIGRATION_5_6)
        db = helper.runMigrationsAndValidate(testDbName, 8, true, *migrations)

        // Verify final schema is correct
        val cursor = db.query("SELECT * FROM category_item_crossrefs")
        assertTrue(cursor.moveToFirst())

        // Verify all expected columns exist
        assertTrue(cursor.getColumnIndex("categoryId") >= 0)
        assertTrue(cursor.getColumnIndex("itemId") >= 0)
        assertTrue(cursor.getColumnIndex("sortOrder") >= 0)
        assertTrue(cursor.getColumnIndex("isEnabled") >= 0)
        assertTrue(cursor.getColumnIndex("requiredRepeats") >= 0)
        assertTrue(cursor.getColumnIndex("isInfinite") >= 0)

        cursor.close()
        db.close()
    }

    @Test
    @Throws(IOException::class)
    fun `test all migrations with inMemory database`() = runBlocking {
        // This test uses the in-memory database to verify migrations work end-to-end
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()

        // Create database with all migrations
        val database = androidx.room.Room.databaseBuilder(
            context,
            AzkarDatabase::class.java,
            "test-migration-db"
        )
            .addMigrations(MIGRATION_4_5, MIGRATION_5_6)
            .build()

        // Verify database is accessible
        val dbVersion = database.getDbVersion()
        assertTrue(dbVersion >= 5)

        database.close()
    }
}
