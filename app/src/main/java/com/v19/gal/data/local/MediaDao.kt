package com.v19.gal.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.v19.gal.data.model.Media

@Dao
interface MediaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<Media>)

    @Query(
        "SELECT albumId, albumName, MAX(dateModified) as lastUpdated, id, displayName, " +
                "mimeType, filePath, dateModified, type, relativePath, count(*) as contentCount, " +
                "CASE " +
                "WHEN relativePath = 'DCIM/Camera/' THEN 1 " +
                "ELSE 0 " +
                "END AS priority " +
                " FROM images GROUP BY albumId ORDER BY priority DESC, lastUpdated DESC"
    )
    suspend fun getAlbums(): List<Media>?

    @Query("SELECT * FROM images where type = :mediaType ORDER BY dateModified DESC")
    suspend fun getAllMediaByType(mediaType: Int): List<Media>

    @Query("SELECT * FROM images WHERE albumId = :albumId ORDER BY dateModified DESC")
    suspend fun getMediaByAlbum(albumId: Long): List<Media>

    @Query("SELECT MAX(dateModified) FROM images WHERE type = :image")
    suspend fun getLastDateModified(image: Int): Long?

    @Query("SELECT * FROM images WHERE type = :mediaType ORDER BY dateModified DESC LIMIT 1")
    suspend fun getTopMedia(mediaType: Int): Media?

    @Query("SELECT count(*) as contentCount FROM images WHERE type = :mediaType")
    suspend fun getMediaCount(mediaType: Int): Int

    @Query("DELETE FROM images WHERE id IN (:mediaStoreIds) and type = :mediaType")
    suspend fun delete(mediaStoreIds: Set<Long>, mediaType: Int)

    @Query("DELETE FROM images WHERE type = :mediaType")
    suspend fun deleteAllMedia(mediaType: Int)

    @Query("select id from images WHERE type = :mediaType")
    abstract fun getAllMediaIds(mediaType: Int): List<Long>

}