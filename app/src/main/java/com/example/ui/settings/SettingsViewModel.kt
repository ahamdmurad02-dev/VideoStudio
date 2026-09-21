package com.example.ui.settings

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.repository.VideoRepository
import com.example.python.PythonVideoProcessor
import com.example.util.BatteryMonitor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.io.File

enum class AppThemeMode {
    SYSTEM, DARK, LIGHT
}

data class SettingsUiState(
    val themeMode: AppThemeMode = AppThemeMode.SYSTEM,
    val languageCode: String = "en",
    val defaultQuality: String = "FHD",
    val autoStopLowBattery: Boolean = true,
    val lowBatteryThreshold: Int = 5,
    val currentBatteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val totalVideos: Int = 0,
    val storageUsedFormatted: String = "0 MB",
    val pythonEngineInfo: Map<String, String> = emptyMap()
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VideoRepository
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val prefs = application.getSharedPreferences("video_studio_prefs", Context.MODE_PRIVATE)

    init {
        val db = AppDatabase.getInstance(application)
        repository = VideoRepository(db.videoDao())

        val savedTheme = prefs.getString("theme_mode", AppThemeMode.SYSTEM.name) ?: AppThemeMode.SYSTEM.name
        val savedLang = prefs.getString("language_code", "en") ?: "en"
        val savedQuality = prefs.getString("default_quality", "FHD") ?: "FHD"
        val savedAutoStop = prefs.getBoolean("auto_stop_battery", true)
        val savedThreshold = prefs.getInt("low_battery_threshold", 5)

        val engineInfo = PythonVideoProcessor.getEngineInfo(application)
        val initialBattery = BatteryMonitor.getCurrentBatteryInfo(application)

        _uiState.value = _uiState.value.copy(
            themeMode = try { AppThemeMode.valueOf(savedTheme) } catch (_: Exception) { AppThemeMode.SYSTEM },
            languageCode = savedLang,
            defaultQuality = savedQuality,
            autoStopLowBattery = savedAutoStop,
            lowBatteryThreshold = savedThreshold,
            currentBatteryLevel = initialBattery.level,
            isCharging = initialBattery.isCharging,
            pythonEngineInfo = engineInfo
        )

        observeVideos()
        observeBattery()
    }

    private fun observeBattery() {
        viewModelScope.launch {
            BatteryMonitor.observeBatteryInfo(getApplication()).collectLatest { info ->
                _uiState.value = _uiState.value.copy(
                    currentBatteryLevel = info.level,
                    isCharging = info.isCharging
                )
            }
        }
    }

    private fun observeVideos() {
        viewModelScope.launch {
            repository.allVideos.collectLatest { videos ->
                calculateStorage(videos.size)
            }
        }
    }

    private fun calculateStorage(videoCount: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val videosDir = File(getApplication<Application>().filesDir, "videos")
            var totalBytes = 0L
            if (videosDir.exists()) {
                videosDir.listFiles()?.forEach { totalBytes += it.length() }
            }
            val thumbsDir = File(getApplication<Application>().filesDir, "thumbnails")
            if (thumbsDir.exists()) {
                thumbsDir.listFiles()?.forEach { totalBytes += it.length() }
            }

            val formatted = PythonVideoProcessor.formatFileSize(totalBytes)
            _uiState.value = _uiState.value.copy(
                totalVideos = videoCount,
                storageUsedFormatted = formatted
            )
        }
    }

    fun setThemeMode(mode: AppThemeMode) {
        _uiState.value = _uiState.value.copy(themeMode = mode)
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setLanguage(languageCode: String) {
        _uiState.value = _uiState.value.copy(languageCode = languageCode)
        prefs.edit().putString("language_code", languageCode).apply()
        val appLocale = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun setDefaultQuality(quality: String) {
        _uiState.value = _uiState.value.copy(defaultQuality = quality)
        prefs.edit().putString("default_quality", quality).apply()
    }

    fun setAutoStopLowBattery(enabled: Boolean) {
        _uiState.value = _uiState.value.copy(autoStopLowBattery = enabled)
        prefs.edit().putBoolean("auto_stop_battery", enabled).apply()
    }

    fun setLowBatteryThreshold(threshold: Int) {
        _uiState.value = _uiState.value.copy(lowBatteryThreshold = threshold)
        prefs.edit().putInt("low_battery_threshold", threshold).apply()
    }
}
