package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Rule
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.screentime.mobile.R
import com.screentime.mobile.ui.theme.PeachPlum

/**
 * Today · Rules · Family + a round Unlock button on a dark pill (design/
 * i6c-peach-plum tokens.json#sizes navPillHeight/navItemHeight — the phone
 * counterpart to web's NavPill.tsx). Folds in what used to be the
 * Requests/Codes tabs: Requests is now inline on Today, Codes is the
 * standalone Unlock sheet, so neither gets a tab of its own any more.
 */
enum class NavTab(val route: String, @StringRes val labelRes: Int, val icon: ImageVector) {
    Today("today", R.string.today_nav_today, Icons.Filled.Home),
    Rules("rules", R.string.today_nav_rules, Icons.AutoMirrored.Filled.Rule),
    Family("family", R.string.today_nav_family, Icons.Filled.Group),
}

@Composable
fun PeachPlumBottomNavBar(
    selectedRoute: String,
    onTabClick: (NavTab) -> Unit,
    onUnlockClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp)
                .background(PeachPlum.colors.ink, PeachPlum.radius.pill)
                .padding(6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab.entries.forEach { tab ->
                NavItem(tab = tab, selected = selectedRoute == tab.route, onClick = { onTabClick(tab) }, modifier = Modifier.weight(1f))
            }
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(PeachPlum.colors.primary, PeachPlum.radius.pill)
                    .clickable(onClick = onUnlockClick),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.LockOpen,
                    contentDescription = stringResource(R.string.today_unlock_aria),
                    tint = PeachPlum.colors.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val label = stringResource(tab.labelRes)
    Box(
        modifier = modifier
            .height(46.dp)
            .background(if (selected) Color.White.copy(alpha = 0.12f) else Color.Transparent, PeachPlum.radius.pill)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            style = PeachPlum.typography.label,
            color = if (selected) PeachPlum.colors.background else PeachPlum.colors.inkFaint,
        )
    }
}
