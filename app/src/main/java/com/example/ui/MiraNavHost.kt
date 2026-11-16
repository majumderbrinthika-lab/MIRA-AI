package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChatBubble
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.AppLockOverlay
import com.example.ui.components.ModelManagerSheet
import com.example.ui.localization.AppLanguage
import com.example.ui.screens.CharactersScreen
import com.example.ui.screens.ChatScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.ImageStudioScreen
import com.example.ui.screens.MemoriesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VoiceConversationScreen
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

@Serializable object HomeRoute
@Serializable object ChatRoute
@Serializable object VoiceRoute
@Serializable object StudioRoute
@Serializable object GirlsRoute
@Serializable object MemoriesRoute
@Serializable object SettingsRoute

private data class NavDestinationItem(
    val route: Any,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MiraRootScreen(
    state: MiraUiState,
    viewModel: MiraViewModel
) {
    if (!state.isAppUnlocked) {
        AppLockOverlay(
            strings = state.strings,
            onAttemptUnlock = { pin -> viewModel.attemptUnlock(pin) }
        )
        return
    }

    val context = LocalContext.current
    val navController = rememberNavController()
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showModelManager by remember { mutableStateOf(false) }

    LaunchedEffect(state.statusNotice) {
        val msg = state.statusNotice
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearNotice()
        }
    }

    val strings = state.strings
    val allDestinations = listOf(
        NavDestinationItem(HomeRoute, strings.navHome, Icons.Rounded.Home, Icons.Outlined.Home, "nav_home"),
        NavDestinationItem(ChatRoute, strings.navChat, Icons.Rounded.ChatBubble, Icons.Outlined.ChatBubbleOutline, "nav_chat"),
        NavDestinationItem(VoiceRoute, strings.navVoice, Icons.Rounded.Mic, Icons.Outlined.Mic, "nav_voice"),
        NavDestinationItem(StudioRoute, strings.navStudio, Icons.Rounded.AutoAwesome, Icons.Outlined.AutoAwesome, "nav_studio"),
        NavDestinationItem(GirlsRoute, strings.navGirls, Icons.Rounded.Groups, Icons.Outlined.Groups, "nav_girls"),
        NavDestinationItem(MemoriesRoute, strings.navMemories, Icons.Rounded.Favorite, Icons.Outlined.FavoriteBorder, "nav_memories"),
        NavDestinationItem(SettingsRoute, strings.navSettings, Icons.Rounded.Settings, Icons.Outlined.Settings, "nav_settings")
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDest = navBackStackEntry?.destination

    fun navigateTo(route: Any) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    val currentTitle = allDestinations.firstOrNull { item ->
        currentDest?.hasRoute(item.route::class) == true
    }?.label ?: "MIRA"

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        ModalNavigationDrawer(
            drawerState = drawerState,
            gesturesEnabled = !useRail,
            drawerContent = {
                ModalDrawerSheet {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "MIRA • মিরা",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    Text(
                        text = strings.offlineBadge,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 2.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    allDestinations.forEach { item ->
                        val selected = currentDest?.hasRoute(item.route::class) == true
                        NavigationDrawerItem(
                            selected = selected,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navigateTo(item.route)
                            },
                            icon = {
                                Icon(
                                    if (selected) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.label
                                )
                            },
                            label = { Text(item.label) },
                            modifier = Modifier
                                .padding(horizontal = 12.dp, vertical = 2.dp)
                                .testTag("drawer_${item.testTag}")
                        )
                    }
                }
            }
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                if (useRail) {
                    NavigationRail {
                        Spacer(Modifier.height(12.dp))
                        allDestinations.forEach { item ->
                            val selected = currentDest?.hasRoute(item.route::class) == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateTo(item.route) },
                                icon = {
                                    Icon(
                                        if (selected) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.label
                                    )
                                },
                                label = {
                                    Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                }

                Scaffold(
                    snackbarHost = { SnackbarHost(snackbarHostState) },
                    topBar = {
                        TopAppBar(
                            navigationIcon = {
                                if (!useRail) {
                                    IconButton(
                                        onClick = { scope.launch { drawerState.open() } },
                                        modifier = Modifier.testTag("open_drawer_button")
                                    ) {
                                        Icon(Icons.Rounded.Menu, contentDescription = "Menu")
                                    }
                                }
                            },
                            title = {
                                Text(
                                    text = currentTitle,
                                    style = MaterialTheme.typography.titleLarge,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            actions = {
                                val nextLang = if (state.preferences.uiLanguage == AppLanguage.BN.code) {
                                    AppLanguage.EN
                                } else {
                                    AppLanguage.BN
                                }
                                TextButton(
                                    onClick = { viewModel.setUiLanguage(nextLang) },
                                    modifier = Modifier.testTag("topbar_lang_toggle")
                                ) {
                                    Icon(Icons.Rounded.Translate, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (nextLang == AppLanguage.BN) "বাংলা" else "EN")
                                }
                            }
                        )
                    },
                    bottomBar = {
                        if (!useRail) {
                            NavigationBar(windowInsets = WindowInsets.navigationBars) {
                                allDestinations.take(4).forEach { item ->
                                    val selected = currentDest?.hasRoute(item.route::class) == true
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTo(item.route) },
                                        icon = {
                                            Icon(
                                                if (selected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = item.label
                                            )
                                        },
                                        label = {
                                            Text(item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        },
                                        modifier = Modifier.testTag(item.testTag)
                                    )
                                }
                                val moreSelected = currentDest?.hasRoute(GirlsRoute::class) == true ||
                                    currentDest?.hasRoute(MemoriesRoute::class) == true ||
                                    currentDest?.hasRoute(SettingsRoute::class) == true
                                NavigationBarItem(
                                    selected = moreSelected,
                                    onClick = { scope.launch { drawerState.open() } },
                                    icon = { Icon(Icons.Rounded.Menu, contentDescription = strings.navMore) },
                                    label = { Text(strings.navMore, maxLines = 1) },
                                    modifier = Modifier.testTag("nav_more")
                                )
                            }
                        }
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = HomeRoute,
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        composable<HomeRoute> {
                            HomeScreen(
                                state = state,
                                onOpenChat = { navigateTo(ChatRoute) },
                                onOpenVoice = { navigateTo(VoiceRoute) },
                                onOpenStudio = { navigateTo(StudioRoute) },
                                onOpenGirls = { navigateTo(GirlsRoute) },
                                onOpenMemories = { navigateTo(MemoriesRoute) },
                                onOpenSettings = { navigateTo(SettingsRoute) },
                                onOpenModelManager = { showModelManager = true },
                                onFreeRam = { viewModel.freeRamNow() }
                            )
                        }
                        composable<ChatRoute> {
                            ChatScreen(
                                state = state,
                                onSendMessage = { viewModel.sendMessage(it) },
                                onSpeakMessage = { text, lang -> viewModel.speakText(text, lang) },
                                onSelectChatLanguage = { viewModel.setPreferredChatLanguage(it) },
                                onOpenModelManager = { showModelManager = true }
                            )
                        }
                        composable<VoiceRoute> {
                            VoiceConversationScreen(
                                state = state,
                                onStartListening = { viewModel.startVoiceListening(it) },
                                onStopListening = { viewModel.stopVoiceListening() },
                                onStopSpeaking = { viewModel.stopSpeaking() },
                                onUpdateVoiceTuning = { speed, vol, name ->
                                    viewModel.updateVoiceTuning(speed, vol, name)
                                },
                                onSelectLanguage = { viewModel.setPreferredChatLanguage(it) },
                                onInstallTtsData = {
                                    runCatching {
                                        context.startActivity(viewModel.buildInstallTtsDataIntent())
                                    }.onFailure {
                                        Toast.makeText(
                                            context,
                                            "Open Android Settings > Language & Input > Text-to-Speech to install offline voices.",
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                },
                                onRefreshVoices = { viewModel.refreshVoiceAudit() }
                            )
                        }
                        composable<StudioRoute> {
                            ImageStudioScreen(
                                state = state,
                                onComposePortrait = { char, outfit, hair, bg, pose, style, res, uri, consent ->
                                    viewModel.composeStudioPortrait(
                                        char, outfit, hair, bg, pose, style, res, uri, consent
                                    )
                                },
                                onDeleteImage = { viewModel.deleteStudioImage(it) },
                                onSetAsPortrait = { viewModel.setStudioImageAsCharacterPortrait(it) }
                            )
                        }
                        composable<GirlsRoute> {
                            CharactersScreen(
                                state = state,
                                onSelectAndChat = { id ->
                                    viewModel.selectActiveCharacter(id)
                                    navigateTo(ChatRoute)
                                },
                                onUpdateTraits = { id, traits -> viewModel.updateCharacterTraits(id, traits) },
                                onUpdateReferencePhoto = { id, uri -> viewModel.updateCharacterReferencePhoto(id, uri) },
                                onCreateCharacter = { name, nativeName, tag, bio, app, lang, traits, uri ->
                                    viewModel.createNewCharacter(name, nativeName, tag, bio, app, lang, traits, uri)
                                },
                                onDeleteCharacter = { viewModel.deleteCharacter(it) }
                            )
                        }
                        composable<MemoriesRoute> {
                            MemoriesScreen(
                                state = state,
                                onAddMemory = { charId, cat, k, v -> viewModel.addManualMemory(charId, cat, k, v) },
                                onUpdateMemory = { viewModel.updateMemory(it) },
                                onDeleteMemory = { viewModel.deleteMemory(it) },
                                onClearConversation = { viewModel.clearConversation(it) }
                            )
                        }
                        composable<SettingsRoute> {
                            SettingsScreen(
                                state = state,
                                onSelectUiLanguage = { viewModel.setUiLanguage(it) },
                                onToggleDarkTheme = { viewModel.setDarkTheme(it) },
                                onToggleStrictOffline = { viewModel.setStrictOfflineLock(it) },
                                onConfigureAppLock = { enabled, pin -> viewModel.configureAppLock(enabled, pin) },
                                onOpenModelManager = { showModelManager = true },
                                onClearStudioCache = { viewModel.clearStudioCache() },
                                onFreeRam = { viewModel.freeRamNow() },
                                onClearCrashReport = { viewModel.clearCrashReport() }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showModelManager) {
        ModelManagerSheet(
            state = state,
            onDismiss = { showModelManager = false },
            onImportUri = { viewModel.importGgufModel(it) },
            onDownloadModel = { viewModel.downloadRecommendedModel(it) },
            onCancelDownload = { viewModel.cancelModelDownload() },
            onLoadModel = { viewModel.loadModel(it) },
            onUnloadModel = { viewModel.unloadModel() },
            onDeleteModel = { viewModel.deleteModel(it) }
        )
    }
}
