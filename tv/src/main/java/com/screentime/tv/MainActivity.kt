package com.screentime.tv

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import com.screentime.tv.ui.components.StatusKind
import com.screentime.tv.ui.components.TvCanvas
import com.screentime.tv.ui.components.TvGhostButton
import com.screentime.tv.ui.components.TvPrimaryButton
import com.screentime.tv.ui.components.TvStatusCircle
import com.screentime.tv.ui.components.TvStepDots
import com.screentime.tv.ui.pairing.PairingScreen
import com.screentime.tv.ui.theme.ScreenTimeTvTheme
import com.screentime.tv.ui.theme.Sprout
import com.screentime.tv.usage.InstalledAppsReporter
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @OptIn(ExperimentalTvMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ScreenTimeTvTheme {
                RootScreen()
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
                headline = "One quick setup",
                body = "ScreenTime needs Usage Access so it can see which app is open and step in only when limits are hit.",
                primary = "Open Android settings",
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
                headline = "One more setting",
                body = "ScreenTime needs \"Display over other apps\" so it can show the gentle reminder when time's up.",
                primary = "Open Android settings",
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
                headline = "Almost there!",
                body = "Turn on the ScreenTime accessibility service so it can tell which app is open and step in only when needed.",
                primary = "Open Android settings",
                onPrimary = {
                    context.startActivity(
                        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            familyId == null -> PairingScreen()
            else -> OperationalScreen()
        }
    }
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
                    "You're all set!",
                    style = Sprout.typography.displayHero,
                    color = Sprout.colors.tvCream,
                    textAlign = TextAlign.Center,
                )
                Text(
                    "ScreenTime is running in the background. We'll only show up when a limit hits — promise.",
                    style = Sprout.typography.bodyLarge,
                    color = Sprout.colors.tvMutedText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.widthIn(max = 550.dp),
                )
            }
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
        TvGhostButton(text = "Why do you need this?", onClick = { /* expandable info — TODO */ })
    }
}
