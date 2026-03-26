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

    private val _continueWatching = MutableStateFlow<List<PlaybackRecord>>(emptyList())
    val continueWatching: StateFlow<List<PlaybackRecord>> = _continueWatching

    val rootDirectory: File = File(Environment.getExternalStorageDirectory(), "Animes_server")

    suspend fun refreshCatalog() = withContext(Dispatchers.IO) {
        val scanned = scanRootDirectory(rootDirectory)
        _collections.value = scanned
        
        // AGORA REAGRUPA POR ANIME:
        val allRecords = playbackPreferences.loadRecords()
        val grouped = allRecords.groupBy { it.collectionId }
            .mapNotNull { entry -> 
                entry.value.maxByOrNull { it.updatedAt } 
            }
            .sortedByDescending { it.updatedAt }
        
        _continueWatching.value = grouped
    }

    fun findCollectionById(id: Long): MediaCollection? = _collections.value.firstOrNull { it.id == id }

    fun getResumeRecord(videoPath: String): PlaybackRecord? {
        // Busca o registro específico de um vídeo para pintar as cores
        return playbackPreferences.loadRecords().firstOrNull { it.videoPath == videoPath }
    }

    private fun scanRootDirectory(root: File): List<MediaCollection> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val animeFolders = root.listFiles { file -> file.isDirectory && !file.name.startsWith(".") }.orEmpty()
        
        return animeFolders.mapNotNull { folder ->
            val allVideos = mutableListOf<VideoItem>()
            val stableId = folder.absolutePath.hashCode().toLong().absoluteValue()

            // Vídeos na raiz
            folder.listFiles { f -> f.isFile && isVideoFile(f) }?.forEachIndexed { i, f ->
                allVideos.add(VideoItem(f.name, f.absolutePath, f.nameWithoutExtension, i, stableId, "Principal"))
            }

            // Vídeos em subpastas (Temporadas)
            folder.listFiles { f -> f.isDirectory }?.forEach { sub ->
                sub.listFiles { f -> f.isFile && isVideoFile(f) }?.forEachIndexed { i, f ->
                    allVideos.add(VideoItem(f.name, f.absolutePath, f.nameWithoutExtension, i, stableId, sub.name))
                }
            }

            if (allVideos.isEmpty()) return@mapNotNull null
            MediaCollection(stableId, folder.name, folder.absolutePath, findCover(folder)?.absolutePath, allVideos)
        }.sortedBy { it.title.lowercase(Locale.getDefault()) }
    }

    private fun isVideoFile(f: File) = f.extension.lowercase() in setOf("mp4", "mkv", "avi", "webm")
    private fun findCover(f: File) = f.listFiles { file -> file.extension.lowercase() in setOf("jpg", "png", "jpeg") }?.firstOrNull()
    private fun Long.absoluteValue(): Long = abs(this)
}
