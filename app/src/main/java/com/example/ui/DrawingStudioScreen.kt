package com.example.ui

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint as AndroidPaint
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.*
import java.io.File
import java.io.FileOutputStream

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DrawingStudioScreen(
    viewModel: DrawingViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val layers by viewModel.layers.collectAsState()
    val activeLayerId by viewModel.activeLayerId.collectAsState()
    val currentProject by viewModel.currentProject.collectAsState()

    // Tools & Settings flows
    val activeBrush by viewModel.activeBrushType.collectAsState()
    val activeShape by viewModel.activeShapeType.collectAsState()
    val strokeWidth by viewModel.strokeWidth.collectAsState()
    val opacity by viewModel.opacity.collectAsState()
    val selectedColorInt by viewModel.selectedColorInt.collectAsState()
    val stickerSymbol by viewModel.stickerSymbol.collectAsState()

    val snapToGrid by viewModel.snapToGrid.collectAsState()
    val perspectiveActive by viewModel.perspectiveGrid.collectAsState()
    val smoothing by viewModel.handwritingSmoothing.collectAsState()

    val colorPalette by viewModel.colorPalette.collectAsState()
    val presets by viewModel.brushPresets.collectAsState()

    // Dialog & overlay visibilities
    var showColorPicker by remember { mutableStateOf(false) }
    var showLayersPanel by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var showExportsPanel by remember { mutableStateOf(false) }
    var showGridSettings by remember { mutableStateOf(false) }
    var showBrushPresetDialog by remember { mutableStateOf(false) }
    var showCustomPresetCreator by remember { mutableStateOf(false) }
    var newPresetName by remember { mutableStateOf("") }

    // History & Export collections
    val canUndo by viewModel.canUndo.collectAsState()
    val canRedo by viewModel.canRedo.collectAsState()
    val exportedFiles by viewModel.exportedFiles.collectAsState()

    // Visual active tools dropdown triggers
    var shapeMenuExpanded by remember { mutableStateOf(false) }
    var stickerMenuExpanded by remember { mutableStateOf(false) }

    // Timelapse playback triggers
    val isTimelapsePlaying by viewModel.isTimelapsePlaying.collectAsState()
    val timelapseProgress by viewModel.timelapseProgress.collectAsState()

    // Pick dynamic background image launcher
    var pickedImageUriState = remember { mutableStateOf<String?>(null) }
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri: Uri? ->
            if (uri != null) {
                pickedImageUriState.value = uri.toString()
                Toast.makeText(context, "Reference layout background imported!", Toast.LENGTH_SHORT).show()
            }
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = com.example.ui.theme.ElegantDarkBackground,
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Core Customizable Digital Art Canvas
            DrawingCanvas(
                viewModel = viewModel,
                modifier = Modifier.fillMaxSize()
            )

            // Dynamic watermarked sticker indicating background image if active
            pickedImageUriState.value?.let { _ ->
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp, bottom = 120.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = Color.LightGray, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "Reference active", color = Color.LightGray, fontSize = 9.sp)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear reference",
                            tint = Color.Red,
                            modifier = Modifier
                                .size(14.dp)
                                .clickable {
                                    pickedImageUriState.value = null
                                }
                        )
                    }
                }
            }

            // 2. Pro Header Controls Bar Overlay (Elegant Edge-to-Edge Top App Bar)
            if (!isTimelapsePlaying) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .background(com.example.ui.theme.ElegantDarkBackground)
                        .drawBehind {
                            drawLine(
                                color = com.example.ui.theme.ElegantBorder,
                                start = Offset(0f, size.height),
                                end = Offset(size.width, size.height),
                                strokeWidth = 1.dp.toPx()
                            )
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .align(Alignment.TopCenter),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                viewModel.saveCurrentProjectState()
                                viewModel.isGalleryMode.value = true
                            },
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.04f))
                                .testTag("back_to_gallery_button")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to list", tint = com.example.ui.theme.ElegantTextPrimary)
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = currentProject?.name ?: "Sandbox Sketch",
                                color = com.example.ui.theme.ElegantTextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1
                            )
                            Text(
                                text = "PROJECT · OFFLINE • 2.4 MB",
                                color = com.example.ui.theme.ElegantTextSecondary,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    // Undo/Redo & Quick settings & Export Call To Action
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        IconButton(
                            onClick = { viewModel.undo() },
                            enabled = canUndo,
                            modifier = Modifier.testTag("undo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Undo",
                                tint = if (canUndo) com.example.ui.theme.ElegantTextPrimary else com.example.ui.theme.ElegantTextMuted.copy(alpha = 0.40f)
                            )
                        }

                        IconButton(
                            onClick = { viewModel.redo() },
                            enabled = canRedo,
                            modifier = Modifier.testTag("redo_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Redo,
                                contentDescription = "Redo",
                                tint = if (canRedo) com.example.ui.theme.ElegantTextPrimary else com.example.ui.theme.ElegantTextMuted.copy(alpha = 0.40f)
                            )
                        }

                        IconButton(onClick = { viewModel.resetCanvasView() }) {
                            Icon(Icons.Default.FilterCenterFocus, contentDescription = "Recenter canvas viewport", tint = com.example.ui.theme.ElegantTextPrimary)
                        }

                        IconButton(onClick = { showGridSettings = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Grid guidelines options", tint = com.example.ui.theme.ElegantTextPrimary)
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        Button(
                            onClick = { showExportDialog = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = com.example.ui.theme.ElegantLavender,
                                contentColor = com.example.ui.theme.ElegantLavenderTextCombined
                            ),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = CircleShape,
                            modifier = Modifier.height(36.dp)
                        ) {
                            Text("Export", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // 3. Float layers anchor trigger
            if (!isTimelapsePlaying) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 84.dp, end = 12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        FloatingActionButton(
                            onClick = { showLayersPanel = !showLayersPanel },
                            containerColor = if (showLayersPanel) MaterialTheme.colorScheme.primary else Color(0xFF161622),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Default.Layers, contentDescription = "Toggle Layer manager")
                        }

                        FloatingActionButton(
                            onClick = { showBrushPresetDialog = true },
                            containerColor = Color(0xFF161622),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Default.Book, contentDescription = "Toggle Brush presets")
                        }

                        FloatingActionButton(
                            onClick = { viewModel.startTimelapseAnimation() },
                            containerColor = Color(0xFF161622),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Default.PlayCircle, contentDescription = "Play vector drawing timelapse animation", tint = Color.Yellow)
                        }

                        FloatingActionButton(
                            onClick = { showExportsPanel = !showExportsPanel },
                            containerColor = if (showExportsPanel) MaterialTheme.colorScheme.primary else Color(0xFF161622),
                            contentColor = Color.White,
                            shape = CircleShape,
                            modifier = Modifier.size(50.dp)
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = "Toggle saved exports directory", tint = Color.LightGray)
                        }
                    }
                }
            }

            // Slide out Overlay Layer Manager Card Panel
            AnimatedVisibility(
                visible = showLayersPanel && !isTimelapsePlaying,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 74.dp)
                    .width(240.dp)
                    .heightIn(max = 420.dp)
            ) {
                LayersCardList(
                    layers = layers,
                    activeLayerId = activeLayerId,
                    onAddLayer = { viewModel.addLayer() },
                    onDeleteLayer = { viewModel.deleteLayer(it) },
                    onSelectLayer = { viewModel.selectActiveLayer(it) },
                    onToggleVisibility = { viewModel.toggleLayerVisibility(it) },
                    onToggleLock = { viewModel.toggleLayerLock(it) },
                    onOpacityChange = { id, opacityValue -> viewModel.updateLayerOpacity(id, opacityValue) },
                    onMoveUp = { viewModel.moveLayerUp(it) },
                    onMoveDown = { viewModel.moveLayerDown(it) },
                    onMergeDown = { viewModel.mergeLayers() }
                )
            }

            // Slide out Overlay Saved Exports Manager Panel
            AnimatedVisibility(
                visible = showExportsPanel && !isTimelapsePlaying,
                enter = slideInHorizontally(initialOffsetX = { it }) + fadeIn(),
                exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 84.dp, end = 74.dp)
                    .width(260.dp)
                    .heightIn(max = 420.dp)
            ) {
                ExportsCardList(
                    exportedFiles = exportedFiles,
                    onDeleteFile = { viewModel.deleteExportFile(it) },
                    onShareFile = { file ->
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                                type = when (file.extension.lowercase()) {
                                    "svg" -> "text/xml"
                                    "png" -> "image/png"
                                    "jpg", "jpeg" -> "image/jpeg"
                                    else -> "*/*"
                                }
                                putExtra(android.content.Intent.EXTRA_SUBJECT, "NexSketch Design")
                                putExtra(android.content.Intent.EXTRA_TEXT, "Exported: ${file.name}\nsaved on secure internal storage.")
                            }
                            context.startActivity(android.content.Intent.createChooser(intent, "Share Artwork File Details"))
                        } catch (e: Exception) {
                            Toast.makeText(context, "Direct save export: ${file.name}", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
            }

            // 4. Timelapse Dynamic Playing Controls HUD
            AnimatedVisibility(
                visible = isTimelapsePlaying,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF111116))
                    .border(1.dp, Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                    .align(Alignment.BottomCenter)
                    .padding(20.dp)
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Visual Sketch Timelapse",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Chronological vector redraw simulation",
                                color = Color.Gray,
                                fontSize = 10.sp
                            )
                        }

                        Button(
                            onClick = { viewModel.stopTimelapseAnimation() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Exit Lab", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Slider(
                        value = timelapseProgress,
                        onValueChange = { viewModel.timelapseProgress.value = it },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Drawing Progress: ${(timelapseProgress * 100).toInt()}%",
                        color = Color.Yellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 5. Classic Pro Drawing Core tools Box footer (Elegant Dark Styled Panel)
            if (!isTimelapsePlaying) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(com.example.ui.theme.ElegantDarkSurface)
                        .border(1.dp, com.example.ui.theme.ElegantBorder, RoundedCornerShape(28.dp))
                        .padding(14.dp)
                ) {
                    // Color Quick Picks & Palette Customizer Trigger
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val quickColors = listOf(
                                Color(0xFFFFB4AB), // Peach/Red
                                Color(0xFFD0BCFF), // Lavender
                                Color(0xFFB2EEB1), // Mint
                                Color(0xFFFFD9E3), // Soft Pink
                                Color(0xFFFFFFFF)  // White
                            )
                            quickColors.forEach { qColor ->
                                val isSelected = selectedColorInt == qColor.toArgb()
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(qColor)
                                        .then(
                                            if (isSelected) {
                                                Modifier.border(2.dp, Color.White, CircleShape)
                                            } else {
                                                Modifier.border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                            }
                                        )
                                        .clickable {
                                            viewModel.selectedColorInt.value = qColor.toArgb()
                                        }
                                )
                            }
                        }

                        // Gradient palette custom color selector button - matches the html's rounded bg corner
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            Color(0xFFFFB4AB),
                                            Color(0xFFD0BCFF),
                                            Color(0xFFB2EEB1)
                                        )
                                    )
                                )
                                .clickable { showColorPicker = true }
                                .testTag("color_picker_trigger"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Palette,
                                contentDescription = "Pick custom color",
                                tint = Color.Black.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tool parameter values sliders
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Size and opacity sliders stack
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.FormatSize, contentDescription = null, tint = com.example.ui.theme.ElegantTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("SIZE: ${strokeWidth.toInt()}dp", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Slider(
                                    value = strokeWidth,
                                    onValueChange = { viewModel.strokeWidth.value = it },
                                    valueRange = 1f..100f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = com.example.ui.theme.ElegantLavender,
                                        activeTrackColor = com.example.ui.theme.ElegantLavender,
                                        inactiveTrackColor = com.example.ui.theme.ElegantBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Opacity, contentDescription = null, tint = com.example.ui.theme.ElegantTextSecondary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("OPACITY: ${(opacity * 100).toInt()}%", color = com.example.ui.theme.ElegantTextSecondary, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
                                Slider(
                                    value = opacity,
                                    onValueChange = { viewModel.opacity.value = it },
                                    valueRange = 0f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = com.example.ui.theme.ElegantLavender,
                                        activeTrackColor = com.example.ui.theme.ElegantLavender,
                                        inactiveTrackColor = com.example.ui.theme.ElegantBorder
                                    ),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tool Choice selection row (Embed in a custom stylized darker dock matching HTML's bg-[#1C1B1F])
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(com.example.ui.theme.ElegantDarkInnerSurface)
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Pencil
                        item {
                            ToolPillItem(
                                label = "Pencil",
                                icon = Icons.Default.Edit,
                                active = activeBrush == BrushType.PENCIL,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.PENCIL
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                        // Pen
                        item {
                            ToolPillItem(
                                label = "Pen",
                                icon = Icons.Default.Create,
                                active = activeBrush == BrushType.PEN && activeShape == ShapeType.FREEHAND,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.PEN
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                        // Watercolor Brush
                        item {
                            ToolPillItem(
                                label = "Brush",
                                icon = Icons.Default.Brush,
                                active = activeBrush == BrushType.BRUSH,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.BRUSH
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                        // Highlighter Marker
                        item {
                            ToolPillItem(
                                label = "Marker",
                                icon = Icons.Default.Highlight,
                                active = activeBrush == BrushType.MARKER,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.MARKER
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                        // Smooth Smudge charcoal blur
                        item {
                            ToolPillItem(
                                label = "Smudge",
                                icon = Icons.Default.Waves,
                                active = activeBrush == BrushType.SMUDGE,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.SMUDGE
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                        // Sticker trails
                        item {
                            Box {
                                ToolPillItem(
                                    label = "Sticker ($stickerSymbol)",
                                    icon = Icons.Default.Star,
                                    active = activeBrush == BrushType.STICKER,
                                    onClick = {
                                        viewModel.activeBrushType.value = BrushType.STICKER
                                        viewModel.activeShapeType.value = ShapeType.FREEHAND
                                        stickerMenuExpanded = !stickerMenuExpanded
                                    }
                                )
                                DropdownMenu(
                                    expanded = stickerMenuExpanded,
                                    onDismissRequest = { stickerMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E1E2C))
                                ) {
                                    val emojiStickers = listOf("⭐", "❤️", "✨", "🌸", "🎈", "🎨", "🍀", "👍", "🔥")
                                    Text("Pick Stamp Sticker", color = Color.Gray, fontSize = 10.sp, modifier = Modifier.padding(10.dp))
                                    HorizontalDivider()
                                    emojiStickers.forEach { sym ->
                                        DropdownMenuItem(
                                            text = { Text(sym, fontSize = 18.sp) },
                                            onClick = {
                                                viewModel.stickerSymbol.value = sym
                                                viewModel.activeBrushType.value = BrushType.STICKER
                                                stickerMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Geometry Shapes Outlines builder
                        item {
                            Box {
                                val isShapeActive = activeShape != ShapeType.FREEHAND
                                ToolPillItem(
                                    label = if (isShapeActive) activeShape.name else "Shapes",
                                    icon = Icons.Default.Category,
                                    active = isShapeActive,
                                    onClick = {
                                        shapeMenuExpanded = !shapeMenuExpanded
                                    }
                                )
                                DropdownMenu(
                                    expanded = shapeMenuExpanded,
                                    onDismissRequest = { shapeMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E1E2C))
                                ) {
                                    ShapeType.values().forEach { shape ->
                                        DropdownMenuItem(
                                            text = { Text(shape.name, color = Color.White, fontSize = 12.sp) },
                                            onClick = {
                                                viewModel.activeShapeType.value = shape
                                                shapeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        // Precision Eraser
                        item {
                            ToolPillItem(
                                label = "Eraser",
                                icon = Icons.Default.CleaningServices,
                                active = activeBrush == BrushType.ERASER,
                                onClick = {
                                    viewModel.activeBrushType.value = BrushType.ERASER
                                    viewModel.activeShapeType.value = ShapeType.FREEHAND
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "NEXSKETCH BY PRINCE AR",
                        color = com.example.ui.theme.ElegantTextMuted,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
            }

            // Dialogs integration

            // 1. Core Color Picker Hub
            if (showColorPicker) {
                ColorPickerDialog(
                    currentColor = selectedColorInt,
                    savedPalette = colorPalette,
                    onColorSelected = { viewModel.selectedColorInt.value = it },
                    onSaveToPalette = { viewModel.saveColorToPalette(it) },
                    onDismissRequest = { showColorPicker = false }
                )
            }

            // 2. Settings, Grids, Perspektives & Hand Smoothing Modal
            if (showGridSettings) {
                GridSettingsDialog(
                    snapToGrid = viewModel.snapToGrid,
                    perspectiveActive = viewModel.perspectiveGrid,
                    smoothing = viewModel.handwritingSmoothing,
                    canvasTemplate = viewModel.canvasTemplate,
                    onImportReference = {
                        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        showGridSettings = false
                    },
                    onClearActive = {
                        viewModel.clearActiveCanvas()
                        showGridSettings = false
                    },
                    onDismiss = { showGridSettings = false }
                )
            }

            // 3. Brush presets library dialog
            if (showBrushPresetDialog) {
                BrushPresetsDialog(
                    presets = presets,
                    onSelectPreset = {
                        viewModel.applyPreset(it)
                        showBrushPresetDialog = false
                    },
                    onCreatePresetClick = {
                        showBrushPresetDialog = false
                        showCustomPresetCreator = true
                    },
                    onDismiss = { showBrushPresetDialog = false }
                )
            }

            if (showCustomPresetCreator) {
                AlertDialog(
                    onDismissRequest = { showCustomPresetCreator = false },
                    containerColor = Color(0xFF1E1E2C),
                    title = { Text("Save Active Brush as Preset", color = Color.White) },
                    text = {
                        Column {
                            Text("Current size: ${strokeWidth.toInt()}dp, opacity: ${(opacity * 100).toInt()}%", color = Color.Gray, fontSize = 12.sp)
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = newPresetName,
                                onValueChange = { newPresetName = it },
                                label = { Text("Preset Name") },
                                colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                if (newPresetName.isNotBlank()) {
                                    viewModel.saveBrushPreset(newPresetName)
                                    newPresetName = ""
                                    showCustomPresetCreator = false
                                }
                            }
                        ) {
                            Text("Add Preset")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomPresetCreator = false }) {
                            Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                        }
                    }
                )
            }

            // 4. Secure Export Dialog (Saving PNG preview blocks offline)
            if (showExportDialog) {
                SecureExportDialog(
                    onDismiss = { showExportDialog = false },
                    onConfirmExport = { targetFormat, transparent ->
                        showExportDialog = false
                        viewModel.exportActiveProject(targetFormat, transparent) { file ->
                            if (file != null) {
                                Toast.makeText(
                                    context,
                                    "Successfully saved as $targetFormat! Directory: NexSketchExports/${file.name}",
                                    Toast.LENGTH_LONG
                                ).show()
                            } else {
                                Toast.makeText(context, "Export to internal storage failed.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ExportsCardList(
    exportedFiles: List<java.io.File>,
    onDeleteFile: (java.io.File) -> Unit,
    onShareFile: (java.io.File) -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161622).copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.padding(bottom = 8.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "My Saved Exports",
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 2.dp)
            )

            Text(
                text = "Safe on sandbox internal storage",
                color = Color.Gray,
                fontSize = 10.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            if (exportedFiles.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No exports compiled yet.\nTap 'Export' in the top-right header to render HD files.",
                        color = Color.LightGray.copy(alpha = 0.5f),
                        fontSize = 10.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 280.dp)
                ) {
                    items(exportedFiles) { file ->
                        val ext = file.extension.uppercase()
                        val sizeKb = file.length() / 1024f
                        val formattedSize = if (sizeKb > 1024) String.format("%.1f MB", sizeKb / 1024f) else String.format("%.1f KB", sizeKb)

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White.copy(alpha = 0.04f))
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    // Strip leading custom prefix if saved
                                    text = file.name.replace("SKETCH_", "").substringBeforeLast("."),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(
                                                when (ext) {
                                                    "SVG" -> Color(0xFFE91E63)
                                                    "PNG" -> Color(0xFF2196F3)
                                                    "JPG", "JPEG" -> Color(0xFF4CAF50)
                                                    else -> Color.DarkGray
                                                }
                                            )
                                            .padding(horizontal = 4.dp, vertical = 1.dp)
                                    ) {
                                        Text(
                                            text = ext,
                                            color = Color.White,
                                            fontSize = 7.5.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Text(
                                        text = formattedSize,
                                        color = Color.Gray,
                                        fontSize = 9.sp
                                    )
                                }
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                IconButton(
                                    onClick = { onShareFile(file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Share,
                                        contentDescription = "Share export details",
                                        tint = Color.LightGray.copy(alpha = 0.8f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { onDeleteFile(file) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete export",
                                        tint = Color.Red.copy(alpha = 0.65f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ToolPillItem(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    active: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(if (active) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (active) MaterialTheme.colorScheme.onPrimary else Color.LightGray.copy(alpha = 0.8f),
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                color = if (active) MaterialTheme.colorScheme.onPrimary else Color.LightGray.copy(alpha = 0.8f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun LayersCardList(
    layers: List<CanvasLayer>,
    activeLayerId: String?,
    onAddLayer: () -> Unit,
    onDeleteLayer: (String) -> Unit,
    onSelectLayer: (String) -> Unit,
    onToggleVisibility: (String) -> Unit,
    onToggleLock: (String) -> Unit,
    onOpacityChange: (String, Float) -> Unit,
    onMoveUp: (String) -> Unit,
    onMoveDown: (String) -> Unit,
    onMergeDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
        modifier = Modifier
            .fillMaxSize()
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(18.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Layers manager", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                
                Row {
                    IconButton(onClick = onMergeDown, enabled = layers.size >= 2) {
                        Icon(Icons.Default.MergeType, contentDescription = "Merge layer down", tint = Color.LightGray, modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onAddLayer) {
                        Icon(Icons.Default.Add, contentDescription = "Add Layer", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                items(layers.asReversed()) { layer ->
                    val isActive = layer.id == activeLayerId
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else Color(0xFF12121D)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = if (isActive) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.05f),
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectLayer(layer.id) }
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Icon(
                                        imageVector = if (isActive) Icons.Default.PlayArrow else Icons.Default.HorizontalRule,
                                        contentDescription = null,
                                        tint = if (isActive) MaterialTheme.colorScheme.primary else Color.Gray,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = layer.name,
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { onToggleVisibility(layer.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = "Toggle Visibility",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onToggleLock(layer.id) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (layer.isLocked) Icons.Default.Lock else Icons.Default.LockOpen,
                                            contentDescription = "Toggle Lock",
                                            tint = if (layer.isLocked) Color.Red else Color.LightGray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { onDeleteLayer(layer.id) },
                                        enabled = layers.size > 1,
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete layer",
                                            tint = if (layers.size > 1) Color.Red else Color.Gray,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }

                            // Layer blend slider & reordering row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                    Text("Blend:", color = Color.Gray, fontSize = 7.sp)
                                    Slider(
                                        value = layer.alpha,
                                        onValueChange = { onOpacityChange(layer.id, it) },
                                        valueRange = 0f..1f,
                                        modifier = Modifier
                                            .height(18.dp)
                                            .padding(horizontal = 4.dp)
                                    )
                                }

                                Row {
                                    IconButton(onClick = { onMoveUp(layer.id) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move Up", tint = Color.LightGray)
                                    }
                                    IconButton(onClick = { onMoveDown(layer.id) }, modifier = Modifier.size(20.dp)) {
                                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move Down", tint = Color.LightGray)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GridSettingsDialog(
    snapToGrid: MutableStateFlow<Boolean>,
    perspectiveActive: MutableStateFlow<Boolean>,
    smoothing: MutableStateFlow<Boolean>,
    canvasTemplate: MutableStateFlow<CanvasTemplate>,
    onImportReference: () -> Unit,
    onClearActive: () -> Unit,
    onDismiss: () -> Unit
) {
    val snap by snapToGrid.collectAsState()
    val perspective by perspectiveActive.collectAsState()
    val smooth by smoothing.collectAsState()
    val pattern by canvasTemplate.collectAsState()

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Studio Workbench Settings", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Template picker inside drawing
                Text("Active Pattern Blueprint", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CanvasTemplate.values().forEach { t ->
                        val active = pattern == t
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF12121D))
                                .border(1.dp, if (active) Color.White else Color.Transparent, RoundedCornerShape(6.dp))
                                .clickable { canvasTemplate.value = t }
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(t.name, color = if (active) Color.White else Color.LightGray, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Switches
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Snap-to-Grid (40px points)", color = Color.White, fontSize = 12.sp)
                    Switch(checked = snap, onCheckedChange = { snapToGrid.value = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Double Point Perspective Horizon Guides", color = Color.White, fontSize = 12.sp)
                    Switch(checked = perspective, onCheckedChange = { perspectiveActive.value = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Handwriting Smoothening Engine", color = Color.White, fontSize = 12.sp)
                    Switch(checked = smooth, onCheckedChange = { smoothing.value = it })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Additional quick utilities (Reference import + Clear canvas)
                Text("Creative Utilities", color = Color.Gray, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onImportReference,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1335)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import Photo Overlay from Gallery", fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = onClearActive,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF421E1E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = null, tint = Color.Red)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Purge active layer strokes", fontSize = 12.sp, color = Color.Red)
                }

                Spacer(modifier = Modifier.height(20.dp))

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.BottomEnd) {
                    TextButton(onClick = onDismiss) {
                        Text("Save Workbench", color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    }
}

@Composable
fun BrushPresetsDialog(
    presets: List<BrushPreset>,
    onSelectPreset: (BrushPreset) -> Unit,
    onCreatePresetClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Visual Brush Library", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onCreatePresetClick) {
                        Icon(Icons.Default.AddCircle, contentDescription = "Create Custom Preset", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.heightIn(max = 280.dp)) {
                    items(presets) { preset ->
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF12121D)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectPreset(preset) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = when (preset.brushType) {
                                            BrushType.PENCIL -> Icons.Default.Edit
                                            BrushType.PEN -> Icons.Default.Create
                                            BrushType.BRUSH -> Icons.Default.Brush
                                            BrushType.MARKER -> Icons.Default.Highlight
                                            BrushType.STICKER -> Icons.Default.Star
                                            else -> Icons.Default.Waves
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(preset.name, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        Text("Type: ${preset.brushType.name.lowercase()} · ${preset.size.toInt()}dp · ${(preset.opacity * 100).toInt()}% op", color = Color.Gray, fontSize = 9.sp)
                                    }
                                }

                                if (preset.stickerSymbol != null) {
                                    Text(preset.stickerSymbol, fontSize = 16.sp, modifier = Modifier.padding(end = 4.dp))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Close", color = Color.White.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun SecureExportDialog(
    onDismiss: () -> Unit,
    onConfirmExport: (String, Boolean) -> Unit
) {
    val formats = listOf("PNG image", "JPG image", "SVG vector")
    var selectedFormatIndex by remember { mutableStateOf(0) }
    var transparentBg by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2C)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("Studio High-Res Export Compiler", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                Text("Target Storage Format", color = Color.Gray, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    formats.forEachIndexed { idx, f ->
                        val active = idx == selectedFormatIndex
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (active) MaterialTheme.colorScheme.primary else Color(0xFF12121D))
                                .clickable {
                                    selectedFormatIndex = idx
                                }
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(f.split(" ").first(), color = if (active) Color.White else Color.Gray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                if (selectedFormatIndex == 0 || selectedFormatIndex == 2) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Alfa Translucency Overlay", color = Color.White, fontSize = 13.sp)
                            Text("Remove paper color for alpha channels", color = Color.Gray, fontSize = 10.sp)
                        }
                        Switch(checked = transparentBg, onCheckedChange = { transparentBg = it })
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Text(
                    text = "HD/4K (300 DPI vector scale rasterization rendering scheme guarantees maximum production print sharpness offline.)",
                    color = Color.Yellow.copy(alpha = 0.8f),
                    lineHeight = 15.sp,
                    fontSize = 10.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.6f))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Button(
                        onClick = {
                            onConfirmExport(
                                formats[selectedFormatIndex].split(" ").first(),
                                transparentBg
                            )
                        }
                    ) {
                        Text("Render Off-line")
                    }
                }
            }
        }
    }
}
