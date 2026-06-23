package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.*
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.toSize
import com.example.data.*
import kotlin.math.*

@OptIn(ExperimentalTextApi::class)
@Composable
fun DrawingCanvas(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    // Collect settings from Viewmodel
    val layers by viewModel.layers.collectAsState()
    val currentStroke by viewModel.currentStroke.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()

    val zoom by viewModel.canvasZoom.collectAsState()
    val pan by viewModel.canvasPan.collectAsState()
    val rotation by viewModel.canvasRotation.collectAsState()

    val template by viewModel.canvasTemplate.collectAsState()
    val bgColorInt by viewModel.canvasBgColorInt.collectAsState()
    val perspectiveActive by viewModel.perspectiveGrid.collectAsState()

    // Timelapse playback filtered layers
    val isTimelapsePlaying by viewModel.isTimelapsePlaying.collectAsState()
    val activeLayers = if (isTimelapsePlaying) {
        viewModel.getTimelapseFilteredLayers()
    } else {
        layers
    }

    val textMeasurer = rememberTextMeasurer()

    val canvasWidth = currentProject?.canvasWidth ?: 1080f
    val canvasHeight = currentProject?.canvasHeight ?: 1920f
    val bgColor = Color(bgColorInt)

    // Gesture control wrapper
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF1E1E2C)) // Dark AMOLED workbench border surrounding canvas
            .pointerInput(isTimelapsePlaying) {
                if (isTimelapsePlaying) return@pointerInput
                // Detect Zoom, Pan & Rotate Gestures
                detectTransformGestures { _, panAmount, zoomAmount, rotationAmount ->
                    viewModel.canvasZoom.value = (zoom * zoomAmount).coerceIn(0.2f, 10f)
                    viewModel.canvasPan.value = pan + panAmount
                    viewModel.canvasRotation.value = rotation + rotationAmount
                }
            }
            .pointerInput(isTimelapsePlaying) {
                if (isTimelapsePlaying) return@pointerInput
                // Detect active drawing strokes
                detectDragGestures(
                    onDragStart = { startOffset ->
                        // Calculate relative local offset inside the pan/zoom/rotate space
                        val localOffset = transformScreenToCanvas(startOffset, pan, zoom, rotation, size.toSize())
                        viewModel.startNewStroke(localOffset)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val localOffset = transformScreenToCanvas(change.position, pan, zoom, rotation, size.toSize())
                        viewModel.updateCurrentStroke(localOffset)
                    },
                    onDragEnd = {
                        viewModel.finishCurrentStroke()
                    },
                    onDragCancel = {
                        viewModel.finishCurrentStroke()
                    }
                )
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            // Apply zoom, pan, and center rotation inside withTransform
            val centerX = size.width / 2f
            val centerY = size.height / 2f

            withTransform({
                // 1. Center the drawing viewport
                translate(centerX + pan.x, centerY + pan.y)
                scale(zoom, zoom, pivot = Offset.Zero)
                rotate(rotation, pivot = Offset.Zero)
            }) {
                // Draw Canvas Paper Base Sheet
                val rectX = -canvasWidth / 2f
                val rectY = -canvasHeight / 2f
                drawRect(
                    color = bgColor,
                    topLeft = Offset(rectX, rectY),
                    size = Size(canvasWidth, canvasHeight)
                )

                // Render Canvas Template Overlays (Behind sketches)
                drawCanvasTemplate(template, rectX, rectY, canvasWidth, canvasHeight)

                if (perspectiveActive) {
                    drawPerspectiveGrid(rectX, rectY, canvasWidth, canvasHeight)
                }

                // Render Saved Layer Node Strokes Sequentially (Bottom to Top)
                for (layer in activeLayers) {
                    if (!layer.isVisible) continue

                    // Apply Layer level opacity
                    val layerAlpha = layer.alpha

                    for (stroke in layer.strokes) {
                        drawStrokeItem(stroke, layerAlpha, textMeasurer)
                    }
                }

                // Draw Real-time Active Drafting Stroke Preview
                currentStroke?.let { stroke ->
                    drawStrokeItem(stroke, 1f, textMeasurer)
                }

                // Draw Border around Canvas boundaries
                drawRect(
                    color = Color.White.copy(alpha = 0.2f),
                    topLeft = Offset(rectX, rectY),
                    size = Size(canvasWidth, canvasHeight),
                    style = Stroke(width = 2f)
                )
            }
        }
    }
}

// Map screen gestures back into local canvas space centered coordinates
private fun transformScreenToCanvas(
    screenPos: Offset,
    pan: Offset,
    zoom: Float,
    rotation: Float,
    viewportSize: Size
): Offset {
    val cx = viewportSize.width / 2f
    val cy = viewportSize.height / 2f

    // 1. Translate screen position relative to canvas pan translation & center
    val tx = screenPos.x - cx - pan.x
    val ty = screenPos.y - cy - pan.y

    // 2. Rotate inverse
    val rad = Math.toRadians(-rotation.toDouble())
    val rx = tx * cos(rad) - ty * sin(rad)
    val ry = tx * sin(rad) + ty * cos(rad)

    // 3. Scale inverse
    val sx = rx / zoom
    val sy = ry / zoom

    return Offset(sx.toFloat(), sy.toFloat())
}

private fun DrawScope.drawCanvasTemplate(
    template: CanvasTemplate,
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val step = 80f
    val color = Color.Gray.copy(alpha = 0.2f)

    when (template) {
        CanvasTemplate.BLANK -> {}
        CanvasTemplate.GRID -> {
            // Horizontal lines
            var currentY = top
            while (currentY <= top + height) {
                drawLine(
                    color = color,
                    start = Offset(left, currentY),
                    end = Offset(left + width, currentY),
                    strokeWidth = 1f
                )
                currentY += step
            }
            // Vertical lines
            var currentX = left
            while (currentX <= left + width) {
                drawLine(
                    color = color,
                    start = Offset(currentX, top),
                    end = Offset(currentX, top + height),
                    strokeWidth = 1f
                )
                currentX += step
            }
        }
        CanvasTemplate.DOT -> {
            var currentY = top + step / 2f
            while (currentY <= top + height) {
                var currentX = left + step / 2f
                while (currentX <= left + width) {
                    drawCircle(
                        color = color.copy(alpha = 0.4f),
                        radius = 2.5f,
                        center = Offset(currentX, currentY)
                    )
                    currentX += step
                }
                currentY += step
            }
        }
        CanvasTemplate.PERSPECTIVE -> {
            // Drawn via drawPerspectiveGrid separately
        }
    }
}

private fun DrawScope.drawPerspectiveGrid(
    left: Float,
    top: Float,
    width: Float,
    height: Float
) {
    val centerY = top + height / 2f
    val horizonColor = Color(0xFFE91E63).copy(alpha = 0.35f) // Pink horizon line
    val lineLight = Color(0xFF2196F3).copy(alpha = 0.2f) // Light blue perspektive lines

    // Horizon line
    drawLine(
        color = horizonColor,
        start = Offset(left, centerY),
        end = Offset(left + width, centerY),
        strokeWidth = 2f
    )

    // Off-screen Left & Right Vanishing points
    val lvp = Offset(left - 200f, centerY)
    val rvp = Offset(left + width + 200f, centerY)

    drawCircle(color = horizonColor, radius = 5f, center = lvp)
    drawCircle(color = horizonColor, radius = 5f, center = rvp)

    // Radiating angles
    for (angleDeg in -75..75 step 15) {
        val rad = Math.toRadians(angleDeg.toDouble())
        val cosA = cos(rad).toFloat()
        val sinA = sin(rad).toFloat()

        // Line left vanishing point going rightward
        drawLine(
            color = lineLight,
            start = lvp,
            end = Offset(lvp.x + 3000f * cosA, lvp.y + 3000f * sinA),
            strokeWidth = 1f
        )

        // Line right vanishing point going leftward
        drawLine(
            color = lineLight,
            start = rvp,
            end = Offset(rvp.x - 3000f * cosA, rvp.y + 3000f * sinA),
            strokeWidth = 1f
        )
    }
}

@OptIn(ExperimentalTextApi::class)
private fun DrawScope.drawStrokeItem(
    stroke: DrawStroke,
    layerAlpha: Float,
    textMeasurer: TextMeasurer
) {
    val color = Color(stroke.color)
    val alpha = stroke.opacity * layerAlpha
    val pathEffect = if (stroke.brushType == BrushType.PENCIL) {
        PathEffect.dashPathEffect(floatArrayOf(5f, 5f), 0f)
    } else {
        null
    }

    val capStyle = if (stroke.brushType == BrushType.MARKER) StrokeCap.Square else StrokeCap.Round
    val strokeWidth = stroke.strokeWidth

    // Shapes rendering (Non-freehand)
    if (stroke.shapeType != ShapeType.FREEHAND && stroke.startX != null && stroke.startY != null && stroke.endX != null && stroke.endY != null) {
        val start = Offset(stroke.startX, stroke.startY)
        val end = Offset(stroke.endX, stroke.endY)

        when (stroke.shapeType) {
            ShapeType.LINE -> {
                drawLine(
                    color = color,
                    start = start,
                    end = end,
                    strokeWidth = strokeWidth,
                    alpha = alpha,
                    pathEffect = pathEffect,
                    cap = capStyle
                )
            }
            ShapeType.ARROW -> {
                drawArrowLine(start, end, color, strokeWidth, alpha, pathEffect)
            }
            ShapeType.RECTANGLE -> {
                val minX = min(start.x, end.x)
                val minY = min(start.y, end.y)
                val rectW = abs(start.x - end.x)
                val rectH = abs(start.y - end.y)
                drawRect(
                    color = color,
                    topLeft = Offset(minX, minY),
                    size = Size(rectW, rectH),
                    alpha = alpha,
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                )
            }
            ShapeType.CIRCLE -> {
                val dx = start.x - end.x
                val dy = start.y - end.y
                val radius = sqrt(dx * dx + dy * dy)
                drawCircle(
                    color = color,
                    radius = radius,
                    center = start,
                    alpha = alpha,
                    style = Stroke(width = strokeWidth, pathEffect = pathEffect)
                )
            }
            else -> {}
        }
        return
    }

    // Freehand/Sticker rendering
    if (stroke.points.isEmpty()) return

    if (stroke.brushType == BrushType.STICKER && stroke.stickerSymbol != null) {
        val symbol = stroke.stickerSymbol
        val fontScale = strokeWidth * 1.5f
        // Render characters along points with spacing
        val textStyle = TextStyle(fontSize = fontScale.sp)
        var lastPoint: Offset? = null
        val minSpacing = fontScale * 0.8f // pixel spacing based on size

        for (pt in stroke.points) {
            val currentOffset = Offset(pt.x, pt.y)
            val distance = lastPoint?.let { (currentOffset - it).getDistance() } ?: Float.MAX_VALUE
            if (distance >= minSpacing) {
                // Render sticker centered on point
                val textLayoutResult = textMeasurer.measure(
                    text = AnnotatedString(symbol),
                    style = textStyle
                )
                val tw = textLayoutResult.size.width
                val th = textLayoutResult.size.height
                drawText(
                    textMeasurer = textMeasurer,
                    text = symbol,
                    style = textStyle.copy(color = color.copy(alpha = alpha)),
                    topLeft = Offset(currentOffset.x - tw / 2f, currentOffset.y - th / 2f)
                )
                lastPoint = currentOffset
            }
        }
    } else if (stroke.brushType == BrushType.SMUDGE) {
        // Draw charcoal / smudge as a super soft, wide semi-transparent stroke
        val smudgePath = Path().apply {
            val first = stroke.points.first()
            moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                lineTo(pt.x, pt.y)
            }
        }
        // Compose multiple layered strokes with high alpha falloff to simulate beautiful smudge
        drawPath(
            path = smudgePath,
            color = Color.LightGray,
            alpha = alpha * 0.15f,
            style = Stroke(
                width = strokeWidth * 2.2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
        drawPath(
            path = smudgePath,
            color = Color.Gray,
            alpha = alpha * 0.08f,
            style = Stroke(
                width = strokeWidth * 1.2f,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round
            )
        )
    } else {
        // Standard ink/pencil/marker line drawing
        val drawPath = Path().apply {
            val first = stroke.points.first()
            moveTo(first.x, first.y)
            for (i in 1 until stroke.points.size) {
                val pt = stroke.points[i]
                lineTo(pt.x, pt.y)
            }
        }
        drawPath(
            path = drawPath,
            color = color,
            alpha = alpha,
            style = Stroke(
                width = strokeWidth,
                cap = capStyle,
                join = StrokeJoin.Round,
                pathEffect = pathEffect
            )
        )
    }
}

private fun DrawScope.drawArrowLine(
    start: Offset,
    end: Offset,
    color: Color,
    strokeWidth: Float,
    alpha: Float,
    pathEffect: PathEffect?
) {
    drawLine(
        color = color,
        start = start,
        end = end,
        strokeWidth = strokeWidth,
        alpha = alpha,
        pathEffect = pathEffect,
        cap = StrokeCap.Round
    )

    // Calculate Arrowhead lines
    val dx = end.x - start.x
    val dy = end.y - start.y
    val angle = atan2(dy, dx)
    val arrowLen = (strokeWidth * 3f).coerceAtLeast(24f)
    val arrowAngleRad = Math.toRadians(30.0)

    val x1 = end.x - arrowLen * cos(angle - arrowAngleRad).toFloat()
    val y1 = end.y - arrowLen * sin(angle - arrowAngleRad).toFloat()
    val x2 = end.x - arrowLen * cos(angle + arrowAngleRad).toFloat()
    val y2 = end.y - arrowLen * sin(angle + arrowAngleRad).toFloat()

    drawLine(
        color = color,
        start = end,
        end = Offset(x1, y1),
        strokeWidth = strokeWidth,
        alpha = alpha,
        pathEffect = pathEffect,
        cap = StrokeCap.Round
    )
    drawLine(
        color = color,
        start = end,
        end = Offset(x2, y2),
        strokeWidth = strokeWidth,
        alpha = alpha,
        pathEffect = pathEffect,
        cap = StrokeCap.Round
    )
}
