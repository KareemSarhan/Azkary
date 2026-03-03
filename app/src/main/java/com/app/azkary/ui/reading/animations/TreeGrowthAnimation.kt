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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Tree growth animation for "Subhan Allah w bihamdu" and similar zikr
 * Tree grows with each tap, branches animate, and leaves appear
 */
@Composable
fun TreeGrowthAnimation(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFF4CAF50)
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "treeGrowth"
    )

    val leafAnimation by animateFloatAsState(
        targetValue = if (progress > 0.3f) 1f else 0f,
        animationSpec = tween(600, delayMillis = 200),
        label = "leafAnimation"
    )

    val colors = MaterialTheme.colorScheme
    val trunkColor = Color(0xFF8D6E63)
    val leafColor = color

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerX = canvasWidth / 2

            // Tree trunk grows from bottom
            val trunkHeight = canvasHeight * 0.4f * animatedProgress
            val trunkWidth = 16.dp.toPx()

            if (trunkHeight > 0) {
                // Draw trunk
                drawRect(
                    color = trunkColor,
                    topLeft = Offset(centerX - trunkWidth / 2, canvasHeight - trunkHeight),
                    size = androidx.compose.ui.geometry.Size(trunkWidth, trunkHeight)
                )

                // Draw branches
                if (animatedProgress > 0.3f) {
                    val branchProgress = (animatedProgress - 0.3f) / 0.7f
                    drawBranches(
                        centerX = centerX,
                        startY = canvasHeight - trunkHeight * 0.7f,
                        progress = branchProgress,
                        color = trunkColor,
                        depth = 3
                    )
                }

                // Draw leaves
                if (leafAnimation > 0 && animatedProgress > 0.4f) {
                    drawLeaves(
                        centerX = centerX,
                        baseY = canvasHeight - trunkHeight * 0.5f,
                        progress = leafAnimation * ((animatedProgress - 0.4f) / 0.6f),
                        color = leafColor
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawBranches(
    centerX: Float,
    startY: Float,
    progress: Float,
    color: Color,
    depth: Int,
    angle: Float = -90f,
    length: Float = 80f
) {
    if (depth <= 0 || progress <= 0) return

    val angleRad = Math.toRadians(angle.toDouble())
    val endX = centerX + (length * progress * cos(angleRad)).toFloat()
    val endY = startY + (length * progress * sin(angleRad)).toFloat()

    // Draw branch
    drawLine(
        color = color,
        start = Offset(centerX, startY),
        end = Offset(endX, endY),
        strokeWidth = maxOf(2f, depth * 2f),
        cap = StrokeCap.Round
    )

    // Recursive sub-branches
    if (progress > 0.5f) {
        val subProgress = (progress - 0.5f) * 2f
        drawBranches(
            centerX = endX,
            startY = endY,
            progress = subProgress,
            color = color,
            depth = depth - 1,
            angle = angle - 30f,
            length = length * 0.7f
        )
        drawBranches(
            centerX = endX,
            startY = endY,
            progress = subProgress,
            color = color,
            depth = depth - 1,
            angle = angle + 30f,
            length = length * 0.7f
        )
    }
}

private fun DrawScope.drawLeaves(
    centerX: Float,
    baseY: Float,
    progress: Float,
    color: Color
) {
    val leafCount = 12
    val radius = 100f * progress

    for (i in 0 until leafCount) {
        val angle = (i * 360f / leafCount) * (Math.PI / 180f)
        val x = centerX + (radius * cos(angle)).toFloat() + (i % 3 - 1) * 40f
        val y = baseY + (radius * sin(angle)).toFloat() * 0.5f - 20f

        drawCircle(
            color = color.copy(alpha = 0.7f + (i % 3) * 0.1f),
            radius = 12f * progress,
            center = Offset(x, y)
        )
    }
}

@Preview
@Composable
private fun TreeGrowthAnimationPreview() {
    var progress by remember { mutableFloatStateOf(0.5f) }
    TreeGrowthAnimation(
        progress = progress,
        modifier = Modifier.size(200.dp)
    )
}
