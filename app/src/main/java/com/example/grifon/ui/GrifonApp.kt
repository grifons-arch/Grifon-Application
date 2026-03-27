package com.example.grifon.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.ui.screens.LocalCanDisplayPrices
import com.example.grifon.viewmodel.AppViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GrifonApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var forceSearchOpen by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .filter { it.length >= 2 }
            .debounce(500)
            .distinctUntilChanged()
            .collect { query ->
                navController.navigate(Routes.plpRoute(query = query)) { launchSingleTop = true }
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Αφαιρέθηκε το nestedScroll που προκαλούσε το κόλλημα
        topBar = {
            AppTopBar(
                shopLabel = appState.shopName,
                query = searchQuery,
                isSearchExpanded = forceSearchOpen || searchQuery.isNotEmpty(),
                onQueryChange = { searchQuery = it },
                onLogoClick = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onSearchIconClick = { forceSearchOpen = true },
                onScanClick = { navController.navigate(Routes.SCAN) },
            )
        },
        bottomBar = { AppBottomNav(navController = navController, cartCount = appState.cartCount) },
    ) { padding ->
        CompositionLocalProvider(LocalCanDisplayPrices provides appState.canDisplayPrices) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavHost(navController = navController, paddingValues = padding)
            }
        }
    }
}
