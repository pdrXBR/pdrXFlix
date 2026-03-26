package com.pdrxflix.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.pdrxflix.data.model.PlaybackRecord

class PlaybackPreferences(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("pdrxflix_playback", Context.MODE_PRIVATE)
    private val gson = Gson()

    fun loadRecords(): List<PlaybackRecord> {
        val raw = prefs.getString(KEY_RECORDS, null) ?: return emptyList()
        return runCatching {
            val type = object : TypeToken<List<PlaybackRecord>>() {}.type
            gson.fromJson<List<PlaybackRecord>>(raw, type).orEmpty()
        }.getOrDefault(emptyList())
    }

    fun saveRecord(record: PlaybackRecord) {
        val existing = loadRecords().toMutableList()
        val index = existing.indexOfFirst { it.videoPath == record.videoPath }
        if (index >= 0) existing[index] = record else existing.add(0, record)
        val cleaned = existing
            .distinctBy { it.videoPath }
            .sortedByDescending { it.updatedAt }
        prefs.edit().putString(KEY_RECORDS, gson.toJson(cleaned)).apply()
    }

    fun clearRecord(videoPath: String) {
        val updated = loadRecords().filterNot { it.videoPath == videoPath }
        prefs.edit().putString(KEY_RECORDS, gson.toJson(updated)).apply()
    }

    fun loadAutoPlay(): Boolean = prefs.getBoolean(KEY_AUTOPLAY, true)

    fun saveAutoPlay(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_AUTOPLAY, enabled).apply()
    }

    companion object {
        private const val KEY_RECORDS = "records"
        private const val KEY_AUTOPLAY = "autoplay"
    }
}
