package com.example.python

data class VideoMetadata(
    val format: String = "mp4",
    val durationMs: Long = 0L,
    val durationFormatted: String = "00:00",
    val fileSize: Long = 0L,
    val fileSizeFormatted: String = "0 MB",
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val hasAudio: Boolean = true,
    val bitrateBps: Long = 0L,
    val frameRate: Float = 30.0f
)

data class CropGeometry(
    val aspectRatioName: String,
    val targetRatio: Float,
    val cropLeft: Int,
    val cropTop: Int,
    val cropWidth: Int,
    val cropHeight: Int,
    val normLeft: Float,
    val normTop: Float,
    val normWidth: Float,
    val normHeight: Float
)

data class OverlayConfig(
    val text: String,
    val position: String,
    val normX: Float,
    val normY: Float,
    val fontSizeSp: Float,
    val textColor: Long = 0xFFFFFFFF,
    val backgroundColor: Long = 0x99000000
)

data class CapCutBackgroundConfig(
    val mode: String = "NONE",
    val colorHex: String = "#000000",
    val chromaKeyColor: String = "GREEN",
    val chromaThreshold: Float = 0.4f,
    val blurRadius: Float = 25f
)

data class CapCutColorConfig(
    val filterPreset: String = "NORMAL",
    val brightness: Float = 0.0f,
    val contrast: Float = 1.0f,
    val saturation: Float = 1.0f,
    val vignette: Float = 0.0f
)

data class EditPlan(
    val status: String = "READY",
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = 0L,
    val speedFactor: Float = 1.0f,
    val outputDurationMs: Long = 0L,
    val rotationDegrees: Int = 0,
    val cropGeometry: CropGeometry,
    val outputWidth: Int,
    val outputHeight: Int,
    val targetBitrateKbps: Int,
    val muteAudio: Boolean,
    val volumePercent: Int = 100,
    val overlay: OverlayConfig?,
    val backgroundConfig: CapCutBackgroundConfig = CapCutBackgroundConfig(),
    val colorConfig: CapCutColorConfig = CapCutColorConfig(),
    val estimatedSizeBytes: Long,
    val estimatedSizeFormatted: String
)
