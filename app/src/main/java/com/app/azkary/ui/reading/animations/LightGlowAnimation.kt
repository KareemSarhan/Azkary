package com.app.azkary.ui.reading.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Light/Glow animation for light-related zikr (like "Allah nur al-samawat...")
 * Rays of light emanating from center with glowing orb
 */
@Composable
fun LightGlowAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    primaryColor: Color = Color(0xFFFFD700),
    secondaryColor: Color = Color(0xFFFFA000)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "lightGlow")

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "lightProgress"
    )

    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2
            val maxRadius = minOf(canvasWidth, canvasHeight) * 0.45f

            if (animatedProgress > 0) {
                // Draw outer glow rings
                val ringCount = 3
                for (i in 0 until ringCount) {
                    val ringProgress = (animatedProgress - i * 0.15f).coerceIn(0f, 1f)
                    if (ringProgress > 0) {
                        val ringRadius = maxRadius * (0.3f + i * 0.35f) * ringProgress * glowPulse
                        val alpha = (0.3f - i * 0.08f) * ringProgress

                        drawCircle(
                            color = primaryColor.copy(alpha = alpha),
                            radius = ringRadius,
                            center = Offset(centerX, centerY)
                        )
                    }
                }

                // Draw light rays
                if (animatedProgress > 0.2f) {
                    val rayProgress = (animatedProgress - 0.2f) / 0.8f
                    drawLightRays(
                        centerX = centerX,
                        centerY = centerY,
                        maxRadius = maxRadius,
                        progress = rayProgress,
                        rotation = rotation,
                        color = secondaryColor
                    )
                }

                // Draw central orb
                val orbSize = maxRadius * 0.25f * animatedProgress
                drawCircle(
                    color = Color.White,
                    radius = orbSize,
                    center = Offset(centerX, centerY)
                )

                // Inner bright core
                drawCircle(
                    color = primaryColor,
                    radius = orbSize * 0.7f,
                    center = Offset(centerX, centerY)
                )

                // Sparkle particles
                if (animatedProgress > 0.4f) {
                    drawLightParticles(
                        centerX = centerX,
                        centerY = centerY,
                        radius = maxRadius * 1.2f,
                        progress = (animatedProgress - 0.4f) / 0.6f,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightRays(
    centerX: Float,
    centerY: Float,
    maxRadius: Float,
    progress: Float,
    rotation: Float,
    color: Color
) {
    val rayCount = 12
    val rayWidth = 15f

    for (i in 0 until rayCount) {
        val angle = (i * 30f + rotation) * (Math.PI / 180f)
        val rayLength = maxRadius * 0.6f * progress * (0.8f + (i % 3) * 0.1f)

        val startX = centerX + (maxRadius * 0.2f * cos(angle)).toFloat()
        val startY = centerY + (maxRadius * 0.2f * sin(angle)).toFloat()
        val endX = centerX + ((maxRadius * 0.2f + rayLength) * cos(angle)).toFloat()
        val endY = centerY + ((maxRadius * 0.2f + rayLength) * sin(angle)).toFloat()

        val alpha = 0.4f * progress * (1f - (i % 2) * 0.2f)

        drawLine(
            color = color.copy(alpha = alpha),
            start = Offset(startX, startY),
            end = Offset(endX, endY),
            strokeWidth = rayWidth * (0.7f + (i % 3) * 0.15f),
            cap = StrokeCap.Round
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawLightParticles(
    centerX: Float,
    centerY: Float,
    radius: Float,
    progress: Float,
    color: Color
) {
    val particleCount = 16

    for (i in 0 until particleCount) {
        val angle = (i * 22.5f + i * 7f) * (Math.PI / 180f)
        val distance = radius * (0.3f + (i % 5) * 0.15f) * progress
        val x = centerX + (distance * cos(angle)).toFloat()
        val y = centerY + (distance * sin(angle)).toFloat()
        val particleSize = 3f * progress * (0.8f + (i % 4) * 0.1f)

        drawCircle(
            color = color.copy(alpha = 0.5f + (i % 3) * 0.15f),
            radius = particleSize,
            center = Offset(x, y)
        )
    }
}

@Preview
@Composable
private fun LightGlowAnimationPreview() {
    LightGlowAnimation(
        progress = 0.8f,
        modifier = Modifier.size(200.dp)
    )
}
