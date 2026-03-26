package com.pdrxflix.ui.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.pdrxflix.PdrXFlixApp
import com.pdrxflix.data.model.MediaCollection
import kotlinx.coroutines.launch

class DetailsViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as PdrXFlixApp).repository
    val selectedCollection = MutableLiveData<MediaCollection?>()

    fun load(collectionId: Long) {
        viewModelScope.launch {
            selectedCollection.value = repository.getCollection(collectionId)
        }
    }

    fun saveAutoPlay(enabled: Boolean) = repository.setAutoPlay(enabled)

    fun isAutoPlayEnabled(): Boolean = repository.getAutoPlay()
}
