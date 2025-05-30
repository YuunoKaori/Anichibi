package eu.kanade.tachiyomi.ui.leer

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
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.filled.AutoStories
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
import eu.kanade.presentation.browse.manga.BrowseSourceContent
import eu.kanade.presentation.browse.manga.components.BrowseMangaSourceToolbar
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.common.Constants
import eu.kanade.tachiyomi.source.CatalogueSource
import eu.kanade.tachiyomi.source.online.HttpSource
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreenModel.Listing
import eu.kanade.tachiyomi.ui.entries.manga.MangaScreen
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.launch
import tachiyomi.domain.source.manga.interactor.GetRemoteManga
import tachiyomi.domain.source.manga.service.MangaSourceManager
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.source.local.entries.manga.LocalMangaSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.extension.manga.MangaExtensionManager
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.flow.collectLatest
import eu.kanade.tachiyomi.ui.browse.manga.migration.search.MigrateMangaDialogScreenModel
import eu.kanade.tachiyomi.ui.category.CategoriesTab
import eu.kanade.presentation.entries.manga.DuplicateMangaDialog
import eu.kanade.presentation.browse.anime.components.RemoveEntryDialog
import eu.kanade.presentation.category.components.ChangeCategoryDialog
import eu.kanade.tachiyomi.ui.browse.manga.migration.search.MigrateMangaDialog

class LeerTab : Tab {

    @OptIn(ExperimentalAnimationGraphicsApi::class)
    override val options: TabOptions
        @Composable
        get() {
            val isSelected = LocalTabNavigator.current.current.key == key
            val image = AnimatedImageVector.animatedVectorResource(R.drawable.anim_library_enter)
            return TabOptions(
                index = 6u,
                title = stringResource(MR.strings.label_manga),
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
        
        // Obtener las dependencias localmente en el composable en lugar de usar las propiedades de clase
        val sourcePreferences = remember { Injekt.get<SourcePreferences>() }
        val sourceManager = remember { Injekt.get<MangaSourceManager>() }
        val extensionManager = remember { Injekt.get<MangaExtensionManager>() }
        
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
        val starredSourceId = sourcePreferences.starredMangaSource().get()?.toLongOrNull()
        
        // Estado para forzar la recomposición cuando cambie la extensión
        var forceRecompose by remember { mutableStateOf(0) }
        
        val snackbarHostState = remember { SnackbarHostState() }

        // Observar cambios en la fuente destacada y forzar recomposición cuando cambie
        LaunchedEffect(Unit) {
            sourcePreferences.starredMangaSource().changes()
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
                // Obtener la primera fuente disponible
                val firstSource = installedExtensions.firstOrNull()?.sources?.firstOrNull()
                
                if (firstSource != null) {
                    // Establecer esta fuente como la predeterminada
                    sourcePreferences.starredMangaSource().set(firstSource.id.toString())
                    
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
                    HomeScreen.openTab(HomeScreen.Tab.Browse(toExtensions = true, anime = false))
                }
            }
        }

        // Obtener nuevamente el ID después de posible cambio
        val currentStarredSourceId = sourcePreferences.starredMangaSource().get()?.toLongOrNull()
        
        // Clave única para forzar la recreación completa del screenModel cuando cambia la fuente
        val screenModelKey = remember(currentStarredSourceId, forceRecompose) { "$currentStarredSourceId-$forceRecompose" }

        if (currentStarredSourceId != null) {
            // Si hay una fuente con estrella, mostrar su contenido directamente en la pestaña
            // Usar la clave para forzar la recreación del screenModel cuando cambia la fuente
            val screenModel = rememberScreenModel(screenModelKey) { 
                BrowseMangaSourceScreenModel(
                    sourceId = currentStarredSourceId,
                    listingQuery = GetRemoteManga.QUERY_LATEST
                )
            }
            
            // Asegurar que se cargue el contenido cada vez que cambia la fuente
            LaunchedEffect(screenModelKey) {
                // Refrescar datos completamente cuando cambia la fuente seleccionada
                screenModel.resetFilters()
                // Modificado: Cargar Latest si es compatible, si no, Popular
                if (screenModel.source is CatalogueSource && screenModel.source.supportsLatest) {
                    screenModel.setListing(Listing.Latest)
                } else {
                    screenModel.setListing(Listing.Popular)
                }
            }
            
            val state by screenModel.state.collectAsState()
            val pagingFlow by screenModel.mangaPagerFlowFlow.collectAsState()

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
                        BrowseMangaSourceToolbar(
                            searchQuery = state.toolbarQuery,
                            onSearchQueryChange = screenModel::setToolbarQuery,
                            source = screenModel.source,
                            displayMode = screenModel.displayMode,
                            onDisplayModeChange = { screenModel.displayMode = it },
                            navigateUp = null,
                            onWebViewClick = {
                                val source = screenModel.source as? HttpSource ?: return@BrowseMangaSourceToolbar
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
                            // Primero Latest (Reciente)
                            if ((screenModel.source as CatalogueSource).supportsLatest) {
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
                BrowseSourceContent(
                    source = screenModel.source,
                    mangaList = pagingFlow.collectAsLazyPagingItems(),
                    columns = screenModel.getColumnsPreference(LocalConfiguration.current.orientation),
                    displayMode = screenModel.displayMode,
                    snackbarHostState = snackbarHostState,
                    contentPadding = paddingValues,
                    onWebViewClick = {
                        val source = screenModel.source as? HttpSource ?: return@BrowseSourceContent
                        navigator.push(
                            WebViewScreen(
                                url = source.baseUrl,
                                initialTitle = source.name,
                                sourceId = source.id,
                            )
                        )
                    },
                    onHelpClick = { uriHandler.openUri(Constants.URL_HELP) },
                    onLocalSourceHelpClick = { uriHandler.openUri(LocalMangaSource.HELP_URL) },
                    onMangaClick = { manga -> 
                        scope.launch { 
                            navigator.push(MangaScreen(manga.id))
                        } 
                    },
                    onMangaLongClick = { manga ->
                        scope.launch {
                            try {
                                val duplicateManga = screenModel.getDuplicateLibraryManga(manga)
                                when {
                                    manga.favorite -> screenModel.setDialog(
                                        BrowseMangaSourceScreenModel.Dialog.RemoveManga(manga),
                                    )
                                    duplicateManga != null -> screenModel.setDialog(
                                        BrowseMangaSourceScreenModel.Dialog.AddDuplicateManga(
                                            manga,
                                            duplicateManga,
                                        ),
                                    )
                                    else -> screenModel.addFavorite(manga)
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
                    is BrowseMangaSourceScreenModel.Dialog.Filter -> {
                        // Temporalmente comentamos esta parte hasta encontrar la clase correcta
                        // SourceFilterMangaDialog(
                        //     onDismissRequest = onDismissRequest,
                        //     filters = state.filters,
                        //     onReset = screenModel::resetFilters,
                        //     onFilter = { screenModel.search(filters = state.filters) },
                        //     onUpdate = screenModel::setFilters,
                        // )
                        // En su lugar, simplemente cerramos el diálogo
                        screenModel.setDialog(null)
                    }
                    is BrowseMangaSourceScreenModel.Dialog.AddDuplicateManga -> {
                        // Igual que browse: permitir agregar, migrar o abrir duplicado
                         DuplicateMangaDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { screenModel.addFavorite(dialog.manga) },
                            onOpenManga = { navigator.push(MangaScreen(dialog.duplicate.id)) },
                            onMigrate = {
                                screenModel.setDialog(
                                    BrowseMangaSourceScreenModel.Dialog.Migrate(dialog.manga, dialog.duplicate),
                                )
                            },
                        )
                    }
                    is BrowseMangaSourceScreenModel.Dialog.Migrate -> {
                        MigrateMangaDialog(
                            oldManga = dialog.oldManga,
                            newManga = dialog.newManga,
                            screenModel = MigrateMangaDialogScreenModel(),
                            onDismissRequest = onDismissRequest,
                            onClickTitle = { navigator.push(MangaScreen(dialog.oldManga.id)) },
                            onPopScreen = { onDismissRequest() },
                        )
                    }
                    is BrowseMangaSourceScreenModel.Dialog.RemoveManga -> {
                        RemoveEntryDialog(
                            onDismissRequest = onDismissRequest,
                            onConfirm = { screenModel.changeMangaFavorite(dialog.manga) },
                            entryToRemove = dialog.manga.title,
                        )
                    }
                    is BrowseMangaSourceScreenModel.Dialog.ChangeMangaCategory -> {
                        ChangeCategoryDialog(
                            initialSelection = dialog.initialSelection,
                            onDismissRequest = onDismissRequest,
                            onEditCategories = { navigator.push(CategoriesTab) },
                            onConfirm = { include: List<Long>, _ ->
                                screenModel.changeMangaFavorite(dialog.manga)
                                screenModel.moveMangaToCategories(dialog.manga, include)
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
                    text = "No hay una extensión de manga destacada. Por favor, marca una con estrella.",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        }
    }
} 
