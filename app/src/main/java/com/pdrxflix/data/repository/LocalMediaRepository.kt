package com.pdrxflix.data.repository

import android.content.Context
import android.os.Environment
import com.pdrxflix.data.model.MediaCollection
import com.pdrxflix.data.model.PlaybackRecord
import com.pdrxflix.data.model.VideoItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale
import kotlin.math.abs

class LocalMediaRepository(private val context: Context) {

    private val playbackPreferences = PlaybackPreferences(context.applicationContext)
    private val _collections = MutableStateFlow<List<MediaCollection>>(emptyList())
    val collections: StateFlow<List<MediaCollection>> = _collections

    private val _continueWatching = MutableStateFlow<List<PlaybackRecord>>(playbackPreferences.loadRecords())
    val continueWatching: StateFlow<List<PlaybackRecord>> = _continueWatching

    private val _autoPlay = MutableStateFlow(playbackPreferences.loadAutoPlay())
    val autoPlay: StateFlow<Boolean> = _autoPlay

    val rootDirectory: File = File(Environment.getExternalStorageDirectory(), "Animes_server")

    suspend fun refreshCatalog() = withContext(Dispatchers.IO) {
        val scanned = scanRootDirectory(rootDirectory)
        _collections.value = scanned
        _continueWatching.value = playbackPreferences.loadRecords()
    }

    fun findCollectionById(id: Long): MediaCollection? = _collections.value.firstOrNull { it.id == id }

    fun findVideoByPath(path: String): VideoItem? = _collections.value
        .asSequence()
        .flatMap { it.videos.asSequence() }
        .firstOrNull { it.filePath == path }

    fun getResumeRecord(videoPath: String): PlaybackRecord? = _continueWatching.value.firstOrNull { it.videoPath == videoPath }

    fun saveProgress(record: PlaybackRecord) {
        playbackPreferences.saveRecord(record)
        _continueWatching.value = playbackPreferences.loadRecords()
    }

    fun clearProgress(videoPath: String) {
        playbackPreferences.clearRecord(videoPath)
        _continueWatching.value = playbackPreferences.loadRecords()
    }

    fun setAutoPlay(enabled: Boolean) {
        playbackPreferences.saveAutoPlay(enabled)
        _autoPlay.value = enabled
    }

    fun getAutoPlay(): Boolean = _autoPlay.value

    fun resolveNextVideo(collectionId: Long, currentEpisodeIndex: Int): VideoItem? {
        val collection = findCollectionById(collectionId) ?: return null
        return collection.videos.getOrNull(currentEpisodeIndex + 1)
    }

    fun getCollectionEpisodes(collectionId: Long): List<VideoItem> =
        findCollectionById(collectionId)?.videos.orEmpty()

    fun getCollection(collectionId: Long): MediaCollection? = findCollectionById(collectionId)

    private fun scanRootDirectory(root: File): List<MediaCollection> {
        if (!root.exists() || !root.isDirectory) return emptyList()

        val folders = root.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }.orEmpty()
        return folders.mapNotNull { folder ->
            val videos = folder.listFiles { file ->
                file.isFile && isVideoFile(file)
            }.orEmpty().sortedWith(::naturalFileComparator)

            if (videos.isEmpty()) return@mapNotNull null

            val cover = findCover(folder)
            val stableId = folder.absolutePath.hashCode().toLong().absoluteValue()
            val items = videos.mapIndexed { index, file ->
                VideoItem(
                    fileName = file.name,
                    filePath = file.absolutePath,
                    displayName = file.nameWithoutExtension,
                    episodeIndex = index,
                    folderId = stableId
                )
            }

            MediaCollection(
                id = stableId,
                title = folder.name,
                folderPath = folder.absolutePath,
                coverPath = cover?.absolutePath,
                videos = items
            )
        }.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun findCover(folder: File): File? {
        val preferred = listOf("capa.png", "capa.jpg", "capa.jpeg", "cover.png", "cover.jpg", "cover.jpeg")
        preferred.forEach { name ->
            val f = File(folder, name)
            if (f.exists() && f.isFile) return f
        }
        return folder.listFiles { file ->
            file.isFile && file.extension.lowercase(Locale.getDefault()) in setOf("png", "jpg", "jpeg", "webp")
        }?.firstOrNull()
    }

    private fun isVideoFile(file: File): Boolean {
        return file.extension.lowercase(Locale.getDefault()) in setOf("mp4", "mkv", "avi", "mov", "webm", "m4v")
    }

    private fun naturalFileComparator(a: File, b: File): Int {
        val left = a.name.lowercase(Locale.getDefault())
        val right = b.name.lowercase(Locale.getDefault())
        val leftParts = splitNatural(left)
        val rightParts = splitNatural(right)
        val count = minOf(leftParts.size, rightParts.size)
        for (i in 0 until count) {
            val l = leftParts[i]
            val r = rightParts[i]
            val cmp = when {
                l.isInt && r.isInt -> l.number.compareTo(r.number)
                else -> l.text.compareTo(r.text)
            }
            if (cmp != 0) return cmp
        }
        return leftParts.size.compareTo(rightParts.size)
    }

    private data class NaturalPart(val text: String, val number: Int, val isInt: Boolean)

    private fun splitNatural(input: String): List<NaturalPart> {
        val regex = Regex("(\d+|\D+)")
        return regex.findAll(input).map { match ->
            val token = match.value
            val intValue = token.toIntOrNull()
            NaturalPart(
                text = token,
                number = intValue ?: 0,
                isInt = intValue != null
            )
        }.toList()
    }

    private fun Long.absoluteValue(): Long = abs(this)
}
