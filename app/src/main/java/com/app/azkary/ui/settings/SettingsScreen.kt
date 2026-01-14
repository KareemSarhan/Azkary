package com.app.azkary.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.app.azkary.data.prefs.AppLanguage
import kotlinx.coroutines.launch
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.ui.draw.alpha

private val NavyDark = Color(0xFF0B1220)
private val NavyLight = Color(0xFF161D2F)
private val ToggleRed = Color(0xFFE53935)
private val ToggleOff = Color(0xFF252D3F)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()
) {
    val currentLanguage by viewModel.appLanguage.collectAsState(initial = AppLanguage.SYSTEM)
    val scope = rememberCoroutineScope()

    var showLanguageSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        containerColor = NavyDark, topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                containerColor = NavyDark,
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            ),
                title = { Text("Settings", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                })
        }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(NavyDark, Color(0xFF050914))))
                .padding(16.dp)
        ) {
            SettingsItem(
                title = "Language", value = when (currentLanguage) {
                    AppLanguage.ARABIC -> "العربية"
                    AppLanguage.ENGLISH -> "English"
                    AppLanguage.SYSTEM -> "System Default"
                }, onClick = { showLanguageSheet = true })
        }

        if (showLanguageSheet) {
            ModalBottomSheet(
                onDismissRequest = { showLanguageSheet = false },
                sheetState = sheetState,
                containerColor = NavyLight,
                scrimColor = Color.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                LanguageSelectionContent(
                    selectedLanguage = currentLanguage, onLanguageSelected = { lang ->
                        scope.launch {
                            viewModel.setAppLanguage(lang)
                            showLanguageSheet = false
                        }
                    })
            }
        }
    }
}

@Composable
fun SettingsItem(
    title: String, value: String, onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = NavyLight,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 0.dp,
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(2.dp))
                Text(value, color = Color.White.copy(alpha = 0.55f), fontSize = 14.sp)
            }

            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun LanguageSelectionContent(
    selectedLanguage: AppLanguage, onLanguageSelected: (AppLanguage) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 18.dp)
            .padding(bottom = 28.dp)
    ) {
        // Optional small handle spacing is already provided by sheet
        Text(
            text = "App Language",
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // “Card” container
        Surface(
            color = Color(0xFF2A3448), // slightly lighter than NavyLight for contrast
            shape = RoundedCornerShape(20.dp),
            shadowElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {

                LanguageOptionRow(
                    label = "System Default",
                    isSelected = selectedLanguage == AppLanguage.SYSTEM,
                    onClick = { onLanguageSelected(AppLanguage.SYSTEM) })

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                LanguageOptionRow(
                    label = "العربية",
                    isSelected = selectedLanguage == AppLanguage.ARABIC,
                    onClick = { onLanguageSelected(AppLanguage.ARABIC) })

                HorizontalDivider(color = Color.White.copy(alpha = 0.06f), thickness = 1.dp)

                LanguageOptionRow(
                    label = "English",
                    isSelected = selectedLanguage == AppLanguage.ENGLISH,
                    onClick = { onLanguageSelected(AppLanguage.ENGLISH) })
            }
        }

        Spacer(Modifier.height(18.dp))
    }
}

@Composable
private fun LanguageOptionRow(
    label: String, isSelected: Boolean, onClick: () -> Unit
) {
    Row(modifier = Modifier
        .fillMaxWidth()
        .height(78.dp)
        .clickable { onClick() }
        .padding(horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        // Toggle on the LEFT (like your reference)
        CustomIosToggle(
            isOn = isSelected, onToggle = onClick
        )

        Spacer(Modifier.width(14.dp))

        // Label on the RIGHT: use a Box to force right alignment cleanly
        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f),
                fontSize = 20.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }
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
        contentAlignment = Alignment.CenterStart) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(thumbSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .background(Color.White, CircleShape)
        )
    }
}
