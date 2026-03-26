package com.pdrxflix.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pdrxflix.PdrXFlixApp
import com.pdrxflix.data.model.LibraryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HomeViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = (app as PdrXFlixApp).repository
    private val query = MutableStateFlow("")

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.collections,
        repository.continueWatching,
        query
    ) { collections, continueWatching, q ->
        val filtered = if (q.isBlank()) {
            collections
        } else {
            collections.filter { it.title.contains(q, ignoreCase = true) }
        }
        LibraryUiState(
            collections = filtered,
            continueWatching = continueWatching,
            loading = false,
            query = q
        )
    }.let { flow ->
        val result = MutableStateFlow(LibraryUiState(loading = true))
        viewModelScope.launch {
            flow.collect { result.value = it }
        }
        result
    }

    fun refresh() = viewModelScope.launch { repository.refreshCatalog() }

    fun updateQuery(text: String) { query.value = text }

    fun setAutoPlay(enabled: Boolean) = repository.setAutoPlay(enabled)

    fun isAutoPlayEnabled(): Boolean = repository.getAutoPlay()
}
