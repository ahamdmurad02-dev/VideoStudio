package com.example.ui.library

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.VideoEntity
import com.example.data.repository.VideoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

enum class LibraryTab {
    ALL, RECORDED, EDITED
}

data class LibraryUiState(
    val selectedTab: LibraryTab = LibraryTab.ALL,
    val videos: List<VideoEntity> = emptyList(),
    val playingVideo: VideoEntity? = null,
    val renamingVideo: VideoEntity? = null,
    val deletingVideo: VideoEntity? = null,
    val message: String? = null
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: VideoRepository
    private val _selectedTab = MutableStateFlow(LibraryTab.ALL)
    private val _playingVideo = MutableStateFlow<VideoEntity?>(null)
    private val _renamingVideo = MutableStateFlow<VideoEntity?>(null)
    private val _deletingVideo = MutableStateFlow<VideoEntity?>(null)
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<LibraryUiState>

    init {
        val db = AppDatabase.getInstance(application)
        repository = VideoRepository(db.videoDao())

        val filteredVideosFlow = combine(_selectedTab, repository.allVideos) { tab, allVideos ->
            val list = when (tab) {
                LibraryTab.ALL -> allVideos
                LibraryTab.RECORDED -> allVideos.filter { !it.isEdited }
                LibraryTab.EDITED -> allVideos.filter { it.isEdited }
            }
            Pair(tab, list)
        }

        uiState = combine(
            filteredVideosFlow,
            _playingVideo,
            _renamingVideo,
            _deletingVideo
        ) { (tab, videos), playing, renaming, deleting ->
            LibraryUiState(
                selectedTab = tab,
                videos = videos,
                playingVideo = playing,
                renamingVideo = renaming,
                deletingVideo = deleting,
                message = _message.value
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = LibraryUiState()
        )
    }

    fun selectTab(tab: LibraryTab) {
        _selectedTab.value = tab
    }

    fun playVideo(video: VideoEntity?) {
        _playingVideo.value = video
    }

    fun promptRename(video: VideoEntity?) {
        _renamingVideo.value = video
    }

    fun confirmRename(id: Long, newTitle: String) {
        viewModelScope.launch {
            if (newTitle.isNotBlank()) {
                repository.updateTitle(id, newTitle.trim())
            }
            _renamingVideo.value = null
        }
    }

    fun promptDelete(video: VideoEntity?) {
        _deletingVideo.value = video
    }

    fun confirmDelete() {
        val video = _deletingVideo.value ?: return
        viewModelScope.launch {
            repository.deleteVideo(video)
            _deletingVideo.value = null
        }
    }

    fun shareVideo(context: Context, video: VideoEntity) {
        try {
            val file = File(video.filePath)
            if (!file.exists()) return
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, video.title))
        } catch (e: Exception) {
            _message.value = "Unable to share video: ${e.message}"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
