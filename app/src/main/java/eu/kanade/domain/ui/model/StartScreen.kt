package eu.kanade.domain.ui.model

import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.util.Tab
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.history.HistoriesTab
import eu.kanade.tachiyomi.ui.library.anime.AnimeLibraryTab
import eu.kanade.tachiyomi.ui.library.manga.MangaLibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import eu.kanade.tachiyomi.ui.ver.VerTab
import eu.kanade.tachiyomi.ui.leer.LeerTab
import tachiyomi.i18n.MR

enum class StartScreen(val titleRes: dev.icerock.moko.resources.StringResource) {
    ANIME(MR.strings.label_anime_library),
    MANGA(MR.strings.label_manga_library),
    UPDATES(MR.strings.label_recent_updates),
    HISTORY(MR.strings.label_recent_manga),
    BROWSE(MR.strings.browse),
    VER(MR.strings.label_anime),
    LEER(MR.strings.manga),
    MORE(MR.strings.label_more);

    val tab
        get() = when (this) {
            ANIME -> AnimeLibraryTab
            MANGA -> MangaLibraryTab
            UPDATES -> UpdatesTab
            HISTORY -> HistoriesTab
            BROWSE -> BrowseTab
            VER -> VerTab()
            LEER -> LeerTab()
            MORE -> MoreTab
        }
}
