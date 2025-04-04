package eu.kanade.domain.ui.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CollectionsBookmark
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Update
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.FavoritesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.recents.RecentsTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.ui.ver.VerTab
import eu.kanade.tachiyomi.ui.leer.LeerTab
import tachiyomi.i18n.MR

enum class NavStyle(
    val titleRes: StringResource,
    val moreTab: Tab,
) {
    MOVE_MANGA_TO_MORE(titleRes = MR.strings.pref_bottom_nav_no_manga, moreTab = MangaLibraryTab),
    MOVE_UPDATES_TO_MORE(titleRes = MR.strings.pref_bottom_nav_no_updates, moreTab = UpdatesTab),
    MOVE_HISTORY_TO_MORE(titleRes = MR.strings.pref_bottom_nav_no_history, moreTab = HistoriesTab),
    MOVE_BROWSE_TO_MORE(titleRes = MR.strings.pref_bottom_nav_no_browse, moreTab = BrowseTab),
    GROUP_AND_MOVE_UPDATES(titleRes = MR.strings.history, moreTab = UpdatesTab),
    FAVORITES_AND_HISTORY(titleRes = MR.strings.label_library, moreTab = UpdatesTab),
    ;

    val moreIcon: ImageVector
        @Composable
        get() = when (this) {
            MOVE_MANGA_TO_MORE -> Icons.Outlined.CollectionsBookmark
            MOVE_UPDATES_TO_MORE -> Icons.Outlined.Update
            MOVE_HISTORY_TO_MORE -> Icons.Outlined.History
            MOVE_BROWSE_TO_MORE -> Icons.Outlined.Explore
            GROUP_AND_MOVE_UPDATES -> Icons.Outlined.Update
            FAVORITES_AND_HISTORY -> Icons.Outlined.CollectionsBookmark
        }

    val tabs: List<Tab>
        get() {
            return when (this) {
                GROUP_AND_MOVE_UPDATES -> listOf(
                    VerTab(),
                    LeerTab(),
                    RecentsTab,
                    HistoriesTab,
                    BrowseTab,
                    MoreTab,
                )
                FAVORITES_AND_HISTORY -> listOf(
                    VerTab(),
                    LeerTab(),
                    FavoritesTab,
                    HistoriesTab,
                    BrowseTab,
                    MoreTab,
                )
                else -> mutableListOf(
                    VerTab(),
                    LeerTab(),
                    AnimeLibraryTab,
                    MangaLibraryTab,
                    UpdatesTab,
                    HistoriesTab,
                    BrowseTab,
                    MoreTab,
                ).apply { remove(this@NavStyle.moreTab) }
            }
        }
}
