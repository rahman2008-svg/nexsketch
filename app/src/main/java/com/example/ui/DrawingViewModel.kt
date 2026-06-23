package com.example.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID

class DrawingViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val repository = ProjectRepository(database.projectDao())

    // All projects from DB
    val allProjects: StateFlow<List<Project>> = repository.allProjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current Active Project
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    // Workspace Canvas Layers
    private val _layers = MutableStateFlow<List<CanvasLayer>>(emptyList())
    val layers: StateFlow<List<CanvasLayer>> = _layers.asStateFlow()

    // Active Layer ID
    private val _activeLayerId = MutableStateFlow<String?>(null)
    val activeLayerId: StateFlow<String?> = _activeLayerId.asStateFlow()

    // Real-time Current Stroke Drafting
    private val _currentStroke = MutableStateFlow<DrawStroke?>(null)
    val currentStroke: StateFlow<DrawStroke?> = _currentStroke.asStateFlow()

    // Undo / Redo Stacks
    private val undoStack = mutableListOf<List<CanvasLayer>>()
    private val redoStack = mutableListOf<List<CanvasLayer>>()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    fun updateHistoryStates() {
        _canUndo.value = undoStack.isNotEmpty()
        _canRedo.value = redoStack.isNotEmpty()
    }

    private fun clearRedo() {
        redoStack.clear()
        updateHistoryStates()
    }

    // Direct internal storage exported images list
    private val _exportedFiles = MutableStateFlow<List<File>>(emptyList())
    val exportedFiles: StateFlow<List<File>> = _exportedFiles.asStateFlow()

    fun loadExportedFiles() {
        _exportedFiles.value = CanvasExporter.listInternalExports(getApplication())
    }

    fun exportActiveProject(format: String, transparent: Boolean, onComplete: (File?) -> Unit) {
        val proj = _currentProject.value ?: return
        val currentLayers = _layers.value
        val bgCol = canvasBgColorInt.value
        val w = proj.canvasWidth.toInt().coerceAtLeast(300)
        val h = proj.canvasHeight.toInt().coerceAtLeast(300)
        viewModelScope.launch {
            val file = CanvasExporter.exportToInternalStorage(
                getApplication(),
                proj.name,
                format,
                transparent,
                w,
                h,
                bgCol,
                currentLayers
            )
            if (file != null) {
                loadExportedFiles()
            }
            onComplete(file)
        }
    }

    fun deleteExportFile(file: File) {
        viewModelScope.launch {
            if (file.exists()) {
                file.delete()
                loadExportedFiles()
            }
        }
    }

    // Active Drawing Tool State
    val activeBrushType = MutableStateFlow(BrushType.PEN)
    val activeShapeType = MutableStateFlow(ShapeType.FREEHAND)
    val strokeWidth = MutableStateFlow(8f)
    val opacity = MutableStateFlow(1f)
    val selectedColorInt = MutableStateFlow(-16777216) // Black default
    val stickerSymbol = MutableStateFlow("⭐") // Star sticker default

    // Canvas Settings
    val canvasTemplate = MutableStateFlow(CanvasTemplate.BLANK)
    val canvasBgColorInt = MutableStateFlow(-1) // White default
    val snapToGrid = MutableStateFlow(false)
    val perspectiveGrid = MutableStateFlow(false)
    val handwritingSmoothing = MutableStateFlow(true)

    // Zoom, Pan & Rotate
    val canvasZoom = MutableStateFlow(1f)
    val canvasPan = MutableStateFlow(Offset.Zero)
    val canvasRotation = MutableStateFlow(0f)

    // Custom Brush Presets
    private val _brushPresets = MutableStateFlow<List<BrushPreset>>(
        listOf(
            BrushPreset("1", "Fine Pencil", BrushType.PENCIL, 3f, 0.5f),
            BrushPreset("2", "Heavy Pen", BrushType.PEN, 12f, 1f),
            BrushPreset("3", "Soft Watercolor", BrushType.BRUSH, 26f, 0.6f),
            BrushPreset("4", "Neon Marker", BrushType.MARKER, 18f, 0.4f),
            BrushPreset("5", "Star Trail", BrushType.STICKER, 20f, 1f, "⭐"),
            BrushPreset("6", "Heart Glow", BrushType.STICKER, 24f, 1f, "❤️"),
            BrushPreset("7", "Sparkles", BrushType.STICKER, 16f, 1f, "✨")
        )
    )
    val brushPresets: StateFlow<List<BrushPreset>> = _brushPresets.asStateFlow()

    // Color Palette Saving List
    private val _colorPalette = MutableStateFlow<List<Int>>(
        listOf(
            -16777216, // Black
            -1,        // White
            -65536,    // Red
            -16711936, // Green
            -16776961, // Blue
            -256,      // Yellow
            -16711681, // Cyan
            -65281,    // Magenta
            -8355712,  // Gray
            -23296,    // Orange
            -11184811, // Slate Dark
            -1577040   // Cream Beige
        )
    )
    val colorPalette: StateFlow<List<Int>> = _colorPalette.asStateFlow()

    // Timelapse Playback States
    val isTimelapsePlaying = MutableStateFlow(false)
    val timelapseProgress = MutableStateFlow(1f) // range 0..1f
    private var timelapseJob: Job? = null

    // Navigation UI Mode
    // true = Project Gallery list, false = Drawing Canvas Studio
    val isGalleryMode = MutableStateFlow(true)

    // Project Name for Dialogs
    val projectDialogName = MutableStateFlow("")

    private var autoSaveJob: Job? = null

    init {
        // Pre-populate some projects if empty
        viewModelScope.launch {
            allProjects.collectLatest { list ->
                if (list.isEmpty()) {
                    createProject("My First Masterpiece", CanvasTemplate.BLANK, -1, 1080f, 1920f)
                }
            }
        }
        loadExportedFiles()
    }

    // --- Core Project Actions ---

    fun createProject(
        name: String,
        template: CanvasTemplate,
        bgColor: Int,
        width: Float,
        height: Float
    ) {
        viewModelScope.launch {
            val projectTitle = name.ifBlank { "Untitled Project" }
            val initialLayer = CanvasLayer(
                id = UUID.randomUUID().toString(),
                name = "Background Sketch"
            )
            val initialLayerList = listOf(initialLayer)
            val converters = Converters()
            val layersJson = converters.toLayersJson(initialLayerList)

            val newProj = Project(
                name = projectTitle,
                canvasTemplate = template.name,
                canvasBgColor = bgColor,
                layersJson = layersJson,
                canvasWidth = width,
                canvasHeight = height
            )
            val insertedId = repository.insertProject(newProj)
            val updatedProj = newProj.copy(id = insertedId.toInt())
            loadProject(updatedProj)
            isGalleryMode.value = false
        }
    }

    fun loadProject(project: Project) {
        viewModelScope.launch {
            _currentProject.value = project
            canvasTemplate.value = CanvasTemplate.valueOf(project.canvasTemplate)
            canvasBgColorInt.value = project.canvasBgColor
            val converters = Converters()
            val loadedLayers = converters.fromLayersJson(project.layersJson)
            _layers.value = loadedLayers
            _activeLayerId.value = loadedLayers.firstOrNull()?.id

            // Clear history on project load
            undoStack.clear()
            redoStack.clear()
            updateHistoryStates()

            // Reset view transforms
            resetCanvasView()
        }
    }

    fun duplicateProject(project: Project) {
        viewModelScope.launch {
            val duplicated = Project(
                name = "${project.name} (Copy)",
                canvasTemplate = project.canvasTemplate,
                canvasBgColor = project.canvasBgColor,
                layersJson = project.layersJson,
                canvasWidth = project.canvasWidth,
                canvasHeight = project.canvasHeight,
                thumbnailPath = project.thumbnailPath
            )
            repository.insertProject(duplicated)
        }
    }

    fun renameProject(project: Project, newName: String) {
        viewModelScope.launch {
            val updated = project.copy(
                name = newName.ifBlank { project.name },
                updatedAt = System.currentTimeMillis()
            )
            repository.updateProject(updated)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = updated
            }
        }
    }

    fun deleteProject(project: Project) {
        viewModelScope.launch {
            repository.deleteProject(project)
            if (_currentProject.value?.id == project.id) {
                _currentProject.value = null
                _layers.value = emptyList()
                _activeLayerId.value = null
            }
        }
    }

    fun deleteActiveProject() {
        _currentProject.value?.let { deleteProject(it) }
        isGalleryMode.value = true
    }

    fun saveCurrentProjectState() {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val converters = Converters()
            val layersJson = converters.toLayersJson(_layers.value)
            val updated = proj.copy(
                layersJson = layersJson,
                canvasTemplate = canvasTemplate.value.name,
                canvasBgColor = canvasBgColorInt.value,
                updatedAt = System.currentTimeMillis()
            )
            _currentProject.value = updated
            repository.updateProject(updated)
        }
    }

    private fun triggerAutoSave() {
        autoSaveJob?.cancel()
        autoSaveJob = viewModelScope.launch {
            delay(1000) // Debounce auto-save by 1 second after last stroke
            saveCurrentProjectState()
        }
    }

    // --- Active Drawing Operations ---

    fun startNewStroke(startOffset: Offset) {
        if (isTimelapsePlaying.value) return
        val activeId = _activeLayerId.value ?: return
        val layer = _layers.value.find { it.id == activeId }
        if (layer == null || layer.isLocked || !layer.isVisible) return

        val point = processTouchPoint(startOffset)
        _currentStroke.value = DrawStroke(
            strokeWidth = strokeWidth.value,
            color = if (activeBrushType.value == BrushType.ERASER) canvasBgColorInt.value else selectedColorInt.value,
            opacity = opacity.value,
            brushType = activeBrushType.value,
            shapeType = activeShapeType.value,
            startX = point.x,
            startY = point.y,
            endX = point.x,
            endY = point.y,
            points = listOf(point),
            stickerSymbol = stickerSymbol.value
        )
    }

    fun updateCurrentStroke(offset: Offset) {
        if (isTimelapsePlaying.value) return
        val stroke = _currentStroke.value ?: return
        val point = processTouchPoint(offset)

        _currentStroke.value = if (stroke.shapeType == ShapeType.FREEHAND) {
            // Apply smoothing in real-time if enabled
            val smoothedPoints = if (handwritingSmoothing.value && stroke.points.isNotEmpty()) {
                val lastPoint = stroke.points.last()
                val alpha = 0.3f // Smoothing factor
                val smoothedX = lastPoint.x * (1f - alpha) + point.x * alpha
                val smoothedY = lastPoint.y * (1f - alpha) + point.y * alpha
                stroke.points + PointD(smoothedX, smoothedY)
            } else {
                stroke.points + point
            }
            stroke.copy(points = smoothedPoints, endX = point.x, endY = point.y)
        } else {
            stroke.copy(endX = point.x, endY = point.y)
        }
    }

    fun finishCurrentStroke() {
        if (isTimelapsePlaying.value) return
        val activeId = _activeLayerId.value ?: return
        val stroke = _currentStroke.value ?: return
        _currentStroke.value = null

        // Format direct shape lines or freehand
        val finalStroke = if (stroke.shapeType != ShapeType.FREEHAND) {
            stroke
        } else {
            // Ensure single-tap points are recorded nicely
            if (stroke.points.isEmpty()) {
                stroke.copy(points = listOf(PointD(stroke.startX ?: 0f, stroke.startY ?: 0f)))
            } else {
                stroke
            }
        }

        saveStateToUndo()
        clearRedo()

        _layers.value = _layers.value.map { layer ->
            if (layer.id == activeId) {
                layer.copy(strokes = layer.strokes + finalStroke)
            } else {
                layer
            }
        }
        triggerAutoSave()
    }

    // --- Layers Management ---

    fun addLayer() {
        saveStateToUndo()
        clearRedo()
        val num = _layers.value.size + 1
        val newLayer = CanvasLayer(name = "Layer $num")
        _layers.value = _layers.value + newLayer
        _activeLayerId.value = newLayer.id
        triggerAutoSave()
    }

    fun deleteLayer(layerId: String) {
        val current = _layers.value
        if (current.size <= 1) return // Keep at least one layer
        saveStateToUndo()
        clearRedo()

        _layers.value = current.filterNot { it.id == layerId }
        if (_activeLayerId.value == layerId) {
            _activeLayerId.value = _layers.value.lastOrNull()?.id
        }
        triggerAutoSave()
    }

    fun toggleLayerVisibility(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isVisible = !it.isVisible) else it
        }
        triggerAutoSave()
    }

    fun toggleLayerLock(layerId: String) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(isLocked = !it.isLocked) else it
        }
        triggerAutoSave()
    }

    fun updateLayerOpacity(layerId: String, alpha: Float) {
        _layers.value = _layers.value.map {
            if (it.id == layerId) it.copy(alpha = alpha.coerceIn(0f, 1f)) else it
        }
        triggerAutoSave()
    }

    fun moveLayerUp(layerId: String) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index > 0) {
            saveStateToUndo()
            clearRedo()
            val temp = list[index]
            list[index] = list[index - 1]
            list[index - 1] = temp
            _layers.value = list
            triggerAutoSave()
        }
    }

    fun moveLayerDown(layerId: String) {
        val list = _layers.value.toMutableList()
        val index = list.indexOfFirst { it.id == layerId }
        if (index >= 0 && index < list.size - 1) {
            saveStateToUndo()
            clearRedo()
            val temp = list[index]
            list[index] = list[index + 1]
            list[index + 1] = temp
            _layers.value = list
            triggerAutoSave()
        }
    }

    fun mergeLayers() {
        val list = _layers.value
        if (list.size < 2) return
        val activeIndex = list.indexOfFirst { it.id == _activeLayerId.value }
        if (activeIndex < 0) return

        // Merge target is the layer below active index (active index - 1)
        val targetIndex = if (activeIndex > 0) activeIndex - 1 else 1

        saveStateToUndo()
        clearRedo()

        val activeLayer = list[activeIndex]
        val targetLayer = list[targetIndex]

        val mergedStrokes = targetLayer.strokes + activeLayer.strokes
        val mergedLayer = targetLayer.copy(strokes = mergedStrokes, name = "${targetLayer.name} (Merged)")

        val newList = list.toMutableList()
        newList[targetIndex] = mergedLayer
        newList.removeAt(activeIndex)

        _layers.value = newList
        _activeLayerId.value = mergedLayer.id
        triggerAutoSave()
    }

    fun selectActiveLayer(layerId: String) {
        _activeLayerId.value = layerId
    }

    // --- Undo / Redo Execution ---

    private fun saveStateToUndo() {
        if (undoStack.size >= 30) {
            undoStack.removeAt(0) // Cap undo size to 30 snapshots to optimize memory
        }
        undoStack.add(_layers.value.toList()) // Perform deep copy of the layer structures
        updateHistoryStates()
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(_layers.value)
            _layers.value = previous
            if (_layers.value.find { it.id == _activeLayerId.value } == null) {
                _activeLayerId.value = _layers.value.firstOrNull()?.id
            }
            updateHistoryStates()
            triggerAutoSave()
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val nextState = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(_layers.value)
            _layers.value = nextState
            if (_layers.value.find { it.id == _activeLayerId.value } == null) {
                _activeLayerId.value = _layers.value.firstOrNull()?.id
            }
            updateHistoryStates()
            triggerAutoSave()
        }
    }

    fun clearActiveCanvas() {
        val activeId = _activeLayerId.value ?: return
        saveStateToUndo()
        clearRedo()
        _layers.value = _layers.value.map {
            if (it.id == activeId) it.copy(strokes = emptyList()) else it
        }
        triggerAutoSave()
    }

    // --- Preset & Custom Palette Management ---

    fun applyPreset(preset: BrushPreset) {
        activeBrushType.value = preset.brushType
        strokeWidth.value = preset.size
        opacity.value = preset.opacity
        preset.stickerSymbol?.let { stickerSymbol.value = it }
    }

    fun saveBrushPreset(name: String) {
        val p = BrushPreset(
            name = name,
            brushType = activeBrushType.value,
            size = strokeWidth.value,
            opacity = opacity.value,
            stickerSymbol = if (activeBrushType.value == BrushType.STICKER) stickerSymbol.value else null
        )
        _brushPresets.value = _brushPresets.value + p
    }

    fun saveColorToPalette(colorValue: Int) {
        if (colorValue !in _colorPalette.value) {
            _colorPalette.value = _colorPalette.value + colorValue
        }
    }

    // --- View Transform Actions ---

    fun resetCanvasView() {
        canvasZoom.value = 1f
        canvasPan.value = Offset.Zero
        canvasRotation.value = 0f
    }

    // --- Snap to Grid snap calculations ---

    private fun processTouchPoint(offset: Offset): PointD {
        if (!snapToGrid.value) return PointD(offset.x, offset.y)
        val gridStep = 40f
        val snappedX = Math.round(offset.x / gridStep) * gridStep
        val snappedY = Math.round(offset.y / gridStep) * gridStep
        return PointD(snappedX, snappedY)
    }

    // --- Timelapse Engine ---

    fun startTimelapseAnimation() {
        timelapseJob?.cancel()
        isTimelapsePlaying.value = true
        timelapseProgress.value = 0f

        val allStrokesCount = _layers.value.sumOf { it.strokes.size }
        if (allStrokesCount == 0) {
            isTimelapsePlaying.value = false
            timelapseProgress.value = 1f
            return
        }

        timelapseJob = viewModelScope.launch {
            // Animate stroke count reveal
            var added = 0
            val delayDuration = (5000f / allStrokesCount).coerceIn(15f, 250f).toLong() // target 5sec max

            for (i in 1..allStrokesCount) {
                if (!isTimelapsePlaying.value) break
                timelapseProgress.value = i.toFloat() / allStrokesCount
                delay(delayDuration)
            }
            isTimelapsePlaying.value = false
            timelapseProgress.value = 1f
        }
    }

    fun stopTimelapseAnimation() {
        timelapseJob?.cancel()
        isTimelapsePlaying.value = false
        timelapseProgress.value = 1f
    }

    fun getTimelapseFilteredLayers(): List<CanvasLayer> {
        val current = _layers.value
        val progress = timelapseProgress.value
        if (progress >= 1f) return current

        val allStrokes = current.flatMap { l -> l.strokes.map { s -> Pair(l.id, s) } }
            .sortedBy { it.second.timestamp }
        val strokeThreshold = (allStrokes.size * progress).toInt()
        val visibleStrokeIds = allStrokes.take(strokeThreshold).map { it.second.id }.toSet()

        return current.map { l ->
            l.copy(strokes = l.strokes.filter { s -> s.id in visibleStrokeIds })
        }
    }
}
