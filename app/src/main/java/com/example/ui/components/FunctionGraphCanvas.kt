package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.math.Complex
import com.example.math.MathParser
import com.example.ui.GraphFunctionUiModel
import kotlin.math.*

/**
 * High-performance, anti-aliased mathematical Canvas graphing component.
 * Visualizes arbitrary single and multi-variable mathematical functions in real-time.
 */
@Composable
fun FunctionGraphCanvas(
    functions: List<GraphFunctionUiModel>,
    mathParser: MathParser,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    traceX: Double?,
    showTangent: Boolean = false,
    showAreaShading: Boolean = false,
    areaRange: Pair<Double, Double> = -2.0 to 2.0,
    onViewportChange: (newXMin: Double, newXMax: Double, newYMin: Double, newYMax: Double) -> Unit,
    onTraceChange: (Double?) -> Unit,
    modifier: Modifier = Modifier
) {
    val textPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#94A3B8")
            textSize = 28f
            isAntiAlias = true
        }
    }

    val axisTextPaint = remember {
        android.graphics.Paint().apply {
            color = android.graphics.Color.parseColor("#CBD5E1")
            textSize = 30f
            isFakeBoldText = true
            isAntiAlias = true
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .testTag("function_graph_canvas")
            .pointerInput(xMin, xMax, yMin, yMax) {
                detectTransformGestures { _, pan, zoom, _ ->
                    val cx = (xMin + xMax) / 2.0
                    val cy = (yMin + yMax) / 2.0
                    val currentHalfW = (xMax - xMin) / 2.0
                    val currentHalfH = (yMax - yMin) / 2.0

                    val newHalfW = if (zoom != 1f) (currentHalfW / zoom).coerceIn(0.001, 100000.0) else currentHalfW
                    val newHalfH = if (zoom != 1f) (currentHalfH / zoom).coerceIn(0.001, 100000.0) else currentHalfH

                    val w = newHalfW * 2.0
                    val h = newHalfH * 2.0
                    val dx = if (pan.x != 0f) -pan.x / size.width * w else 0.0
                    val dy = if (pan.y != 0f) pan.y / size.height * h else 0.0 // screen y is inverted

                    onViewportChange(
                        cx - newHalfW + dx,
                        cx + newHalfW + dx,
                        cy - newHalfH + dy,
                        cy + newHalfH + dy
                    )
                }
            }
            .pointerInput(xMin, xMax) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val xVal = xMin + (offset.x / size.width) * (xMax - xMin)
                        onTraceChange(xVal)
                    },
                    onDrag = { change, _ ->
                        val xVal = xMin + (change.position.x / size.width) * (xMax - xMin)
                        onTraceChange(xVal)
                    },
                    onDragEnd = { /* retain trace point for inspection */ },
                    onDragCancel = { onTraceChange(null) }
                )
            }
    ) {
        val canvasW = size.width
        val canvasH = size.height

        if (canvasW <= 0f || canvasH <= 0f) return@Canvas

        val xSpan = (xMax - xMin).coerceAtLeast(1e-7)
        val ySpan = (yMax - yMin).coerceAtLeast(1e-7)

        fun toScreenX(x: Double): Float = ((x - xMin) / xSpan * canvasW).toFloat()
        fun toScreenY(y: Double): Float = ((yMax - y) / ySpan * canvasH).toFloat()
        fun toMathX(px: Float): Double = xMin + (px / canvasW) * xSpan
        fun toMathY(py: Float): Double = yMax - (py / canvasH) * ySpan

        // Draw dynamic coordinate grid
        drawCartesianGrid(
            canvasW = canvasW,
            canvasH = canvasH,
            xMin = xMin,
            xMax = xMax,
            yMin = yMin,
            yMax = yMax,
            xSpan = xSpan,
            ySpan = ySpan,
            toScreenX = ::toScreenX,
            toScreenY = ::toScreenY,
            textPaint = textPaint,
            axisTextPaint = axisTextPaint
        )

        // Shaded area under the curve if active
        val primaryVisible = functions.firstOrNull { it.isVisible && it.expression.isNotBlank() }
        if (showAreaShading && primaryVisible != null) {
            drawAreaUnderCurve(
                fn = primaryVisible,
                parser = mathParser,
                areaRange = areaRange,
                xMin = xMin,
                xMax = xMax,
                canvasH = canvasH,
                toScreenX = ::toScreenX,
                toScreenY = ::toScreenY
            )
        }

        // Plot each mathematical function
        val pixelStep = 2 // Sample every 2 horizontal pixels
        val numSamples = (canvasW / pixelStep).toInt() + 1

        functions.forEach { fn ->
            if (fn.isVisible && fn.expression.isNotBlank()) {
                val path = Path()
                var isPathStarted = false
                var lastValidY = Float.NaN

                for (i in 0..numSamples) {
                    val px = (i * pixelStep).toFloat().coerceAtMost(canvasW)
                    val x = toMathX(px)
                    val y = mathParser.evaluateReal(fn.expression, x)

                    if (y.isNaN() || y.isInfinite()) {
                        isPathStarted = false
                        lastValidY = Float.NaN
                        continue
                    }

                    val py = toScreenY(y)

                    // Asymptote clipping: discard huge vertical jump (e.g. tan(x), 1/x)
                    if (!lastValidY.isNaN() && abs(py - lastValidY) > canvasH * 0.75f) {
                        isPathStarted = false
                    }

                    if (!isPathStarted) {
                        path.moveTo(px, py)
                        isPathStarted = true
                    } else {
                        path.lineTo(px, py)
                    }
                    lastValidY = py
                }

                drawPath(
                    path = path,
                    color = fn.color,
                    style = Stroke(width = 3.5f)
                )
            }
        }

        // Draw Tangent Line at Trace Cursor if enabled
        if (showTangent && traceX != null && primaryVisible != null) {
            val tx = traceX
            val ty = mathParser.evaluateReal(primaryVisible.expression, tx)
            if (!ty.isNaN() && !ty.isInfinite()) {
                // Compute numerical derivative f'(x)
                val h = 1e-5
                val yPlus = mathParser.evaluateReal(primaryVisible.expression, tx + h)
                val yMinus = mathParser.evaluateReal(primaryVisible.expression, tx - h)
                val slope = (yPlus - yMinus) / (2.0 * h)

                if (!slope.isNaN() && !slope.isInfinite()) {
                    // Line equation: y - ty = slope * (x - tx) => y = slope*(x - tx) + ty
                    val x1 = xMin
                    val y1 = slope * (x1 - tx) + ty
                    val x2 = xMax
                    val y2 = slope * (x2 - tx) + ty

                    drawLine(
                        color = Color(0xFFFBBF24), // Amber tangent
                        start = Offset(toScreenX(x1), toScreenY(y1)),
                        end = Offset(toScreenX(x2), toScreenY(y2)),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // Draw interactive trace cursor and intersection points
        traceX?.let { tx ->
            if (tx in xMin..xMax) {
                val screenX = toScreenX(tx)

                // Vertical inspection line
                drawLine(
                    color = Color(0x99FFFFFF),
                    start = Offset(screenX, 0f),
                    end = Offset(screenX, canvasH),
                    strokeWidth = 1.5f
                )

                // Plot glowing cursor marker on each visible curve
                functions.forEach { fn ->
                    if (fn.isVisible && fn.expression.isNotBlank()) {
                        val ty = mathParser.evaluateReal(fn.expression, tx)
                        if (!ty.isNaN() && !ty.isInfinite()) {
                            val screenY = toScreenY(ty)
                            // Outer glowing aura
                            drawCircle(
                                color = fn.color.copy(alpha = 0.4f),
                                radius = 9.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                            // Main dot
                            drawCircle(
                                color = fn.color,
                                radius = 6.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                            // White core
                            drawCircle(
                                color = Color.White,
                                radius = 2.5.dp.toPx(),
                                center = Offset(screenX, screenY)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun DrawScope.drawCartesianGrid(
    canvasW: Float,
    canvasH: Float,
    xMin: Double,
    xMax: Double,
    yMin: Double,
    yMax: Double,
    xSpan: Double,
    ySpan: Double,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float,
    textPaint: android.graphics.Paint,
    axisTextPaint: android.graphics.Paint
) {
    // Calculate smart grid interval
    val targetSteps = 8
    val rawStep = xSpan / targetSteps
    val magnitude = 10.0.pow(floor(log10(rawStep.coerceAtLeast(1e-12))))
    val normalized = rawStep / magnitude
    val stepMultiplier = when {
        normalized < 1.5 -> 1.0
        normalized < 3.5 -> 2.0
        normalized < 7.5 -> 5.0
        else -> 10.0
    }
    val gridStep = stepMultiplier * magnitude

    val gridColor = Color(0xFF161F30)
    val axisColor = Color(0xFF475569)

    // Vertical grid lines (X axis ticks)
    val firstX = ceil(xMin / gridStep) * gridStep
    var currX = firstX
    while (currX <= xMax + 1e-9) {
        val px = toScreenX(currX)
        val isYAxis = abs(currX) < 1e-6

        drawLine(
            color = if (isYAxis) axisColor else gridColor,
            start = Offset(px, 0f),
            end = Offset(px, canvasH),
            strokeWidth = if (isYAxis) 2.5f else 1f
        )

        // Draw numerical label
        if (!isYAxis && px > 15f && px < canvasW - 15f) {
            val label = Complex.formatDouble(currX, 2)
            val yPos = (toScreenY(0.0) + 30f).coerceIn(36f, canvasH - 12f)
            drawContext.canvas.nativeCanvas.drawText(label, px - 12f, yPos, textPaint)
        }
        currX += gridStep
    }

    // Horizontal grid lines (Y axis ticks)
    val firstY = ceil(yMin / gridStep) * gridStep
    var currY = firstY
    while (currY <= yMax + 1e-9) {
        val py = toScreenY(currY)
        val isXAxis = abs(currY) < 1e-6

        drawLine(
            color = if (isXAxis) axisColor else gridColor,
            start = Offset(0f, py),
            end = Offset(canvasW, py),
            strokeWidth = if (isXAxis) 2.5f else 1f
        )

        // Draw numerical label
        if (!isXAxis && py > 15f && py < canvasH - 15f) {
            val label = Complex.formatDouble(currY, 2)
            val xPos = (toScreenX(0.0) + 12f).coerceIn(12f, canvasW - 65f)
            drawContext.canvas.nativeCanvas.drawText(label, xPos, py - 6f, textPaint)
        }
        currY += gridStep
    }

    // Origin marker (0,0)
    if (0.0 in xMin..xMax && 0.0 in yMin..yMax) {
        val originX = toScreenX(0.0)
        val originY = toScreenY(0.0)
        drawCircle(
            color = axisColor,
            radius = 3.dp.toPx(),
            center = Offset(originX, originY)
        )
    }
}

private fun DrawScope.drawAreaUnderCurve(
    fn: GraphFunctionUiModel,
    parser: MathParser,
    areaRange: Pair<Double, Double>,
    xMin: Double,
    xMax: Double,
    canvasH: Float,
    toScreenX: (Double) -> Float,
    toScreenY: (Double) -> Float
) {
    val (startA, endB) = areaRange
    val a = min(startA, endB).coerceIn(xMin, xMax)
    val b = max(startA, endB).coerceIn(xMin, xMax)
    if (b <= a) return

    val path = Path()
    val y0 = toScreenY(0.0).coerceIn(0f, canvasH)
    val startPx = toScreenX(a)
    path.moveTo(startPx, y0)

    val step = 3
    var currX = a
    val delta = (b - a) / 100.0
    while (currX <= b + 1e-9) {
        val px = toScreenX(currX)
        val y = parser.evaluateReal(fn.expression, currX)
        if (!y.isNaN() && !y.isInfinite()) {
            val py = toScreenY(y)
            path.lineTo(px, py)
        }
        currX += delta
    }

    val endPx = toScreenX(b)
    path.lineTo(endPx, y0)
    path.close()

    drawPath(
        path = path,
        color = fn.color.copy(alpha = 0.2f)
    )
}
