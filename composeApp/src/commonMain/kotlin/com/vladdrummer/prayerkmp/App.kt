package com.vladdrummer.prayerkmp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.toRoute
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vladdrummer.prayerkmp.feature.arrays.PrayerArraysRepository
import com.vladdrummer.prayerkmp.feature.navigation.PersonalData
import com.vladdrummer.prayerkmp.feature.navigation.PrayerNavigation
import com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen
import com.vladdrummer.prayerkmp.feature.navigation.PrayerScreen
import com.vladdrummer.prayerkmp.feature.navigation.Favorites
import com.vladdrummer.prayerkmp.feature.navigation.GospelReadings
import com.vladdrummer.prayerkmp.feature.navigation.BiblePrayerBridge
import com.vladdrummer.prayerkmp.feature.navigation.BibleReader
import com.vladdrummer.prayerkmp.feature.navigation.Psalter
import com.vladdrummer.prayerkmp.feature.navigation.PsalterAfterPrayer
import com.vladdrummer.prayerkmp.feature.navigation.PsalterBeforePrayer
import com.vladdrummer.prayerkmp.feature.navigation.PsalterReader
import com.vladdrummer.prayerkmp.feature.navigation.RuleEdit
import com.vladdrummer.prayerkmp.feature.prayer.PrayerScrollSession
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.storage.rememberAppStorage
import com.vladdrummer.prayerkmp.feature.tableofcontents.TableOfContentsRepository
import androidx.compose.material3.TextField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import kotlinproject.composeapp.generated.resources.Res
import kotlinproject.composeapp.generated.resources.cs_icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.ui.tooling.preview.Preview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Preview
fun App() {
    val storage = rememberAppStorage()
    val json = remember { Json { ignoreUnknownKeys = true } }
    val isDarkTheme by storage.booleanFlow(AppStorageKeys.DarkTheme, default = false)
        .collectAsState(initial = false)
    val bibleLastBookRaw by storage.stringFlow(AppStorageKeys.BibleLastBook, default = "")
        .collectAsState(initial = "")
    val favoriteRaw by storage.stringFlow(AppStorageKeys.FavoritePrayers, default = "")
        .collectAsState(initial = "")
    val favoriteIds = remember(favoriteRaw) {
        if (favoriteRaw.isBlank()) emptyList()
        else runCatching {
            json.decodeFromString(ListSerializer(String.serializer()), favoriteRaw)
        }.getOrDefault(emptyList())
    }
    var stringsReady by remember { mutableStateOf(false) }
    var initError by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        runCatching {
            initError = null
            runInitStep("PrayerArraysRepository") { PrayerArraysRepository.init() }
            runInitStep("TableOfContentsRepository") { TableOfContentsRepository.init() }
            stringsReady = true
        }.onFailure { error ->
            stringsReady = false
            val root = error.cause ?: error
            val details = root.message ?: root.toString()
            initError = "Ошибка инициализации: $details"
            println("app-init: failed, root=$root")
        }
    }
    val scope = rememberCoroutineScope()
    val bookColorScheme = lightColorScheme(
        primary = Color(0xFF7B1E2F),
        onPrimary = Color(0xFFFFF7FA),
        primaryContainer = Color(0xFFF4DCE4),
        onPrimaryContainer = Color(0xFF320914),
        secondary = Color(0xFF4B5D7A),
        onSecondary = Color(0xFFF7F9FF),
        secondaryContainer = Color(0xFFDCE5F5),
        onSecondaryContainer = Color(0xFF17233A),
        background = Color(0xFFF4F6FA),
        onBackground = Color(0xFF1B1E26),
        surface = Color(0xFFF8FAFF),
        onSurface = Color(0xFF1B1E26),
        surfaceVariant = Color(0xFFE2E6EF),
        onSurfaceVariant = Color(0xFF3A4251),
        outline = Color(0xFF9AA3B5),
        inverseSurface = Color(0xFF272C38),
        inverseOnSurface = Color(0xFFEFF2FB)
    )
    val bookDarkColorScheme = darkColorScheme(
        primary = Color(0xFFC45A72),
        onPrimary = Color(0xFF2D0A14),
        primaryContainer = Color(0xFF4A1524),
        onPrimaryContainer = Color(0xFFFFDCE5),
        secondary = Color(0xFF9CB2D7),
        onSecondary = Color(0xFF162238),
        secondaryContainer = Color(0xFF2A3A57),
        onSecondaryContainer = Color(0xFFDCE6FF),
        background = Color(0xFF12151D),
        onBackground = Color(0xFFE7EBF5),
        surface = Color(0xFF1A1F2A),
        onSurface = Color(0xFFE7EBF5),
        surfaceVariant = Color(0xFF313847),
        onSurfaceVariant = Color(0xFFC2CBDC),
        outline = Color(0xFF7D879B),
        inverseSurface = Color(0xFFE7EBF5),
        inverseOnSurface = Color(0xFF1C2230)
    )

    MaterialTheme(
        colorScheme = if (isDarkTheme) bookDarkColorScheme else bookColorScheme
    ) {
        if (!stringsReady) {
            SplashLoadingScreen(errorMessage = initError)
            return@MaterialTheme
        }

        val navController = rememberNavController()
        var pendingTopBarTitle by remember { mutableStateOf<String?>(null) }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val destinationRoute = navBackStackEntry?.destination?.route.orEmpty()
        val isPrayerListScreen = destinationRoute.contains("PrayerListScreen")
        val isPrayerScreen = destinationRoute.contains("PrayerScreen")
        val isPersonalDataScreen = destinationRoute.contains("PersonalData")
        val isRuleEditScreen = destinationRoute.contains("RuleEdit")
        val isMessageBoardScreen = destinationRoute.contains("MessageBoard")
        val isSupportScreen = destinationRoute.contains("Support")
        val isCloudScreen = destinationRoute.contains("Cloud")
        val isFavoritesScreen = destinationRoute.contains("Favorites")
        val isGospelReadingsScreen = destinationRoute.contains("GospelReadings")
        val isBiblePrayerBridgeScreen = destinationRoute.contains("BiblePrayerBridge")
        val isBibleReaderScreen = destinationRoute.contains("BibleReader")
        val isBibleScreen = destinationRoute.contains("Bible") && !isBiblePrayerBridgeScreen && !isBibleReaderScreen
        val isPsalterScreen = destinationRoute.contains("Psalter") && !destinationRoute.contains("PsalterReader") && !destinationRoute.contains("PsalterBeforePrayer") && !destinationRoute.contains("PsalterAfterPrayer")
        val isPsalterReaderScreen = destinationRoute.contains("PsalterReader")
        val isPsalterBeforePrayerScreen = destinationRoute.contains("PsalterBeforePrayer")
        val isPsalterAfterPrayerScreen = destinationRoute.contains("PsalterAfterPrayer")
        val isMainMenuByRoute = destinationRoute.contains("MainMenu")
        val prayerListRoute = if (isPrayerListScreen) runCatching { navBackStackEntry?.toRoute<PrayerListScreen>() }.getOrNull() else null
        val prayerScreenRoute = if (isPrayerScreen) runCatching { navBackStackEntry?.toRoute<PrayerScreen>() }.getOrNull() else null
        val gospelReadingsRoute = if (isGospelReadingsScreen) runCatching { navBackStackEntry?.toRoute<GospelReadings>() }.getOrNull() else null
        val biblePrayerBridgeRoute = if (isBiblePrayerBridgeScreen) runCatching { navBackStackEntry?.toRoute<BiblePrayerBridge>() }.getOrNull() else null
        val bibleReaderRoute = if (isBibleReaderScreen) runCatching { navBackStackEntry?.toRoute<BibleReader>() }.getOrNull() else null
        val psalterReaderRoute = if (isPsalterReaderScreen) runCatching { navBackStackEntry?.toRoute<PsalterReader>() }.getOrNull() else null
        val psalterBeforePrayerRoute = if (isPsalterBeforePrayerScreen) runCatching { navBackStackEntry?.toRoute<PsalterBeforePrayer>() }.getOrNull() else null
        val psalterAfterPrayerRoute = if (isPsalterAfterPrayerScreen) runCatching { navBackStackEntry?.toRoute<PsalterAfterPrayer>() }.getOrNull() else null
        val isMainMenuScreen =
            isMainMenuByRoute || (!isPrayerListScreen && !isPrayerScreen && !isPersonalDataScreen && !isRuleEditScreen && !isMessageBoardScreen && !isSupportScreen && !isCloudScreen && !isFavoritesScreen && !isGospelReadingsScreen && !isBibleScreen && !isBiblePrayerBridgeScreen && !isBibleReaderScreen && !isPsalterScreen && !isPsalterReaderScreen && !isPsalterBeforePrayerScreen && !isPsalterAfterPrayerScreen)
        val isSearchAvailable = isMainMenuScreen || isPrayerListScreen
        var isSearchActive by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        val searchFocusRequester = remember { FocusRequester() }
        val allPrayerSearchItems = remember(stringsReady) {
            TableOfContentsRepository.state.value
                .asSequence()
                .flatMap { toc -> toc.item.asSequence() }
                .mapNotNull { prayer ->
                    val resId = prayer.resid.orEmpty()
                    val title = prayer.name.orEmpty()
                    if (resId.isBlank() || title.isBlank()) null
                    else PrayerSearchItem(resId = resId, title = title, addable = prayer.addable)
                }
                .distinctBy { it.resId }
                .toList()
        }
        val scopedPrayerSearchItems = remember(isPrayerListScreen, prayerListRoute, allPrayerSearchItems) {
            if (!isPrayerListScreen || prayerListRoute == null) {
                allPrayerSearchItems
            } else {
                val index = when (prayerListRoute.typeEnum()) {
                    com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen.PrayerListScreenType.AllPrayer -> 0
                    com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen.PrayerListScreenType.CannonAcathists -> 1
                    com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen.PrayerListScreenType.Saints -> 2
                }
                TableOfContentsRepository.state.value.getOrNull(index)
                    ?.item
                    .orEmpty()
                    .mapNotNull { prayer ->
                        val resId = prayer.resid.orEmpty()
                        val title = prayer.name.orEmpty()
                        if (resId.isBlank() || title.isBlank()) null
                        else PrayerSearchItem(resId = resId, title = title, addable = prayer.addable)
                    }
                    .distinctBy { it.resId }
            }
        }
        val searchResults = remember(searchQuery, scopedPrayerSearchItems) {
            val q = searchQuery.trim()
            if (q.isBlank()) emptyList()
            else scopedPrayerSearchItems
                .filter { it.title.contains(q, ignoreCase = true) }
                .take(20)
        }
        val currentTitle =
            if (isMainMenuScreen) {
                "Молитвослов"
            } else {
                prayerScreenRoute?.title
                    ?: prayerListRoute?.title
                    ?: gospelReadingsRoute?.title
                    ?: if (isPsalterScreen || isPsalterReaderScreen) "Псалтирь" else null
                    ?: psalterBeforePrayerRoute?.title
                    ?: psalterAfterPrayerRoute?.title
                    ?: if (isPersonalDataScreen) "Персональные данные" else null
                    ?: if (isMessageBoardScreen) "Молитвы друг за друга" else null
                    ?: if (isSupportScreen) "Поддержать проект" else null
                    ?: if (isCloudScreen) "Облачное сохранение" else null
                    ?: if (isFavoritesScreen) "Избранное" else null
                    ?: biblePrayerBridgeRoute?.title
                    ?: if (isRuleEditScreen) "Редактор правила" else null
                    ?: if (isBibleScreen) "Библия" else null
                    ?: bibleLastBookRaw.ifBlank { null }
                    ?: bibleReaderRoute?.book
                    ?: pendingTopBarTitle
                    ?: "Молитвослов"
            }
        val shouldShowBackButton =
            !isMainMenuScreen
        val isPrayerFavorite = prayerScreenRoute?.resId?.let { id -> favoriteIds.contains(id) } == true
        val hasAnyFavorites = favoriteIds.isNotEmpty()
        val isInPrayerScreen = prayerScreenRoute != null
        LaunchedEffect(isSearchAvailable) {
            if (!isSearchAvailable) {
                isSearchActive = false
                searchQuery = ""
            }
        }
        LaunchedEffect(isSearchActive) {
            if (isSearchActive) searchFocusRequester.requestFocus()
        }
        LaunchedEffect(prayerListRoute?.title, prayerScreenRoute?.title, gospelReadingsRoute?.title, biblePrayerBridgeRoute?.title, bibleReaderRoute?.book, psalterBeforePrayerRoute?.title, psalterAfterPrayerRoute?.title, isPersonalDataScreen, isRuleEditScreen, isMessageBoardScreen, isSupportScreen, isCloudScreen, isFavoritesScreen, isBibleScreen, isBiblePrayerBridgeScreen, isBibleReaderScreen, isPsalterScreen, isPsalterReaderScreen, isPsalterBeforePrayerScreen, isPsalterAfterPrayerScreen, isMainMenuScreen) {
            pendingTopBarTitle = null
        }
        PlatformBackHandler(enabled = isSearchActive) {
            isSearchActive = false
            searchQuery = ""
        }
        Box(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.background)
                .fillMaxSize()
        ) {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            if (isSearchActive && isSearchAvailable) {
                                TextField(
                                    value = searchQuery,
                                    onValueChange = { searchQuery = it },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .focusRequester(searchFocusRequester),
                                    singleLine = true,
                                    colors = TextFieldDefaults.colors(
                                        focusedContainerColor = Color.Transparent,
                                        unfocusedContainerColor = Color.Transparent,
                                        disabledContainerColor = Color.Transparent,
                                        focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                                        focusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                        unfocusedPlaceholderColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                        cursorColor = MaterialTheme.colorScheme.onPrimary,
                                        focusedIndicatorColor = MaterialTheme.colorScheme.onPrimary,
                                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.65f)
                                    ),
                                    placeholder = {
                                        Text(
                                            text = "Поиск молитвы",
                                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                                        )
                                    }
                                )
                            } else {
                                Text(
                                    text = currentTitle,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        },
                        navigationIcon = {
                            if (shouldShowBackButton) {
                                IconButton(
                                    onClick = {
                                        if (prayerScreenRoute != null) {
                                            scope.launch {
                                                val resId = prayerScreenRoute.resId
                                                val mapRaw = storage.stringFlow(AppStorageKeys.SavedPrayerScrollMap, "").first()
                                                val map = runCatching {
                                                    json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), mapRaw)
                                                }.getOrDefault(emptyMap()).toMutableMap()
                                                map.remove(resId)
                                                storage.setString(
                                                    AppStorageKeys.SavedPrayerScrollMap,
                                                    json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), map)
                                                )
                                                navController.navigateUp()
                                            }
                                        } else if (psalterReaderRoute != null) {
                                            navController.navigate(
                                                PsalterAfterPrayer(
                                                    mode = psalterReaderRoute.mode,
                                                    kathisma = psalterReaderRoute.kathisma
                                                )
                                            ) {
                                                popUpTo<PsalterReader> { inclusive = true }
                                            }
                                        } else {
                                            navController.navigateUp()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Назад",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                        },
                        actions = {
                            if (isSearchAvailable) {
                                IconButton(
                                    onClick = {
                                        isSearchActive = !isSearchActive
                                        if (!isSearchActive) searchQuery = ""
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Search,
                                        contentDescription = "Поиск",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            if (prayerScreenRoute != null) {
                                IconButton(
                                    onClick = {
                                        val currentResId = prayerScreenRoute.resId
                                        val currentScroll = if (PrayerScrollSession.currentResId == currentResId) {
                                            PrayerScrollSession.currentScroll
                                        } else {
                                            0
                                        }
                                        scope.launch {
                                            val mapRaw = storage.stringFlow(AppStorageKeys.SavedPrayerScrollMap, "").first()
                                            val map = runCatching {
                                                json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), mapRaw)
                                            }.getOrDefault(emptyMap()).toMutableMap()
                                            map[currentResId] = currentScroll
                                            storage.setString(
                                                AppStorageKeys.SavedPrayerScrollMap,
                                                json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), map)
                                            )
                                            navController.navigateUp()
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Save,
                                        contentDescription = "Сохранить место чтения",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            }
                            IconButton(
                                onClick = {
                                    val prayerResId = prayerScreenRoute?.resId
                                    if (prayerResId != null) {
                                        scope.launch {
                                            val updated = favoriteIds.toMutableList()
                                            if (updated.contains(prayerResId)) {
                                                updated.removeAll { it == prayerResId }
                                            } else {
                                                updated.add(prayerResId)
                                            }
                                            storage.setString(
                                                AppStorageKeys.FavoritePrayers,
                                                json.encodeToString(ListSerializer(String.serializer()), updated.distinct())
                                            )
                                        }
                                    } else {
                                        navController.navigate(Favorites) { launchSingleTop = true }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = when {
                                        isInPrayerScreen && isPrayerFavorite -> Icons.Filled.Favorite
                                        isInPrayerScreen -> Icons.Filled.FavoriteBorder
                                        hasAnyFavorites -> Icons.Filled.Favorite
                                        else -> Icons.Filled.FavoriteBorder
                                    },
                                    contentDescription = if (isInPrayerScreen && isPrayerFavorite) "Убрать из избранного" else "Избранное",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        storage.setBoolean(AppStorageKeys.DarkTheme, !isDarkTheme)
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = if (isDarkTheme) Icons.Filled.LightMode else Icons.Filled.DarkMode,
                                    contentDescription = if (isDarkTheme) "Светлая тема" else "Тёмная тема",
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            titleContentColor = MaterialTheme.colorScheme.onPrimary,
                            actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                },
                contentWindowInsets = WindowInsets.safeDrawing
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues = padding)
                ) {
                    PrayerNavigation(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        navController = navController,
                        onNavigateToContentListStarted = { title ->
                            pendingTopBarTitle = title
                        }
                    )
                    if (isSearchActive) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    isSearchActive = false
                                    searchQuery = ""
                                }
                        )
                        if (searchQuery.isNotBlank() && searchResults.isNotEmpty()) {
                            Surface(
                                modifier = Modifier
                                    .align(Alignment.TopCenter)
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                shape = MaterialTheme.shapes.medium,
                                tonalElevation = 4.dp,
                                shadowElevation = 4.dp
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 320.dp)
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    searchResults.forEachIndexed { index, item ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    isSearchActive = false
                                                    searchQuery = ""
                                                    navController.navigate(
                                                        PrayerScreen(
                                                            resId = item.resId,
                                                            title = item.title,
                                                            addable = item.addable
                                                        )
                                                    )
                                                }
                                                .padding(horizontal = 12.dp, vertical = 10.dp)
                                        ) {
                                            Text(
                                                text = item.title,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        if (index < searchResults.lastIndex) {
                                            HorizontalDivider()
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private suspend fun runInitStep(
    stage: String,
    action: suspend () -> Unit
) {
    println("app-init: start $stage")
    runCatching { action() }
        .onSuccess { println("app-init: done $stage") }
        .onFailure {
            println("app-init: failed $stage, error=$it")
            throw IllegalStateException("Initialization stage failed: $stage", it)
        }
}

@Composable
private fun SplashLoadingScreen(errorMessage: String?) {
    Box(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.background)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    painter = painterResource(Res.drawable.cs_icon),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "Молитвослов",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(28.dp)
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

private data class PrayerSearchItem(
    val resId: String,
    val title: String,
    val addable: Boolean,
)
