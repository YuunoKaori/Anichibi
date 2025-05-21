package eu.kanade.presentation.browse.manga

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import eu.kanade.domain.source.service.SourcePreferences
import eu.kanade.presentation.browse.manga.components.BaseMangaSourceItem
import eu.kanade.tachiyomi.ui.browse.manga.source.MangaSourcesScreenModel
import eu.kanade.tachiyomi.ui.browse.manga.source.browse.BrowseMangaSourceScreenModel.Listing
import eu.kanade.tachiyomi.util.system.LocaleHelper
import eu.kanade.tachiyomi.util.system.isTvBox
import tachiyomi.domain.source.manga.model.Pin
import tachiyomi.domain.source.manga.model.Source
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.SECONDARY_ALPHA
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.material.topSmallPaddingValues
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.EmptyScreen
import tachiyomi.presentation.core.screens.LoadingScreen
import tachiyomi.presentation.core.theme.header
import tachiyomi.presentation.core.util.plus
import tachiyomi.source.local.entries.manga.LocalMangaSource
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlinx.coroutines.launch

@Composable
fun MangaSourcesScreen(
    state: MangaSourcesScreenModel.State,
    contentPadding: PaddingValues,
    onClickItem: (Source, Listing) -> Unit,
    onClickPin: (Source) -> Unit,
    onLongClickItem: (Source) -> Unit,
) {
    when {
        state.isLoading -> LoadingScreen(Modifier.padding(contentPadding))
        state.isEmpty -> EmptyScreen(
            stringRes = MR.strings.source_empty_screen,
            modifier = Modifier.padding(contentPadding),
        )
        else -> {
            ScrollbarLazyColumn(
                contentPadding = contentPadding + topSmallPaddingValues,
            ) {
                items(
                    items = state.items,
                    contentType = {
                        when (it) {
                            is MangaSourceUiModel.Header -> "header"
                            is MangaSourceUiModel.Item -> "item"
                        }
                    },
                    key = {
                        when (it) {
                            is MangaSourceUiModel.Header -> it.hashCode()
                            is MangaSourceUiModel.Item -> "source-${it.source.key()}"
                        }
                    },
                ) { model ->
                    when (model) {
                        is MangaSourceUiModel.Header -> {
                            SourceHeader(
                                modifier = Modifier.animateItem(),
                                language = model.language,
                            )
                        }
                        is MangaSourceUiModel.Item -> SourceItem(
                            modifier = Modifier.animateItem(),
                            source = model.source,
                            onClickItem = onClickItem,
                            onLongClickItem = onLongClickItem,
                            onClickPin = onClickPin,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SourceHeader(
    language: String,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Text(
        text = LocaleHelper.getSourceDisplayName(language, context),
        modifier = modifier
            .padding(
                horizontal = MaterialTheme.padding.medium,
                vertical = MaterialTheme.padding.small,
            ),
        style = MaterialTheme.typography.header,
    )
}

@Composable
private fun SourceItem(
    source: Source,
    onClickItem: (Source, Listing) -> Unit,
    onLongClickItem: (Source) -> Unit,
    onClickPin: (Source) -> Unit,
    modifier: Modifier = Modifier,
) {
    val sourcePreferences: SourcePreferences = remember { Injekt.get() }
    val starredSourceId = sourcePreferences.starredMangaSource().get()?.toLongOrNull()
    val isStarred = starredSourceId == source.id
    val scope = rememberCoroutineScope()

    // Detectar si estamos en Android TV para comportamiento especial
    val context = LocalContext.current
    val isAndroidTV = remember { isTvBox(context) }
    
    // En Android TV, utilizamos un estilo visual diferente para la fuente seleccionada actual
    val backgroundColor = if (isAndroidTV && isStarred) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    
    // En Android TV, usamos un clic en el elemento para seleccionar como predeterminado
    // y el botón de "Latest" para navegar directamente
    BaseMangaSourceItem(
        modifier = modifier
            .then(
                if (isAndroidTV) {
                    // En Android TV, un clic normal marca la extensión como predeterminada
                    // y navega directamente a ella
                    Modifier
                        .clickable { 
                            // Establecer como predeterminada
                            sourcePreferences.starredMangaSource().set(source.id.toString())
                            
                            // Mostrar feedback visual - usar context capturado en el ámbito externo
                            Toast.makeText(
                                context,
                                "${source.name} establecido como predeterminado y abierto",
                                Toast.LENGTH_SHORT
                            ).show()
                            
                            // Navegar directamente a la fuente
                            onClickItem(source, Listing.Popular)
                        }
                        .background(backgroundColor)
                } else {
                    // En móvil, comportamiento normal
                    Modifier.clickable { onClickItem(source, Listing.Popular) }
                }
            ),
        source = source,
        onClickItem = { 
            if (isAndroidTV) {
                // En TV, el clic ya hace algo diferente (marcar como predeterminado y navegar)
                sourcePreferences.starredMangaSource().set(source.id.toString())
                // Navegar a la fuente
                onClickItem(source, Listing.Popular)
            } else {
                // En móvil, comportamiento normal
                onClickItem(source, Listing.Popular) 
            }
        },
        onLongClickItem = { onLongClickItem(source) },
        action = {
            // Botón Latest siempre visible
            if (source.supportsLatest) {
                TextButton(
                    onClick = { onClickItem(source, Listing.Latest) },
                    // En TV, aumentar el tamaño para facilitar la navegación con D-pad
                    modifier = if (isAndroidTV) Modifier.padding(horizontal = 8.dp, vertical = 4.dp) else Modifier
                ) {
                    Text(
                        text = stringResource(MR.strings.latest),
                        style = LocalTextStyle.current.copy(
                            color = MaterialTheme.colorScheme.primary,
                            // En TV, texto más grande para mejor legibilidad - corregir unidad sp
                            fontSize = if (isAndroidTV) 16.sp else LocalTextStyle.current.fontSize
                        ),
                    )
                }
            }
            
            // En Android TV, mostrar star como indicador de estado, no como botón
            if (isAndroidTV) {
                // Solo mostrar el icono de estrella si está seleccionado para feedback visual
                if (isStarred) {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        tint = MaterialTheme.colorScheme.primary,
                        contentDescription = "Fuente predeterminada",
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            } else {
                // En móvil, mostrar botón interactivo de estrella como antes
            val starIcon = if (isStarred) {
                Icons.Filled.Star
            } else {
                Icons.Outlined.Star
            }
            val starTint = if (isStarred) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onBackground.copy(alpha = SECONDARY_ALPHA)
            }
            val starDescription = if (isStarred) {
                "Quitar como predeterminada"
            } else {
                "Marcar como predeterminada"
            }
            
            IconButton(
                onClick = {
                        sourcePreferences.starredMangaSource().set(
                            if (isStarred) "" else source.id.toString()
                        )
                }
            ) {
                Icon(
                    imageVector = starIcon,
                    tint = starTint,
                    contentDescription = starDescription,
                )
                }
            }
            
            // Botón de pin, solo en móvil
            if (!isAndroidTV) {
            SourcePinButton(
                isPinned = Pin.Pinned in source.pin,
                onClick = { onClickPin(source) },
            )
            }
        },
    )
}

@Composable
private fun SourcePinButton(
    isPinned: Boolean,
    onClick: () -> Unit,
) {
    val icon = if (isPinned) Icons.Filled.PushPin else Icons.Outlined.PushPin
    val tint = if (isPinned) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onBackground.copy(
            alpha = SECONDARY_ALPHA,
        )
    }
    val description = if (isPinned) MR.strings.action_unpin else MR.strings.action_pin
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            tint = tint,
            contentDescription = stringResource(description),
        )
    }
}

@Composable
fun MangaSourceOptionsDialog(
    source: Source,
    onClickPin: () -> Unit,
    onClickDisable: () -> Unit,
    // SY -->
    onClickToggleDataSaver: (() -> Unit)?,
    // SY <--
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(text = source.visualName)
        },
        text = {
            Column {
                val textId = if (Pin.Pinned in source.pin) MR.strings.action_unpin else MR.strings.action_pin
                Text(
                    text = stringResource(textId),
                    modifier = Modifier
                        .clickable(onClick = onClickPin)
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                )
                if (source.id != LocalMangaSource.ID) {
                    Text(
                        text = stringResource(MR.strings.action_disable),
                        modifier = Modifier
                            .clickable(onClick = onClickDisable)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                // SY -->
                if (onClickToggleDataSaver != null) {
                    Text(
                        text = if (source.isExcludedFromDataSaver) {
                            stringResource(MR.strings.data_saver_stop_exclude)
                        } else {
                            stringResource(MR.strings.data_saver_exclude)
                        },
                        modifier = Modifier
                            .clickable(onClick = onClickToggleDataSaver)
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                    )
                }
                // SY <--
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {},
    )
}

sealed interface MangaSourceUiModel {
    data class Item(val source: Source) : MangaSourceUiModel
    data class Header(val language: String) : MangaSourceUiModel
}
