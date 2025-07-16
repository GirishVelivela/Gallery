package com.v19.gal.albumcontent.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.v19.gal.data.model.Media
import com.v19.gal.data.repo.MediaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlbumContentViewModel(
    private val mediaRepository: MediaRepository
) : ViewModel() {

    private val _media = MutableStateFlow<List<Media>>(emptyList())
    val medias: StateFlow<List<Media>> = _media.asStateFlow()

    fun loadMedia(bucketId: Long, mediaType: Int) {
        viewModelScope.launch {
            mediaRepository.getMedia(bucketId, mediaType).collect {
                _media.value = it
            }
        }
    }

    class AlbumContentViewModelFactory(
        private val repository: MediaRepository
    ) :
        ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return modelClass.getConstructor(MediaRepository::class.java)
                .newInstance(repository)
        }
    }
}
