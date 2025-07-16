package com.v19.gal.data.repo.impl

import com.v19.gal.data.local.MediaDao
import com.v19.gal.data.model.Media
import com.v19.gal.data.repo.MediaRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

class MediaRepositoryImpl(private val mediaDao: MediaDao) : MediaRepository {
    override fun getAlbumsFlow(): Flow<List<Media>> = flow {
        val list = mutableListOf<Media>()
        val image = mediaDao.getTopMedia(Media.IMAGE)
        if (image == null) {
            list.add(Media(1, -1, "All Photos", 0, "", "", "", "", 0, Media.IMAGE))
        } else {
            list.add(
                image.copy(
                    albumId = -1,
                    albumName = "All Photos",
                    contentCount = mediaDao.getMediaCount(Media.IMAGE)
                )
            )
        }

        val video = mediaDao.getTopMedia(Media.VIDEO)
        if (video == null) {
            list.add(Media(1, -1, "All Videos", 0, "", "", "", "", 0, Media.IMAGE))
        } else {
            list.add(
                video.copy(
                    albumId = -1,
                    albumName = "All Videos",
                    contentCount = mediaDao.getMediaCount(Media.VIDEO)
                )
            )
        }
        mediaDao.getAlbums()?.let {
            list.addAll(it)
        }
        emit(list)
    }.flowOn(Dispatchers.IO)

    override fun getMedia(bucketId: Long, mediaType: Int): Flow<List<Media>> = flow {
        if (bucketId == -1L) {
            emit(mediaDao.getAllMediaByType(mediaType))
        } else {
            emit(mediaDao.getMediaByAlbum(bucketId))
        }
    }.flowOn(Dispatchers.IO)
}