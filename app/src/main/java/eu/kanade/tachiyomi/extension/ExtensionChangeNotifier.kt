package eu.kanade.tachiyomi.extension

import android.content.Context
import androidx.core.app.NotificationCompat
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.core.security.SecurityPreferences
import eu.kanade.tachiyomi.data.notification.NotificationReceiver
import eu.kanade.tachiyomi.data.notification.Notifications
import eu.kanade.tachiyomi.util.system.cancelNotification
import eu.kanade.tachiyomi.util.system.notify
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/**
 * Clase para notificar al usuario cuando necesite reiniciar la app
 * tras cambiar una extensión predeterminada que no pudo ser recargada automáticamente.
 */
class ExtensionChangeNotifier(
    private val context: Context,
    private val securityPreferences: SecurityPreferences = Injekt.get(),
) {

    /**
     * Muestra una notificación indicando que se requiere reiniciar la app
     * para aplicar los cambios de la extensión predeterminada.
     *
     * @param sourceName El nombre de la fuente que se cambió
     * @param isAnime Si es una extensión de anime o manga
     */
    fun promptRestart(sourceName: String, isAnime: Boolean) {
        context.notify(
            Notifications.ID_EXTENSION_CHANGE,
            Notifications.CHANNEL_EXTENSION_CHANGE,
        ) {
            setContentTitle("Se requiere reiniciar la aplicación")
            if (!securityPreferences.hideNotificationContent().get()) {
                val message = "Has cambiado ${if(isAnime) "la extensión de anime" else "la extensión de manga"} a $sourceName. Reinicia para aplicar los cambios."
                setContentText(message)
                setStyle(NotificationCompat.BigTextStyle().bigText(message))
            }
            setSmallIcon(R.drawable.ic_extension_24dp)
            
            // Abrir la aplicación al hacer clic en la notificación
            if (isAnime) {
                setContentIntent(NotificationReceiver.openAnimeExtensionsPendingActivity(context))
            } else {
                setContentIntent(NotificationReceiver.openExtensionsPendingActivity(context))
            }
            
            setAutoCancel(true)
        }
    }

    /**
     * Oculta la notificación de reinicio
     */
    fun dismiss() {
        context.cancelNotification(Notifications.ID_EXTENSION_CHANGE)
    }
} 