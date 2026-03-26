package com.pdrxflix

import android.app.Application
import com.pdrxflix.data.repository.LocalMediaRepository

class PdrXFlixApp : Application() {
    val repository: LocalMediaRepository by lazy { LocalMediaRepository(this) }
}
