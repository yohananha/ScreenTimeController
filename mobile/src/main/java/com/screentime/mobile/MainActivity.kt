package com.screentime.mobile

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.screentime.mobile.ui.auth.AuthState
import com.screentime.mobile.ui.auth.AuthViewModel
import com.screentime.mobile.ui.auth.SignInScreen
import com.screentime.mobile.ui.components.NavTab
import com.screentime.mobile.ui.components.PeachPlumBottomNavBar
import com.screentime.mobile.ui.components.UnlockSheet
import com.screentime.mobile.ui.family.FamilyOnboardingScreen
import com.screentime.mobile.ui.family.FamilyScreen
import com.screentime.mobile.ui.limits.TimeFrameScreen
import com.screentime.mobile.ui.rules.RulesScreen
import com.screentime.mobile.ui.theme.ScreenTimeTheme
import com.screentime.mobile.ui.theme.PeachPlum
import com.screentime.mobile.ui.today.TodayScreen
import com.screentime.mobile.whatsnew.WhatsNewDialog
import com.screentime.mobile.whatsnew.WhatsNewViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    // AppCompatActivity (not ComponentActivity) so
    // AppCompatDelegate.setApplicationLocales() actually recreates this
    // Activity with the new locale on API < 33. Requires
    // Theme.ScreenTimeMobile to be AppCompat-derived (see themes.xml) —
    // otherwise onCreate() throws IllegalStateException immediately.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ScreenTimeTheme {
                Box(modifier = Modifier.fillMaxSize().background(PeachPlum.colors.background)) {
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
            CircularProgressIndicator(color = PeachPlum.colors.primary)
        }
        AuthState.NeedsSignIn -> SignInScreen()
        AuthState.NeedsFamily -> FamilyOnboardingScreen()
        is AuthState.Authenticated -> AppShell(familyId = current.familyId)
    }
}

@Composable
private fun AppShell(
    familyId: String,
    whatsNewViewModel: WhatsNewViewModel = hiltViewModel(),
) {
    val nav = rememberNavController()
    val current by nav.currentBackStackEntryAsState()
    val currentRoute = current?.destination?.route ?: NavTab.Today.route
    val whatsNewEntry by whatsNewViewModel.entryToShow.collectAsState()
    whatsNewEntry?.let { entry ->
        WhatsNewDialog(entry = entry, onDismiss = whatsNewViewModel::dismiss)
    }
    var unlockOpen by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = PeachPlum.colors.background,
        bottomBar = {
            PeachPlumBottomNavBar(
                selectedRoute = currentRoute,
                onTabClick = { tab ->
                    nav.navigate(tab.route) {
                        launchSingleTop = true
                        restoreState = true
                        popUpTo(nav.graph.startDestinationId) { saveState = true }
                    }
                },
                onUnlockClick = { unlockOpen = true },
            )
        },
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = NavTab.Today.route,
            modifier = Modifier.padding(padding),
        ) {
            composable(NavTab.Today.route) {
                TodayScreen(
                    onOpenRules = { nav.navigate(NavTab.Rules.route) },
                    onOpenFamily = { nav.navigate(NavTab.Family.route) },
                )
            }
            composable(NavTab.Rules.route) {
                RulesScreen(
                    onOpenFamily = { nav.navigate(NavTab.Family.route) },
                    onOpenTimeFrame = { nav.navigate("timeframe") },
                )
            }
            composable("timeframe") {
                TimeFrameScreen(onBack = { nav.popBackStack() })
            }
            composable(NavTab.Family.route) { FamilyScreen(familyId = familyId) }
        }
    }

    if (unlockOpen) {
        UnlockSheet(onDismiss = { unlockOpen = false })
    }
}
