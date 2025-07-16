package com.v19.gal.data.repo

import com.v19.gal.data.model.Media
import kotlinx.coroutines.flow.Flow

interface MediaRepository {
    fun getAlbumsFlow(): Flow<List<Media>>

    fun getMedia(bucketId: Long, mediaType: Int): Flow<List<Media>>
}