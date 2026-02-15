package com.app.azkary.ui.settings

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.azkary.R
import com.app.azkary.data.prefs.ThemeMode
import kotlinx.coroutines.launch

private val ToggleRed = Color(0xFFE53935)
private val ToggleOff = Color(0xFF252D3F)
private val AccentBlue = Color(0xFF42A5F5)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()
) {
    LocalContext.current
    val locationPrefs by viewModel.locationPreferences.collectAsState()
    val isRefreshing by viewModel.isRefreshingLocation.collectAsState()
    val locationError by viewModel.locationError.collectAsState()
    val currentLanguageName = viewModel.getCurrentLanguageDisplayName()
    val themeSettings by viewModel.themeSettings.collectAsState()
    val holdToComplete by viewModel.holdToComplete.collectAsState()

    // Support/Feedback sheet state
    var showSupportSheet by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Get colors from MaterialTheme
    val backgroundColor = MaterialTheme.colorScheme.background
    val surfaceColor = MaterialTheme.colorScheme.surface
    MaterialTheme.colorScheme.surfaceVariant
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    val snackbarHostState = remember { SnackbarHostState() }

    // Location permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            viewModel.refreshLocation()
        } else {
            // Show error or prompt to enable in settings
        }
    }

    LaunchedEffect(locationError) {
        locationError?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearLocationError()
        }
    }

    Scaffold(
        containerColor = backgroundColor,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = onBackgroundColor,
                    navigationIconContentColor = onBackgroundColor
                ),
                title = {
                    Text(
                        stringResource(R.string.settings_title),
                        fontWeight = FontWeight.SemiBold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.settings_back_content_description)
                        )
                    }
                })
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(backgroundColor)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Location Section
            SectionHeader(
                title = stringResource(R.string.settings_section_prayer_times),
                color = onBackgroundColor.copy(alpha = 0.6f)
            )

            SettingsToggleItem(
                title = stringResource(R.string.settings_use_location_title),
                isEnabled = locationPrefs.useLocation,
                onToggle = { viewModel.toggleUseLocation(it) },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )

            Spacer(Modifier.height(8.dp))

            AnimatedVisibility(
                visible = locationPrefs.useLocation,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column {
                    LocationDisplayItem(
                        location = locationPrefs.lastResolvedLocation,
                        locationName = locationPrefs.locationName,
                        isRefreshing = isRefreshing,
                        onRefresh = {
                            permissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        },
                        surfaceColor = surfaceColor,
                        onSurfaceColor = onSurfaceColor,
                        onSurfaceVariantColor = onSurfaceVariantColor,
                        accentColor = AccentBlue
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Language Section
            SectionHeader(
                title = stringResource(R.string.settings_section_general),
                color = onBackgroundColor.copy(alpha = 0.6f)
            )

            SettingsClickableItem(
                title = stringResource(R.string.settings_language),
                subtitle = currentLanguageName,
                onClick = { viewModel.openLanguageSettings() },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )

            Spacer(Modifier.height(8.dp))

            SettingsClickableItem(
                title = stringResource(R.string.settings_support_feedback),
                subtitle = "",
                onClick = { showSupportSheet = true },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor
            )

            Spacer(Modifier.height(8.dp))

            SettingsToggleItem(
                title = stringResource(R.string.settings_hold_to_complete_title),
                isEnabled = holdToComplete,
                onToggle = { viewModel.setHoldToComplete(it) },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )

            Spacer(Modifier.height(16.dp))

            // Theme Section
            SectionHeader(
                title = stringResource(R.string.settings_section_theme),
                color = onBackgroundColor.copy(alpha = 0.6f)
            )

            ThemeSettingItem(
                themeMode = themeSettings.themeMode,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor
            )

            Spacer(Modifier.height(32.dp))

            val context = LocalContext.current
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                        ?: "Unknown"
                } catch (e: Exception) {
                    "Unknown"
                }
            }
            Text(
                text = stringResource(R.string.settings_version, versionName),
                color = onSurfaceVariantColor,
                fontSize = 12.sp,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }

    // Support/Feedback Bottom Sheet
    if (showSupportSheet) {
        SupportFeedbackSheet(
            onDismiss = { showSupportSheet = false },
            onShowToast = { message ->
                coroutineScope.launch {
                    snackbarHostState.showSnackbar(message)
                }
            }
        )
    }
}

@Composable
private fun SectionHeader(
    title: String, color: Color
) {
    Text(
        title,
        color = color,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
    )
}

@Composable
private fun SettingsToggleItem(
    title: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                title,
                color = onSurfaceColor,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            CustomIosToggle(
                isOn = isEnabled, onToggle = { onToggle(!isEnabled) })
        }
    }
}

@Composable
private fun LocationDisplayItem(
    location: com.app.azkary.data.model.LatLng?,
    locationName: String?,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    accentColor: Color
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    stringResource(R.string.settings_current_location),
                    color = onSurfaceColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))

                when {
                    locationName != null -> {
                        Text(
                            locationName,
                            color = onSurfaceColor.copy(alpha = 0.8f),
                            fontSize = 14.sp
                        )
                        location?.let {
                            Text(
                                it.toString(), color = onSurfaceVariantColor, fontSize = 12.sp
                            )
                        }
                    }

                    location != null -> {
                        Text(
                            location.toString(), color = onSurfaceVariantColor, fontSize = 13.sp
                        )
                    }

                    else -> {
                        Text(
                            stringResource(R.string.settings_location_not_available),
                            color = onSurfaceVariantColor,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp), color = accentColor, strokeWidth = 2.dp
                )
            } else {
                IconButton(onClick = onRefresh) {
                    Icon(
                        Icons.Default.Refresh,
                        contentDescription = stringResource(R.string.settings_refresh_location_content_description),
                        tint = accentColor
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsClickableItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() }) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title, color = onSurfaceColor, fontWeight = FontWeight.Medium
                )
                if (subtitle.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        subtitle, color = onSurfaceVariantColor, fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSettingItem(
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit,
    surfaceColor: Color,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color
) {
    Surface(
        color = surfaceColor,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 2.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // System option
            ThemeRadioOption(
                title = stringResource(R.string.theme_system),
                isSelected = themeMode == ThemeMode.SYSTEM,
                onClick = { onThemeModeChange(ThemeMode.SYSTEM) },
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor
            )

            Spacer(Modifier.height(8.dp))

            // Light option
            ThemeRadioOption(
                title = stringResource(R.string.theme_light),
                isSelected = themeMode == ThemeMode.LIGHT,
                onClick = { onThemeModeChange(ThemeMode.LIGHT) },
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor
            )

            Spacer(Modifier.height(8.dp))

            // Dark option
            ThemeRadioOption(
                title = stringResource(R.string.theme_dark),
                isSelected = themeMode == ThemeMode.DARK,
                onClick = { onThemeModeChange(ThemeMode.DARK) },
                onSurfaceColor = onSurfaceColor,
                onSurfaceVariantColor = onSurfaceVariantColor,
                primaryColor = primaryColor
            )
        }
    }
}

@Composable
private fun ThemeRadioOption(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onSurfaceColor: Color,
    onSurfaceVariantColor: Color,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected, onClick = onClick, colors = RadioButtonDefaults.colors(
                selectedColor = primaryColor, unselectedColor = onSurfaceVariantColor
            )
        )
        Text(
            text = title,
            color = onSurfaceColor,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
fun CustomIosToggle(
    isOn: Boolean, onToggle: () -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (isOn) ToggleRed else ToggleOff, label = "trackColor"
    )

    // sizes
    val w = 56.dp
    val h = 32.dp
    val padding = 3.dp
    val thumbSize = 26.dp

    // computed max offset = width - (thumbSize + 2*padding)
    val maxOffset = w - thumbSize - (padding * 2)
    val thumbOffset by animateDpAsState(
        targetValue = if (isOn) maxOffset else 0.dp, label = "thumbOffset"
    )

    Box(
        modifier = Modifier
            .size(width = w, height = h)
            .shadow(elevation = 2.dp, shape = CircleShape, clip = false)
            .background(trackColor, CircleShape)
            .clickable { onToggle() }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
