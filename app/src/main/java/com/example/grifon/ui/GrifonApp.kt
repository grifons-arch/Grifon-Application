package com.example.grifon.ui

import android.util.Log
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
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.ui.screens.LocalCanDisplayPrices
import com.example.grifon.ui.screens.LocalIsLoggedIn
import com.example.grifon.viewmodel.AppViewModel
import com.example.grifon.viewmodel.SettingsViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GrifonApp() {
    Log.d("CrashLog", "GrifonApp: Start")
    
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val hasBackStack = navController.previousBackStackEntry != null
    var searchQuery by remember { mutableStateOf("") }
    var forceSearchOpen by remember { mutableStateOf(false) }
    val topLevelRoutes = remember {
        setOf(
            Routes.HOME,
            Routes.CATEGORIES,
            Routes.CART,
            Routes.FAVORITES,
            Routes.ACCOUNT,
            Routes.SETTINGS,
        )
    }
    val shouldShowBackArrow = currentRoute != null && currentRoute !in topLevelRoutes
        && hasBackStack

    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .debounce(500)
            .distinctUntilChanged()
            .collect { query ->
                navController.navigate(Routes.plpRoute(query = query))
            }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(), // Αφαιρέθηκε το nestedScroll που προκαλούσε το κόλλημα
        topBar = {
            AppTopBar(
                shopLabel = appState.shopName,
                query = searchQuery,
                isSearchExpanded = forceSearchOpen || searchQuery.isNotEmpty(),
                showBackArrow = shouldShowBackArrow,
                showHomeInfoBanner = currentRoute == Routes.HOME,
                currentLanguage = appState.languageCode,
                onQueryChange = { searchQuery = it },
                onBackClick = { navController.navigateUp() },
                onLogoClick = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(navController.graph.findStartDestination().id)
                        launchSingleTop = true
                    }
                },
                onSearchIconClick = { forceSearchOpen = !forceSearchOpen },
                onScanClick = { navController.navigate(Routes.SCAN) },
                onLanguageSelect = { settingsViewModel.setLanguage(it) },
                onDismissSearch = {
                    forceSearchOpen = false
                    searchQuery = ""
                },
                onSearchSubmit = { submittedQuery ->
                    searchQuery = submittedQuery
                    navController.navigate(Routes.plpRoute(query = submittedQuery))
                },
            )
        },
        bottomBar = {
            AppBottomNav(
                navController = navController,
                cartCount = appState.cartCount,
                favoriteCount = appState.favoriteCount,
                isLoggedIn = appState.isLoggedIn,
                canDisplayPrices = appState.canDisplayPrices,
            )
        },
    ) { padding ->
        CompositionLocalProvider(
            LocalCanDisplayPrices provides appState.canDisplayPrices,
            LocalIsLoggedIn provides appState.isLoggedIn,
        ) {
            Surface(modifier = Modifier.fillMaxSize()) {
                AppNavHost(navController = navController, paddingValues = padding)
            }
        }
    }
}
