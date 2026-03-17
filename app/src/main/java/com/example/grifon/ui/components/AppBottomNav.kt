package com.example.grifon.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.grifon.navigation.Routes

private data class BottomItem(val route: String, val label: String)

@Composable
fun AppBottomNav(navController: NavHostController, cartCount: Int) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = listOf(
        BottomItem(Routes.HOME, "Home"),
        BottomItem(Routes.CATEGORIES, "Categories"),
        BottomItem(Routes.CART, "Cart"),
        BottomItem(Routes.ACCOUNT, "Account"),
        BottomItem(Routes.SETTINGS, "Settings"),
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    when (item.route) {
                        Routes.HOME -> Icon(Icons.Outlined.Home, contentDescription = item.label)
                        Routes.CATEGORIES -> Icon(Icons.Outlined.Category, contentDescription = item.label)
                        Routes.CART -> BadgedBox(
                            badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } },
                        ) { Icon(Icons.Outlined.ShoppingCart, contentDescription = item.label) }
                        Routes.ACCOUNT -> Icon(Icons.Outlined.AccountCircle, contentDescription = item.label)
                        Routes.SETTINGS -> Icon(Icons.Outlined.Settings, contentDescription = item.label)
                        else -> Icon(Icons.Outlined.AccountCircle, contentDescription = item.label)
                    }
                },
                label = { Text(item.label) },
            )
        }
    }
}
