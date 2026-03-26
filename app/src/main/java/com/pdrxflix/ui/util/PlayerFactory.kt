package com.pdrxflix.ui.util

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer

object PlayerFactory {
    fun build(context: Context): ExoPlayer {
        return ExoPlayer.Builder(context).build()
    }
}
