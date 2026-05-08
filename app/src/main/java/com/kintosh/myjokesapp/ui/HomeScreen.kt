package com.kintosh.myjokesapp.ui

import android.content.Context
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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.airbnb.lottie.compose.*
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kintosh.myjokesapp.UserManager
import com.kintosh.myjokesapp.viewmodel.LiveJokeViewModel
import kotlinx.coroutines.launch

data class Joke(val setup: String, val punchline: String, val category: String = "Misc")

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

enum class ViewType { FEED, FAVORITES, RECENT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    isDarkMode: Boolean,
    onThemeToggle: () -> Unit,
    onLogout: () -> Unit
) {
    val viewModel: LiveJokeViewModel = viewModel()
    val liveJoke by viewModel.joke.collectAsState()
    val context = LocalContext.current
    var loggedInUser by remember { mutableStateOf(UserManager.getLoggedInUser(context) ?: "Jester") }
    val allJokes = remember { loadLocalJokes(context).shuffled() }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    
    var selectedCategory by remember { mutableStateOf("All") }
    var expandedIndex by remember { mutableIntStateOf(-1) }
    var favorites by remember { mutableStateOf(setOf<Int>()) }
    var recentJokes by remember { mutableStateOf(listOf<Joke>()) }
    var currentView by remember { mutableStateOf(ViewType.FEED) }

    // Drawer Expansion States
    var categoriesExpanded by remember { mutableStateOf(true) }
    var laughsExpanded by remember { mutableStateOf(false) }
    var settingsExpanded by remember { mutableStateOf(false) }

    // Profile Dialog State
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

    val jokesToShow = when (currentView) {
        ViewType.FEED -> if (selectedCategory == "All") allJokes else allJokes.filter { it.category == selectedCategory }
        ViewType.FAVORITES -> allJokes.filter { favorites.contains(it.hashCode()) }
        ViewType.RECENT -> recentJokes
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
                // 1. Drawer Header
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
                            text = "Level: Rookie Prankster 🤡",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }

                LazyColumn(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                    // 🌈 Section: Categories
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
                                    expandedIndex = -1
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // ❤️ Section: My Laughs
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
                                    scope.launch { drawerState.close() }
                                },
                                modifier = Modifier.padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
                    }

                    // ⚙️ Section: Settings & Account
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
                                onClick = {
                                    showProfileDialog = true
                                },
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
                        Text(
                            when(currentView) {
                                ViewType.FEED -> if(selectedCategory == "All") "Kadafa Jokes" else selectedCategory
                                ViewType.FAVORITES -> "Favorites ❤️"
                                ViewType.RECENT -> "Recent Hits 🔥"
                            },
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent,
            floatingActionButton = {
                if (currentView == ViewType.FEED) {
                    FloatingActionButton(
                        onClick = { viewModel.fetchJoke(selectedCategory) },
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
                        // Show Live Joke only in Feed
                        if (currentView == ViewType.FEED && (selectedCategory == "All" || liveJoke?.category == selectedCategory)) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(end = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    SectionHeader("Live Discovery 🌐")
                                    IconButton(
                                        onClick = { viewModel.fetchJoke(selectedCategory) },
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
                                        JokeCard(
                                            joke = Joke(
                                                it.setup ?: "Thinking...", 
                                                it.punchline ?: "Wait for it...",
                                                it.category
                                            ),
                                            isExpanded = true,
                                            onClick = { addToRecent(Joke(it.setup ?: "", it.punchline ?: "", it.category)) },
                                            isFavorite = favorites.contains(it.hashCode()),
                                            onFavoriteClick = {
                                                val id = it.hashCode()
                                                favorites = if (favorites.contains(id)) favorites - id else favorites + id
                                            },
                                            cardType = CardType.LIVE
                                        )
                                    }
                                }
                            }
                        }

                        if (jokesToShow.isNotEmpty()) {
                            val title = when(currentView) {
                                ViewType.FEED -> "$selectedCategory Archive 📚"
                                ViewType.FAVORITES -> "Liked Laughs ❤️"
                                ViewType.RECENT -> "Last Laughed 🔥"
                            }
                            item { SectionHeader(title) }

                            itemsIndexed(jokesToShow) { index, joke ->
                                JokeCard(
                                    joke = joke,
                                    isExpanded = expandedIndex == index,
                                    onClick = {
                                        expandedIndex = if (expandedIndex == index) -1 else index
                                        if (expandedIndex != -1) addToRecent(joke)
                                    },
                                    isFavorite = favorites.contains(joke.hashCode()),
                                    onFavoriteClick = {
                                        val id = joke.hashCode()
                                        favorites = if (favorites.contains(id))
                                            favorites - id else favorites + id
                                    },
                                    cardType = CardType.NORMAL
                                )
                            }
                        } else {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                    val msg = when(currentView) {
                                        ViewType.FEED -> "No $selectedCategory yet! 🙊"
                                        ViewType.FAVORITES -> "No favorites yet! Go find some laughs ❤️"
                                        ViewType.RECENT -> "No recent jokes! Start exploring 🔥"
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
fun CategoryChip(category: String, isSelected: Boolean, onSelected: () -> Unit) {
    Surface(
        modifier = Modifier.clickable { onSelected() },
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
        border = if (isSelected) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
    ) {
        Text(
            text = category,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            style = MaterialTheme.typography.labelLarge,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
        )
    }
}

enum class CardType { NORMAL, LIVE, FEATURED }

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
                    Text(
                        text = joke.category.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (cardType == CardType.NORMAL) MaterialTheme.colorScheme.primary else contentColor.copy(alpha = 0.8f),
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = joke.setup,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            lineHeight = 24.sp
                        )
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
