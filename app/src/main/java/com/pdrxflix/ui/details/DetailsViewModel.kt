package com.pdrxflix.ui.details

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.pdrxflix.data.model.MediaCollection
import com.pdrxflix.data.model.PlaybackRecord
import com.pdrxflix.data.repository.LocalMediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class DetailsViewModel(
    private val repository: LocalMediaRepository
) : ViewModel() {

    private val _selectedCollection = MutableStateFlow<MediaCollection?>(null)
    val selectedCollection: LiveData<MediaCollection?> = _selectedCollection.asLiveData()

    fun load(collectionId: Long) {
        viewModelScope.launch {
            val collection = repository.findCollectionById(collectionId)
            _selectedCollection.value = collection
        }
    }

    fun getRecordForVideo(videoPath: String): PlaybackRecord? {
        return repository.getRecordForVideo(videoPath)
    }

    // A Factory permite que o Fragment passe o repository para o ViewModel
    class Factory(private val repository: LocalMediaRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DetailsViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return DetailsViewModel(repository) as T
            }
            throw IllegalArgumentException("Classe ViewModel desconhecida")
        }
    }
}
