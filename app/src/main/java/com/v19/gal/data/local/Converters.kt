package com.v19.gal.data.local

import com.v19.gal.data.model.Album
import com.v19.gal.data.model.Media

fun Media.toAlbum(): Album {
    return Album(
        id = albumId.hashCode().toLong(),
        name = albumName ?: "",
        lastUpdated = dateModified,
        latestMedia = this
    )
}
