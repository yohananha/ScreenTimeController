package com.screentime.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.screentime.mobile.ui.theme.Sprout
import com.screentime.mobile.ui.theme.SproutRadius

import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.ui.unit.sp

enum class NavTab(val route: String, val label: String, val icon: ImageVector) {
    Limits("limits", "Limits", Icons.Filled.AccessTime),
    Requests("requests", "Requests", Icons.Filled.NotificationsActive),
    Codes("codes", "Codes", Icons.Filled.Dialpad),
    Family("family", "Family", Icons.Filled.People),
}

@Composable
fun SproutBottomNavBar(
    selectedRoute: String,
    pendingCount: Int,
    onTabClick: (NavTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(Sprout.colors.outline)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Sprout.colors.surface)
                .padding(start = 6.dp, end = 6.dp, top = 8.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NavTab.entries.forEach { tab ->
                val selected = selectedRoute == tab.route ||
                    (tab == NavTab.Limits && selectedRoute == "history")
                NavItem(
                    tab = tab,
                    selected = selected,
                    pendingCount = if (tab == NavTab.Requests) pendingCount else 0,
                    onClick = { onTabClick(tab) },
                )
            }
        }
    }
}

@Composable
private fun NavItem(tab: NavTab, selected: Boolean, pendingCount: Int, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(72.dp)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            val pillModifier = if (selected) {
                Modifier
                    .width(60.dp)
                    .height(32.dp)
                    .background(Sprout.colors.accentContainer, SproutRadius.pill)
            } else {
                Modifier.height(32.dp).width(60.dp)
            }
            Box(modifier = pillModifier, contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = tab.icon,
                    contentDescription = tab.label,
                    tint = if (selected) Sprout.colors.ink else Sprout.colors.inkMuted,
                    modifier = Modifier.size(22.dp),
                )
            }
            if (pendingCount > 0) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .offset(x = 4.dp, y = (-4).dp)
                        .background(Sprout.colors.overDisplay, SproutRadius.pill)
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                        .sizeIn(minWidth = 16.dp, minHeight = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = pendingCount.toString(),
                        color = Color.White,
                        style = Sprout.typography.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.ExtraBold),
                    )
                }
            }
        }
        Text(
            tab.label,
            style = Sprout.typography.caption.copy(
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold,
            ),
            color = if (selected) Sprout.colors.ink else Sprout.colors.inkMuted,
        )
    }
}

@Suppress("unused")
private val _color: Color = Color.Transparent
