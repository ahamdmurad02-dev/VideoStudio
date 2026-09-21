package com.example.util

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.text.DecimalFormat

data class StorageCheckResult(
    val availableBytes: Long,
    val availableMB: Long,
    val totalBytes: Long,
    val totalMB: Long,
    val isLowStorage: Boolean,
    val formattedAvailable: String,
    val formattedTotal: String
)

object StorageHelper {
    const val LOW_STORAGE_THRESHOLD_MB = 500L
    const val LOW_STORAGE_THRESHOLD_BYTES = LOW_STORAGE_THRESHOLD_MB * 1024L * 1024L

    fun getAvailableStorageBytes(context: Context): Long {
        return try {
            val path: File = context.filesDir ?: Environment.getDataDirectory()
            val stat = StatFs(path.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            Long.MAX_VALUE
        }
    }

    fun getTotalStorageBytes(context: Context): Long {
        return try {
            val path: File = context.filesDir ?: Environment.getDataDirectory()
            val stat = StatFs(path.path)
            stat.blockCountLong * stat.blockSizeLong
        } catch (_: Exception) {
            0L
        }
    }

    fun checkStorage(context: Context, thresholdMB: Long = LOW_STORAGE_THRESHOLD_MB): StorageCheckResult {
        val availableBytes = getAvailableStorageBytes(context)
        val totalBytes = getTotalStorageBytes(context)
        val availableMB = availableBytes / (1024 * 1024)
        val totalMB = totalBytes / (1024 * 1024)
        val isLow = availableBytes < (thresholdMB * 1024L * 1024L)

        return StorageCheckResult(
            availableBytes = availableBytes,
            availableMB = availableMB,
            totalBytes = totalBytes,
            totalMB = totalMB,
            isLowStorage = isLow,
            formattedAvailable = formatBytes(availableBytes),
            formattedTotal = formatBytes(totalBytes)
        )
    }

    fun isLowStorage(context: Context, thresholdMB: Long = LOW_STORAGE_THRESHOLD_MB): Boolean {
        val availableBytes = getAvailableStorageBytes(context)
        return availableBytes < (thresholdMB * 1024L * 1024L)
    }

    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt().coerceIn(0, units.size - 1)
        return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
    }
}
