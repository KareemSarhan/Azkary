package com.app.azkary

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.azkary.data.prefs.AppLanguage
import com.app.azkary.data.prefs.UserPreferencesRepository
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.ui.reading.ReadingScreen
import com.app.azkary.ui.settings.SettingsScreen
import com.app.azkary.ui.summary.SummaryScreen
import com.app.azkary.ui.theme.AzkaryTheme
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var repository: AzkarRepository
    @Inject lateinit var userPreferencesRepository: UserPreferencesRepository

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val appLanguage by userPreferencesRepository.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
            
            val layoutDirection = when (appLanguage) {
                AppLanguage.ARABIC -> ComposeLayoutDirection.Rtl
                AppLanguage.ENGLISH -> ComposeLayoutDirection.Ltr
                AppLanguage.SYSTEM -> {
                    val locale = LocalConfiguration.current.locales[0]
                    if (locale.language == "ar") ComposeLayoutDirection.Rtl else ComposeLayoutDirection.Ltr
                }
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AzkaryTheme {
                    LaunchedEffect(Unit) {
                        repository.seedDatabase()
                    }
                    
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        NavHost(navController = navController, startDestination = "summary") {
                            composable("summary") {
                                SummaryScreen(
                                    onNavigateToCategory = { id ->
                                        navController.navigate("reading/$id")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    }
                                )
                            }
                            composable("reading/{categoryId}") { backStackEntry ->
                                val categoryId = backStackEntry.arguments?.getString("categoryId") ?: ""
                                ReadingScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
