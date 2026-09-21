package com.example.media

import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.local.VideoEntity
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ScreenRecordingStatus {
    IDLE,
    COUNTDOWN,
    RECORDING,
    PAUSED,
    SAVING
}

data class ScreenRecordingUiState(
    val status: ScreenRecordingStatus = ScreenRecordingStatus.IDLE,
    val isAudioEnabled: Boolean = true,
    val selectedQuality: String = "1080p",
    val selectedFps: Int = 30,
    val selectedBitrateMbps: Int = 8,
    val countdownRemaining: Int = 3,
    val elapsedSeconds: Long = 0L,
    val lastRecordedVideo: VideoEntity? = null,
    val errorMessage: String? = null,
    val isCountdownEnabled: Boolean = true
)

object ScreenRecordingManager {
    private val _uiState = MutableStateFlow(ScreenRecordingUiState())
    val uiState: StateFlow<ScreenRecordingUiState> = _uiState.asStateFlow()

    private val _recordingSavedEvent = MutableSharedFlow<VideoEntity>(extraBufferCapacity = 1)
    val recordingSavedEvent: SharedFlow<VideoEntity> = _recordingSavedEvent.asSharedFlow()

    fun updateStatus(status: ScreenRecordingStatus) {
        _uiState.value = _uiState.value.copy(status = status)
    }

    fun updateElapsedSeconds(seconds: Long) {
        _uiState.value = _uiState.value.copy(elapsedSeconds = seconds)
    }

    fun updateCountdown(countdown: Int) {
        _uiState.value = _uiState.value.copy(countdownRemaining = countdown)
    }

    fun setAudioEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isAudioEnabled = enabled)
    }

    fun setQuality(quality: String) {
        _uiState.value = _uiState.value.copy(selectedQuality = quality)
    }

    fun setFps(fps: Int) {
        _uiState.value = _uiState.value.copy(selectedFps = fps)
    }

    fun setBitrate(bitrateMbps: Int) {
        _uiState.value = _uiState.value.copy(selectedBitrateMbps = bitrateMbps)
    }

    fun setCountdownEnabled(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(isCountdownEnabled = enabled)
    }

    fun setError(error: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = error)
    }

    fun onRecordingSaved(video: VideoEntity) {
        _uiState.value = _uiState.value.copy(
            status = ScreenRecordingStatus.IDLE,
            elapsedSeconds = 0L,
            lastRecordedVideo = video
        )
        _recordingSavedEvent.tryEmit(video)
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(
            status = ScreenRecordingStatus.IDLE,
            elapsedSeconds = 0L,
            errorMessage = null
        )
    }

    fun startService(context: Context, resultCode: Int, data: Intent) {
        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_START
            putExtra(ScreenRecordingService.EXTRA_RESULT_CODE, resultCode)
            putExtra(ScreenRecordingService.EXTRA_RESULT_DATA, data)
            putExtra(ScreenRecordingService.EXTRA_AUDIO_ENABLED, _uiState.value.isAudioEnabled)
            putExtra(ScreenRecordingService.EXTRA_QUALITY, _uiState.value.selectedQuality)
            putExtra(ScreenRecordingService.EXTRA_FPS, _uiState.value.selectedFps)
            putExtra(ScreenRecordingService.EXTRA_BITRATE, _uiState.value.selectedBitrateMbps * 1_000_000)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun pauseService(context: Context) {
        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_PAUSE
        }
        context.startService(intent)
    }

    fun resumeService(context: Context) {
        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_RESUME
        }
        context.startService(intent)
    }

    fun stopService(context: Context) {
        val intent = Intent(context, ScreenRecordingService::class.java).apply {
            action = ScreenRecordingService.ACTION_STOP
        }
        context.startService(intent)
    }
}
