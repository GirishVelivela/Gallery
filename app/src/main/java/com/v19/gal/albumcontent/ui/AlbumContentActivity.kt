package com.v19.gal.albumcontent.ui

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.media.ThumbnailUtils
import android.os.Bundle
import android.util.Size
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.asFlow
import androidx.lifecycle.lifecycleScope
import androidx.work.WorkManager
import coil.compose.AsyncImage
import com.v19.gal.R
import com.v19.gal.albumcontent.viewmodel.AlbumContentViewModel
import com.v19.gal.data.local.AppDatabase
import com.v19.gal.data.model.Media
import com.v19.gal.data.repo.impl.MediaRepositoryImpl
import com.v19.gal.hasMediaPermissions
import com.v19.gal.scheduleMediaSyncWorker
import com.v19.gal.sync.MediaObserver
import com.v19.gal.sync.ObserverCallback
import com.v19.gal.ui.theme.GalTestTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.withContext
import java.io.File

class AlbumContentActivity : ComponentActivity() {

    private lateinit var albumContentViewModel: AlbumContentViewModel

    private val observerCallback by lazy {
        object : ObserverCallback {
            override fun onChange(selfChange: Boolean) {
                scheduleMediaSyncWorker(applicationContext)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val bucketId = intent?.getLongExtra("bucket_id", 0L) ?: 0L
        val mediaType = intent?.getIntExtra("media_type", 0) ?: 0

        val dao = AppDatabase.getInstance(applicationContext).mediaDao()
        val repository = MediaRepositoryImpl(dao)
        albumContentViewModel = ViewModelProvider(
            this,
            AlbumContentViewModel.AlbumContentViewModelFactory(repository)
        )[AlbumContentViewModel::class.java]

        MediaObserver.getInstance(applicationContext)
            .register(observerCallback)

        WorkManager.getInstance(applicationContext)
            .getWorkInfosForUniqueWorkLiveData("on_launch_media_sync")
            .asFlow()
            .onEach { infos ->
                albumContentViewModel.loadMedia(bucketId, mediaType)
            }
            .launchIn(lifecycleScope)

        enableEdgeToEdge()
        setContent {
            GalTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    AlbumContentGridScreen(
                        albumContentViewModel = albumContentViewModel,
                        modifier = Modifier.padding(innerPadding)
                    )
                    albumContentViewModel.loadMedia(bucketId, mediaType)
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        MediaObserver.getInstance(applicationContext)
            .unregister(observerCallback)
    }
}

@Composable
fun AlbumContentGridScreen(albumContentViewModel: AlbumContentViewModel, modifier: Modifier) {
    val albums by albumContentViewModel.medias.collectAsState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(3), // 2 columns
        modifier = Modifier
            .then(modifier)
            .fillMaxSize(),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(albums.size) { index ->
            AlbumContentGridItem(albums[index])
        }
    }
}

@SuppressLint("NewApi")
@Composable
fun AlbumContentGridItem(media: Media) {
    val context = LocalContext.current
    val thumbnailState = remember { mutableStateOf<Bitmap?>(null) }
    if (media.type == Media.VIDEO && media.filePath != null && hasMediaPermissions(context)) {
        val videoFile = File(media.filePath)
        LaunchedEffect(videoFile) {
            withContext(Dispatchers.IO) {
                if (videoFile.exists()) {
                    val bmp = ThumbnailUtils.createVideoThumbnail(
                        videoFile,
                        Size(320, 240), null
                    )
                    thumbnailState.value = bmp
                }
            }
        }
    }

    AsyncImage(
        model = if (media.type == Media.VIDEO) {
            thumbnailState.value
        } else {
            File(media.filePath)
        },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        error = painterResource(id = R.drawable.baseline_image_not_supported_24),
        placeholder = painterResource(id = R.drawable.baseline_image_24),
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f) // Square grid item
            .clip(RoundedCornerShape(16.dp))
            .clickable {
            }
    )
}