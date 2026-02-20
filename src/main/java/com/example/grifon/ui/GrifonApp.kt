package com.example.grifon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.viewmodel.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GrifonApp() {
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Παρακολούθηση της τρέχουσας διαδρομής
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Βελτιωμένη λογική αναζήτησης
    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            snapshotFlow { searchQuery }
                .debounce(600)
                .distinctUntilChanged()
                .collect { query ->
                    navController.navigate(Routes.plpRoute(query = query)) {
                        popUpTo(Routes.HOME) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Μενού", 
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                HorizontalDivider()
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        NavigationDrawerItem(
                            label = { Text("Αρχική Οθόνη", fontSize = 16.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.HOME)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Λογαριασμός", fontSize = 16.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.ACCOUNT)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Βάσεις Δεδομένων", fontSize = 16.sp) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.CATEGORIES)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                AppTopBar(
                    shopLabel = if (appState.activeShopId == "1") "SE" else "GR",
                    onMenuClick = {
                        scope.launch { drawerState.open() }
                    },
                    onHomeClick = { 
                        searchQuery = "" 
                        navController.navigateToTopLevel(Routes.HOME) 
                    },
                    onCartClick = { navController.navigateToTopLevel(Routes.CART) },
                    onNotificationsClick = { navController.navigateToTopLevel(Routes.ACCOUNT) },
                    categories = appState.categories,
                    onCategoryClick = { category ->
                        navController.navigate(Routes.plpRoute(category = category.id))
                    },
                    showSearch = currentRoute == Routes.HOME, // Μόνο στην αρχική
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onScanClick = { navController.navigate(Routes.SCAN) }
                )
            },
            bottomBar = {
                AppBottomNav(navController = navController)
            },
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {
                AppNavHost(
                    navController = navController,
                    paddingValues = PaddingValues(0.dp)
                )
            }
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
