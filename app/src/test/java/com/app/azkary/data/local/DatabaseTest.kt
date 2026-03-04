package com.app.azkary.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
abstract class DatabaseTest {

    protected lateinit var database: AzkarDatabase
    protected lateinit var context: Context
    protected val testDispatcher: TestDispatcher = StandardTestDispatcher()

    @Before
    open fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(
            context,
            AzkarDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    open fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }
}
