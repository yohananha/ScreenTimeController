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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Layers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.tv.material3.Button
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import androidx.lifecycle.viewModelScope
import com.screentime.shared.auth.FamilyIdProvider
import com.screentime.tv.ui.components.IconBadge
import com.screentime.tv.ui.pairing.PairingScreen
import com.screentime.tv.ui.theme.ScreenTimeTvTheme
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

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        when {
            !permissions.usageAccess -> PermissionWall(
                icon = Icons.Filled.BarChart,
                promptRes = R.string.usage_access_prompt,
                actionRes = R.string.grant_usage_access,
                onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            !permissions.overlay -> PermissionWall(
                icon = Icons.Filled.Layers,
                promptRes = R.string.overlay_prompt,
                actionRes = R.string.grant_overlay,
                onClick = {
                    val uri = Uri.parse("package:${context.packageName}")
                    context.startActivity(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, uri)
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                    )
                },
            )
            !permissions.accessibility -> PermissionWall(
                icon = Icons.Filled.Accessibility,
                promptRes = R.string.accessibility_prompt,
                actionRes = R.string.grant_accessibility,
                onClick = {
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
private fun OperationalScreen(viewModel: OperationalViewModel = hiltViewModel()) {
    Column(
        modifier = Modifier.widthIn(max = 720.dp).padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(
            icon = Icons.Filled.CheckCircle,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
            size = 64.dp,
        )
        Text(text = stringResource(R.string.permissions_complete_title), style = MaterialTheme.typography.headlineLarge)
        Text(text = stringResource(R.string.permissions_complete), textAlign = TextAlign.Center)
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
private fun PermissionWall(icon: ImageVector, promptRes: Int, actionRes: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier.widthIn(max = 720.dp).padding(48.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        IconBadge(icon = icon, size = 64.dp)
        Text("Screen Time TV", style = MaterialTheme.typography.headlineLarge)
        Text(stringResource(promptRes), textAlign = TextAlign.Center)
        Button(onClick = onClick) { Text(stringResource(actionRes)) }
    }
}
