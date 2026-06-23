package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Colorize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    currentColor: Int,
    savedPalette: List<Int>,
    onColorSelected: (Int) -> Unit,
    onSaveToPalette: (Int) -> Unit,
    onDismissRequest: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(Color(currentColor)) }
    var hexInputText by remember { mutableStateOf(colorToHex(currentColor)) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scopePresetColors = listOf(
        0xFF000000.toInt(), // Black
        0xFFFFFFFFF.toInt(), // White
        0xFFE91E63.toInt(), // Rose Magenta
        0xFF9C27B0.toInt(), // Grape Purple
        0xFF673AB7.toInt(), // Deep Violet
        0xFF3F51B5.toInt(), // Indigo
        0xFF2196F3.toInt(), // Sky Blue
        0xFF03A9F4.toInt(), // Ocean Coast
        0xFF00BCD4.toInt(), // Cyan
        0xFF009688.toInt(), // Teal
        0xFF4CAF50.toInt(), // Forest Green
        0xFF8BC34A.toInt(), // Lime Green
        0xFFCDDC39.toInt(), // Acid Yellow
        0xFFFFEB3B.toInt(), // Lemon Yellow
        0xFFFFC107.toInt(), // Amber Golden
        0xFFFF9800.toInt(), // Orange Sunshine
        0xFFFF5722.toInt(), // Warm Orange-Red
        0xFFF44336.toInt(), // Crimson Red
        0xFF795548.toInt(), // Cocoa Brown
        0xFF607D8B.toInt()  // Slate Blue Gray
    )

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = com.example.ui.theme.ElegantDarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .border(1.dp, com.example.ui.theme.ElegantBorder, RoundedCornerShape(28.dp)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Colorize,
                        contentDescription = "Color Picker Icon",
                        tint = com.example.ui.theme.ElegantLavender,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Visual Color Studio",
                        color = com.example.ui.theme.ElegantTextPrimary,
                        fontSize = 20.sp,
                        style = MaterialTheme.typography.titleLarge
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Interactive Color Hue/Saturation Canvas Bar
                Text(
                     text = "Spectrum Blend Hue",
                     color = com.example.ui.theme.ElegantTextSecondary,
                     fontSize = 12.sp,
                     modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))

                HueGradientSlider(
                    selectedColor = selectedColor,
                    onColorChange = {
                        selectedColor = it
                        hexInputText = colorToHex(it.toArgb())
                        errorMessage = null
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Hex input & Preview Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Preview indicator circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(selectedColor)
                            .border(2.dp, Color.White, CircleShape)
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    // Text Input
                    OutlinedTextField(
                        value = hexInputText,
                        onValueChange = { newVal ->
                            hexInputText = newVal
                            val cleanHex = newVal.removePrefix("#").trim()
                            if (cleanHex.length == 6 || cleanHex.length == 8) {
                                try {
                                    val parsed = android.graphics.Color.parseColor("#$cleanHex")
                                    selectedColor = Color(parsed)
                                    errorMessage = null
                                } catch (e: Exception) {
                                    errorMessage = "Invalid Color Code"
                                }
                            } else {
                                errorMessage = "Type 6 hex chars (e.g. FF5722)"
                            }
                        },
                        label = { Text("Color HEX Value", color = com.example.ui.theme.ElegantTextSecondary) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                            unfocusedTextColor = com.example.ui.theme.ElegantTextPrimary,
                            focusedBorderColor = com.example.ui.theme.ElegantLavender,
                            unfocusedBorderColor = com.example.ui.theme.ElegantBorder,
                            focusedLabelColor = com.example.ui.theme.ElegantLavender,
                            unfocusedLabelColor = com.example.ui.theme.ElegantBorder,
                            errorBorderColor = MaterialTheme.colorScheme.error,
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        ),
                        isError = errorMessage != null,
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.Start)
                            .padding(start = 72.dp, top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Saved Palettes
                Text(
                     text = "Classic Swatches & Layout Palette",
                     color = com.example.ui.theme.ElegantTextSecondary,
                     fontSize = 12.sp,
                     modifier = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(8.dp))

                // FlowRow for Palette swatches
                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 100.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val combinedColors = (scopePresetColors + savedPalette).distinct()
                    for (cInt in combinedColors) {
                        val col = Color(cInt)
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(col)
                                .border(
                                    width = if (selectedColor.toArgb() == cInt) 2.dp else 1.dp,
                                    color = if (selectedColor.toArgb() == cInt) Color.White else Color.White.copy(alpha = 0.15f),
                                    shape = CircleShape
                                )
                                .clickable {
                                    selectedColor = col
                                    hexInputText = colorToHex(cInt)
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor.toArgb() == cInt) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected color indicator",
                                    tint = if (isLightColor(col)) Color.Black else Color.White,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Bottom Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Save Palette slot
                    TextButton(
                        onClick = {
                            onSaveToPalette(selectedColor.toArgb())
                        },
                        colors = ButtonDefaults.textButtonColors(contentColor = com.example.ui.theme.ElegantLavender)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Icon", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add to Palette", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }

                    Row {
                        TextButton(onClick = onDismissRequest) {
                            Text("Cancel", color = com.example.ui.theme.ElegantTextSecondary)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onColorSelected(selectedColor.toArgb())
                                onDismissRequest()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.ElegantLavender,
                                contentColor = Color(0xFF13131A)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Confirm", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HueGradientSlider(
    selectedColor: Color,
    onColorChange: (Color) -> Unit
) {
    val density = LocalDensity.current
    val rainbowColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF9800), // Orange
        Color(0xFFFFEB3B), // Yellow
        Color(0xFF4CAF50), // Green
        Color(0xFF00BCD4), // Cyan
        Color(0xFF2196F3), // Blue
        Color(0xFF9C27B0), // Violet/Pink
        Color(0xFFFF0000)  // Wrap Red
    )

    var widthPx by remember { mutableStateOf(1f) }
    var dragPercent by remember { mutableStateOf(0.5f) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(30.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Brush.horizontalGradient(rainbowColors))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val rawX = offset.x.coerceIn(0f, widthPx)
                    dragPercent = rawX / widthPx
                    val computedColor = getColorAtPercent(dragPercent, rainbowColors)
                    onColorChange(computedColor)
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    val rawX = change.position.x.coerceIn(0f, widthPx)
                    dragPercent = rawX / widthPx
                    val computedColor = getColorAtPercent(dragPercent, rainbowColors)
                    onColorChange(computedColor)
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            widthPx = size.width
        }

        // Draw selection ring handle
        val handleOffsetX = (dragPercent * (widthPx / density.density)).dp
        Box(
            modifier = Modifier
                .offset(x = handleOffsetX - 15.dp)
                .size(30.dp)
                .clip(CircleShape)
                .border(3.dp, Color.White, CircleShape)
                .background(selectedColor)
        )
    }
}

private fun colorToHex(color: Int): String {
    return String.format("%06X", 0xFFFFFF and color)
}

private fun isLightColor(color: Color): Boolean {
    val luminance = 0.299f * color.red + 0.587f * color.green + 0.114f * color.blue
    return luminance > 0.5f
}

private fun getColorAtPercent(percent: Float, colors: List<Color>): Color {
    val safePercent = percent.coerceIn(0f, 1f)
    if (safePercent == 1f) return colors.last()

    val size = colors.size - 1
    val scaled = safePercent * size
    val index = scaled.toInt()
    val localPercent = scaled - index

    val col1 = colors[index]
    val col2 = colors[index + 1]

    return Color(
        red = col1.red + (col2.red - col1.red) * localPercent,
        green = col1.green + (col2.green - col1.green) * localPercent,
        blue = col1.blue + (col2.blue - col1.blue) * localPercent,
        alpha = 1f
    )
}
