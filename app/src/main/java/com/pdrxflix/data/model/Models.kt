package com.pdrxflix.data.model

data class VideoItem(
    val fileName: String,
    val filePath: String,
    val displayName: String,
    val episodeIndex: Int,
    val folderId: Long,
)

data class MediaCollection(
    val id: Long,
    val title: String,
    val folderPath: String,
    val coverPath: String?,
    val videos: List<VideoItem>,
) {
    val itemCount: Int get() = videos.size
}

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
) {
    val progress: Float
        get() = if (durationMs > 0L) lastPositionMs.toFloat() / durationMs.toFloat() else 0f
}

data class LibraryUiState(
    val collections: List<MediaCollection> = emptyList(),
    val continueWatching: List<PlaybackRecord> = emptyList(),
    val loading: Boolean = false,
    val query: String = "",
)

data class PlayerArgs(
    val collectionId: Long,
    val videoPath: String,
    val startPositionMs: Long,
    val episodeIndex: Int,
)
