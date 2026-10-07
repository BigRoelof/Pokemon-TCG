package com.example.pokemontcg.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.pokemontcg.ui.theme.BallWhite
import com.example.pokemontcg.ui.theme.Ink
import com.example.pokemontcg.ui.theme.PokeRed

/**
 * A Poké Ball. When [filled] it's drawn in full color; otherwise as a [outlineColor] outline,
 * which the app uses for "not caught yet" and empty states.
 */
@Composable
fun PokeBall(
    filled: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    outlineColor: Color = Ink
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = this.size.minDimension * 0.09f
        val radius = (this.size.minDimension - stroke) / 2
        val lineColor = if (filled) Ink else outlineColor
        if (filled) {
            drawArc(
                color = PokeRed, startAngle = 180f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2)
            )
            drawArc(
                color = BallWhite, startAngle = 0f, sweepAngle = 180f, useCenter = true,
                topLeft = Offset(center.x - radius, center.y - radius), size = Size(radius * 2, radius * 2)
            )
        }
        drawCircle(color = lineColor, radius = radius, style = Stroke(stroke))
        drawLine(
            color = lineColor, strokeWidth = stroke,
            start = Offset(center.x - radius, center.y), end = Offset(center.x + radius, center.y)
        )
        val buttonRadius = radius * 0.32f
        if (filled) drawCircle(color = BallWhite, radius = buttonRadius)
        drawCircle(color = lineColor, radius = buttonRadius, style = Stroke(stroke))
    }
}
