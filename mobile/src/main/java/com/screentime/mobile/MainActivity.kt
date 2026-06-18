package com.screentime.mobile

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.screentime.mobile.ui.components.NavTab
import com.screentime.mobile.ui.components.SproutBottomNavBar
import com.screentime.mobile.ui.family.FamilyOnboardingScreen
import com.screentime.mobile.ui.family.InviteScreen
import com.screentime.mobile.ui.history.HistoryScreen
import com.screentime.mobile.ui.limits.LimitsScreen
import com.screentime.mobile.ui.limits.TimeFrameScreen
import com.screentime.mobile.ui.requests.RequestsBadgeViewModel
import com.screentime.mobile.ui.requests.RequestsScreen
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.theme.Sprout
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                Box(modifier = Modifier.fillMaxSize().background(Sprout.colors.background)) {
                    AuthGate()
                }
            }
        }
    }
}

@Composable
private fun AuthGate(viewModel: AuthViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    when (val current = state) {
        AuthState.Loading -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Sprout.colors.primary)
        }
        AuthState.NeedsSignIn -> SignInScreen()
        AuthState.NeedsFamily -> FamilyOnboardingScreen()
        is AuthState.Authenticated -> AppShell(familyId = current.familyId)
    }
}

@Composable
private fun AppShell(familyId: String, badgeViewModel: RequestsBadgeViewModel = hiltViewModel()) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val pendingRequestsCount by badgeViewModel.pendingCount.collectAsState()
    val currentRoute = current?.destination?.route ?: NavTab.Limits.route
    Scaffold(
        containerColor = Sprout.colors.background,
        bottomBar = {
            SproutBottomNavBar(
                selectedRoute = currentRoute,
                pendingCount = pendingRequestsCount,
                onTabClick = { tab ->
                    nav.navigate(tab.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                    }
                },
            )
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = NavTab.Limits.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(NavTab.Limits.route) {
                LimitsScreen(
                    onOpenHistory = { nav.navigate("history") },
                    onOpenTimeFrame = { nav.navigate("timeframe") },
                )
            }
            composable("timeframe") {
                TimeFrameScreen(onBack = { nav.popBackStack() })
            }
            composable(NavTab.Requests.route) { RequestsScreen() }
            composable(NavTab.Codes.route) { CodesScreen() }
            composable(NavTab.Family.route) { InviteScreen(familyId = familyId) }
            composable("history") { HistoryScreen() }
        }
    }
}
