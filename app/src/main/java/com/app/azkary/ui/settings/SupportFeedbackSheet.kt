package com.app.azkary.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.azkary.R
import com.app.azkary.util.SupportHelper
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * SupportFeedbackSheet - Bottom sheet for Support/Feedback options
 *
 * Shows two options:
 * - Email: Opens email client with pre-filled support message
 * - Discord: Opens Discord invite link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportFeedbackSheet(
    onDismiss: () -> Unit,
    onShowToast: (String) -> Unit
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    // Get SupportHelper through Hilt entry point
    val supportHelper = rememberSupportHelper(context)

    val surfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.surface
    val onSurfaceColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface
    val primaryColor = androidx.compose.material3.MaterialTheme.colorScheme.primary

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = surfaceColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Text(
                text = stringResource(R.string.settings_support_feedback),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = onSurfaceColor,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Spacer(Modifier.height(8.dp))

            // Email Option
            SupportOptionItem(
                icon = {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = stringResource(R.string.support_option_email),
                        tint = primaryColor,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = stringResource(R.string.support_option_email),
                subtitle = stringResource(R.string.support_email_address),
                onClick = {
                    val intent = supportHelper.buildEmailIntent()
                    val launched = supportHelper.launchIntentSafely(intent)
                    if (launched) {
                        onDismiss()
                    } else {
                        onShowToast(supportHelper.getString(R.string.support_no_app_found))
                    }
                },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )

            Spacer(Modifier.height(8.dp))

            // Discord Option
            SupportOptionItem(
                icon = {
                    Icon(
                        painter = androidx.compose.ui.res.painterResource(R.drawable.ic_discord),
                        contentDescription = stringResource(R.string.support_option_discord),
                        tint = androidx.compose.ui.graphics.Color.Unspecified,
                        modifier = Modifier.size(24.dp)
                    )
                },
                title = stringResource(R.string.support_option_discord),
                subtitle = stringResource(R.string.support_discord_description),
                onClick = {
                    val intent = supportHelper.buildDiscordIntent()
                    val launched = supportHelper.launchIntentSafely(intent)
                    if (launched) {
                        onDismiss()
                    } else {
                        onShowToast(supportHelper.getString(R.string.support_no_app_found))
                    }
                },
                surfaceColor = surfaceColor,
                onSurfaceColor = onSurfaceColor
            )
        }
    }
}

/**
 * SupportOptionItem - Individual option item in the support sheet
 */
@Composable
private fun SupportOptionItem(
    icon: @Composable () -> Unit,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
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
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            icon()

            Spacer(Modifier.size(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = onSurfaceColor,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = subtitle,
                    color = onSurfaceColor.copy(alpha = 0.6f),
                    fontSize = 13.sp
                )
            }
        }
    }
}

/**
 * Helper function to get SupportHelper via Hilt entry point
 */
@Composable
private fun rememberSupportHelper(context: android.content.Context): SupportHelper {
    return androidx.compose.runtime.remember {
        val entryPoint = EntryPointAccessors.fromApplication(
            context.applicationContext,
            SupportHelperEntryPoint::class.java
        )
        entryPoint.supportHelper()
    }
}

/**
 * Hilt entry point for accessing SupportHelper in Compose
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SupportHelperEntryPoint {
    fun supportHelper(): SupportHelper
}
