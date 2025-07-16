package com.v19.gal.album.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v19.gal.data.model.Media
import com.v19.gal.data.repo.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumViewModel(private val mediaRepository: MediaRepository) :
    ViewModel() {

    private val _albums = MutableStateFlow<List<Media>>(emptyList())
    val albums: StateFlow<List<Media>> = _albums.asStateFlow()
    /*mediaRepository.getAlbumsFlow()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )*/

    init {
        loadAlbums()

    }

    fun loadAlbums() {
        viewModelScope.launch {
            mediaRepository.getAlbumsFlow().collect {
                _albums.value = it
            }
        }
    }

    class AlbumViewModelFactory(
        private val repository: MediaRepository
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(MediaRepository::class.java)
                .newInstance(repository)
        }
    }
}
