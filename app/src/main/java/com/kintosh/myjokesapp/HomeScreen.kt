package com.kintosh.myjokesapp

import android.content.Context
import androidx.compose.animation.animateColor
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kintosh.myjokesapp.viewmodel.LiveJokeViewModel

data class Joke(val setup: String, val punchline: String)

fun loadLocalJokes(context: Context): List<Joke> {
    return try {
        val json = context.assets.open("jokes.json")
            .bufferedReader().use { it.readText() }

        val gson = Gson()
        val type = object : TypeToken<List<Joke>>() {}.type
        gson.fromJson(json, type)
    } catch (e: Exception) {
        e.printStackTrace()
        emptyList()
    }
}

@Composable
fun HomeScreen() {
    val viewModel: LiveJokeViewModel = viewModel()
    val liveJoke by viewModel.joke.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.fetchJoke()
    }

    val context = LocalContext.current
    val jokes = remember { loadLocalJokes(context).shuffled() }
    val jokeOfTheDay = remember { jokes.firstOrNull() }
    var expandedIndex by remember { mutableStateOf(-1) }
    var favorites by remember { mutableStateOf(setOf<Int>()) }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedGradientBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "🤣 Tap a Joke to Reveal the Punchline!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 🌟 Joke of the Day (Local)
                item {
                    jokeOfTheDay?.let {
                        JokeCard(
                            joke = it,
                            isExpanded = true,
                            onClick = {},
                            isFavorite = false,
                            onFavoriteClick = {},
                            isJokeOfTheDay = true
                        )
                    }
                }

                // 🌐 Live Joke from API
                item {
                    liveJoke?.let {
                        println("🆕 Live joke: ${it.setup} - ${it.punchline}")
                        JokeCard(
                            joke = Joke(it.setup, it.punchline),
                            isExpanded = true,
                            onClick = {},
                            isFavorite = false,
                            onFavoriteClick = {},
                            isJokeOfTheDay = false
                        )
                    }
                }

                // 🔁 Refresh Button
                item {
                    Button(
                        onClick = { viewModel.fetchJoke() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Text("Refresh Live Joke 🌐")
                    }
                }

                // 📚 Rest of the Jokes
                itemsIndexed(jokes) { index, joke ->
                    JokeCard(
                        joke = joke,
                        isExpanded = expandedIndex == index,
                        onClick = {
                            expandedIndex = if (expandedIndex == index) -1 else index
                        },
                        isFavorite = favorites.contains(index),
                        onFavoriteClick = {
                            favorites = if (favorites.contains(index))
                                favorites - index else favorites + index
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun JokeCard(
    joke: Joke,
    isExpanded: Boolean,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    isJokeOfTheDay: Boolean = false
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .padding(horizontal = 4.dp),
        elevation = CardDefaults.cardElevation(6.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isJokeOfTheDay) Color(0xFFD1C4E9) else Color.White.copy(alpha = 0.95f)
        )
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isJokeOfTheDay) "🌟 Joke of the Day: ${joke.setup}" else joke.setup,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onFavoriteClick) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            if (isExpanded || isJokeOfTheDay) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "💥 ${joke.punchline}",
                    fontSize = 15.sp,
                    color = Color(0xFF5D4037)
                )

                LottieEmojiLaugh()
            }
        }
    }
}

@Composable
fun LottieEmojiLaugh() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("laugh_emoji.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = 1)
    LottieAnimation(
        composition,
        progress,
        modifier = Modifier
            .height(60.dp)
            .padding(top = 8.dp)
    )
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "backgroundAnim")
    val color by infiniteTransition.animateColor(
        initialValue = Color(0xFFFFF9C4),
        targetValue = Color(0xFFF8BBD0),
        animationSpec = infiniteRepeatable(
            animation = tween(6000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgColor"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
    )
}
