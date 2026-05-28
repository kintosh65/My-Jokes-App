package com.kintosh.myjokesapp.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.BlockThreshold
import com.google.ai.client.generativeai.type.HarmCategory
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.SafetySetting
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kintosh.myjokesapp.UserManager
import com.kintosh.myjokesapp.viewmodel.LiveJokeViewModel
import kotlinx.coroutines.launch

data class Joke(
    val setup: String = "", 
    val punchline: String = "",
    val category: String = "Misc",
    val is18Plus: Boolean = false,
    val lang: String = "en"
)

fun loadLocalJokes(context: Context): List<Joke> {
    return try {
        val json = context.assets.open("jokes.json")
            .bufferedReader().use { it.readText() }
        val gson = Gson()
        val type = object : TypeToken<List<Joke>>() {}.type
        gson.fromJson(json, type)
    } catch (e: Exception) {
        emptyList()
    }
}

fun shareJoke(context: Context, joke: Joke) {
    val sendIntent: Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, "${joke.setup}\n\n${joke.punchline}\n\nShared via Kadafa Jokes! 😂")
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, "Share this laugh!")
    context.startActivity(shareIntent)
}

enum class ViewType { FEED, FAVORITES, RECENT }
enum class CardType { NORMAL, LIVE, FEATURED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: LiveJokeViewModel = viewModel()
    val liveJoke by viewModel.joke.collectAsState()
    val remoteResults by viewModel.searchResults.collectAsState()
    val isSearchingRemote by viewModel.isSearching.collectAsState()
    
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    
    var loggedInUser by remember { mutableStateOf(UserManager.getLoggedInUser(context) ?: "Jester") }
    val allJokes = remember { loadLocalJokes(context) }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchExpanded by remember { mutableStateOf(false) }

    // AI States
    var showAiDialog by remember { mutableStateOf(false) }
    var aiTargetJoke by remember { mutableStateOf<Joke?>(null) }

    // Trigger remote search when query changes
    LaunchedEffect(searchQuery) {
        viewModel.searchRemoteJokes(searchQuery)
    }
    
    var expandedJokeId by remember { mutableIntStateOf(-1) }
    var favorites by remember { mutableStateOf(setOf<Int>()) }
    var recentJokes by remember { mutableStateOf(listOf<Joke>()) }
    var currentView by remember { mutableStateOf(ViewType.FEED) }

    // Drawer Expansion States
    var categoriesExpanded by remember { mutableStateOf(true) }
    var laughsExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    var showProfileDialog by remember { mutableStateOf(false) }

    val categories = listOf(
        "All" to Icons.Default.Home,
        "One-liners" to Icons.Default.Star,
        "Puns" to Icons.Default.Face,
        "Knock-knock jokes" to Icons.Default.Notifications,
        "Dad jokes" to Icons.Default.Person,
        "Dark humor" to Icons.Default.Warning,
        "Observational humor" to Icons.Default.Info,
        "Slapstick" to Icons.Default.Build,
        "Anecdotal jokes" to Icons.Default.Edit,
        "Satire" to Icons.Default.Share,
        "Sarcasm" to Icons.Default.ThumbUp,
        "Absurd / surreal humor" to Icons.Default.Refresh,
        "Riddles" to Icons.Default.Search
    )

    // Advanced Filtering Logic
    val filteredJokes = remember(selectedCategory, searchQuery, currentView, allJokes, favorites, recentJokes) {
        val baseList = when (currentView) {
            ViewType.FEED -> if (selectedCategory == "All") allJokes else allJokes.filter { it.category == selectedCategory }
            ViewType.FAVORITES -> allJokes.filter { favorites.contains(it.hashCode()) }
            ViewType.RECENT -> recentJokes
        }
        
        val list = if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter { 
                it.setup.contains(searchQuery, ignoreCase = true) || 
                it.punchline.contains(searchQuery, ignoreCase = true) 
            }
        }
        
        // Stable shuffle to prevent reshuffling on every interaction
        if (currentView == ViewType.FEED && searchQuery.isBlank()) {
            list.shuffled(java.util.Random(selectedCategory.hashCode().toLong()))
        } else {
            list
        }
    }

    fun addToRecent(joke: Joke) {
        recentJokes = (listOf(joke) + recentJokes.filter { it.hashCode() != joke.hashCode() }).take(10)
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp),
                drawerContainerColor = MaterialTheme.colorScheme.surface,
            ) {
                // Drawer Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(24.dp)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = loggedInUser.take(1).uppercase(),
                                color = MaterialTheme.colorScheme.onPrimary,
                                style = MaterialTheme.typography.headlineMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = loggedInUser,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = "Level: Comedy Legend 🔥",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    item {
                        DrawerSectionHeader(
                            title = "Categories",
                            isExpanded = categoriesExpanded,
                            onToggle = { categoriesExpanded = !categoriesExpanded },
                            icon = Icons.AutoMirrored.Filled.List
                        )
                    }
                    if (categoriesExpanded) {
                        itemsIndexed(categories) { _, pair ->
                            NavigationDrawerItem(
                                label = { Text(pair.first) },
                                icon = { Icon(pair.second, contentDescription = null) },
                                selected = currentView == ViewType.FEED && selectedCategory == pair.first,
                                onClick = {
                                    currentView = ViewType.FEED
                                    selectedCategory = pair.first
                                    expandedJokeId = -1
                                    searchQuery = ""
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    item {
                        DrawerSectionHeader(
                            title = "My Laughs",
                            isExpanded = laughsExpanded,
                            onToggle = { laughsExpanded = !laughsExpanded },
                            icon = Icons.Default.Favorite
                        )
                    }
                    if (laughsExpanded) {
                        item {
                            NavigationDrawerItem(
                                label = { Text("Favorites") },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                                selected = currentView == ViewType.FAVORITES,
                                onClick = {
                                    currentView = ViewType.FAVORITES
                                    searchQuery = ""
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                        item {
                            NavigationDrawerItem(
                                label = { Text("Recent Hits") },
                                icon = { Icon(Icons.Default.History, contentDescription = null) },
                                selected = currentView == ViewType.RECENT,
                                onClick = {
                                    currentView = ViewType.RECENT
                                    searchQuery = ""
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    item {
                        DrawerSectionHeader(
                            title = "Settings & Account",
                            isExpanded = settingsExpanded,
                            onToggle = { settingsExpanded = !settingsExpanded },
                            icon = Icons.Default.Settings
                        )
                    }
                    if (settingsExpanded) {
                        item {
                            ListItem(
                                headlineContent = { Text("Dark Mode") },
                                leadingContent = { Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.LightMode, null) },
                                trailingContent = {
                                    Switch(checked = isDarkMode, onCheckedChange = { onThemeToggle() })
                                },
                                modifier = Modifier.clip(RoundedCornerShape(12.dp))
                            )
                        }
                        item {
                            NavigationDrawerItem(
                                label = { Text("Profile Settings") },
                                icon = { Icon(Icons.Default.Person, contentDescription = null) },
                                selected = false,
                                onClick = { showProfileDialog = true },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))

                NavigationDrawerItem(
                    label = { Text("Log Out") },
                    icon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null) },
                    selected = false,
                    onClick = {
                        UserManager.logout(context)
                        onLogout()
                    },
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        if (isSearchExpanded) {
                            TextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                placeholder = { Text("Search laughs...") },
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                trailingIcon = {
                                    IconButton(onClick = { 
                                        searchQuery = ""
                                        isSearchExpanded = false 
                                    }) {
                                        Icon(Icons.Default.Close, null)
                                    }
                                }
                            )
                        } else {
                            Text(
                                when(currentView) {
                                    ViewType.FEED -> if(selectedCategory == "All") "Kadafa Jokes" else selectedCategory
                                    ViewType.FAVORITES -> "Favorites ❤️"
                                    ViewType.RECENT -> "Recent Hits 🔥"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        if (!isSearchExpanded) {
                            IconButton(onClick = { isSearchExpanded = true }) {
                                Icon(Icons.Default.Search, contentDescription = "Search")
                            }
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (currentView == ViewType.FEED && searchQuery.isEmpty()) {
                    FloatingActionButton(
                        onClick = { 
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.fetchJoke(selectedCategory) 
                        },
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(16.dp).shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            }
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                AnimatedGradientBackground()

                Column(modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp)) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp)
                    ) {
                        // Show Live Joke only in Feed and if not searching
                        if (currentView == ViewType.FEED && searchQuery.isEmpty() && (selectedCategory == "All" || liveJoke?.category == selectedCategory)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader("Live Discovery 🌐")
                                    IconButton(
                                        onClick = { 
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            viewModel.fetchJoke(selectedCategory) 
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Refresh,
                                            contentDescription = "Refresh",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                if (liveJoke == null) {
                                    LoadingCard()
                                } else {
                                    liveJoke?.let {
                                        val isSafe = it.safe
                                        val is18 = !isSafe || (it.flags?.nsfw == true || it.flags?.explicit == true)
                                        JokeCard(
                                            joke = Joke(
                                                it.setup ?: "Thinking...", 
                                                it.punchline ?: "Wait for it...",
                                                it.category,
                                                is18Plus = is18,
                                                lang = it.lang
                                            ),
                                            isExpanded = expandedJokeId == it.hashCode(),
                                            onClick = { 
                                                expandedJokeId = if (expandedJokeId == it.hashCode()) -1 else it.hashCode()
                                                addToRecent(Joke(it.setup ?: "", it.punchline ?: "", it.category, is18, it.lang)) 
                                            },
                                            isFavorite = favorites.contains(it.hashCode()),
                                            onFavoriteClick = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                val id = it.hashCode()
                                                favorites = if (favorites.contains(id)) favorites - id else favorites + id
                                            },
                                            onShareClick = { shareJoke(context, Joke(it.setup ?: "", it.punchline ?: "", it.category, is18, it.lang)) },
                                            onAiClick = {
                                                aiTargetJoke = Joke(it.setup ?: "", it.punchline ?: "", it.category, is18, it.lang)
                                                showAiDialog = true
                                            },
                                            cardType = CardType.LIVE
                                        )
                                    }
                                }
                            }
                        }

                        if (filteredJokes.isNotEmpty()) {
                            val title = when(currentView) {
                                ViewType.FEED -> if (searchQuery.isNotEmpty()) "Local Matches 📚" else "$selectedCategory Archive 📚"
                                ViewType.FAVORITES -> "Liked Laughs ❤️"
                                ViewType.RECENT -> "Last Laughed 🔥"
                            }
                            item { SectionHeader(title) }

                            itemsIndexed(filteredJokes) { _, joke ->
                                JokeCard(
                                    joke = joke,
                                    isExpanded = expandedJokeId == joke.hashCode(),
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expandedJokeId = if (expandedJokeId == joke.hashCode()) -1 else joke.hashCode()
                                        if (expandedJokeId != -1) addToRecent(joke)
                                    },
                                    isFavorite = favorites.contains(joke.hashCode()),
                                    onFavoriteClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        val id = joke.hashCode()
                                        favorites = if (favorites.contains(id))
                                            favorites - id else favorites + id
                                    },
                                    onShareClick = { shareJoke(context, joke) },
                                    onAiClick = {
                                        aiTargetJoke = joke
                                        showAiDialog = true
                                    },
                                    cardType = CardType.NORMAL
                                )
                            }
                        }

                        // 🌐 Remote Search Results
                        if (searchQuery.isNotEmpty()) {
                            if (isSearchingRemote) {
                                item {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                        Text("Searching the web...", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                                    }
                                }
                            } else if (remoteResults.isNotEmpty()) {
                                item { SectionHeader("Online Discoveries 🌐") }
                                itemsIndexed(remoteResults) { _, remoteJoke ->
                                    val isSafe = remoteJoke.safe
                                    val is18 = !isSafe || (remoteJoke.flags?.nsfw == true || remoteJoke.flags?.explicit == true)
                                    val joke = Joke(remoteJoke.setup ?: "", remoteJoke.punchline ?: "", remoteJoke.category, is18, remoteJoke.lang)
                                    JokeCard(
                                        joke = joke,
                                        isExpanded = expandedJokeId == joke.hashCode(),
                                        onClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            expandedJokeId = if (expandedJokeId == joke.hashCode()) -1 else joke.hashCode()
                                            if (expandedJokeId != -1) addToRecent(joke)
                                        },
                                        isFavorite = favorites.contains(joke.hashCode()),
                                        onFavoriteClick = {
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            val id = joke.hashCode()
                                            favorites = if (favorites.contains(id)) favorites - id else favorites + id
                                        },
                                        onShareClick = { shareJoke(context, joke) },
                                        onAiClick = {
                                            aiTargetJoke = joke
                                            showAiDialog = true
                                        },
                                        cardType = CardType.LIVE
                                    )
                                }
                            }
                        }

                        if (filteredJokes.isEmpty() && remoteResults.isEmpty() && !isSearchingRemote) {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    val msg = if (searchQuery.isNotEmpty()) {
                                        "No laughs found for \"$searchQuery\" in English or Kiswahili 💨"
                                    } else {
                                        when(currentView) {
                                            ViewType.FEED -> "No $selectedCategory yet! 🙊"
                                            ViewType.FAVORITES -> "No favorites yet! Go find some laughs ❤️"
                                            ViewType.RECENT -> "No recent jokes! Start exploring 🔥"
                                        }
                                    }
                                    Text(msg, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f), textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        ProfileSettingsDialog(
            currentName = loggedInUser,
            onDismiss = { showProfileDialog = false },
            onUpdate = { newName, newPass ->
                if (UserManager.updateProfile(context, newName, newPass)) {
                    loggedInUser = newName
                    showProfileDialog = false
                    Toast.makeText(context, "Profile Updated! ✨", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Username already exists! 🤡", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    if (showAiDialog && aiTargetJoke != null) {
        AiJokeExplainerDialog(
            joke = aiTargetJoke!!,
            onDismiss = { 
                showAiDialog = false
                aiTargetJoke = null
            }
        )
    }
}

@Composable
fun DrawerSectionHeader(
    title: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    icon: ImageVector
) {
    Surface(
        onClick = onToggle,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Icon(
                if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun ProfileSettingsDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onUpdate: (String, String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    var newPassword by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Profile Settings 👤", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = newName,
                    onValueChange = { newName = it },
                    label = { Text("Stage Name") },
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(12.dp))
                
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text("New Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Button(
                        onClick = { onUpdate(newName, newPassword) },
                        enabled = newName.isNotBlank() && newPassword.length >= 4
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

@Composable
fun AiJokeExplainerDialog(
    joke: Joke,
    onDismiss: () -> Unit
) {
    var explanation by remember { mutableStateOf("Thinking... 🤔") }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(joke) {
        try {
            val generativeModel = GenerativeModel(
                modelName = "gemini-1.0-pro",
                apiKey = "AIzaSyDyNHjsr8CkzwbESGdF2lGHMEiGqpSXh1k"
            )
            val prompt = "Explain the humor in this joke, specifically for someone who might be learning the language or culture. " +
                        "If the joke is in Kiswahili, explain it in English. Joke Setup: ${joke.setup}. Joke Punchline: ${joke.punchline}"
            
            val response = generativeModel.generateContent(prompt)
            explanation = response.text ?: "I couldn't quite grasp the humor here. 🤡"
        } catch (e: Exception) {
            Log.e("AiExplainer", "Error explaining joke", e)
            explanation = "AI is currently backstage! 🎭 Please try again later or check your internet connection."
        } finally {
            isLoading = false
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AutoAwesome, null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("AI Explainer", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(joke.setup, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                Text(joke.punchline, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                
                HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
                
                if (isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                
                Text(
                    text = explanation,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Got it!")
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
        modifier = Modifier.padding(bottom = 8.dp, start = 8.dp)
    )
}

@Composable
fun JokeCard(
    joke: Joke,
    isExpanded: Boolean,
    onClick: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onShareClick: () -> Unit,
    onAiClick: () -> Unit,
    cardType: CardType
) {
    val containerColor = when (cardType) {
        CardType.FEATURED -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
        CardType.LIVE -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.95f)
        CardType.NORMAL -> MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
    }

    val contentColor = when (cardType) {
        CardType.FEATURED -> MaterialTheme.colorScheme.onPrimaryContainer
        CardType.LIVE -> MaterialTheme.colorScheme.onSecondaryContainer
        CardType.NORMAL -> MaterialTheme.colorScheme.onSurface
    }

    val accentColor = when (cardType) {
        CardType.FEATURED -> MaterialTheme.colorScheme.primary
        CardType.LIVE -> MaterialTheme.colorScheme.secondary
        CardType.NORMAL -> MaterialTheme.colorScheme.secondary
    }

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(if (isExpanded) 16.dp else 4.dp, RoundedCornerShape(20.dp))
            .border(1.dp, accentColor.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
            .animateContentSize(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = joke.category.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (cardType == CardType.NORMAL) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f),
                            fontWeight = FontWeight.ExtraBold
                        )
                        if (joke.is18Plus) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                color = Color.Red,
                                shape = CircleShape,
                                modifier = Modifier.size(18.dp)
                            ) {
                                Text(
                                    "18+", 
                                    fontSize = 8.sp, 
                                    color = Color.White, 
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = joke.setup,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
                    )
                }
                
                Row {
                    IconButton(onClick = onAiClick) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = "AI Explain",
                            tint = contentColor.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onShareClick) {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = contentColor.copy(alpha = 0.5f)
                        )
                    }
                    IconButton(onClick = onFavoriteClick) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite",
                            tint = if (isFavorite) Color(0xFFE91E63) else contentColor.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(16.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(contentColor.copy(alpha = 0.05f))
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "👉 ${joke.punchline}",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                color = contentColor.copy(alpha = 0.9f)
                            )
                        )
                    }
                    LottieEmojiLaugh()
                }
            }
        }
    }
}

@Composable
fun LoadingCard() {
    Card(
        modifier = Modifier.fillMaxWidth().height(100.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
    ) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
                modifier = Modifier.size(32.dp)
            )
        }
    }
}

@Composable
fun LottieEmojiLaugh() {
    val composition by rememberLottieComposition(LottieCompositionSpec.Asset("laugh_emoji.json"))
    val progress by animateLottieCompositionAsState(composition, iterations = 1)
    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = Modifier.height(70.dp).padding(top = 12.dp).fillMaxWidth(),
        alignment = Alignment.Center
    )
}

@Composable
fun AnimatedGradientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    
    val color1 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.background,
        targetValue = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
        animationSpec = infiniteRepeatable(tween(8000), RepeatMode.Reverse),
        label = "c1"
    )
    val color2 by infiniteTransition.animateColor(
        initialValue = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.1f),
        targetValue = MaterialTheme.colorScheme.background,
        animationSpec = infiniteRepeatable(tween(10000, easing = LinearEasing), RepeatMode.Reverse),
        label = "c2"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(color1, color2)))
    )
}
