package com.v19.gal.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.v19.gal.data.model.Media
import com.v19.gal.ui.theme.GalTestTheme
import java.io.File

class MediaActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val media = intent?.getParcelableExtra<Media>("media")
        enableEdgeToEdge()
        setContent {
            GalTestTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (media?.type == Media.VIDEO) {
                        VideoPlayer(media.filePath ?: "", Modifier.padding(innerPadding))
                    }
                    AsyncImage(
                        model = File(media?.filePath),
                        contentDescription = null,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(innerPadding)
                            .fillMaxSize()
                    )
                }
            }
        }
    }
}

@Composable
fun VideoPlayer(
    filePath: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val videoUri = remember(filePath) {
        Uri.fromFile(File(filePath)) // 👈 Convert file path to Uri
    }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(videoUri))
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(context).apply {
                player = exoPlayer
                useController = true
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
            }
        }
    )
}
