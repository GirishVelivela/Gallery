package com.v19.gal.data.model

data class Album(
    val id: Long,
    val name: String,
    val lastUpdated: Long,
    val latestMedia: Media?
)
