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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.*

/**
 * Heart pulse animation for love-related zikr (like "Allahumma habbibni...")
 * Beats with each tap, creating a pulsing effect
 */
@Composable
fun HeartPulseAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFE91E63)
) {
    val infiniteTransition = rememberInfiniteTransition(label = "heartPulse")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "heartProgress"
    )

    val glowAlpha by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 0.5f) * 2f,
        animationSpec = tween(500),
        label = "glowAlpha"
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
            val baseSize = minOf(canvasWidth, canvasHeight) * 0.3f * animatedProgress

            // Draw glow effect
            if (glowAlpha > 0) {
                drawCircle(
                    color = color.copy(alpha = glowAlpha * 0.3f),
                    radius = baseSize * 1.5f * pulseScale,
                    center = Offset(centerX, centerY)
                )
            }

            // Draw heart shape
            if (baseSize > 0) {
                val scale = pulseScale * (0.8f + animatedProgress * 0.2f)
                drawHeart(
                    centerX = centerX,
                    centerY = centerY,
                    size = baseSize * scale,
                    color = color
                )

                // Draw sparkle particles
                if (animatedProgress > 0.3f) {
                    drawSparkles(
                        centerX = centerX,
                        centerY = centerY,
                        radius = baseSize * 1.2f,
                        progress = (animatedProgress - 0.3f) / 0.7f,
                        color = Color(0xFFFFD700)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawHeart(
    centerX: Float,
    centerY: Float,
    size: Float,
    color: Color
) {
    val path = Path().apply {
        // Heart shape using bezier curves
        val topCurveHeight = size * 0.3f
        val bottomPointY = centerY + size * 0.4f

        moveTo(centerX, centerY + size * 0.2f)

        // Left curve
        cubicTo(
            centerX - size * 0.5f, centerY - size * 0.3f,
            centerX - size * 0.5f, centerY - size * 0.6f,
            centerX - size * 0.25f, centerY - size * 0.3f
        )

        // Top left to top right
        cubicTo(
            centerX - size * 0.1f, centerY - size * 0.1f,
            centerX, centerY - size * 0.1f,
            centerX, centerY - size * 0.1f
        )

        cubicTo(
            centerX, centerY - size * 0.1f,
            centerX + size * 0.1f, centerY - size * 0.1f,
            centerX + size * 0.25f, centerY - size * 0.3f
        )

        // Right curve
        cubicTo(
            centerX + size * 0.5f, centerY - size * 0.6f,
            centerX + size * 0.5f, centerY - size * 0.3f,
            centerX, centerY + size * 0.2f
        )

        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Fill
    )

    // Add shine effect
    drawPath(
        path = path,
        color = Color.White.copy(alpha = 0.3f),
        style = Stroke(width = 2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSparkles(
    centerX: Float,
    centerY: Float,
    radius: Float,
    progress: Float,
    color: Color
) {
    val sparkleCount = 8

    for (i in 0 until sparkleCount) {
        val angle = (i * 45f) * (Math.PI / 180f)
        val distance = radius * (0.8f + (i % 3) * 0.15f) * progress
        val x = centerX + (distance * cos(angle)).toFloat()
        val y = centerY + (distance * sin(angle)).toFloat()
        val sparkleSize = 4f * progress * (1f + (i % 2) * 0.5f)

        drawCircle(
            color = color.copy(alpha = 0.6f + (i % 3) * 0.13f),
            radius = sparkleSize,
            center = Offset(x, y)
        )
    }
}

@Preview
@Composable
private fun HeartPulseAnimationPreview() {
    HeartPulseAnimation(
        progress = 0.7f,
        modifier = Modifier.size(200.dp)
    )
}
