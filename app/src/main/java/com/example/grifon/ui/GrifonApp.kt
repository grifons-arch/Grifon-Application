package com.example.grifon.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppMenuItem
import com.example.grifon.ui.components.AppSearchBar
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.viewmodel.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.snapshotFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GrifonApp() {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val appViewModel: AppViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val activeRoute = navBackStackEntry?.destination?.route
    var searchSheetOpen by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val menuItems = listOf(
        AppMenuItem("Αρχική", Routes.HOME),
        AppMenuItem("Κατηγορίες", Routes.CATEGORIES),
        AppMenuItem("Καλάθι", Routes.CART),
        AppMenuItem("Λογαριασμός", Routes.ACCOUNT),
        AppMenuItem("Ρυθμίσεις", Routes.SETTINGS),
    )

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .debounce(350)
            .distinctUntilChanged()
            .collect { query ->
                navController.navigate(Routes.plpRoute(query = query))
            }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            AppTopBar(
                shopLabel = if (appState.activeShopId == "shop_a") "Shop A" else "Shop B",
                onLogoClick = { navController.navigateToTopLevel(Routes.HOME) },
                onSearchClick = { searchSheetOpen = true },
                onScanClick = { navController.navigate(Routes.SCAN) },
                scrollBehavior = scrollBehavior,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                menuItems = menuItems,
                activeRoute = activeRoute,
                onMenuClick = { item -> navController.navigateToTopLevel(item.route) },
            )
        },
        bottomBar = {
            AppBottomNav(navController = navController, cartCount = appState.cartCount)
        },
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize()) {
            AppNavHost(navController = navController, paddingValues = paddingValues)
        }
    }

    if (searchSheetOpen) {
        ModalBottomSheet(onDismissRequest = { searchSheetOpen = false }) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = { query -> searchQuery = query },
                onScanClick = { navController.navigate(Routes.SCAN) },
            )
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
