package eu.kanade.tachiyomi.ui.ver

import androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi
import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalUriHandler
import androidx.paging.compose.collectAsLazyPagingItems
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabOptions
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.anime.BrowseAnimeSourceContent
import eu.kanade.presentation.browse.anime.components.BrowseAnimeSourceToolbar
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.animesource.AnimeCatalogueSource
import eu.kanade.tachiyomi.animesource.online.AnimeHttpSource
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.BrowseAnimeSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.entries.anime.AnimeScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.domain.source.anime.interactor.GetRemoteAnime
import tachiyomi.domain.source.anime.service.AnimeSourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.entries.anime.LocalAnimeSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.ui.browse.anime.source.browse.SourceFilterAnimeDialog
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class VerTab : Tab {

    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_tv_enter)
            return TabOptions(
                index = 5u,
                title = stringResource(MR.strings.label_anime),
                icon = rememberAnimatedVectorPainter(image, isSelected),
            )
        }

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current!!
        val context = LocalContext.current
        val uriHandler = LocalUriHandler.current
        val haptic = LocalHapticFeedback.current
        val scope = rememberCoroutineScope()
        val sourcePreferences: SourcePreferences = Injekt.get()
        val sourceManager: AnimeSourceManager = Injekt.get()
        
        // Obtener la fuente destacada de las preferencias
        val starredSourceId = sourcePreferences.starredAnimeSource().get()?.toLongOrNull()
        
        val snackbarHostState = remember { SnackbarHostState() }

        if (starredSourceId != null) {
            // Si hay una fuente con estrella, mostrar su contenido directamente en la pestaña
            val screenModel = rememberScreenModel { 
                BrowseAnimeSourceScreenModel(
                    sourceId = starredSourceId,
                    listingQuery = GetRemoteAnime.QUERY_POPULAR
                )
            }
            val state by screenModel.state.collectAsState()
            val pagingFlow by screenModel.animePagerFlowFlow.collectAsState()

            var topBarHeight by remember { mutableIntStateOf(0) }
            var showSourceMenu by remember { mutableStateOf(false) }
            
            Scaffold(
                topBar = {
                    Column(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.surface)
                            .onGloballyPositioned { layoutCoordinates ->
                                topBarHeight = layoutCoordinates.size.height
                            },
                    ) {
                        BrowseAnimeSourceToolbar(
                            searchQuery = state.toolbarQuery,
                            onSearchQueryChange = screenModel::setToolbarQuery,
                            source = screenModel.source,
                            displayMode = screenModel.displayMode,
                            onDisplayModeChange = { screenModel.displayMode = it },
                            navigateUp = { },
                            onWebViewClick = {
                                val source = screenModel.source as? AnimeHttpSource ?: return@BrowseAnimeSourceToolbar
                                navigator.push(
                                    WebViewScreen(
                                        url = source.baseUrl,
                                        initialTitle = source.name,
                                        sourceId = source.id,
                                    )
                                )
                            },
                            onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                            onSettingsClick = { },
                            onSearch = screenModel::search,
                            scrollBehavior = null,
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.padding.small),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            FilterChip(
                                selected = state.listing == Listing.Popular,
                                onClick = {
                                    screenModel.resetFilters()
                                    screenModel.setListing(Listing.Popular)
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Outlined.Favorite,
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(FilterChipDefaults.IconSize),
                                    )
                                },
                                label = {
                                    Text(text = stringResource(MR.strings.popular))
                                },
                            )
                            if ((screenModel.source as AnimeCatalogueSource).supportsLatest) {
                                FilterChip(
                                    selected = state.listing == Listing.Latest,
                                    onClick = {
                                        screenModel.resetFilters()
                                        screenModel.setListing(Listing.Latest)
                                    },
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.NewReleases,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = {
                                        Text(text = stringResource(MR.strings.latest))
                                    },
                                )
                            }
                            if (state.filters.isNotEmpty()) {
                                FilterChip(
                                    selected = state.listing is Listing.Search,
                                    onClick = screenModel::openFilterSheet,
                                    leadingIcon = {
                                        Icon(
                                            imageVector = Icons.Outlined.FilterList,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(FilterChipDefaults.IconSize),
                                        )
                                    },
                                    label = {
                                        Text(text = stringResource(MR.strings.action_filter))
                                    },
                                )
                            }
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { paddingValues ->
                BrowseAnimeSourceContent(
                    source = screenModel.source,
                    animeList = pagingFlow.collectAsLazyPagingItems(),
                    columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                    displayMode = screenModel.displayMode,
                    snackbarHostState = snackbarHostState,
                    contentPadding = paddingValues,
                    onWebViewClick = {
                        val source = screenModel.source as? AnimeHttpSource ?: return@BrowseAnimeSourceContent
                        navigator.push(
                            WebViewScreen(
                                url = source.baseUrl,
                                initialTitle = source.name,
                                sourceId = source.id,
                            )
                        )
                    },
                    onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                    onLocalAnimeSourceHelpClick = { uriHandler.openUri(LocalAnimeSource.HELP_URL) },
                    onAnimeClick = { anime -> 
                        scope.launch { 
                            navigator.push(AnimeScreen(anime.id))
                        } 
                    },
                    onAnimeLongClick = { anime -> 
                        scope.launch { 
                            navigator.push(AnimeScreen(anime.id))
                        } 
                    },
                )

                // Manejo de diálogos
                val onDismissRequest = { screenModel.setDialog(null) }
                when (val dialog = state.dialog) {
                    is BrowseAnimeSourceScreenModel.Dialog.Filter -> {
                        SourceFilterAnimeDialog(
                            onDismissRequest = onDismissRequest,
                            filters = state.filters,
                            onReset = screenModel::resetFilters,
                            onFilter = { screenModel.search(filters = state.filters) },
                            onUpdate = screenModel::setFilters,
                        )
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.AddDuplicateAnime -> {
                        // Aquí se manejaría el diálogo de anime duplicado si se requiere
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.Migrate -> {
                        // Aquí se manejaría el diálogo de migración si se requiere
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.RemoveAnime -> {
                        // Aquí se manejaría el diálogo de eliminación si se requiere
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.ChangeAnimeCategory -> {
                        // Aquí se manejaría el diálogo de cambio de categoría si se requiere
                    }
                    else -> {}
                }
            }
        } else {
            // Si no hay fuente con estrella, mostrar un mensaje
            LaunchedEffect(Unit) {
                context.toast("No hay una extensión de anime destacada. Por favor, marca una con estrella.")
                
                // Navegar a la pestaña Browse y mostrar la sección de extensiones de anime
                scope.launch {
                    HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true, anime = true))
                }
            }
            
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay una extensión de anime destacada. Por favor, marca una con estrella.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
} 