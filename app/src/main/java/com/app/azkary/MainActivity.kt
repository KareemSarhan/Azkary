package com.app.azkary

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.app.azkary.data.prefs.ThemePreferencesRepository
import com.app.azkary.data.prefs.ThemeSettings
import com.app.azkary.data.repository.AzkarRepository
import com.app.azkary.domain.AppRatingManager
import com.app.azkary.ui.reading.ReadingScreen
import com.app.azkary.ui.settings.SettingsScreen
import com.app.azkary.ui.summary.SummaryScreen
import com.app.azkary.ui.theme.AzkaryTheme
import com.app.azkary.ui.category.CategoryCreationScreen
import com.app.azkary.util.InAppUpdateManager
import com.app.azkary.util.InAppUpdateManager.Companion.UPDATE_REQUEST_CODE
import com.app.azkary.util.LocaleManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import androidx.compose.ui.unit.LayoutDirection as ComposeLayoutDirection

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var repository: AzkarRepository
    @Inject
    lateinit var themePreferencesRepository: ThemePreferencesRepository
    @Inject lateinit var localeManager: LocaleManager
    @Inject lateinit var appRatingManager: AppRatingManager

    private lateinit var inAppUpdateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        enableEdgeToEdge()
        
        inAppUpdateManager = InAppUpdateManager(this, this)
        inAppUpdateManager.checkForUpdate()

        setContent {
            val themeSettings by themePreferencesRepository.themeSettings.collectAsState(initial = ThemeSettings())

            val layoutDirection = if (localeManager.isCurrentLocaleRtl(this)) {
                ComposeLayoutDirection.Rtl
            } else {
                ComposeLayoutDirection.Ltr
            }

            CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
                AzkaryTheme(themeSettings = themeSettings) {
                    var showRatingPrompt by remember { mutableStateOf(false) }
                    
                    LaunchedEffect(Unit) {
                        repository.seedDatabase()
                        showRatingPrompt = appRatingManager.shouldShowRatingPrompt()
                    }

                    if (showRatingPrompt) {
                        LaunchedEffect(Unit) {
                            appRatingManager.requestReview(this@MainActivity) {
                                showRatingPrompt = false
                            }
                        }
                    }

                    val navController = rememberNavController()

                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0)
                    ) { paddingValues ->
                        NavHost(
                            navController = navController,
                            startDestination = "summary",
                            modifier = Modifier.padding(paddingValues)
                        ) {
                            composable("summary") {
                                SummaryScreen(
                                    onNavigateToCategory = { id ->
                                        navController.navigate("reading/$id")
                                    },
                                    onNavigateToSettings = {
                                        navController.navigate("settings")
                                    },
                                    onNavigateToCreateCategory = {
                                        navController.navigate("category/create")
                                    },
                                    onNavigateToEditCategory = { categoryId ->
                                        navController.navigate("category/edit/$categoryId")
                                    }
                                )
                            }
                            composable("reading/{categoryId}") { backStackEntry ->
                                ReadingScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("settings") {
                                SettingsScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("category/create") {
                                CategoryCreationScreen(
                                    onBack = { navController.popBackStack() }
                                )
                            }
                            composable("category/edit/{categoryId}") { backStackEntry ->
                                val categoryId = backStackEntry.arguments?.getString("categoryId")
                                CategoryCreationScreen(
                                    onBack = { navController.popBackStack() },
                                    categoryId = categoryId
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        localeManager.notifyLocaleChanged()
    }

    override fun onResume() {
        super.onResume()
        inAppUpdateManager.onResume()
        localeManager.notifyLocaleChanged()
    }

    override fun onDestroy() {
        super.onDestroy()
        inAppUpdateManager.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == UPDATE_REQUEST_CODE) {
            inAppUpdateManager.onActivityResult(requestCode, resultCode)
        }
    }
}
