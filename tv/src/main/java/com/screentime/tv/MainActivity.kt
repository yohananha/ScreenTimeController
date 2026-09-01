package com.screentime.tv

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewModelScope
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.tv.locale.TvLocaleController
import com.screentime.tv.ui.components.StatusKind
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.components.TvPrimaryButton
import com.screentime.tv.ui.components.TvStatusCircle
import com.screentime.tv.ui.components.TvStepDots
import com.screentime.tv.ui.pairing.PairingScreen
import com.screentime.tv.ui.theme.ScreenTimeTvTheme
import com.screentime.tv.ui.theme.Sprout
import com.screentime.tv.update.UpdateUiState
import com.screentime.tv.update.UpdateViewModel
import com.screentime.tv.usage.InstalledAppsReporter
import com.screentime.tv.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var localeController: TvLocaleController

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // The locale arrives asynchronously from Firestore, so this
            // shadows LocalContext/LocalLayoutDirection live instead of
            // migrating to AppCompatActivity + attachBaseContext (which
            // would need a recreate loop to react to a value that isn't
            // known at process start).
            //
            // Caveat: LocalContext.current below is now a config-wrapped
            // ContextImpl, not this Activity — startActivity on it requires
            // FLAG_ACTIVITY_NEW_TASK (every startActivity call in this file
            // already sets it).
            val locale by localeController.locale.collectAsState()
            val localizedContext = remember(locale) { localeController.wrap(this) }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalLayoutDirection provides localeController.layoutDirection(),
            ) {
                ScreenTimeTvTheme {
                    RootScreen()
                }
            }
        }
    }
}

@HiltViewModel
class FamilyIdViewModel @Inject constructor(
    familyIdProvider: FamilyIdProvider,
) : ViewModel() {
    val familyId: StateFlow<String?> = familyIdProvider.familyId
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun RootScreen(familyVm: FamilyIdViewModel = hiltViewModel()) {
    val context = LocalContext.current
    var permissions by remember { mutableStateOf(PermissionState.read(context)) }
    val familyId by familyVm.familyId.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) permissions = PermissionState.read(context)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            !permissions.usageAccess -> PermissionWall(
                stepCurrent = 1,
                stepTotal = 3,
                headline = stringResource(R.string.permission_usage_headline),
                body = stringResource(R.string.permission_usage_body),
                primary = stringResource(R.string.permission_open_settings),
                onPrimary = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            !permissions.overlay -> PermissionWall(
                stepCurrent = 2,
                stepTotal = 3,
                headline = stringResource(R.string.permission_overlay_headline),
                body = stringResource(R.string.permission_overlay_body),
                primary = stringResource(R.string.permission_open_settings),
                onPrimary = {
                    val uri = Uri.parse("package:${context.packageName}")
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            !permissions.accessibility -> PermissionWall(
                stepCurrent = 3,
                stepTotal = 3,
                headline = stringResource(R.string.permission_accessibility_headline),
                body = stringResource(R.string.permission_accessibility_body),
                primary = stringResource(R.string.permission_open_settings),
                onPrimary = { openAccessibilitySettings(context) },
            )
            familyId == null -> PairingScreen()
            else -> OperationalScreen()
        }
    }
}

/**
 * There's no public framework intent to deep-link straight to a specific
 * accessibility service's toggle on Android TV (the private fragment-args
 * extras that work on phone Settings aren't honored by the leanback/Google TV
 * Settings app), so this opens the top-level accessibility list. The
 * permission_accessibility_body copy tells the user where to look from there.
 */
private fun openAccessibilitySettings(context: Context) {
    context.startActivity(
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
}

@HiltViewModel
class OperationalViewModel @Inject constructor(
    familyIdProvider: FamilyIdProvider,
    private val installedAppsReporter: InstalledAppsReporter,
) : ViewModel() {
    init {
        viewModelScope.launch {
            familyIdProvider.familyId.filterNotNull().collectLatest { familyId ->
                installedAppsReporter.sync(familyId)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun OperationalScreen(@Suppress("UNUSED_PARAMETER") viewModel: OperationalViewModel = hiltViewModel()) {
    TvCanvas {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TvStatusCircle(kind = StatusKind.Mint)
            Column(
                modifier = Modifier.padding(top = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Text(
                    stringResource(R.string.operational_title),
                    style = Sprout.typography.displayHero,
                    color = Sprout.colors.tvCream,
                    textAlign = TextAlign.Center,
                )
                Text(
                    stringResource(R.string.operational_body),
                    style = Sprout.typography.bodyLarge,
                    color = Sprout.colors.tvMutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 550.dp),
                )
                UpdateSection()
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun UpdateSection(viewModel: UpdateViewModel = hiltViewModel()) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()

    Column(
        modifier = Modifier.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        when (val s = state) {
            is UpdateUiState.Idle ->
                TvGhostButton(text = stringResource(R.string.update_check_button), onClick = viewModel::checkForUpdate)

            is UpdateUiState.Checking ->
                Text(stringResource(R.string.update_checking), style = Sprout.typography.bodyLarge, color = Sprout.colors.tvMutedText)

            is UpdateUiState.UpToDate ->
                Text(stringResource(R.string.update_up_to_date), style = Sprout.typography.bodyLarge, color = Sprout.colors.tvMutedText)

            is UpdateUiState.Available -> {
                Text(
                    stringResource(R.string.update_available, s.versionName),
                    style = Sprout.typography.bodyLarge,
                    color = Sprout.colors.tvCream,
                    textAlign = TextAlign.Center,
                )
                TvPrimaryButton(
                    text = stringResource(R.string.update_download_button),
                    onClick = { viewModel.startDownload(s.downloadUrl, s.versionName) },
                )
            }

            is UpdateUiState.Downloading ->
                Text(stringResource(R.string.update_downloading), style = Sprout.typography.bodyLarge, color = Sprout.colors.tvMutedText)

            is UpdateUiState.ReadyToInstall -> {
                if (!viewModel.canRequestInstall()) {
                    Text(
                        stringResource(R.string.update_install_permission_needed),
                        style = Sprout.typography.bodyLarge,
                        color = Sprout.colors.tvMutedText,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.widthIn(max = 550.dp),
                    )
                }
                TvPrimaryButton(
                    text = stringResource(R.string.update_install_button),
                    onClick = {
                        if (viewModel.canRequestInstall()) {
                            viewModel.installIntent(s.downloadId)?.let { context.startActivity(it) }
                        } else {
                            context.startActivity(viewModel.requestInstallPermissionIntent())
                        }
                    },
                )
            }

            is UpdateUiState.Failed ->
                Text(
                    stringResource(R.string.update_failed, s.message),
                    style = Sprout.typography.bodyLarge,
                    color = Sprout.colors.tvMutedText,
                    textAlign = TextAlign.Center,
                )
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PermissionWall(
    stepCurrent: Int,
    stepTotal: Int,
    headline: String,
    body: String,
    primary: String,
    onPrimary: () -> Unit,
) {
    TvCanvas {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 100.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TvStatusCircle(kind = StatusKind.Lilac, size = 84)
            Column(
                modifier = Modifier.padding(top = 22.dp).widthIn(max = 550.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    headline,
                    style = Sprout.typography.displayLarge,
                    color = Sprout.colors.tvCream,
                    textAlign = TextAlign.Center,
                )
                Text(
                    body,
                    style = Sprout.typography.bodyLarge,
                    color = Sprout.colors.tvMutedText,
                    textAlign = TextAlign.Center,
                )
                TvStepDots(current = stepCurrent, total = stepTotal)
                Row(stepCurrent = stepCurrent, primary = primary, onPrimary = onPrimary)
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun Row(stepCurrent: Int, primary: String, onPrimary: () -> Unit) {
    androidx.compose.foundation.layout.Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TvPrimaryButton(text = primary, onClick = onPrimary)
        TvGhostButton(text = stringResource(R.string.permission_why_needed), onClick = { /* expandable info — TODO */ })
    }
}
