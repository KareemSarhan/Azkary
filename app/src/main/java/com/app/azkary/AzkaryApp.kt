package com.app.azkary

import android.app.Application
import com.app.azkary.data.prefs.UserPreferencesRepository
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class AzkaryApp : Application() {
    
    @Inject
    lateinit var userPreferencesRepository: UserPreferencesRepository
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun onCreate() {
        super.onCreate()
        
        applicationScope.launch {
            userPreferencesRepository.initializeFirstInstallDate()
            userPreferencesRepository.incrementAppOpenCount()
        }
    }
}
