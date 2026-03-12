package com.vladdrummer.prayerkmp.feature.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.vladdrummer.prayerkmp.getPlatform
import com.vladdrummer.prayerkmp.feature.auth.GoogleEmailAuthBottomSheet
import com.vladdrummer.prayerkmp.feature.bible.BibleScreen
import com.vladdrummer.prayerkmp.feature.bible.view_model.BibleReaderTarget
import com.vladdrummer.prayerkmp.feature.bible.view_model.BibleViewModel
import com.vladdrummer.prayerkmp.feature.cloud.CloudScreen
import com.vladdrummer.prayerkmp.feature.cloud.view_model.CloudViewModel
import com.vladdrummer.prayerkmp.feature.contentlist.ContentListScreen
import com.vladdrummer.prayerkmp.feature.contentlist.view_model.ContentListViewModel
import com.vladdrummer.prayerkmp.feature.favorites.FavoritesScreen
import com.vladdrummer.prayerkmp.feature.favorites.view_model.FavoritesViewModel
import com.vladdrummer.prayerkmp.feature.mainmenu.MainMenuScreen
import com.vladdrummer.prayerkmp.feature.mainmenu.view_model.MainViewModel
import com.vladdrummer.prayerkmp.feature.messageboard.MessageBoardScreen
import com.vladdrummer.prayerkmp.feature.messageboard.view_model.MessageBoardViewModel
import com.vladdrummer.prayerkmp.feature.personaldata.PersonalDataScreen
import com.vladdrummer.prayerkmp.feature.personaldata.view_model.PersonalDataViewModel
import com.vladdrummer.prayerkmp.feature.prayer.PrayerScreen as PrayerReadingScreen
import com.vladdrummer.prayerkmp.feature.prayer.PrayerTextBuilder
import com.vladdrummer.prayerkmp.feature.prayer.view_model.PrayerViewModel
import com.vladdrummer.prayerkmp.feature.psalter.PsalterMode
import com.vladdrummer.prayerkmp.feature.psalter.PsalterReaderScreen
import com.vladdrummer.prayerkmp.feature.psalter.PsalterScreen
import com.vladdrummer.prayerkmp.feature.psalter.view_model.PsalterReaderViewModel
import com.vladdrummer.prayerkmp.feature.psalter.view_model.PsalterViewModel
import com.vladdrummer.prayerkmp.feature.readings.ReadingsScreen
import com.vladdrummer.prayerkmp.feature.readings.view_model.ReadingsViewModel
import com.vladdrummer.prayerkmp.feature.ruleedit.RuleEditScreen
import com.vladdrummer.prayerkmp.feature.ruleedit.view_model.RuleEditViewModel
import com.vladdrummer.prayerkmp.feature.support.SupportScreen
import com.vladdrummer.prayerkmp.feature.support.view_model.SupportViewModel
import com.vladdrummer.prayerkmp.feature.subscription.PremiumOfferDialog
import com.vladdrummer.prayerkmp.feature.subscription.SubscriptionStatusManager
import com.vladdrummer.prayerkmp.feature.navigation.PrayerListScreen.PrayerListScreenType
import com.vladdrummer.prayerkmp.feature.storage.AppStorageKeys
import com.vladdrummer.prayerkmp.feature.storage.rememberAppStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

@Composable
fun PrayerNavigation (
    modifier: Modifier,
    navController: NavHostController,
    onNavigateToContentListStarted: (String) -> Unit = {}
) {
    val rootStorage = rememberAppStorage()
    val rootScope = rememberCoroutineScope()
    val authEmail by rootStorage.stringFlow(AppStorageKeys.GoogleAccountEmail, "").collectAsStateWithLifecycle("")
    val subscriptionManager = remember(rootStorage, rootScope) {
        SubscriptionStatusManager(storage = rootStorage, scope = rootScope)
    }
    val subscriptionState by subscriptionManager.state.collectAsStateWithLifecycle()
    LaunchedEffect(authEmail) {
        subscriptionManager.start(authEmail)
    }
    NavHost(
        modifier = modifier,
        startDestination = MainMenu,
        navController = navController
    ) {
        composable<MainMenu> {
            val scope = rememberCoroutineScope()
            var pendingProtectedDestination by remember { mutableStateOf<ProtectedDestination?>(null) }
            val platformName = remember { getPlatform().name.lowercase() }
            val isAndroidPlatform = platformName.startsWith("android")
            val isIosPlatform = platformName.startsWith("ios")
            fun requiresAuth(destination: ProtectedDestination): Boolean {
                return when {
                    isAndroidPlatform -> destination in setOf(
                        ProtectedDestination.PersonalData,
                        ProtectedDestination.RuleEdit,
                        ProtectedDestination.MessageBoard,
                        ProtectedDestination.Cloud,
                    )
                    isIosPlatform -> destination in setOf(
                        ProtectedDestination.MessageBoard,
                        ProtectedDestination.Cloud,
                    )
                    else -> false
                }
            }
            val viewModel : MainViewModel = viewModel { MainViewModel() }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            MainMenuScreen(
                onNavigateToContentListStarted = onNavigateToContentListStarted,
                viewState = viewState,
                onItemClick = { item ->
                    if (item.id == MainViewModel.PERSONAL_DATA_ITEM_ID) {
                        if (requiresAuth(ProtectedDestination.PersonalData) && authEmail.isBlank()) {
                            pendingProtectedDestination = ProtectedDestination.PersonalData
                        }
                        else navController.navigate(PersonalData)
                    } else if (item.id == MainViewModel.RULE_EDIT_ITEM_ID) {
                        if (requiresAuth(ProtectedDestination.RuleEdit) && authEmail.isBlank()) {
                            pendingProtectedDestination = ProtectedDestination.RuleEdit
                        }
                        else navController.navigate(RuleEdit)
                    } else if (item.id == MainViewModel.BIBLE_ITEM_ID) {
                        navController.navigate(Bible)
                    } else if (item.id == MainViewModel.PSALTER_ITEM_ID) {
                        navController.navigate(Psalter)
                    } else if (item.id == MainViewModel.MESSAGE_BOARD_ITEM_ID) {
                        if (requiresAuth(ProtectedDestination.MessageBoard) && authEmail.isBlank()) {
                            pendingProtectedDestination = ProtectedDestination.MessageBoard
                        }
                        else navController.navigate(MessageBoard)
                    } else if (item.id == MainViewModel.CLOUD_ITEM_ID) {
                        if (requiresAuth(ProtectedDestination.Cloud) && authEmail.isBlank()) {
                            pendingProtectedDestination = ProtectedDestination.Cloud
                        }
                        else navController.navigate(Cloud)
                    } else if (item.id == MainViewModel.SUPPORT_ITEM_ID) {
                        navController.navigate(Support)
                    } else if (item.id == MainViewModel.READINGS_ITEM_ID) {
                        navController.navigate(GospelReadings(title = item.title))
                    } else {
                        val type = when (item.id) {
                            0 -> PrayerListScreenType.AllPrayer
                            1 -> PrayerListScreenType.CannonAcathists
                            else -> PrayerListScreenType.Saints
                        }
                        navController.navigate(PrayerListScreen.from(type = type, title = item.title))
                    }
                }
            )
            if (pendingProtectedDestination != null) {
                GoogleEmailAuthBottomSheet(
                    onDismiss = { pendingProtectedDestination = null },
                    onAuthorized = { email ->
                        scope.launch {
                            rootStorage.setString(AppStorageKeys.GoogleAccountEmail, email)
                            when (pendingProtectedDestination) {
                                ProtectedDestination.PersonalData -> navController.navigate(PersonalData)
                                ProtectedDestination.RuleEdit -> navController.navigate(RuleEdit)
                                ProtectedDestination.MessageBoard -> navController.navigate(MessageBoard)
                                ProtectedDestination.Cloud -> navController.navigate(Cloud)
                                null -> Unit
                            }
                            pendingProtectedDestination = null
                        }
                    }
                )
            }
        }
        composable<GospelReadings> {
            LaunchedEffect(Unit) {
                println("readings: navigation entered GospelReadings route")
            }
            val storage = rememberAppStorage()
            val viewModel: ReadingsViewModel = viewModel { ReadingsViewModel(storage) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            ReadingsScreen(
                viewState = viewState,
                onRetry = viewModel::load
            )
        }
        composable<Bible> {
            val storage = rememberAppStorage()
            val lastReadBookRaw by storage.stringFlow(AppStorageKeys.BibleLastBook, "").collectAsStateWithLifecycle("")
            val lastReadChapterRaw by storage.stringFlow(AppStorageKeys.BibleLastChapter, "").collectAsStateWithLifecycle("")
            val lastReadBook = lastReadBookRaw.ifBlank { null }
            val lastReadChapter = lastReadChapterRaw.toIntOrNull()
            val viewModel: BibleViewModel = viewModel { BibleViewModel(storage = storage) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            BibleScreen(
                viewState = viewState,
                onRetry = viewModel::loadBooks,
                onBookClick = viewModel::toggleBookExpansion,
                onChapterClick = { book, chapter ->
                    navController.navigate(BiblePrayerBridge(book = book, chapter = chapter))
                },
                lastReadBookOverride = lastReadBook,
                lastReadChapterOverride = lastReadChapter,
                onContinueReadClick = { book, chapter ->
                    navController.navigate(BiblePrayerBridge(book = book, chapter = chapter))
                },
                onOpenReaderAt = viewModel::openReaderAt,
                onReaderPageChanged = viewModel::onReaderPageChanged,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
            )
        }
        composable<BibleReader> { backStackEntry ->
            val args = backStackEntry.toRoute<BibleReader>()
            val storage = rememberAppStorage()
            val viewModel: BibleViewModel = viewModel {
                BibleViewModel(
                    storage = storage,
                    initialTarget = BibleReaderTarget(book = args.book, chapter = args.chapter),
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            BibleScreen(
                viewState = viewState,
                onRetry = viewModel::loadBooks,
                onBookClick = viewModel::toggleBookExpansion,
                onChapterClick = { book, chapter ->
                    navController.navigate(BiblePrayerBridge(book = book, chapter = chapter))
                },
                onOpenReaderAt = viewModel::openReaderAt,
                onReaderPageChanged = viewModel::onReaderPageChanged,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
            )
        }
        composable<BiblePrayerBridge> { backStackEntry ->
            val args = backStackEntry.toRoute<BiblePrayerBridge>()
            val storage = rememberAppStorage()
            val viewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    resId = "knig",
                    title = args.title,
                    addable = false,
                    storage = storage,
                    textBuilder = PrayerTextBuilder(storage),
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PrayerReadingScreen(
                viewState = viewState,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
                onNavigateBackWithoutSave = navController::navigateUp,
                bottomActionText = "Перейти дальше к Библии",
                onBottomActionClick = {
                    navController.navigate(BibleReader(book = args.book, chapter = args.chapter)) {
                        popUpTo<BiblePrayerBridge> {
                            inclusive = true
                        }
                    }
                },
            )
        }
        composable<Psalter> {
            val storage = rememberAppStorage()
            val viewModel: PsalterViewModel = viewModel { PsalterViewModel(storage) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PsalterScreen(
                viewState = viewState,
                onModeSelected = viewModel::selectMode,
                onToggleKathisma = viewModel::toggleKathisma,
                onContinueReadClick = { mode, kathisma, page ->
                    navController.navigate(
                        PsalterBeforePrayer(
                            mode = mode.id,
                            kathisma = kathisma,
                            startPsalm = null,
                            startPage = page
                        )
                    )
                },
                onPsalmClick = { kathisma, psalm ->
                    navController.navigate(
                        PsalterBeforePrayer(
                            mode = viewState.selectedMode.id,
                            kathisma = kathisma,
                            startPsalm = psalm,
                            startPage = null
                        )
                    )
                },
                onNameToggle = viewModel::toggleNameSelection
            )
        }
        composable<PsalterBeforePrayer> { backStackEntry ->
            val args = backStackEntry.toRoute<PsalterBeforePrayer>()
            val storage = rememberAppStorage()
            val viewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    resId = "nachaloPsaltyri",
                    title = args.title,
                    addable = false,
                    storage = storage,
                    textBuilder = PrayerTextBuilder(storage),
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PrayerReadingScreen(
                viewState = viewState,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
                onNavigateBackWithoutSave = navController::navigateUp,
                bottomActionText = "К чтению Псалтири",
                onBottomActionClick = {
                    navController.navigate(
                        PsalterReader(
                            mode = args.mode,
                            kathisma = args.kathisma,
                            startPsalm = args.startPsalm,
                            startPage = args.startPage
                        )
                    ) {
                        popUpTo<PsalterBeforePrayer> { inclusive = true }
                    }
                },
            )
        }
        composable<PsalterReader> { backStackEntry ->
            val args = backStackEntry.toRoute<PsalterReader>()
            val mode = PsalterMode.fromId(args.mode)
            val storage = rememberAppStorage()
            val viewModel: PsalterReaderViewModel = viewModel {
                PsalterReaderViewModel(
                    mode = mode,
                    kathisma = args.kathisma,
                    startPsalm = args.startPsalm,
                    initialPage = args.startPage,
                    storage = storage
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PsalterReaderScreen(
                viewState = viewState,
                onRetry = viewModel::load,
                onPageChanged = viewModel::onPageChanged,
                currentKathisma = args.kathisma,
                onOpenKathisma = { nextKathisma ->
                    navController.navigate(
                        PsalterReader(
                            mode = args.mode,
                            kathisma = nextKathisma,
                            startPsalm = null,
                            startPage = null
                        )
                    ) {
                        popUpTo<PsalterReader> { inclusive = true }
                    }
                },
                onFinishReading = {
                    navController.navigate(
                        PsalterAfterPrayer(mode = args.mode, kathisma = args.kathisma)
                    ) {
                        popUpTo<PsalterReader> { inclusive = true }
                    }
                }
            )
        }
        composable<PsalterAfterPrayer> { backStackEntry ->
            val args = backStackEntry.toRoute<PsalterAfterPrayer>()
            val storage = rememberAppStorage()
            val viewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    resId = "psaltyrPoOkonchanii",
                    title = args.title,
                    addable = false,
                    storage = storage,
                    textBuilder = PrayerTextBuilder(storage),
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PrayerReadingScreen(
                viewState = viewState,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
                onNavigateBackWithoutSave = navController::navigateUp,
            )
        }
        composable<PersonalData> {
            val storage = rememberAppStorage()
            val isSubscriptionLocked = authEmail.isNotBlank() && !subscriptionState.hasActiveSubscription
            var showPremiumOffer by remember { mutableStateOf(isSubscriptionLocked) }
            LaunchedEffect(authEmail) {
                if (authEmail.isNotBlank()) {
                    subscriptionManager.refresh(email = authEmail, force = true)
                }
            }
            LaunchedEffect(isSubscriptionLocked) {
                if (isSubscriptionLocked) showPremiumOffer = true
            }
            val viewModel: PersonalDataViewModel = viewModel { PersonalDataViewModel(storage) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PersonalDataScreen(
                viewState = viewState,
                editingEnabled = !isSubscriptionLocked,
                onNameImenitChanged = viewModel::onNameImenitChanged,
                onDuhovnikChanged = viewModel::onDuhovnikChanged,
                onGenderChanged = viewModel::onGenderChanged,
                onPersonNameChanged = viewModel::onPersonNameChanged,
                onPersonGenderChanged = viewModel::onPersonGenderChanged,
                onPersonStatusChanged = viewModel::onPersonStatusChanged,
                onPersonAdded = viewModel::onPersonAdded,
                onPersonRemoved = viewModel::onPersonRemoved,
                onGoogleAccountSelected = viewModel::onGoogleAccountSelected,
                onGoogleAccountCleared = viewModel::onGoogleAccountCleared,
            )
            if (showPremiumOffer && isSubscriptionLocked) {
                PremiumOfferDialog(onDismiss = { showPremiumOffer = false })
            }
        }
        composable<RuleEdit> {
            val storage = rememberAppStorage()
            val isSubscriptionLocked = authEmail.isNotBlank() && !subscriptionState.hasActiveSubscription
            var showPremiumOffer by remember { mutableStateOf(isSubscriptionLocked) }
            LaunchedEffect(authEmail) {
                if (authEmail.isNotBlank()) {
                    subscriptionManager.refresh(email = authEmail, force = true)
                }
            }
            LaunchedEffect(isSubscriptionLocked) {
                if (isSubscriptionLocked) showPremiumOffer = true
            }
            val viewModel: RuleEditViewModel = viewModel { RuleEditViewModel(storage, PrayerTextBuilder(storage)) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            RuleEditScreen(
                viewState = viewState,
                editingEnabled = !isSubscriptionLocked,
                onSelectRule = viewModel::selectRule,
                onPartCheckedChange = viewModel::setPartEnabled,
                onRemoveAdditionalPrayer = viewModel::removeAdditionalPrayer,
                onOpenPartPreview = viewModel::openPartPreview,
                onOpenAdditionalPrayerPreview = viewModel::openAdditionalPrayerPreview,
                onClosePreview = viewModel::closePreview,
            )
            if (showPremiumOffer && isSubscriptionLocked) {
                PremiumOfferDialog(onDismiss = { showPremiumOffer = false })
            }
        }
        composable<MessageBoard> {
            val storage = rememberAppStorage()
            val platformName = remember { getPlatform().name.lowercase() }
            val senderType = remember(platformName) {
                if (platformName.startsWith("ios")) 4 else 3
            }
            val viewModel: MessageBoardViewModel = viewModel(
                key = "messageboard-$authEmail"
            ) {
                MessageBoardViewModel(
                    storage = storage,
                    currentUserEmail = authEmail,
                    senderType = senderType
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            MessageBoardScreen(
                viewState = viewState,
                onRefresh = viewModel::refresh,
                onVote = viewModel::vote,
                onRemove = viewModel::removeMessage,
                onPost = viewModel::postMessage,
                onEdit = viewModel::editMessage,
            )
        }
        composable<Support> {
            val viewModel: SupportViewModel = viewModel { SupportViewModel() }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            SupportScreen(viewState = viewState)
        }
        composable<Cloud> {
            val viewModel: CloudViewModel = viewModel { CloudViewModel() }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            CloudScreen(viewState = viewState)
        }
        composable<Favorites> {
            val storage = rememberAppStorage()
            val viewModel: FavoritesViewModel = viewModel { FavoritesViewModel(storage) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            FavoritesScreen(
                viewState = viewState,
                onOpenPrayer = { item ->
                    navController.navigate(
                        PrayerScreen(
                            resId = item.resId,
                            title = item.title,
                            addable = item.addable,
                        )
                    )
                },
                onRemoveFavorite = viewModel::removeFavorite,
                onMoveFavorite = viewModel::moveFavorite,
            )
        }
        composable<PrayerListScreen> { backStackEntry ->
            val args = backStackEntry.toRoute<PrayerListScreen>()
            val viewModel: ContentListViewModel = viewModel { ContentListViewModel(args.typeEnum()) }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            ContentListScreen(
                viewState = viewState,
                onPrayerClick = { prayer ->
                    val resId = prayer.resid.orEmpty()
                    if (resId.isNotBlank()) {
                        navController.navigate(
                            PrayerScreen(
                                resId = resId,
                                title = prayer.name.orEmpty(),
                                addable = prayer.addable
                            )
                        )
                    }
                }
            )
        }
        composable<PrayerScreen> { backStackEntry ->
            val args = backStackEntry.toRoute<PrayerScreen>()
            val storage = rememberAppStorage()
            val scope = rememberCoroutineScope()
            val isSubscriptionLocked = authEmail.isNotBlank() && !subscriptionState.hasActiveSubscription
            var showPremiumOffer by remember { mutableStateOf(false) }
            LaunchedEffect(authEmail) {
                if (authEmail.isNotBlank()) {
                    subscriptionManager.refresh(email = authEmail, force = true)
                }
            }
            val viewModel: PrayerViewModel = viewModel {
                PrayerViewModel(
                    resId = args.resId,
                    title = args.title,
                    addable = args.addable,
                    storage = storage,
                    textBuilder = PrayerTextBuilder(storage)
                )
            }
            val viewState by viewModel.viewState.collectAsStateWithLifecycle()
            PrayerReadingScreen(
                viewState = viewState,
                isPremiumLocked = isSubscriptionLocked,
                onIncreaseFont = viewModel::increaseFontSize,
                onDecreaseFont = viewModel::decreaseFontSize,
                onSwitchFont = viewModel::switchFont,
                onResetFontDefaults = viewModel::resetFontDefaults,
                onToggleMorning = viewModel::toggleMorning,
                onToggleEvening = viewModel::toggleEvening,
                onPremiumBlockedAction = { showPremiumOffer = true },
                onNavigateBackWithoutSave = {
                    scope.launch {
                        val json = Json { ignoreUnknownKeys = true }
                        val mapRaw = storage.stringFlow(AppStorageKeys.SavedPrayerScrollMap, "").first()
                        val map = runCatching {
                            json.decodeFromString(MapSerializer(String.serializer(), Int.serializer()), mapRaw)
                        }.getOrDefault(emptyMap()).toMutableMap()
                        map.remove(args.resId)
                        storage.setString(
                            AppStorageKeys.SavedPrayerScrollMap,
                            json.encodeToString(MapSerializer(String.serializer(), Int.serializer()), map)
                        )
                        navController.navigateUp()
                    }
                }
            )
            if (showPremiumOffer && isSubscriptionLocked) {
                PremiumOfferDialog(onDismiss = { showPremiumOffer = false })
            }
        }
    }
}

private enum class ProtectedDestination {
    PersonalData,
    RuleEdit,
    MessageBoard,
    Cloud,
}
