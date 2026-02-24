package com.example.grifon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.grifon.navigation.Routes

private data class BottomItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

@Composable
fun AppBottomNav(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val purpleColor = Color(0xFF6200EE)

    val items = listOf(
        BottomItem(Routes.SETTINGS, "Language", Icons.Default.Language),
        BottomItem(Routes.ACCOUNT, "Profile", Icons.Default.Person),
        BottomItem(Routes.FAVORITES, "Favs", Icons.Default.Favorite), // Διόρθωση διαδρομής
        BottomItem(Routes.SETTINGS, "Settings", Icons.Default.Settings),
        BottomItem(Routes.HOME, "Info", Icons.Default.Info),
        BottomItem(Routes.HOME, "Chat", Icons.Default.Chat),
    )

    NavigationBar(
        containerColor = purpleColor,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.label,
                        tint = if (selected) Color.White else Color.White.copy(alpha = 0.7f)
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = Color.White.copy(alpha = 0.2f)
                )
            )
        }
    }
}
