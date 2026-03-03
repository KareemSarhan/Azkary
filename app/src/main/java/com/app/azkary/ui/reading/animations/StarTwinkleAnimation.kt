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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * Star twinkle animation for general zikr
 * Stars that twinkle and appear with each tap
 */
@Composable
fun StarTwinkleAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFFFD700)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "starProgress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "starTwinkle")

    // Create multiple twinkling phases for different stars
    val twinkleOffsets = remember {
        List(15) { Random.nextFloat() }
    }

    // Animated twinkle values for each star
    val twinkleValues = List(15) { index ->
        infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 800 + (index * 150),
                    delayMillis = index * 100,
                    easing = FastOutSlowInEasing
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "twinkle_$index"
        )
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2
            val centerY = canvasHeight / 2

            if (animatedProgress > 0) {
                val activeStars = (15 * animatedProgress).toInt().coerceAtLeast(1)

                // Draw stars in a spiral pattern
                for (i in 0 until activeStars) {
                    val angle = i * 137.5f * (Math.PI / 180f) // Golden angle
                    val radius = 20f + i * 8f
                    val x = centerX + (radius * cos(angle)).toFloat()
                    val y = centerY + (radius * sin(angle)).toFloat()

                    val twinkle = twinkleValues[i].value
                    val starSize = (4f + (i % 4) * 2f) * twinkle

                    // Vary colors slightly
                    val starColor = when (i % 4) {
                        0 -> color
                        1 -> Color(0xFFFFA07A) // Light salmon
                        2 -> Color(0xFF87CEEB) // Sky blue
                        else -> Color(0xFFDDA0DD) // Plum
                    }

                    drawStar(
                        x = x,
                        y = y,
                        size = starSize,
                        color = starColor.copy(alpha = 0.6f + twinkle * 0.4f)
                    )
                }

                // Draw central glowing orb
                if (animatedProgress > 0.3f) {
                    val orbProgress = (animatedProgress - 0.3f) / 0.7f
                    val orbSize = 15f * orbProgress

                    drawCircle(
                        color = Color.White.copy(alpha = 0.8f),
                        radius = orbSize,
                        center = Offset(centerX, centerY)
                    )

                    drawCircle(
                        color = color.copy(alpha = 0.5f * orbProgress),
                        radius = orbSize * 2f,
                        center = Offset(centerX, centerY)
                    )
                }

                // Draw connecting lines for constellation effect
                if (animatedProgress > 0.6f) {
                    val lineProgress = (animatedProgress - 0.6f) / 0.4f
                    drawConstellationLines(
                        centerX = centerX,
                        centerY = centerY,
                        starCount = activeStars,
                        progress = lineProgress,
                        color = color.copy(alpha = 0.3f * lineProgress)
                    )
                }
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawStar(
    x: Float,
    y: Float,
    size: Float,
    color: Color
) {
    // Draw a 4-pointed star
    val path = Path().apply {
        // Top point
        moveTo(x, y - size)
        // Right inner
        lineTo(x + size * 0.3f, y - size * 0.3f)
        // Right point
        lineTo(x + size, y)
        // Bottom inner
        lineTo(x + size * 0.3f, y + size * 0.3f)
        // Bottom point
        lineTo(x, y + size)
        // Left inner
        lineTo(x - size * 0.3f, y + size * 0.3f)
        // Left point
        lineTo(x - size, y)
        // Top inner
        lineTo(x - size * 0.3f, y - size * 0.3f)
        close()
    }

    drawPath(
        path = path,
        color = color,
        style = Fill
    )

    // Add glow
    drawCircle(
        color = color.copy(alpha = 0.3f),
        radius = size * 1.5f,
        center = Offset(x, y)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawConstellationLines(
    centerX: Float,
    centerY: Float,
    starCount: Int,
    progress: Float,
    color: Color
) {
    // Connect stars that are close to each other
    val positions = mutableListOf<Offset>()

    for (i in 0 until starCount) {
        val angle = i * 137.5f * (Math.PI / 180f)
        val radius = 20f + i * 8f
        val x = centerX + (radius * cos(angle)).toFloat()
        val y = centerY + (radius * sin(angle)).toFloat()
        positions.add(Offset(x, y))
    }

    // Draw lines between nearby stars
    for (i in positions.indices) {
        for (j in i + 1 until positions.size) {
            val distance = (positions[i] - positions[j]).getDistance()
            if (distance < 50f) {
                drawLine(
                    color = color,
                    start = positions[i],
                    end = positions[j],
                    strokeWidth = 1f * progress * (1f - distance / 50f),
                    alpha = progress * (1f - distance / 50f)
                )
            }
        }
    }
}

@Preview
@Composable
private fun StarTwinkleAnimationPreview() {
    StarTwinkleAnimation(
        progress = 0.8f,
        modifier = Modifier.size(200.dp)
    )
}
