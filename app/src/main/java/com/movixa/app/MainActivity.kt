
package com.movixa.app

import android.os.Bundle
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import android.content.Context
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private val Night = Color(0xFF090D18)
private val Panel = Color(0xFF151D2D)
private val Aqua = Color(0xFF42C8E8)

private const val DEMO_URL =
    "https://interactive-examples.mdn.mozilla.net/media/cc0-videos/flower.mp4"

data class Movie(
    val title: String,
    val genre: String,
    val year: String,
    val description: String,
    val videoUrl: String,
    val posterUrl: String = ""
)

data class ExtensionInfo(
    val id: String,
    val name: String,
    val version: String,
    val description: String
)

private val demoMovies = listOf(
    Movie("Beyond the Blue", "Adventure", "2025",
        "A fictional journey beyond the edge of the known ocean.", DEMO_URL),
    Movie("Midnight Signal", "Thriller", "2024",
        "A mysterious signal changes one quiet night.", DEMO_URL),
    Movie("Orbit Seven", "Sci-Fi", "2025",
        "Seven explorers search for a new home among the stars.", DEMO_URL),
    Movie("The Last Summer", "Drama", "2023",
        "A story about friendship, family, and a summer to remember.", DEMO_URL)
)

private fun fetchRepositoryMovies(repository: String): List<Movie> {
    val connection =
        URL(repository).openConnection() as HttpURLConnection

    try {
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.useCaches = false

        val response = connection.responseCode

        if (response !in 200..299) {
            throw Exception("HTTP $response")
        }

        val body = connection.inputStream
            .bufferedReader()
            .use { it.readText() }

        val json = JSONObject(body)
        val extensions = json.optJSONArray("extensions")
            ?: return emptyList()

        val movies = mutableListOf<Movie>()

        for (i in 0 until extensions.length()) {
            val extension = extensions.optJSONObject(i)
                ?: continue

            val movieArray = extension.optJSONArray("movies")
                ?: continue

            for (j in 0 until movieArray.length()) {
                val item = movieArray.optJSONObject(j)
                    ?: continue

                val title = item.optString("title")
                val videoUrl = item.optString("videoUrl")

                if (title.isNotBlank() && videoUrl.isNotBlank()) {
                    movies.add(
                        Movie(
                            title = title,
                            genre = item.optString("genre", "Other"),
year = item.optString("year", ""),
description = item.optString("description", ""),
videoUrl = videoUrl,
posterUrl = item.optString("posterUrl")
                        )
                    )
                }
            }
        }

        return movies

    } finally {
        connection.disconnect()
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MovixaApp() }
    }
}

@Composable
fun MovixaApp() {
    val context = LocalContext.current

    val preferences = remember {
        context.getSharedPreferences("movixa_settings", Context.MODE_PRIVATE)
    }

    val repositories = remember {
        mutableStateListOf<String>().apply {
            addAll(
                preferences.getStringSet("repositories", emptySet())
                    ?: emptySet()
            )
        }
    }

    var selected by remember { mutableStateOf<Movie?>(null) }
    var playingMovie by remember { mutableStateOf<Movie?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    var showExtensions by remember { mutableStateOf(false) }

    var extensionMovies by remember {
        mutableStateOf<List<Movie>>(emptyList())
    }

    var catalogStatus by remember {
        mutableStateOf("Belum memuat katalog extension")
    }

    fun saveRepositories() {
        preferences.edit()
            .putStringSet("repositories", repositories.toSet())
            .apply()
    }

    LaunchedEffect(repositories.toList()) {
        if (repositories.isEmpty()) {
            extensionMovies = emptyList()
            catalogStatus = "Tambahkan repository untuk memuat katalog"
        } else {
            catalogStatus = "Memuat katalog extension..."

            val loaded = withContext(Dispatchers.IO) {
                repositories.flatMap { repository ->
                    runCatching {
                        fetchRepositoryMovies(repository)
                    }.getOrDefault(emptyList())
                }
            }

            extensionMovies = loaded
            catalogStatus =
                "${loaded.size} film ditemukan dari ${repositories.size} repository"
        }
    }

    val allMovies = (demoMovies + extensionMovies)
        .distinctBy { it.title }

    val categories = (
        listOf("All", "Adventure", "Thriller", "Sci-Fi", "Drama", "Demo") +
            extensionMovies.map { it.genre }
        ).distinct()

    val visible = allMovies.filter {
        (category == "All" || it.genre == category) &&
            it.title.contains(query, ignoreCase = true)
    }

    MaterialTheme(
        colorScheme = darkColorScheme(
            background = Night,
            surface = Panel,
            primary = Aqua,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Night
        ) {
            when {
                playingMovie != null -> {
                    VideoPlayerScreen(
                        movie = playingMovie!!,
                        onBack = { playingMovie = null }
                    )
                }

                selected != null -> {
                    MovieDetailScreen(
                        movie = selected!!,
                        onBack = { selected = null },
                        onPlay = { playingMovie = selected }
                    )
                }

                showExtensions -> {
                    ExtensionsScreen(
                        repositories = repositories,
                        onBack = { showExtensions = false },
                        onAdd = { url ->
                            if (
                                (url.startsWith("https://") ||
                                    url.startsWith("http://")) &&
                                !repositories.contains(url)
                            ) {
                                repositories.add(url)
                                saveRepositories()
                            }
                        },
                        onRemove = { url ->
                            repositories.remove(url)
                            saveRepositories()
                        }
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                            .padding(horizontal = 20.dp),
                        contentPadding = PaddingValues(vertical = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        item {
                            Text(
                                "MOVIXA",
                                color = Aqua,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black
                            )

                            Text(
                                "YOUR CINEMA. YOUR WORLD.",
                                color = Color.LightGray,
                                fontSize = 11.sp
                            )

                            Spacer(Modifier.height(16.dp))

                            OutlinedButton(
                                onClick = { showExtensions = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🧩  EXTENSIONS & REPOSITORIES")
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                "Discover",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Find your next movie night.",
                                color = Color.LightGray
                            )

                            Text(
                                catalogStatus,
                                color = Aqua,
                                fontSize = 12.sp
                            )
                        }

                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Search titles...") },
                                singleLine = true
                            )
                        }

                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(categories) { item ->
                                    FilterChip(
                                        selected = category == item,
                                        onClick = { category = item },
                                        label = { Text(item) }
                                    )
                                }
                            }
                        }

                        items(visible, key = { it.title }) { movie ->
                            MovieCard(movie) {
                                selected = movie
                            }
                        }

                        item {
                            Text(
                                "MOVIXA • Extension catalog",
                                color = Color.Gray,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExtensionsScreen(
    repositories: List<String>,
    onBack: () -> Unit,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var url by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "← KEMBALI",
            color = Aqua,
            modifier = Modifier.clickable { onBack() }
        )

        Text(
            "EXTENSIONS",
            color = Aqua,
            fontSize = 28.sp,
            fontWeight = FontWeight.Black
        )

        Text(
            "Manage your content repositories",
            color = Color.LightGray
        )

        HorizontalDivider(color = Color.DarkGray)

        Text("ADD REPOSITORY", fontWeight = FontWeight.Bold)

        OutlinedTextField(
            value = url,
            onValueChange = {
                url = it
                error = ""
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://example.com/repository.json") },
            label = { Text("Repository URL") },
            singleLine = true
        )

        if (error.isNotEmpty()) {
            Text(error, color = Color.Red)
        }

        Button(
            onClick = {
                val clean = url.trim()

                if (
                    (clean.startsWith("https://") ||
                        clean.startsWith("http://")) &&
                    !repositories.contains(clean)
                ) {
                    onAdd(clean)
                    url = ""
                    error = ""
                } else {
                    error = "URL tidak valid atau sudah ditambahkan."
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("+ ADD REPOSITORY")
        }

        HorizontalDivider(color = Color.DarkGray)

        Text(
            "MY REPOSITORIES (${repositories.size})",
            fontWeight = FontWeight.Bold
        )

        if (repositories.isEmpty()) {
            Text("Belum ada repository.", color = Color.Gray)
        }

        repositories.forEach { repository ->
            RepositoryCard(
                repository = repository,
                onRemove = { onRemove(repository) }
            )
        }
    }
}

@Composable
fun RepositoryCard(
    repository: String,
    onRemove: () -> Unit
) {
    var loading by remember(repository) { mutableStateOf(true) }
    var repoName by remember(repository) { mutableStateOf("") }
    var extensions by remember(repository) {
        mutableStateOf<List<ExtensionInfo>>(emptyList())
    }
    var error by remember(repository) { mutableStateOf("") }

    LaunchedEffect(repository) {
        loading = true
        error = ""

        try {
            val result = withContext(Dispatchers.IO) {
                val connection =
                    URL(repository).openConnection() as HttpURLConnection

                try {
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 10000
                    connection.readTimeout = 10000
                    connection.useCaches = false

                    val response = connection.responseCode

                    if (response !in 200..299) {
                        throw Exception("HTTP $response")
                    }

                    val body = connection.inputStream
                        .bufferedReader()
                        .use { it.readText() }

                    val json = JSONObject(body)
                    val array = json.getJSONArray("extensions")

                    val list = mutableListOf<ExtensionInfo>()

                    for (i in 0 until array.length()) {
                        val item = array.getJSONObject(i)

                        list.add(
                            ExtensionInfo(
                                id = item.optString("id"),
                                name = item.optString("name"),
                                version = item.optString("version"),
                                description = item.optString("description")
                            )
                        )
                    }

                    Pair(
                        json.optString("name", "Unnamed Repository"),
                        list
                    )
                } finally {
                    connection.disconnect()
                }
            }

            repoName = result.first
            extensions = result.second

        } catch (e: Exception) {
            error = e.message ?: "Gagal membaca repository"
        }

        loading = false
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(14.dp))
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            repoName.ifEmpty { repository },
            color = Aqua,
            fontWeight = FontWeight.Bold
        )

        when {
            loading -> {
                Text("Mengambil data repository...", color = Color.Gray)
            }

            error.isNotEmpty() -> {
                Text("Gagal: $error", color = Color(0xFFFF7777))
            }

            else -> {
                Text(
                    "${extensions.size} extension ditemukan",
                    color = Color(0xFF75D99A)
                )

                extensions.forEach { extension ->
                    HorizontalDivider(color = Color.DarkGray)

                    Text(
                        extension.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )

                    Text(
                        "ID: ${extension.id} • Versi: ${extension.version}",
                        color = Aqua,
                        fontSize = 12.sp
                    )

                    Text(
                        extension.description,
                        color = Color.LightGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        TextButton(onClick = onRemove) {
            Text("HAPUS REPOSITORY", color = Color(0xFFFF7777))
        }
    }
}

@Composable
fun PosterImage(url: String, modifier: Modifier = Modifier) {
    val imageBitmap by produceState<androidx.compose.ui.graphics.ImageBitmap?>(
        initialValue = null,
        key1 = url
    ) {
        value = if (url.isBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    URL(url).openStream().use { stream ->
                        BitmapFactory.decodeStream(stream)?.asImageBitmap()
                    }
                }.getOrNull()
            }
        }
    }

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap!!,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier.background(
                Color(0xFF24344D),
                RoundedCornerShape(10.dp)
            ),
            contentAlignment = Alignment.Center
        ) {
            Text("▶", color = Aqua, fontSize = 26.sp)
        }
    }
}
@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Panel, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PosterImage(
    url = movie.posterUrl,
    modifier = Modifier
        .size(width = 110.dp, height = 150.dp)
        .clip(RoundedCornerShape(14.dp))
)

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                movie.title,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )
            Text(
                "${movie.genre} • ${movie.year}",
                color = Aqua,
                fontSize = 12.sp
            )
            Text(
                movie.description,
                color = Color.LightGray,
                fontSize = 12.sp,
                maxLines = 2
            )
        }
    }
}

@Composable
fun MovieDetailScreen(
    movie: Movie,
    onBack: () -> Unit,
    onPlay: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "← Back",
            color = Aqua,
            modifier = Modifier.clickable { onBack() }
        )

        Text(
            movie.title,
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            "${movie.genre} • ${movie.year}",
            color = Aqua
        )

        Text(movie.description, color = Color.LightGray)

        Button(
            onClick = onPlay,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("▶  PUTAR VIDEO")
        }

        Text(
            "Demo playback. Sumber film asli akan ditambahkan nanti.",
            color = Color.Gray,
            fontSize = 12.sp
        )
    }
}

@Composable
fun VideoPlayerScreen(
    movie: Movie,
    onBack: () -> Unit
) {
    val context = LocalContext.current

    val player = remember(movie.videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(movie.videoUrl))
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

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                    }
                },
                update = { view ->
                    view.player = player
                },
                modifier = Modifier.fillMaxSize()
            )
        }

        Text(
            movie.title,
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(16.dp)
        )
    }
}
