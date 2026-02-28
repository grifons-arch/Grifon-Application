package com.example.grifon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavHostController
import androidx.navigation.NavGraph.Companion.findStartDestination
import com.example.grifon.R
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppSearchBar
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.viewmodel.AppViewModel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.debounce
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.launch
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items

/**
 * Data class representing a category in the navigation drawer.
 * @property nameRes The resource ID for the display name of the category.
 * @property id The unique identifier of the category used for navigation.
 */
private data class DrawerCategory(
    val nameRes: Int,
    val id: String,
)

/**
 * The main entry point for the Grifon application UI.
 * 
 * This composable sets up the overall structure of the app, including:
 * - A [ModalNavigationDrawer] for category-based navigation.
 * - A [Scaffold] containing the [AppTopBar], [AppSearchBar], and [AppBottomNav].
 * - The [AppNavHost] to manage screen transitions.
 * - Logic for debounced search navigation.
 */
@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GrifonApp() {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())
    val appViewModel: AppViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Categories for the drawer (hamburger menu) using string resources
    val categories = listOf(
        DrawerCategory(nameRes = R.string.ceramics, id = "3"),
        DrawerCategory(nameRes = R.string.lighting, id = "4"),
        DrawerCategory(nameRes = R.string.bronze, id = "5"),
        DrawerCategory(nameRes = R.string.toys, id = "6"),
        DrawerCategory(nameRes = R.string.soaps, id = "7"),
        DrawerCategory(nameRes = R.string.textiles, id = "8"),
    )

    // Effect to handle search query changes with debounce and navigation to PLP
    LaunchedEffect(Unit) {
        snapshotFlow { searchQuery }
            .filter { it.length >= 2 }
            .debounce(700)
            .distinctUntilChanged()
            .collect { query ->
                navController.navigate(Routes.plpRoute(query = query)) {
                    launchSingleTop = true 
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
                    stringResource(R.string.shop_by_category), 
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                HorizontalDivider()
                
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(categories) { category ->
                        NavigationDrawerItem(
                            label = { 
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(stringResource(category.nameRes), fontSize = 16.sp)
                                    Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.outline)
                                }
                            },
                            selected = false,
                            onClick = { 
                                scope.launch { drawerState.close() }
                                navController.navigate(Routes.plpRoute(category = category.id))
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    
                    item {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        NavigationDrawerItem(
                            label = { Text(stringResource(R.string.my_account)) },
                            selected = false,
                            onClick = { 
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.ACCOUNT) 
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    AppTopBar(
                        shopLabel = if (appState.activeShopId == "1") "SE" else "GR",
                        onMenuClick = { 
                            scope.launch { drawerState.open() } 
                        },
                        onHomeClick = { navController.navigateToTopLevel(Routes.HOME) },
                        onCartClick = { navController.navigateToTopLevel(Routes.CART) },
                        onNotificationsClick = { navController.navigateToTopLevel(Routes.ACCOUNT) }
                    )
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        onScanClick = { navController.navigate(Routes.SCAN) }
                    )
                }
            },
            bottomBar = {
                AppBottomNav(navController = navController)
            },
        ) { innerPadding ->
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavHost(
                    navController = navController,
                    paddingValues = PaddingValues(0.dp)
                )
            }
        }
    }
}

/**
 * Extension function to navigate to a top-level destination.
 * 
 * It ensures that the back stack is popped up to the start destination,
 * avoids multiple copies of the same destination when re-selecting,
 * and restores state when re-selecting a previously selected item.
 * 
 * @param route The destination route to navigate to.
 */
private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
