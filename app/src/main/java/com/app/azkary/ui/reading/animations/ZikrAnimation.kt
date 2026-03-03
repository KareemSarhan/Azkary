package com.app.azkary.ui.reading.animations

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ZikrAnimation(
    animationType: ZikrAnimationType,
    progress: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFF4CAF50),
    secondaryColor: Color = Color(0xFFFFD700)
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        when (animationType) {
            ZikrAnimationType.TREE_GROWTH -> {
                TreeGrowthAnimation(
                    progress = progress,
                    modifier = Modifier.size(180.dp),
                    color = primaryColor
                )
            }
            ZikrAnimationType.STAR_TWINKLE -> {
                StarTwinkleAnimation(
                    progress = progress,
                    modifier = Modifier.size(180.dp),
                    color = secondaryColor
                )
            }
            ZikrAnimationType.RAIN_MERCY -> {
                RainMercyAnimation(
                    progress = progress,
                    modifier = Modifier.size(180.dp),
                    color = Color(0xFF4FC3F7)
                )
            }
            ZikrAnimationType.HEART_PULSE -> {
                HeartPulseAnimation(
                    progress = progress,
                    modifier = Modifier.size(180.dp),
                    color = Color(0xFFE91E63)
                )
            }
            ZikrAnimationType.LIGHT_GLOW -> {
                LightGlowAnimation(
                    progress = progress,
                    modifier = Modifier.size(180.dp),
                    primaryColor = secondaryColor,
                    secondaryColor = Color(0xFFFFA000)
                )
            }
            ZikrAnimationType.NONE -> {
                // No animation
            }
        }
    }
}
