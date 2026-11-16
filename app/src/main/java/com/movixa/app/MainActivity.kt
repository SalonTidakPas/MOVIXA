
package com.movixa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

private val Night = Color(0xFF090D18)
private val Aqua = Color(0xFF42C8E8)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MovixaPlayerApp()
        }
    }
}

@Composable
fun MovixaPlayerApp() {
    var playing by remember { mutableStateOf(false) }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Night,
            surface = Color(0xFF151D2D),
            primary = Aqua
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Night
        ) {
            if (!playing) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        "MOVIXA",
                        color = Aqua,
                        fontSize = 36.sp
                    )

                    Text(
                        "YOUR CINEMA. YOUR WORLD.",
                        color = Color.LightGray
                    )

                    Spacer(Modifier.height(32.dp))

                    Button(
                        onClick = { playing = true }
                    ) {
                        Text("▶  PUTAR VIDEO")
                    }
                }
            } else {
                VideoPlayerScreen(
                    onBack = { playing = false }
                )
            }
        }
    }
}

@Composable
fun VideoPlayerScreen(onBack: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(
                MediaItem.fromUri(
                    "https://storage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                )
            )
            prepare()
            playWhenReady = true
        }
    }

    DisposableEffect(player) {
        onDispose {
            player.release()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Text(
            "← KEMBALI",
            color = Aqua,
            modifier = Modifier
                .clickable { onBack() }
                .padding(16.dp)
        )

        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = true
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        )

        Text(
            "MOVIXA PLAYER",
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}
