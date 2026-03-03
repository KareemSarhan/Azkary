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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlin.math.*
import kotlin.random.Random

/**
 * Rain/Mercy animation for mercy and forgiveness zikr
 * Rain drops falling with splash effects
 */
@Composable
fun RainMercyAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4FC3F7)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "rainProgress"
    )

    // Remember random positions for drops
    val drops = remember {
        List(20) { index ->
            RainDrop(
                xFraction = Random.nextFloat(),
                delayFraction = index / 20f,
                speed = 0.5f + Random.nextFloat() * 0.5f,
                size = 3f + Random.nextFloat() * 4f
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "rainFall")

    val rainOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainOffset"
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            if (animatedProgress > 0) {
                // Draw rain cloud at top
                val cloudY = canvasHeight * 0.15f
                val cloudWidth = canvasWidth * 0.5f * animatedProgress
                val cloudHeight = canvasHeight * 0.12f

                drawCloud(
                    centerX = canvasWidth / 2,
                    centerY = cloudY,
                    width = cloudWidth,
                    height = cloudHeight,
                    color = Color.Gray.copy(alpha = 0.6f * animatedProgress)
                )

                // Draw rain drops
                if (animatedProgress > 0.2f) {
                    val dropProgress = (animatedProgress - 0.2f) / 0.8f
                    val activeDrops = (drops.size * dropProgress).toInt().coerceAtLeast(1)

                    drops.take(activeDrops).forEachIndexed { index, drop ->
                        val dropAnimation = (rainOffset + drop.delayFraction) % 1f
                        val startY = cloudY + cloudHeight * 0.5f
                        val endY = canvasHeight * 0.85f
                        val dropY = startY + (endY - startY) * dropAnimation * drop.speed

                        // Only draw if within bounds
                        if (dropY < endY) {
                            val x = canvasWidth * drop.xFraction

                            // Draw drop
                            drawLine(
                                color = color.copy(alpha = 0.7f * (1f - dropAnimation * 0.3f)),
                                start = Offset(x, dropY),
                                end = Offset(x, dropY + drop.size * 2),
                                strokeWidth = drop.size,
                                cap = StrokeCap.Round
                            )
                        }

                        // Draw splash when drop hits bottom
                        if (dropAnimation > 0.9f && dropY >= endY) {
                            val splashProgress = (dropAnimation - 0.9f) * 10f
                            val x = canvasWidth * drop.xFraction
                            drawSplash(
                                x = x,
                                y = endY,
                                progress = splashProgress,
                                color = color
                            )
                        }
                    }
                }

                // Draw ripples/puddles at bottom
                if (animatedProgress > 0.5f) {
                    val puddleProgress = (animatedProgress - 0.5f) / 0.5f
                    drawPuddles(
                        canvasWidth = canvasWidth,
                        canvasHeight = canvasHeight,
                        progress = puddleProgress,
                        color = color
                    )
                }
            }
        }
    }
}

private data class RainDrop(
    val xFraction: Float,
    val delayFraction: Float,
    val speed: Float,
    val size: Float
)

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCloud(
    centerX: Float,
    centerY: Float,
    width: Float,
    height: Float,
    color: Color
) {
    // Draw cloud using overlapping circles
    val circles = listOf(
        Offset(centerX - width * 0.3f, centerY),
        Offset(centerX - width * 0.1f, centerY - height * 0.3f),
        Offset(centerX + width * 0.1f, centerY - height * 0.2f),
        Offset(centerX + width * 0.3f, centerY),
        Offset(centerX, centerY + height * 0.1f)
    )

    circles.forEach { offset ->
        drawCircle(
            color = color,
            radius = height * 0.5f,
            center = offset
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSplash(
    x: Float,
    y: Float,
    progress: Float,
    color: Color
) {
    val splashRadius = 10f * progress
    val alpha = 1f - progress

    // Draw splash circles
    for (i in 0 until 3) {
        val angle = (i * 120f) * (Math.PI / 180f)
        val splashX = x + (splashRadius * cos(angle)).toFloat()
        val splashY = y - splashRadius * 0.5f * (1f - progress) + (splashRadius * sin(angle)).toFloat() * 0.3f

        drawCircle(
            color = color.copy(alpha = alpha * 0.6f),
            radius = 3f * (1f - progress * 0.5f),
            center = Offset(splashX, splashY)
        )
    }

    // Ripple
    drawCircle(
        color = color.copy(alpha = alpha * 0.3f),
        radius = splashRadius * 2f,
        center = Offset(x, y),
        style = Stroke(width = 2f)
    )
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawPuddles(
    canvasWidth: Float,
    canvasHeight: Float,
    progress: Float,
    color: Color
) {
    val puddleY = canvasHeight * 0.88f
    val puddleCount = 5

    for (i in 0 until puddleCount) {
        val x = canvasWidth * (0.15f + i * 0.17f)
        val puddleWidth = 30f + (i % 3) * 15f
        val puddleHeight = 8f + (i % 2) * 4f

        drawOval(
            color = color.copy(alpha = 0.3f * progress * (0.7f + (i % 3) * 0.15f)),
            topLeft = Offset(x - puddleWidth / 2, puddleY - puddleHeight / 2),
            size = androidx.compose.ui.geometry.Size(puddleWidth * progress, puddleHeight * progress)
        )
    }
}

@Preview
@Composable
private fun RainMercyAnimationPreview() {
    RainMercyAnimation(
        progress = 0.8f,
        modifier = Modifier.size(200.dp)
    )
}
