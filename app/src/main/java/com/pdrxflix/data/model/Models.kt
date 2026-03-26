package com.pdrxflix.data.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class VideoItem(
    val fileName: String,
    val filePath: String,
    val displayName: String,
    val episodeIndex: Int,
    val folderId: Long,
    val seasonName: String = "Principal" // <-- Adicionado isso
) : Parcelable

@Parcelize
data class MediaCollection(
    val id: Long,
    val title: String,
    val folderPath: String,
    val coverPath: String?,
    val videos: List<VideoItem>,
) : Parcelable {
    
    val itemCount: Int get() = videos.size

    // ESTAS DUAS FUNÇÕES SÃO ESSENCIAIS PARA O ADAPTER FUNCIONAR:
    fun getSeasonNames(): List<String> {
        return videos.map { it.seasonName }.distinct().sorted()
    }

    fun getVideosBySeason(seasonName: String): List<VideoItem> {
        return videos.filter { it.seasonName == seasonName }
    }
}

@Parcelize
data class PlaybackRecord(
    val collectionId: Long,
    val collectionTitle: String,
    val videoPath: String,
    val videoTitle: String,
    val coverPath: String?,
    val episodeIndex: Int,
    val lastPositionMs: Long,
    val durationMs: Long,
    val updatedAt: Long,
) : Parcelable {
    val progress: Float
        get() = if (durationMs > 0L) lastPositionMs.toFloat() / durationMs.toFloat() else 0f
}

data class LibraryUiState(
    val collections: List<MediaCollection> = emptyList(),
    val continueWatching: List<PlaybackRecord> = emptyList(),
    val loading: Boolean = false,
    val query: String = "",
)

@Parcelize
data class PlayerArgs(
    val collectionId: Long,
    val videoPath: String,
    val startPositionMs: Long,
    val episodeIndex: Int,
) : Parcelable
