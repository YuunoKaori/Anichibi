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
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.delay
import eu.kanade.tachiyomi.ui.browse.anime.extension.details.AnimeSourcePreferencesScreen
import eu.kanade.presentation.entries.anime.DuplicateAnimeDialog
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeDialog
import eu.kanade.tachiyomi.ui.browse.anime.migration.search.MigrateAnimeDialogScreenModel
import eu.kanade.presentation.browse.anime.components.RemoveEntryDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog

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
        
        // Obtener las dependencias localmente en el composable en lugar de usar propiedades de clase
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val sourceManager = remember { Injekt.get<AnimeSourceManager>() }
        val extensionManager = remember { Injekt.get<eu.kanade.tachiyomi.extension.anime.AnimeExtensionManager>() }
        
        // Estado para mantener el control de las extensiones instaladas
        var installedExtensions by remember { mutableStateOf(extensionManager.installedExtensionsFlow.value) }
        
        // Estado para controlar la primera carga
        var isInitialLoad by remember { mutableStateOf(true) }
        
        // Observar cambios en las extensiones instaladas
        LaunchedEffect(Unit) {
            extensionManager.installedExtensionsFlow
                .collectLatest { extensions ->
                    installedExtensions = extensions
                }
        }
        
        // Obtener la fuente destacada de las preferencias
        val starredSourceId = sourcePreferences.starredAnimeSource().get()?.toLongOrNull()
        
        // Estado para forzar la recomposición cuando cambie la extensión
        var forceRecompose by remember { mutableStateOf(0) }
        
        val snackbarHostState = remember { SnackbarHostState() }

        // Observar cambios en la fuente destacada y forzar recomposición cuando cambie
        LaunchedEffect(Unit) {
            sourcePreferences.starredAnimeSource().changes()
                .collectLatest { newSourceId ->
                    // Incrementar para forzar recomposición (al cambiar este valor, la interfaz se actualizará)
                    forceRecompose = forceRecompose + 1
                    
                    // Ya no estamos en la carga inicial
                    isInitialLoad = false
                }
        }

        // Seleccionar automáticamente la primera extensión disponible si:
        // 1. No hay una fuente con estrella seleccionada
        // 2. Hay extensiones instaladas
        // 3. Controlar que solo se haga una vez
        var didSelectInitialSource by remember { mutableStateOf(false) }
        
        LaunchedEffect(installedExtensions, starredSourceId) {
            if (starredSourceId == null && installedExtensions.isNotEmpty() && !didSelectInitialSource) {
                // Buscar una fuente compatible (que sea AnimeCatalogueSource)
                val compatibleSource = installedExtensions
                    .flatMap { it.sources }
                    .firstOrNull { it is AnimeCatalogueSource }
                
                if (compatibleSource != null) {
                    // Establecer esta fuente como la predeterminada
                    sourcePreferences.starredAnimeSource().set(compatibleSource.id.toString())
                    
                    // Marcar que ya se seleccionó una extensión inicial
                    didSelectInitialSource = true
                }
            }
        }
        
        // Redirigir a la pantalla de extensiones SOLO si no hay extensiones instaladas
        // y no estamos en proceso de seleccionar una
        LaunchedEffect(installedExtensions) {
            if (installedExtensions.isEmpty()) {
                scope.launch {
                    HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true, anime = true))
                }
            }
        }

        // Obtener nuevamente el ID después de posible cambio
        val currentStarredSourceId = sourcePreferences.starredAnimeSource().get()?.toLongOrNull()
        
        // Clave única para forzar la recreación completa del screenModel cuando cambia la fuente
        val screenModelKey = remember(currentStarredSourceId, forceRecompose) { "$currentStarredSourceId-$forceRecompose" }

        if (currentStarredSourceId != null) {
            // Si hay una fuente con estrella, mostrar su contenido directamente en la pestaña
            // Usar la clave para forzar la recreación del screenModel cuando cambia la fuente
            val screenModel = rememberScreenModel(screenModelKey) { 
                BrowseAnimeSourceScreenModel(
                    sourceId = currentStarredSourceId,
                    listingQuery = GetRemoteAnime.QUERY_LATEST
                )
            }
            
            // Asegurar que se cargue el contenido cada vez que cambia la fuente
            LaunchedEffect(screenModelKey) {
                // Refrescar datos completamente cuando cambia la fuente seleccionada
                screenModel.resetFilters()
                // Modificado: Cargar Latest si es compatible, si no, Popular
                if (screenModel.source is AnimeCatalogueSource && screenModel.source.supportsLatest) {
                    screenModel.setListing(Listing.Latest)
                } else {
                    screenModel.setListing(Listing.Popular)
                }
            }
            
            val state by screenModel.state.collectAsState()
            val pagingFlow by screenModel.animePagerFlowFlow.collectAsState()

            var topBarHeight by remember { mutableIntStateOf(0) }
            var showSourceMenu by remember { mutableStateOf(false) }
            
            // Verificar si la fuente es AnimeCatalogueSource para evitar ClassCastException
            if (screenModel.source !is AnimeCatalogueSource) {
                // Si no es una fuente de catálogo válida, mostrar mensaje de error
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "La fuente seleccionada no es compatible con Android TV. Selecciona otra fuente.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                return@Content
            }
            
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
                            navigateUp = null,
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
                            onSettingsClick = { navigator.push(AnimeSourcePreferencesScreen(screenModel.source.id)) },
                            onSearch = screenModel::search,
                            scrollBehavior = null,
                        )

                        Row(
                            modifier = Modifier
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = MaterialTheme.padding.small),
                            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small),
                        ) {
                            // Primero Latest (Reciente)
                            val catalogueSource = screenModel.source as AnimeCatalogueSource
                            
                            if (catalogueSource.supportsLatest) {
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
                            
                            // Segundo Popular
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
                            
                            // Tercero Filter
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
                            try {
                                val duplicateAnime = screenModel.getDuplicateAnimelibAnime(anime)
                                when {
                                    anime.favorite -> screenModel.setDialog(
                                        BrowseAnimeSourceScreenModel.Dialog.RemoveAnime(anime),
                                    )
                                    duplicateAnime != null -> screenModel.setDialog(
                                        BrowseAnimeSourceScreenModel.Dialog.AddDuplicateAnime(
                                            anime,
                                            duplicateAnime,
                                        ),
                                    )
                                    else -> screenModel.addFavorite(anime)
                                }
                                haptic.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                            } catch (e: UnsupportedOperationException) {
                                snackbarHostState.showSnackbar("Esta acción no es soportada por la extensión.")
                            } catch (e: Exception) {
                                snackbarHostState.showSnackbar(e.message ?: "Error desconocido")
                            }
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
                        // Igual que browse: permitir agregar, migrar o abrir duplicado
                        DuplicateAnimeDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { screenModel.addFavorite(dialog.anime) },
                            onOpenAnime = { navigator.push(AnimeScreen(dialog.duplicate.id)) },
                            onMigrate = {
                                screenModel.setDialog(
                                    BrowseAnimeSourceScreenModel.Dialog.Migrate(dialog.anime, dialog.duplicate),
                                )
                            },
                        )
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.Migrate -> {
                        MigrateAnimeDialog(
                            oldAnime = dialog.oldAnime,
                            newAnime = dialog.newAnime,
                            screenModel = MigrateAnimeDialogScreenModel(),
                            onDismissRequest = onDismissRequest,
                            onClickTitle = { navigator.push(AnimeScreen(dialog.oldAnime.id)) },
                            onPopScreen = { onDismissRequest() },
                        )
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.RemoveAnime -> {
                        RemoveEntryDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { screenModel.changeAnimeFavorite(dialog.anime) },
                            entryToRemove = dialog.anime.title,
                        )
                    }
                    is BrowseAnimeSourceScreenModel.Dialog.ChangeAnimeCategory -> {
                        ChangeCategoryDialog(
                            initialSelection = dialog.initialSelection,
                            onDismissRequest = onDismissRequest,
                            onEditCategories = { navigator.push(eu.kanade.tachiyomi.ui.category.CategoriesTab) },
                            onConfirm = { include, _ ->
                                screenModel.changeAnimeFavorite(dialog.anime)
                                screenModel.moveAnimeToCategories(dialog.anime, include)
                            },
                        )
                    }
                    else -> {}
                }
            }
        } else {
            // Si no hay fuente con estrella, mostrar un mensaje
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