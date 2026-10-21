package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SparklineChart(
    points: List<Float>,
    lineColor: Color,
    modifier: Modifier = Modifier
) {
    if (points.size < 2) return

    Canvas(modifier = modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height

        val minVal = points.minOrNull() ?: 0f
        val maxVal = points.maxOrNull() ?: 1f
        val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

        // Padding top and bottom so line doesn't get clipped
        val verticalPadding = height * 0.12f
        val effectiveHeight = height - (verticalPadding * 2)

        val stepX = width / (points.size - 1)

        val strokePath = Path()
        val fillPath = Path()

        // Helper to get coordinates
        fun getPoint(index: Int): Pair<Float, Float> {
            val x = index * stepX
            val normalizedY = (points[index] - minVal) / range
            // Inverted for canvas coords: 1.0 (max) -> top, 0.0 (min) -> bottom
            val y = height - verticalPadding - (normalizedY * effectiveHeight)
            return Pair(x, y)
        }

        val (firstX, firstY) = getPoint(0)
        strokePath.moveTo(firstX, firstY)
        fillPath.moveTo(firstX, height)
        fillPath.lineTo(firstX, firstY)

        for (i in 0 until points.size - 1) {
            val (currentX, currentY) = getPoint(i)
            val (nextX, nextY) = getPoint(i + 1)
            val cPointX1 = currentX + (nextX - currentX) / 2
            val cPointY1 = currentY
            val cPointX2 = currentX + (nextX - currentX) / 2
            val cPointY2 = nextY

            strokePath.cubicTo(cPointX1, cPointY1, cPointX2, cPointY2, nextX, nextY)
            fillPath.cubicTo(cPointX1, cPointY1, cPointX2, cPointY2, nextX, nextY)
        }

        val (lastX, _) = getPoint(points.size - 1)
        fillPath.lineTo(lastX, height)
        fillPath.close()

        // Draw area gradient fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(
                    lineColor.copy(alpha = 0.35f),
                    lineColor.copy(alpha = 0.08f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = strokePath,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    }
}
