package com.v19.gal.sync

import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.v19.gal.data.local.AppDatabase
import com.v19.gal.data.model.Media
import com.v19.gal.hasMediaPermissions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

class MediaSyncWorker(private val context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    private val TAG = MediaSyncWorker::class.java.simpleName

    override suspend fun doWork(): Result {
        if (hasMediaPermissions(applicationContext)) {
            val imagesJob = CoroutineScope(Dispatchers.IO).launch {
                syncImages(context = context)
            }
            val video = CoroutineScope(Dispatchers.IO).launch {
                syncVideos(context = context)
            }
            imagesJob.join()
            video.join()
        }
        return Result.success()
    }

    private suspend fun delete(cursor: Cursor, mediaType: Int) {
        val mediaStoreIds = mutableSetOf<Long>()
        do {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            mediaStoreIds.add(cursor.getLong(idColumn))
        } while (cursor.moveToNext())
        val dbIds = AppDatabase.getInstance(context).mediaDao().getAllMediaIds(mediaType).toSet()
        Log.d(TAG, "dbIds = ${dbIds.size}")
        Log.d(TAG, "mediaStoreIds = ${mediaStoreIds.size}")
        val deletedIds = dbIds.subtract(mediaStoreIds)
        Log.d(TAG, "deletedIds = ${deletedIds.size}")
        if (deletedIds.isNotEmpty()) {
            AppDatabase.getInstance(context).mediaDao()
                .delete(deletedIds, mediaType)
        }
    }

    private suspend fun syncImages(context: Context) {
        Log.d(TAG, "syncImages --- start")

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        cursor?.let {
            if (cursor.moveToFirst()) {
                val lastLocalDateModified =
                    AppDatabase.getInstance(context).mediaDao().getLastDateModified(Media.IMAGE)
                if (cursor.count == AppDatabase.getInstance(context).mediaDao()
                        .getMediaCount(Media.IMAGE)
                    &&
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)) == lastLocalDateModified
                ) {
                    Log.d(TAG, "No image changes detected.")
                    cursor.close()
                    return
                }
                Log.d(TAG, "Images delete --- Start")
                delete(cursor, Media.IMAGE)
                cursor.close()
                Log.d(TAG, "Images delete --- end")
                sync(
                    context,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    lastLocalDateModified
                        ?: 0,
                    Media.IMAGE
                )
            } else {
                AppDatabase.getInstance(context).mediaDao().deleteAllMedia(Media.IMAGE)
            }
        }
        Log.d(TAG, "syncImages --- end")
    }

    private suspend fun syncVideos(context: Context) {
        Log.d(TAG, "syncVideos --- start")
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATE_MODIFIED,
        )

        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )
        cursor?.let {
            if (cursor.moveToFirst()) {
                val lastLocalDateModified =
                    AppDatabase.getInstance(context).mediaDao().getLastDateModified(Media.VIDEO)
                if (cursor.count == AppDatabase.getInstance(context).mediaDao()
                        .getMediaCount(Media.VIDEO)
                    && cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)) == lastLocalDateModified
                ) {
                    Log.d(TAG, "No Video changes detected.")
                    cursor.close()
                    return
                }
                Log.d(TAG, "Video delete --- Start")
                delete(cursor, Media.VIDEO)
                cursor.close()
                Log.d(TAG, "Video delete --- End")
                sync(
                    context,
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    lastLocalDateModified
                        ?: 0,
                    Media.VIDEO
                )
            } else {
                AppDatabase.getInstance(context).mediaDao().deleteAllMedia(Media.VIDEO)
            }
        }

        Log.d(TAG, "syncVideos --- end")
    }

    private suspend fun sync(context: Context, uri: Uri, lastDateModified: Long, type: Int) {
        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.BUCKET_ID,
            MediaStore.Images.Media.BUCKET_DISPLAY_NAME,
            MediaStore.Images.Media.DATE_MODIFIED,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.MIME_TYPE,
            MediaStore.Images.Media.RELATIVE_PATH,
        )

        val selection = "${MediaStore.Images.Media.DATE_MODIFIED} > ?"
        val sortOrder = "${MediaStore.Images.Media.DATE_MODIFIED} DESC"

        val cursor = context.contentResolver.query(
            uri,
            projection,
            selection,
            arrayOf(lastDateModified.toString()),
            sortOrder
        )

        cursor?.use {
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val bucketIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_ID)
            val bucketNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.BUCKET_DISPLAY_NAME)
            val dateModifiedColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
            val displayNameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
            val relativePath = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.RELATIVE_PATH)

            Log.d(TAG, "Data count ${cursor.count} for type $type")

            val batch = mutableListOf<Media>()
            val insertJobs = mutableListOf<Job>()

            while (it.moveToNext()) {
                batch.add(
                    Media(
                        cursor.getLong(idColumn),
                        cursor.getLong(bucketIdColumn),
                        cursor.getString(bucketNameColumn) ?: "",
                        cursor.getLong(dateModifiedColumn),
                        cursor.getString(displayNameColumn) ?: "",
                        cursor.getString(dataColumn) ?: "",
                        cursor.getString(mimeTypeColumn) ?: "image/*",
                        cursor.getString(relativePath) ?: "image/*",
                        0,
                        type
                    )
                )

                if (batch.size >= 1000) {
                    val batchToInsert = batch.toList()
                    batch.clear()
                    val job = CoroutineScope(Dispatchers.IO).launch {
                        Log.d(TAG, "Inserting completed for type $type")
                        AppDatabase.getInstance(context).mediaDao().insertAll(batchToInsert)
                    }
                    insertJobs.add(job)
                }
            }
            if (batch.isNotEmpty()) {
                val job = CoroutineScope(Dispatchers.IO).launch {
                    Log.d(TAG, "Inserting completed for type $type")
                    AppDatabase.getInstance(context).mediaDao().insertAll(batch)
                }
                insertJobs.add(job)
            }
            insertJobs.joinAll()
            cursor.close()
            Log.d(TAG, "Insertion completed for type $type")
        }
    }

}