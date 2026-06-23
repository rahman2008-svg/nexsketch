package com.example.ui

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import com.example.data.*
import java.io.File
import java.io.FileOutputStream

object CanvasExporter {

    /**
     * Renders drawing layers onto an Android Bitmap.
     */
    fun renderToBitmap(
        width: Int,
        height: Int,
        bgColor: Int,
        layers: List<CanvasLayer>,
        excludeBg: Boolean = false
    ): Bitmap {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw page color or make it transparent
        if (!excludeBg) {
            canvas.drawColor(bgColor)
        } else {
            canvas.drawColor(android.graphics.Color.TRANSPARENT, PorterDuff.Mode.CLEAR)
        }

        // Shift local canvas space centered coordinates (-w/2, -h/2) to standard Android Canvas starting at 0, 0
        canvas.translate(width / 2f, height / 2f)

        val paint = Paint().apply {
            isAntiAlias = true
            isDither = true
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
        }

        for (layer in layers) {
            if (!layer.isVisible) continue
            val layerAlpha = layer.alpha

            for (stroke in layer.strokes) {
                paint.reset()
                paint.isAntiAlias = true
                paint.isDither = true

                val baseColor = stroke.color
                val mergedOpacity = stroke.opacity * layerAlpha
                val alphaInt = (mergedOpacity * 255).toInt().coerceIn(0, 255)

                paint.color = baseColor
                paint.alpha = alphaInt
                paint.strokeWidth = stroke.strokeWidth

                if (stroke.shapeType != ShapeType.FREEHAND) {
                    paint.style = Paint.Style.STROKE
                    val startX = stroke.startX ?: 0f
                    val startY = stroke.startY ?: 0f
                    val endX = stroke.endX ?: 0f
                    val endY = stroke.endY ?: 0f

                    when (stroke.shapeType) {
                        ShapeType.LINE -> {
                            canvas.drawLine(startX, startY, endX, endY, paint)
                        }
                        ShapeType.ARROW -> {
                            canvas.drawLine(startX, startY, endX, endY, paint)
                            // Draw arrowhead lines
                            val dx = endX - startX
                            val dy = endY - startY
                            val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                            val arrowLen = (stroke.strokeWidth * 3f).coerceAtLeast(24f)
                            val arrowAngleRad = Math.toRadians(30.0)
                            val x1 = endX - arrowLen * Math.cos(angle - arrowAngleRad).toFloat()
                            val y1 = endY - arrowLen * Math.sin(angle - arrowAngleRad).toFloat()
                            val x2 = endX - arrowLen * Math.cos(angle + arrowAngleRad).toFloat()
                            val y2 = endY - arrowLen * Math.sin(angle + arrowAngleRad).toFloat()

                            canvas.drawLine(endX, endY, x1, y1, paint)
                            canvas.drawLine(endX, endY, x2, y2, paint)
                        }
                        ShapeType.RECTANGLE -> {
                            val left = Math.min(startX, endX)
                            val top = Math.min(startY, endY)
                            val right = Math.max(startX, endX)
                            val bottom = Math.max(startY, endY)
                            canvas.drawRect(left, top, right, bottom, paint)
                        }
                        ShapeType.CIRCLE -> {
                            val dx = startX - endX
                            val dy = startY - endY
                            val radius = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            canvas.drawCircle(startX, startY, radius, paint)
                        }
                        else -> {}
                    }
                } else {
                    if (stroke.points.isEmpty()) continue

                    if (stroke.brushType == BrushType.STICKER && stroke.stickerSymbol != null) {
                        val symbol = stroke.stickerSymbol
                        val fontSize = stroke.strokeWidth * 1.5f
                        paint.style = Paint.Style.FILL
                        paint.textSize = fontSize
                        paint.textAlign = Paint.Align.CENTER

                        var lastPoint: PointD? = null
                        val minSpacing = fontSize * 0.8f

                        for (pt in stroke.points) {
                            val distance = if (lastPoint != null) {
                                val dx = pt.x - lastPoint.x
                                val dy = pt.y - lastPoint.y
                                Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            } else {
                                Float.MAX_VALUE
                            }
                            if (distance >= minSpacing) {
                                canvas.drawText(symbol, pt.x, pt.y + fontSize / 3f, paint)
                                lastPoint = pt
                            }
                        }
                    } else if (stroke.brushType == BrushType.SMUDGE) {
                        // Soft charcoal smudge line
                        paint.style = Paint.Style.STROKE
                        val path = Path()
                        val first = stroke.points.first()
                        path.moveTo(first.x, first.y)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x, pt.y)
                        }

                        // Layer double smudge lines
                        paint.color = android.graphics.Color.GRAY
                        paint.alpha = (alphaInt * 0.15f).toInt().coerceIn(0, 255)
                        paint.strokeWidth = stroke.strokeWidth * 1.8f
                        canvas.drawPath(path, paint)

                        paint.color = android.graphics.Color.LTGRAY
                        paint.alpha = (alphaInt * 0.3f).toInt().coerceIn(0, 255)
                        paint.strokeWidth = stroke.strokeWidth
                        canvas.drawPath(path, paint)
                    } else {
                        // Standard line/pencil drawing path
                        paint.style = Paint.Style.STROKE
                        paint.strokeJoin = Paint.Join.ROUND
                        paint.strokeCap = if (stroke.brushType == BrushType.MARKER) Paint.Cap.SQUARE else Paint.Cap.ROUND

                        val path = Path()
                        val first = stroke.points.first()
                        path.moveTo(first.x, first.y)
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            path.lineTo(pt.x, pt.y)
                        }
                        canvas.drawPath(path, paint)
                    }
                }
            }
        }

        return bitmap
    }

    /**
     * Converts drawing vector layers into a standard-compliant SVG XML string.
     */
    fun convertToSvg(
        width: Float,
        height: Float,
        bgColor: Int,
        layers: List<CanvasLayer>,
        excludeBg: Boolean = false
    ): String {
        val sb = java.lang.StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n")
        sb.append("<svg width=\"$width\" height=\"$height\" viewBox=\"${-width/2} ${-height/2} $width $height\" xmlns=\"http://www.w3.org/2000/svg\">\n")

        // Solid canvas paper base
        if (!excludeBg) {
            val colorHex = String.format("#%06X", 0xFFFFFF and bgColor)
            sb.append("  <rect x=\"${-width/2}\" y=\"${-height/2}\" width=\"$width\" height=\"$height\" fill=\"$colorHex\" />\n")
        }

        for (layer in layers) {
            if (!layer.isVisible) continue
            val layerOpacity = layer.alpha
            val cleanName = layer.name.replace("\"", "&quot;")
            sb.append("  <g id=\"$cleanName\" opacity=\"$layerOpacity\">\n")

            for (stroke in layer.strokes) {
                val strokeOpacity = stroke.opacity
                val strokeW = stroke.strokeWidth
                val colorHex = String.format("#%06X", 0xFFFFFF and stroke.color)
                val strokeColorStr = if (stroke.brushType == BrushType.ERASER && !excludeBg) {
                    String.format("#%06X", 0xFFFFFF and bgColor)
                } else {
                    colorHex
                }

                if (stroke.shapeType != ShapeType.FREEHAND) {
                    val startX = stroke.startX ?: 0f
                    val startY = stroke.startY ?: 0f
                    val endX = stroke.endX ?: 0f
                    val endY = stroke.endY ?: 0f

                    when (stroke.shapeType) {
                        ShapeType.LINE -> {
                            sb.append("    <line x1=\"$startX\" y1=\"$startY\" x2=\"$endX\" y2=\"$endY\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-linecap=\"round\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                        }
                        ShapeType.ARROW -> {
                            sb.append("    <line x1=\"$startX\" y1=\"$startY\" x2=\"$endX\" y2=\"$endY\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-linecap=\"round\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                            // Draw arrowhead vectors in SVG
                            val dx = endX - startX
                            val dy = endY - startY
                            val angle = Math.atan2(dy.toDouble(), dx.toDouble())
                            val arrowLen = (strokeW * 3f).coerceAtLeast(24f)
                            val arrowAngleRad = Math.toRadians(30.0)
                            val x1 = endX - arrowLen * Math.cos(angle - arrowAngleRad).toFloat()
                            val y1 = endY - arrowLen * Math.sin(angle - arrowAngleRad).toFloat()
                            val x2 = endX - arrowLen * Math.cos(angle + arrowAngleRad).toFloat()
                            val y2 = endY - arrowLen * Math.sin(angle + arrowAngleRad).toFloat()

                            sb.append("    <line x1=\"$endX\" y1=\"$endY\" x2=\"$x1\" y2=\"$y1\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-linecap=\"round\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                            sb.append("    <line x1=\"$endX\" y1=\"$endY\" x2=\"$x2\" y2=\"$y2\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-linecap=\"round\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                        }
                        ShapeType.RECTANGLE -> {
                            val left = Math.min(startX, endX)
                            val top = Math.min(startY, endY)
                            val rectW = Math.abs(startX - endX)
                            val rectH = Math.abs(startY - endY)
                            sb.append("    <rect x=\"$left\" y=\"$top\" width=\"$rectW\" height=\"$rectH\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                        }
                        ShapeType.CIRCLE -> {
                            val dx = startX - endX
                            val dy = startY - endY
                            val radius = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            sb.append("    <circle cx=\"$startX\" cy=\"$startY\" r=\"$radius\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                        }
                        else -> {}
                    }
                } else {
                    if (stroke.points.isEmpty()) continue

                    if (stroke.brushType == BrushType.STICKER && stroke.stickerSymbol != null) {
                        val symbol = stroke.stickerSymbol
                        val fontSize = strokeW * 1.5f
                        var lastPoint: PointD? = null
                        val minSpacing = fontSize * 0.8f

                        for (pt in stroke.points) {
                            val distance = if (lastPoint != null) {
                                val dx = pt.x - lastPoint.x
                                val dy = pt.y - lastPoint.y
                                Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                            } else {
                                Float.MAX_VALUE
                            }
                            if (distance >= minSpacing) {
                                val escapedTxt = symbol.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                                sb.append("    <text x=\"${pt.x}\" y=\"${pt.y + fontSize / 3f}\" font-size=\"$fontSize\" fill=\"$strokeColorStr\" fill-opacity=\"$strokeOpacity\" text-anchor=\"middle\">$escapedTxt</text>\n")
                                lastPoint = pt
                            }
                        }
                    } else {
                        // Path vector representation
                        val strokeCap = if (stroke.brushType == BrushType.MARKER) "square" else "round"
                        sb.append("    <path d=\"")
                        val first = stroke.points.first()
                        sb.append("M ${first.x} ${first.y}")
                        for (i in 1 until stroke.points.size) {
                            val pt = stroke.points[i]
                            sb.append(" L ${pt.x} ${pt.y}")
                        }
                        sb.append("\" stroke=\"$strokeColorStr\" stroke-width=\"$strokeW\" stroke-linecap=\"$strokeCap\" stroke-linejoin=\"round\" stroke-opacity=\"$strokeOpacity\" fill=\"none\" />\n")
                    }
                }
            }
            sb.append("  </g>\n")
        }

        sb.append("</svg>\n")
        return sb.toString()
    }

    /**
     * Direct saves the artwork onto the internal storage directory of the app.
     * Stored strictly in application internal subdirectory "NexSketchExports" to make it highly secure and organized.
     */
    fun exportToInternalStorage(
        context: Context,
        fileName: String,
        format: String, // "PNG", "JPG", "SVG"
        transparent: Boolean,
        width: Int,
        height: Int,
        bgColor: Int,
        layers: List<CanvasLayer>
    ): File? {
        return try {
            val exportDir = File(context.filesDir, "NexSketchExports").apply {
                if (!exists()) mkdirs()
            }

            // Sanitise visual name for safety of local filesystem
            val cleanName = fileName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
            val baseName = if (cleanName.isNotBlank()) cleanName else "Artwork"
            val timestamp = System.currentTimeMillis()
            val finalFileName = when (format.uppercase()) {
                "PNG" -> "SKETCH_${baseName}_$timestamp.png"
                "JPG", "JPEG" -> "SKETCH_${baseName}_$timestamp.jpg"
                "SVG" -> "SKETCH_${baseName}_$timestamp.svg"
                else -> "SKETCH_${baseName}_$timestamp.png"
            }

            val outputFile = File(exportDir, finalFileName)

            if (format.uppercase() == "SVG") {
                val svgContent = convertToSvg(width.toFloat(), height.toFloat(), bgColor, layers, transparent)
                FileOutputStream(outputFile).use { out ->
                    out.write(svgContent.toByteArray(Charsets.UTF_8))
                }
            } else {
                val bitmap = renderToBitmap(width, height, bgColor, layers, transparent)
                FileOutputStream(outputFile).use { out ->
                    if (format.uppercase() == "JPG" || format.uppercase() == "JPEG") {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                    } else {
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    }
                }
                bitmap.recycle() // Recalls GC quickly for memory safety
            }

            outputFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * Lists all exported sketch files present in the internal container directory.
     */
    fun listInternalExports(context: Context): List<File> {
        val exportDir = File(context.filesDir, "NexSketchExports")
        if (!exportDir.exists()) return emptyList()
        return exportDir.listFiles()?.filter { it.isFile }?.sortedByDescending { it.lastModified() } ?: emptyList()
    }
}
