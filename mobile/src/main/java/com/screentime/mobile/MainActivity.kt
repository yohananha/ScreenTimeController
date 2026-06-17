package com.screentime.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.screentime.mobile.ui.auth.AuthState
import com.screentime.mobile.ui.auth.AuthViewModel
import com.screentime.mobile.ui.auth.SignInScreen
import com.screentime.mobile.ui.codes.CodesScreen
import com.screentime.mobile.ui.family.FamilyOnboardingScreen
import com.screentime.mobile.ui.family.InviteScreen
import com.screentime.mobile.ui.limits.LimitsScreen
import com.screentime.mobile.ui.requests.RequestsBadgeViewModel
import com.screentime.mobile.ui.requests.RequestsScreen
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                AuthGate()
            }
        }
    }
}

@Composable
private fun AuthGate(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        AuthState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        AuthState.NeedsSignIn -> SignInScreen()
        AuthState.NeedsFamily -> FamilyOnboardingScreen()
        is AuthState.Authenticated -> AppShell(familyId = current.familyId)
    }
}

private enum class Tab(val route: String, val labelRes: Int, val icon: ImageVector) {
    Limits("limits", R.string.tab_limits, Icons.Filled.LockClock),
    Requests("requests", R.string.tab_requests, Icons.Filled.Notifications),
    Codes("codes", R.string.tab_codes, Icons.Filled.VpnKey),
    Family("family", R.string.tab_family, Icons.Filled.Group),
}

@Composable
private fun AppShell(familyId: String, badgeViewModel: RequestsBadgeViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val hasRequestUpdates by badgeViewModel.hasUpdates.collectAsState()
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    val selected = current?.destination?.hierarchy?.any { it.route == tab.route } == true
                    val showBadge = tab == Tab.Requests && hasRequestUpdates
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            nav.navigate(tab.route) {
                                launchSingleTop = true
                                restoreState = true
                                popUpTo(nav.graph.startDestinationId) { saveState = true }
                            }
                        },
                        icon = {
                            if (showBadge) {
                                BadgedBox(badge = { Badge() }) {
                                    Icon(tab.icon, contentDescription = null)
                                }
                            } else {
                                Icon(tab.icon, contentDescription = null)
                            }
                        },
                        label = { Text(stringResource(tab.labelRes)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Tab.Limits.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(Tab.Limits.route) { LimitsScreen() }
            composable(Tab.Requests.route) { RequestsScreen() }
            composable(Tab.Codes.route) { CodesScreen() }
            composable(Tab.Family.route) { InviteScreen(familyId = familyId) }
        }
    }
}
