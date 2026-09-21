package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val filePath: String,
    val thumbnailPath: String? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val width: Int = 0,
    val height: Int = 0,
    val rotation: Int = 0,
    val hasAudio: Boolean = true,
    val isEdited: Boolean = false,
    val originalVideoId: Long? = null,
    val exportQuality: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
