package com.movixa.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Night = Color(0xFF090D18)
private val Panel = Color(0xFF151D2D)
private val Aqua = Color(0xFF42C8E8)

data class Movie(val title: String, val genre: String, val year: String, val description: String)

private val demoMovies = listOf(
    Movie("Beyond the Blue", "Adventure", "2025", "A fictional journey beyond the edge of the known ocean."),
    Movie("Midnight Signal", "Thriller", "2024", "A mysterious signal changes one quiet night."),
    Movie("Orbit Seven", "Sci-Fi", "2025", "Seven explorers search for a new home among the stars."),
    Movie("The Last Summer", "Drama", "2023", "A story about friendship, family, and a summer to remember.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MovixaApp() }
    }
}

@Composable
fun MovixaApp() {
    var selected by remember { mutableStateOf<Movie?>(null) }
    var query by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("All") }
    val categories = listOf("All", "Adventure", "Thriller", "Sci-Fi", "Drama")
    val visible = demoMovies.filter {
        (category == "All" || it.genre == category) &&
        it.title.contains(query, ignoreCase = true)
    }

    MaterialTheme(colorScheme = darkColorScheme(
        background = Night, surface = Panel, primary = Aqua,
        onBackground = Color.White, onSurface = Color.White
    )) {
        Surface(Modifier.fillMaxSize(), color = Night) {
            if (selected == null) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp),
                    contentPadding = PaddingValues(vertical = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Text("MOVIXA", color = Aqua, fontSize = 30.sp, fontWeight = FontWeight.Black)
                        Text("YOUR CINEMA. YOUR WORLD.", color = Color.LightGray, fontSize = 11.sp)
                        Spacer(Modifier.height(22.dp))
                        Text("Discover", fontSize = 26.sp, fontWeight = FontWeight.Bold)
                        Text("Find your next movie night.", color = Color.LightGray)
                    }
                    item {
                        OutlinedTextField(
                            value = query, onValueChange = { query = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search titles...") },
                            singleLine = true
                        )
                    }
                    item {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.forEach { item ->
                                FilterChip(
                                    selected = category == item,
                                    onClick = { category = item },
                                    label = { Text(item) }
                                )
                            }
                        }
                    }
                    items(visible) { movie ->
                        MovieCard(movie) { selected = movie }
                    }
                    item {
                        Text("Demo catalog only • Add licensed content sources to enable playback.",
                            color = Color.Gray, fontSize = 11.sp)
                    }
                }
            } else {
                Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("← Back", color = Aqua, modifier = Modifier.clickable { selected = null })
                    Text(selected!!.title, fontSize = 30.sp, fontWeight = FontWeight.Bold)
                    Text("${selected!!.genre}  •  ${selected!!.year}", color = Aqua)
                    Text(selected!!.description, color = Color.LightGray)
                    Button(onClick = { }) { Text("Putar demo") }
                    Text("Pemutar video memerlukan URL video demo atau konten yang kamu punya izinnya.",
                        color = Color.Gray, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun MovieCard(movie: Movie, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(width = 76.dp, height = 100.dp)
                .background(Color(0xFF24344D), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) { Text("▶", color = Aqua, fontSize = 26.sp) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(movie.title, fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("${movie.genre} • ${movie.year}", color = Aqua, fontSize = 12.sp)
            Text(movie.description, color = Color.LightGray, fontSize = 12.sp, maxLines = 2)
        }
    }
}
