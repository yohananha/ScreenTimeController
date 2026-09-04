package com.screentime.mobile.ui.settings

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.screentime.mobile.R
import com.screentime.mobile.ui.components.SproutPrimaryButton
import com.screentime.mobile.ui.theme.Sprout

/**
 * Settings-screen counterpart to the system permission prompt: a persistent
 * place to check whether "new time request" pushes are actually reaching
 * this device, and to (re-)enable them, since Android 13+ gates delivery
 * behind POST_NOTIFICATIONS and there was previously no in-app way to ask
 * for it — PushService.onNewToken silently registers the FCM token
 * regardless, but nothing shows if the OS is dropping the notification.
 */
@Composable
fun NotificationsSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    // Once a runtime request has been answered (granted or denied), Android
    // won't show the system dialog again — a second launch() no-ops. Track
    // that so the button falls back to deep-linking into system settings.
    var requestedOnce by rememberSaveable { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                enabled = NotificationManagerCompat.from(context).areNotificationsEnabled()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        enabled = granted
        requestedOnce = true
    }

    val canPromptInApp = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !requestedOnce

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Sprout.colors.surface, Sprout.radius.card)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(stringResource(R.string.settings_notifications_title), style = Sprout.typography.headline, color = Sprout.colors.ink)
        Text(stringResource(R.string.settings_notifications_hint), style = Sprout.typography.caption, color = Sprout.colors.inkMuted)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = if (enabled) stringResource(R.string.settings_notifications_on) else stringResource(R.string.settings_notifications_off),
                style = Sprout.typography.body,
                color = Sprout.colors.ink,
            )
            if (!enabled) {
                SproutPrimaryButton(
                    text = stringResource(
                        if (canPromptInApp) R.string.settings_notifications_enable else R.string.settings_notifications_open_settings,
                    ),
                    onClick = {
                        if (canPromptInApp) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            context.startActivity(
                                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName),
                            )
                        }
                    },
                )
            }
        }
    }
}
