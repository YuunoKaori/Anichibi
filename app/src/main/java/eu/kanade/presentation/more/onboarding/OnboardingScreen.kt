package eu.kanade.presentation.more.onboarding

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RocketLaunch
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import soup.compose.material.motion.animation.materialSharedAxisX
import soup.compose.material.motion.animation.rememberSlideDistance
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.InfoScreen
import eu.kanade.tachiyomi.util.system.isTvBox
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.domain.ui.model.ThemeMode
import tachiyomi.domain.storage.service.StoragePreferences
import tachiyomi.core.common.storage.AndroidStorageFolderProvider
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import eu.kanade.tachiyomi.R as AppR

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onRestoreBackup: () -> Unit,
) {
    val slideDistance = rememberSlideDistance()
    val context = LocalContext.current
    val isTv = isTvBox(context)

    // Si es Android TV, mostrar una versión simplificada
    if (isTv) {
        SimplifiedTvOnboarding(onComplete)
        return
    }

    // Versión normal para dispositivos móviles
    var currentStep by rememberSaveable { mutableIntStateOf(0) }
    val steps = remember {
        listOf(
            ThemeStep(),
            StorageStep(),
            PermissionStep(),
            ExtensionsStep(),
            GuidesStep(onRestoreBackup = onRestoreBackup),
        )
    }
    val isLastStep = currentStep == steps.lastIndex

    BackHandler(enabled = currentStep != 0, onBack = { currentStep-- })

    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = stringResource(MR.strings.onboarding_heading),
        subtitleText = stringResource(MR.strings.onboarding_description),
        acceptText = stringResource(
            if (isLastStep) {
                MR.strings.onboarding_action_finish
            } else {
                MR.strings.onboarding_action_next
            },
        ),
        canAccept = steps[currentStep].isComplete,
        onAcceptClick = {
            if (isLastStep) {
                onComplete()
            } else {
                currentStep++
            }
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.padding.medium)
                .clip(MaterialTheme.shapes.extraLarge)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(MaterialTheme.padding.medium),
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    materialSharedAxisX(
                        forward = targetState > initialState,
                        slideDistance = slideDistance,
                    )
                },
                    label = "step_transition",
                ) { step ->
                    steps[step].Content()
                }
            }
        }
    }
}

@Composable
private fun SimplifiedTvOnboarding(onComplete: () -> Unit) {
    // Configurar tema oscuro automáticamente para TV
    val uiPreferences: UiPreferences = Injekt.get()
    uiPreferences.themeMode().set(ThemeMode.SYSTEM)
    
    // Configurar almacenamiento por defecto
    val storagePreferences = Injekt.get<StoragePreferences>()
    val folderProvider = Injekt.get<AndroidStorageFolderProvider>()
    val storage = folderProvider.directory()
    if (!storage.exists()) {
        storage.mkdirs()
            }
    
    val context = LocalContext.current
    
    InfoScreen(
        icon = Icons.Outlined.RocketLaunch,
        headingText = stringResource(MR.strings.app_name),
        subtitleText = androidx.compose.ui.res.stringResource(AppR.string.tv_onboarding_description),
        acceptText = stringResource(MR.strings.action_ok),
        canAccept = true,
        onAcceptClick = { 
            // Primero ejecutamos la acción original
            onComplete()
            
            // Luego reiniciamos la app
            val activity = context as? Activity
            activity?.let {
                val intent = it.packageManager.getLaunchIntentForPackage(it.packageName)
                intent?.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                intent?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                it.startActivity(intent)
                it.finish()
            }
        },
    ) {
        // Pantalla simplificada, sin pasos adicionales
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(MaterialTheme.padding.medium),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(AppR.string.tv_onboarding_description),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}
