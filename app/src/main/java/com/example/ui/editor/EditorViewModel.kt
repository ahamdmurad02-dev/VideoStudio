package com.example.ui.editor

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.VideoEntity
import com.example.data.repository.VideoRepository
import com.example.media.ThumbnailHelper
import com.example.media.VideoExportManager
import com.example.python.CapCutBackgroundConfig
import com.example.python.CapCutColorConfig
import com.example.python.EditPlan
import com.example.python.PythonVideoProcessor
import com.example.python.VideoMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Stack

data class VideoEditState(
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val speed: Float = 1.0f,
    val rotation: Int = 0,
    val cropAspectRatio: String = "ORIGINAL",
    val isAudioMuted: Boolean = false,
    val volumePercent: Int = 100,
    val overlayText: String = "",
    val overlayPosition: String = "BOTTOM",
    val exportQuality: String = "MEDIUM",
    val backgroundConfig: CapCutBackgroundConfig = CapCutBackgroundConfig(),
    val colorConfig: CapCutColorConfig = CapCutColorConfig()
)

data class EditorUiState(
    val currentVideo: VideoEntity? = null,
    val metadata: VideoMetadata = VideoMetadata(),
    val editState: VideoEditState = VideoEditState(),
    val compiledPlan: EditPlan? = null,
    val canUndo: Boolean = false,
    val canRedo: Boolean = false,
    val isExporting: Boolean = false,
    val exportProgress: Int = 0,
    val exportedVideo: VideoEntity? = null,
    val message: String? = null,
    val activeTab: EditorTab = EditorTab.TRIM,
    val playheadMs: Long = 0L
)

enum class EditorTab {
    TRIM, BACKGROUND, FILTERS, SPEED, CANVAS, TEXT, AUDIO
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VideoRepository
    private val exportManager: VideoExportManager
    private val _uiState = MutableStateFlow(EditorUiState())
    val uiState: StateFlow<EditorUiState> = _uiState.asStateFlow()
    private val undoStack = Stack<VideoEditState>()
    private val redoStack = Stack<VideoEditState>()

    init {
        val db = AppDatabase.getInstance(application)
        repository = VideoRepository(db.videoDao())
        exportManager = VideoExportManager(application)
    }

    fun loadVideo(video: VideoEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val metadata = PythonVideoProcessor.extractMetadata(getApplication(), video.filePath)
            val initialEdit = VideoEditState(
                trimStartMs = 0L,
                trimEndMs = if (metadata.durationMs > 0) metadata.durationMs else video.durationMs,
                speed = 1.0f,
                rotation = 0,
                cropAspectRatio = "ORIGINAL",
                isAudioMuted = !video.hasAudio,
                volumePercent = if (video.hasAudio) 100 else 0,
                overlayText = "",
                overlayPosition = "BOTTOM",
                exportQuality = "MEDIUM",
                backgroundConfig = CapCutBackgroundConfig(),
                colorConfig = CapCutColorConfig()
            )
            undoStack.clear()
            redoStack.clear()
            val plan = compilePlan(metadata, initialEdit)
            _uiState.value = EditorUiState(
                currentVideo = video,
                metadata = metadata,
                editState = initialEdit,
                compiledPlan = plan,
                canUndo = false,
                canRedo = false,
                playheadMs = 0L
            )
        }
    }

    private fun compilePlan(metadata: VideoMetadata, editState: VideoEditState): EditPlan {
        return PythonVideoProcessor.computeEditPlan(
            durationMs = metadata.durationMs,
            trimStartMs = editState.trimStartMs,
            trimEndMs = editState.trimEndMs,
            speed = editState.speed,
            rotation = editState.rotation,
            cropRatio = editState.cropAspectRatio,
            muteAudio = editState.isAudioMuted,
            textOverlay = editState.overlayText,
            overlayPosition = editState.overlayPosition,
            exportQuality = editState.exportQuality,
            sourceWidth = if (metadata.width > 0) metadata.width else 1920,
            sourceHeight = if (metadata.height > 0) metadata.height else 1080,
            backgroundConfig = editState.backgroundConfig,
            colorConfig = editState.colorConfig,
            volumePercent = editState.volumePercent
        )
    }

    private fun pushState(newState: VideoEditState) {
        val current = _uiState.value.editState
        if (current != newState) {
            undoStack.push(current)
            redoStack.clear()
            val plan = compilePlan(_uiState.value.metadata, newState)
            _uiState.value = _uiState.value.copy(
                editState = newState,
                compiledPlan = plan,
                canUndo = undoStack.isNotEmpty(),
                canRedo = false
            )
        }
    }

    fun setPlayhead(posMs: Long) { _uiState.value = _uiState.value.copy(playheadMs = posMs) }
    fun splitClip(posMs: Long, keepSide: String) {
        val currentTrimStart = _uiState.value.editState.trimStartMs
        val currentTrimEnd = _uiState.value.editState.trimEndMs
        val splitPoint = posMs.coerceIn(currentTrimStart + 500L, (currentTrimEnd - 500L).coerceAtLeast(currentTrimStart + 500L))
        when (keepSide) {
            "LEFT" -> setTrim(currentTrimStart, splitPoint)
            "RIGHT" -> setTrim(splitPoint, currentTrimEnd)
        }
    }
    fun setTrim(startMs: Long, endMs: Long) {
        val safeStart = startMs.coerceAtLeast(0L)
        val safeEnd = endMs.coerceAtMost(_uiState.value.metadata.durationMs.coerceAtLeast(100L))
        if (safeEnd > safeStart) pushState(_uiState.value.editState.copy(trimStartMs = safeStart, trimEndMs = safeEnd))
    }
    fun setBackgroundMode(mode: String, colorHex: String = "#000000", chromaColor: String = "GREEN", threshold: Float = 0.4f) {
        val updated = _uiState.value.editState.backgroundConfig.copy(mode = mode, colorHex = colorHex, chromaKeyColor = chromaColor, chromaThreshold = threshold)
        pushState(_uiState.value.editState.copy(backgroundConfig = updated))
    }
    fun setFilterPreset(preset: String) {
        pushState(_uiState.value.editState.copy(colorConfig = _uiState.value.editState.colorConfig.copy(filterPreset = preset)))
    }
    fun setColorAdjustments(brightness: Float, contrast: Float, saturation: Float, vignette: Float) {
        pushState(_uiState.value.editState.copy(colorConfig = _uiState.value.editState.colorConfig.copy(brightness = brightness, contrast = contrast, saturation = saturation, vignette = vignette)))
    }
    fun setVolumePercent(percent: Int) {
        val safe = percent.coerceIn(0, 200)
        pushState(_uiState.value.editState.copy(volumePercent = safe, isAudioMuted = (safe == 0)))
    }
    fun setCropAspectRatio(ratioName: String) { pushState(_uiState.value.editState.copy(cropAspectRatio = ratioName)) }
    fun rotate90() { pushState(_uiState.value.editState.copy(rotation = (_uiState.value.editState.rotation + 90) % 360)) }
    fun setSpeed(speed: Float) { pushState(_uiState.value.editState.copy(speed = speed)) }
    fun setAudioMuted(muted: Boolean) {
        val currentVol = _uiState.value.editState.volumePercent
        val nextVol = if (muted) 0 else (if (currentVol > 0) currentVol else 100)
        pushState(_uiState.value.editState.copy(isAudioMuted = muted, volumePercent = nextVol))
    }
    fun setOverlayText(text: String) { pushState(_uiState.value.editState.copy(overlayText = text)) }
    fun setOverlayPosition(pos: String) { pushState(_uiState.value.editState.copy(overlayPosition = pos)) }
    fun setExportQuality(quality: String) { pushState(_uiState.value.editState.copy(exportQuality = quality)) }
    fun setActiveTab(tab: EditorTab) { _uiState.value = _uiState.value.copy(activeTab = tab) }
    fun undo() {
        if (undoStack.isNotEmpty()) {
            val prev = undoStack.pop()
            redoStack.push(_uiState.value.editState)
            _uiState.value = _uiState.value.copy(editState = prev, compiledPlan = compilePlan(_uiState.value.metadata, prev), canUndo = undoStack.isNotEmpty(), canRedo = true)
        }
    }
    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.pop()
            undoStack.push(_uiState.value.editState)
            _uiState.value = _uiState.value.copy(editState = next, compiledPlan = compilePlan(_uiState.value.metadata, next), canUndo = true, canRedo = redoStack.isNotEmpty())
        }
    }
    fun resetEdits() { _uiState.value.currentVideo?.let { loadVideo(it) } }
    fun exportVideo() {
        val video = _uiState.value.currentVideo ?: return
        val plan = _uiState.value.compiledPlan ?: return
        _uiState.value = _uiState.value.copy(isExporting = true, exportProgress = 0, message = null)
        viewModelScope.launch(Dispatchers.IO) {
            val result = exportManager.exportVideo(video.filePath, plan) { progress ->
                _uiState.value = _uiState.value.copy(exportProgress = progress)
            }
            result.fold(
                onSuccess = { outputPath ->
                    val file = File(outputPath)
                    val metadata = PythonVideoProcessor.extractMetadata(getApplication(), outputPath)
                    val thumbPath = ThumbnailHelper.generateThumbnail(getApplication(), outputPath)
                    val editedEntity = VideoEntity(
                        title = "${video.title}_Edited",
                        filePath = outputPath,
                        thumbnailPath = thumbPath,
                        durationMs = metadata.durationMs,
                        sizeBytes = file.length(),
                        width = metadata.width,
                        height = metadata.height,
                        rotation = metadata.rotation,
                        hasAudio = !plan.muteAudio && metadata.hasAudio,
                        isEdited = true,
                        originalVideoId = video.id,
                        exportQuality = plan.targetBitrateKbps.toString(),
                        createdAt = System.currentTimeMillis()
                    )
                    val savedId = repository.insertVideo(editedEntity)
                    _uiState.value = _uiState.value.copy(isExporting = false, exportProgress = 100, exportedVideo = editedEntity.copy(id = savedId), message = "Video exported successfully!")
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(isExporting = false, exportProgress = 0, message = "Export failed: ${err.message}")
                }
            )
        }
    }
    fun clearMessage() { _uiState.value = _uiState.value.copy(message = null) }
}
