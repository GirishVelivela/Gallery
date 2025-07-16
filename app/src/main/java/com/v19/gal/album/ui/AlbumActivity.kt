package com.v19.gal.album.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.v19.gal.R
import com.v19.gal.album.viewmodel.AlbumViewModel
import com.v19.gal.albumcontent.ui.AlbumContentActivity
import com.v19.gal.data.local.AppDatabase
import com.v19.gal.data.model.Media
import com.v19.gal.data.repo.impl.MediaRepositoryImpl
import com.v19.gal.sync.MediaObserver
import com.v19.gal.sync.MediaSyncWorker
import com.v19.gal.sync.ObserverCallback
import com.v19.gal.hasMediaPermissions
import com.v19.gal.scheduleMediaSyncWorker
import com.v19.gal.ui.theme.AlbumTileBg
import com.v19.gal.ui.theme.GalTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

class AlbumActivity : ComponentActivity() {
    val TAG = AlbumActivity::class.java.simpleName

    private lateinit var albumViewModel: AlbumViewModel

    private val observerCallback by lazy {
        object : ObserverCallback {
            override fun onChange(selfChange: Boolean) {
                scheduleMediaSyncWorker(applicationContext)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val dao = AppDatabase.getInstance(applicationContext).mediaDao()
        val repository = MediaRepositoryImpl(dao)
        albumViewModel = ViewModelProvider(
            this,
            AlbumViewModel.AlbumViewModelFactory(repository)
        )[AlbumViewModel::class.java]

        MediaObserver.getInstance(applicationContext)
            .register(observerCallback)

        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData("on_launch_media_sync")
            .asFlow()
            .onEach { infos ->
                albumViewModel.loadAlbums()
            }
            .launchIn(lifecycleScope)
        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData("periodic_media_sync")
            .asFlow()
            .onEach { infos ->
                albumViewModel.loadAlbums()
            }
            .launchIn(lifecycleScope)

        enableEdgeToEdge()
        setContent {
            GalTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlbumGridScreen(
                        albumViewModel = albumViewModel, modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
        if (hasMediaPermissions(applicationContext)) {
            scheduleMediaSyncWorker(applicationContext)
        } else {
            val permissions = mutableListOf<String>()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions.add(android.Manifest.permission.READ_MEDIA_IMAGES)
                permissions.add(android.Manifest.permission.READ_MEDIA_VIDEO)
            } else {
                permissions.add(android.Manifest.permission.READ_EXTERNAL_STORAGE)
            }
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 100)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaObserver.getInstance(applicationContext)
            .unregister(observerCallback)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray, deviceId: Int
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults, deviceId)
        for (result in grantResults) {
            if (result == PackageManager.PERMISSION_GRANTED) {
                schedulePeriodicMediaSync(applicationContext)
                break
            }
        }
    }

    private fun schedulePeriodicMediaSync(context: Context) {
        Log.d(TAG, "schedulePeriodicMediaSync")
        val periodicWorkRequest = PeriodicWorkRequestBuilder<MediaSyncWorker>(
            15, TimeUnit.MINUTES
        ).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "periodic_media_sync",                  // Unique name
            ExistingPeriodicWorkPolicy.KEEP,        // Don't replace if already scheduled
            periodicWorkRequest
        )
    }

}

@Composable
fun AlbumGridScreen(albumViewModel: AlbumViewModel, modifier: Modifier) {
    val albums by albumViewModel.albums.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // 2 columns
        modifier = Modifier
            .then(modifier)
            .fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums.size) { index ->
            AlbumGridItem(albums[index])
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun AlbumGridItem(album: Media) {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable {
                context.startActivity(
                    Intent(
                        context, AlbumContentActivity::class.java
                    ).putExtra("bucket_id", album.albumId).putExtra("media_type", album.type)
                )
            }) {

        val thumbnailState = remember { mutableStateOf<Bitmap?>(null) }
        if (album.type == Media.VIDEO && album.filePath != null && hasMediaPermissions(context)) {
            val videoFile = File(album.filePath)
            LaunchedEffect(videoFile) {
                withContext(Dispatchers.IO) {
                    if (videoFile.exists()) {
                        val bmp = ThumbnailUtils.createVideoThumbnail(
                            videoFile, Size(320, 240), null
                        )
                        thumbnailState.value = bmp
                    }
                }
            }
        }

        AsyncImage(
            model = if (album.type == Media.VIDEO) {
                thumbnailState.value
            } else {
                album.filePath?.let {
                    File(it)
                }
            },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            error = painterResource(id = R.drawable.baseline_image_not_supported_24),
            placeholder = painterResource(id = R.drawable.baseline_image_24),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f) // Square grid item
        )
        Row(
            modifier = Modifier
                .background(AlbumTileBg)
                .fillMaxWidth()
                .padding(5.dp)
                .align(Alignment.BottomStart)
        ) {
            Text(
                text = album.albumName ?: "",
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = album.contentCount.toString(),
                color = Color.White,
                style = MaterialTheme.typography.bodyMedium
            )
        }

    }
}