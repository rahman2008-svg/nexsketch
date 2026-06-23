package com.example.data

import com.squareup.moshi.JsonClass

enum class BrushType {
    PENCIL,
    PEN,
    BRUSH,
    MARKER,
    ERASER,
    SMUDGE,
    STICKER
}

enum class ShapeType {
    FREEHAND,
    LINE,
    ARROW,
    RECTANGLE,
    CIRCLE
}

enum class CanvasTemplate {
    BLANK,
    GRID,
    DOT,
    PERSPECTIVE
}

@JsonClass(generateAdapter = true)
data class PointD(
    val x: Float,
    val y: Float
)

@JsonClass(generateAdapter = true)
data class DrawStroke(
    val id: String = java.util.UUID.randomUUID().toString(),
    val points: List<PointD> = emptyList(),
    val strokeWidth: Float = 5f,
    val color: Int = -16777216, // Black
    val opacity: Float = 1f,
    val brushType: BrushType = BrushType.PEN,
    val shapeType: ShapeType = ShapeType.FREEHAND,
    val startX: Float? = null,
    val startY: Float? = null,
    val endX: Float? = null,
    val endY: Float? = null,
    val stickerSymbol: String? = null, // e.g. "⭐", "❤️", "✨"
    val timestamp: Long = System.currentTimeMillis()
)

@JsonClass(generateAdapter = true)
data class CanvasLayer(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val strokes: List<DrawStroke> = emptyList(),
    val isVisible: Boolean = true,
    val isLocked: Boolean = false,
    val alpha: Float = 1f
)

@JsonClass(generateAdapter = true)
data class BrushPreset(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val brushType: BrushType,
    val size: Float,
    val opacity: Float,
    val stickerSymbol: String? = null
)
