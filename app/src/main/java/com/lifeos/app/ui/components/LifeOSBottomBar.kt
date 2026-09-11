package com.lifeos.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.lifeos.app.ui.navigation.Screen
import com.lifeos.app.ui.theme.LifeOSLavender
import com.lifeos.app.ui.theme.LifeOSPrimary
import com.lifeos.app.ui.theme.LifeOSPrimaryBright

private data class BottomItem(
    val screen: Screen,
    val label: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

private val items = listOf(
    BottomItem(Screen.Home, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomItem(Screen.Timeline, "Timeline", Icons.Filled.Timeline, Icons.Outlined.Timeline),
    BottomItem(Screen.Tasks, "Tasks", Icons.Filled.CheckCircle, Icons.Outlined.CheckCircle),
    BottomItem(Screen.Habits, "Habits", Icons.Filled.LocalFireDepartment, Icons.Outlined.LocalFireDepartment),
    BottomItem(Screen.Settings, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings),
)

@Composable
fun LifeOSBottomBar(navController: NavHostController) {
    val entry by navController.currentBackStackEntryAsState()
    val currentRoute = entry?.destination?.route
    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = .96f),
        tonalElevation = 0.dp,
        windowInsets = androidx.compose.foundation.layout.WindowInsets.navigationBars
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.screen.route || (item.screen == Screen.Home && currentRoute == Screen.Home.route)
            NavigationBarItem(
                selected = selected,
                onClick = {
                    // Home is always a safe anchor. This avoids a stale restored child state
                    // swallowing a tap when returning from Expenses/Timeline.
                    if (item.screen == Screen.Home) {
                        navController.popBackStack(Screen.Home.route, inclusive = false)
                    } else {
                        navController.navigate(item.screen.route) {
                            popUpTo(Screen.Home.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                label = { Text(item.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = LifeOSPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = LifeOSLavender.copy(alpha = .72f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}
