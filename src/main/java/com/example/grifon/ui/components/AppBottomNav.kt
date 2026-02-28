package com.example.grifon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.grifon.R
import com.example.grifon.navigation.Routes

private data class BottomItem(
    val route: String?,
    val labelRes: Int,
    val icon: ImageVector,
    val isLanguageAction: Boolean = false
)

@Composable
fun AppBottomNav(
    navController: NavHostController,
    onLanguageClick: () -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val purpleColor = Color(0xFF6200EE)

    val items = listOf(
        BottomItem(null, R.string.nav_language, Icons.Default.Language, isLanguageAction = true),
        BottomItem(Routes.ACCOUNT, R.string.nav_profile, Icons.Default.Person),
        BottomItem(Routes.HOME, R.string.nav_favs, Icons.Outlined.FavoriteBorder),
        BottomItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
        BottomItem(Routes.HOME, R.string.nav_info, Icons.Default.Info),
        BottomItem(Routes.HOME, R.string.nav_chat, Icons.Default.Chat),
    )

    NavigationBar(
        containerColor = purpleColor,
        contentColor = Color.White
    ) {
        items.forEach { item ->
            val selected = item.route?.let { route ->
                currentDestination?.hierarchy?.any { it.route == route } == true
            } ?: false
            
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (item.isLanguageAction) {
                        onLanguageClick()
                    } else if (item.route != null) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = stringResource(item.labelRes),
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
