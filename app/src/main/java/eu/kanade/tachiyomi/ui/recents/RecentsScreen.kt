package eu.kanade.tachiyomi.ui.recents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

class RecentsScreen : Screen {

    @Composable
    override fun Content() {
        var selectedTabIndex by remember { mutableStateOf(0) }
        val tabs = remember {
            listOf(
                TabItem(MR.strings.label_anime) { AnimeLibraryTab.Content() },
                TabItem(MR.strings.label_manga) { MangaLibraryTab.Content() },
                TabItem(MR.strings.history) { HistoriesTab.Content() },
            )
        }

        Scaffold { paddingValues ->
            Column(
                modifier = Modifier.padding(paddingValues),
            ) {
                TabRow(selectedTabIndex = selectedTabIndex) {
                    tabs.forEachIndexed { index, tab ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(text = stringResource(tab.titleRes)) },
                        )
                    }
                }

                tabs[selectedTabIndex].content()
            }
        }
    }
}

private data class TabItem(
    val titleRes: dev.icerock.moko.resources.StringResource,
    val content: @Composable () -> Unit,
) 