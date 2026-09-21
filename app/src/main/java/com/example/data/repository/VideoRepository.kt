package com.example.data.repository

import com.example.data.local.VideoDao
import com.example.data.local.VideoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File

class VideoRepository(private val videoDao: VideoDao) {
    val allVideos: Flow<List<VideoEntity>> = videoDao.getAllVideos()
    val recordedVideos: Flow<List<VideoEntity>> = videoDao.getRecordedVideos()
    val editedVideos: Flow<List<VideoEntity>> = videoDao.getEditedVideos()

    suspend fun getVideoById(id: Long): VideoEntity? = withContext(Dispatchers.IO) {
        videoDao.getVideoById(id)
    }

    suspend fun insertVideo(video: VideoEntity): Long = withContext(Dispatchers.IO) {
        videoDao.insertVideo(video)
    }

    suspend fun updateVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        videoDao.updateVideo(video)
    }

    suspend fun updateTitle(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        videoDao.updateTitle(id, newTitle)
    }

    suspend fun deleteVideo(video: VideoEntity) = withContext(Dispatchers.IO) {
        try {
            val file = File(video.filePath)
            if (file.exists()) {
                file.delete()
            }
            video.thumbnailPath?.let {
                val thumbFile = File(it)
                if (thumbFile.exists()) {
                    thumbFile.delete()
                }
            }
        } catch (_: Exception) {}
        videoDao.deleteVideoById(video.id)
    }
}
