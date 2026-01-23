package com.app.azkary.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

private val NavyDark = Color(0xFF0B1220)
private val NavyLight = Color(0xFF161D2F)
private val ToggleRed = Color(0xFFE53935)
private val ToggleOff = Color(0xFF252D3F)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current

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
                title = "Language",  onClick = {
                    val intent = Intent(Settings.ACTION_APP_LOCALE_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    }
                    context.startActivity(intent)
                })
        }
    }
}

@Composable
fun SettingsItem(
    title: String, onClick: () -> Unit
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
