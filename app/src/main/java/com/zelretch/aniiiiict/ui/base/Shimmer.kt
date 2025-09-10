package com.zelretch.aniiiiict.ui.base

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val SHIMMER_ANIMATION_DURATION = 1600
private const val SHIMMER_ANIMATION_INITIAL_VALUE = -400f
private const val SHIMMER_ANIMATION_TARGET_VALUE = 1200f
private const val SHIMMER_GRADIENT_WIDTH_FRACTION = 1.5f

@Composable
fun Modifier.shimmer(cornerRadius: Dp = 0.dp, isLoading: Boolean): Modifier = if (isLoading) {
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.3f),
        Color.White.copy(alpha = 0.6f),
        Color.LightGray.copy(alpha = 0.3f)
    )
    val transition = rememberInfiniteTransition(label = "Shimmer")
    val translateAnim by transition.animateFloat(
        initialValue = SHIMMER_ANIMATION_INITIAL_VALUE,
        targetValue = SHIMMER_ANIMATION_TARGET_VALUE,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = SHIMMER_ANIMATION_DURATION,
                easing = FastOutSlowInEasing
            )
        ),
        label = "Translate"
    )
    this.drawWithCache {
        val brush = Brush.linearGradient(
            colors = shimmerColors,
            start = Offset(translateAnim, 0f),
            end = Offset(translateAnim + size.width / SHIMMER_GRADIENT_WIDTH_FRACTION, size.height)
        )
        val cornerPx = cornerRadius.toPx()
        onDrawWithContent {
            drawRoundRect(
                brush = brush,
                cornerRadius = CornerRadius(cornerPx, cornerPx),
                size = size
            )
        }
    }
} else {
    this
}
