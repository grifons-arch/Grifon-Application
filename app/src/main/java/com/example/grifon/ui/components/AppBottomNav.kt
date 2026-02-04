package com.example.grifon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
    val badgeCount: Int? = null,
)

@Composable
fun AppBottomNav(navController: NavHostController, cartCount: Int) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(
        BottomItem(Routes.HOME, "Home", Icons.Default.Home),
        BottomItem(Routes.CATEGORIES, "Categories", Icons.Default.List),
        BottomItem(Routes.CART, "Cart", Icons.Default.ShoppingCart, badgeCount = cartCount),
        BottomItem(Routes.ACCOUNT, "Account", Icons.Default.AccountCircle),
    )

    NavigationBar {
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
                    if (item.badgeCount != null && item.badgeCount > 0) {
                        BadgedBox(
                            badge = { Badge { Text(item.badgeCount.toString()) } },
                        ) {
                            Icon(imageVector = item.icon, contentDescription = item.label)
                        }
                    } else {
                        Icon(imageVector = item.icon, contentDescription = item.label)
                    }
                },
                label = { Text(text = item.label) },
            )
        }
    }
}
