package com.example.grifon.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
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
    val navBackground = Color(0xFF06080D)
    val tileSelected = Color(0xFF4B2D8F)
    val tileUnselected = Color(0xFF261C3F)
    val selectedUnderline = Color(0xFF16C79A)

    val items = listOf(
        BottomItem(null, R.string.nav_language, Icons.Default.Language, isLanguageAction = true),
        BottomItem(Routes.ACCOUNT, R.string.nav_profile, Icons.Default.Person),
        BottomItem(Routes.HOME, R.string.nav_favs, Icons.Outlined.FavoriteBorder),
        BottomItem(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings),
        BottomItem(Routes.HOME, R.string.nav_info, Icons.Default.Info),
        BottomItem(Routes.HOME, R.string.nav_chat, Icons.Default.Chat),
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = navBackground,
        contentColor = Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items.forEach { item ->
                val selected = item.route?.let { route ->
                    currentDestination?.hierarchy?.any { it.route == route } == true
                } ?: false

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp)
                        .background(
                            color = if (selected) tileSelected else tileUnselected,
                            shape = RoundedCornerShape(4.dp)
                        )
                        .clickable {
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
                        }
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = stringResource(item.labelRes),
                        tint = Color.White,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .size(20.dp)
                    )

                    if (selected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 2.dp)
                                .width(28.dp)
                                .height(3.dp)
                                .background(selectedUnderline, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}
