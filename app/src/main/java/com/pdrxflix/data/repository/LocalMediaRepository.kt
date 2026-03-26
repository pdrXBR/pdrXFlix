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

    // Simulação simples de SharedPreferences para as preferências
    private val prefs = context.getSharedPreferences("pdrx_prefs", Context.MODE_PRIVATE)
    private val playbackPreferences = PlaybackPreferences(context.applicationContext)

    private val _collections = MutableStateFlow<List<MediaCollection>>(emptyList())
    val collections: StateFlow<List<MediaCollection>> = _collections

    private val _continueWatching = MutableStateFlow<List<PlaybackRecord>>(emptyList())
    val continueWatching: StateFlow<List<PlaybackRecord>> = _continueWatching

    val rootDirectory: File = File(Environment.getExternalStorageDirectory(), "Animes_server")

    // Funções de Preferência (AutoPlay)
    fun getAutoPlay(): Boolean = prefs.getBoolean("auto_play", true)
    fun setAutoPlay(enabled: Boolean) = prefs.edit().putBoolean("auto_play", enabled).apply()

    // Funções de Progresso
    fun saveProgress(record: PlaybackRecord) {
        playbackPreferences.saveRecord(record)
    }

    fun getRecordForVideo(path: String): PlaybackRecord? {
        return playbackPreferences.loadRecords().firstOrNull { it.videoPath == path }
    }

    fun getCollection(id: Long): MediaCollection? = _collections.value.firstOrNull { it.id == id }

    suspend fun refreshCatalog() = withContext(Dispatchers.IO) {
        val scanned = scanRootDirectory(rootDirectory)
        _collections.value = scanned
        
        val allRecords = playbackPreferences.loadRecords()
        _continueWatching.value = allRecords.groupBy { it.collectionId }
            .mapNotNull { it.value.maxByOrNull { rec -> rec.updatedAt } }
            .sortedByDescending { it.updatedAt }
    }

    fun findCollectionById(id: Long): MediaCollection? = getCollection(id)

    private fun scanRootDirectory(root: File): List<MediaCollection> {
        if (!root.exists() || !root.isDirectory) return emptyList()
        val folders = root.listFiles { it.isDirectory && !it.name.startsWith(".") }.orEmpty()
        
        return folders.mapNotNull { folder ->
            val allVideos = mutableListOf<VideoItem>()
            val stableId = folder.absolutePath.hashCode().toLong().absoluteValue()

            folder.walkTopDown().maxDepth(2).filter { it.isFile && isVideoFile(it) }.forEach { file ->
                val sName = if (file.parentFile?.absolutePath == folder.absolutePath) "Principal" else file.parentFile!!.name
                allVideos.add(VideoItem(file.name, file.absolutePath, file.nameWithoutExtension, 0, stableId, sName))
            }

            val finalVideos = allVideos.mapIndexed { i, v -> v.copy(episodeIndex = i) }
            if (finalVideos.isEmpty()) return@mapNotNull null
            MediaCollection(stableId, folder.name, folder.absolutePath, findCover(folder)?.absolutePath, finalVideos)
        }.sortedBy { it.title.lowercase() }
    }

    private fun isVideoFile(f: File) = f.extension.lowercase() in setOf("mp4", "mkv", "avi")
    private fun findCover(f: File) = f.listFiles { it.extension.lowercase() in setOf("jpg", "png") }?.firstOrNull()
    private fun Long.absoluteValue() = abs(this)
}
