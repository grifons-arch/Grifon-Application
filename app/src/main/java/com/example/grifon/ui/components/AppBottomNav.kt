package com.example.grifon.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Category
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.grifon.navigation.Routes

private data class BottomItem(val route: String)

@Composable
fun AppBottomNav(
    navController: NavHostController,
    cartCount: Int,
    favoriteCount: Int,
    isLoggedIn: Boolean,
    canDisplayPrices: Boolean,
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val items = buildList {
        add(BottomItem(Routes.HOME))
        add(BottomItem(Routes.CATEGORIES))
        if (isLoggedIn) {
            add(BottomItem(Routes.FAVORITES))
        }
        if (canDisplayPrices) {
            add(BottomItem(Routes.CART))
        }
        add(BottomItem(Routes.ACCOUNT))
        add(BottomItem(Routes.SETTINGS))
    }

    NavigationBar(
        containerColor = MaterialTheme.colorScheme.primary,
        tonalElevation = 0.dp,
    ) {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            val iconScale by animateFloatAsState(
                targetValue = if (selected) 1.18f else 1f,
                animationSpec = tween(durationMillis = 180),
                label = "bottom_nav_icon_scale",
            )
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
                        Routes.HOME -> Icon(
                            if (selected) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                        )
                        Routes.CATEGORIES -> Icon(
                            if (selected) Icons.Filled.Category else Icons.Outlined.Category,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                        )
                        Routes.FAVORITES -> BadgedBox(
                            badge = { if (favoriteCount > 0) Badge { Text(favoriteCount.toString()) } },
                        ) {
                            Icon(
                                if (selected) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                            )
                        }
                        Routes.CART -> BadgedBox(
                            badge = { if (cartCount > 0) Badge { Text(cartCount.toString()) } },
                        ) {
                            Icon(
                                if (selected) Icons.Filled.ShoppingCart else Icons.Outlined.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                            )
                        }
                        Routes.ACCOUNT -> Icon(
                            if (selected) Icons.Filled.AccountCircle else Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                        )
                        Routes.SETTINGS -> Icon(
                            if (selected) Icons.Filled.Settings else Icons.Outlined.Settings,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                        )
                        else -> Icon(
                            Icons.Outlined.AccountCircle,
                            contentDescription = null,
                            modifier = Modifier.graphicsLayer(scaleX = iconScale, scaleY = iconScale),
                        )
                    }
                },
                label = null, // Αφαίρεση της λεζάντας
                alwaysShowLabel = false,
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondary,
                    unselectedIconColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.42f),
                    indicatorColor = MaterialTheme.colorScheme.secondary,
                ),
            )
        }
    }
}
