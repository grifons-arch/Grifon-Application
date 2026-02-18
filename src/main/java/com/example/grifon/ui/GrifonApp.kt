package com.example.grifon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.example.grifon.navigation.AppNavHost
import com.example.grifon.navigation.Routes
import com.example.grifon.ui.components.AppBottomNav
import com.example.grifon.ui.components.AppSearchBar
import com.example.grifon.ui.components.AppTopBar
import com.example.grifon.viewmodel.AppViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

private data class DrawerCategory(
    val name: String,
    val id: String,
    val subCategories: List<DrawerCategory> = emptyList()
)

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

    // Αντιστοίχιση Κατηγοριών βάσει PrestaShop & Screenshot
    val categories = listOf(
        DrawerCategory("Κεραμικά", "4000", listOf(
            DrawerCategory("Διακοσμητικά Κεραμικά", "4001"),
            DrawerCategory("Φανάρια, Καντήλια", "4002")
        )),
        DrawerCategory("Αγαλματίδια κ.α.", "4500", listOf(
            DrawerCategory("Βερονέζ", "4501"),
            DrawerCategory("Αλαβαστρίνα", "4502"),
            DrawerCategory("Μπρούτζινα", "4503"),
            DrawerCategory("Πολυεστερικά", "4504"),
            DrawerCategory("Γύψινα, Πωρόλιθος, Μαρμάρινα", "4505")
        )),
        DrawerCategory("Διακοσμητικά", "5000", listOf(
            DrawerCategory("Φανάρια, Καντήλια", "5001"),
            DrawerCategory("Φωτιστικά", "5002"),
            DrawerCategory("Ρολόγια", "5003"),
            DrawerCategory("Επιτραπέζια", "5004")
        )),
        DrawerCategory("Για χρήση", "7500", listOf(
            DrawerCategory("Κουζίνας κ υαλικά", "7501"),
            DrawerCategory("Σαπούνια", "7502")
        )),
        DrawerCategory("Χόμπι και παιχνίδια", "7000", listOf(
            DrawerCategory("Τάβλι, Σκάκι", "7001"),
            DrawerCategory("Παιχνίδια, Λούτρινα", "7002")
        )),
        DrawerCategory("Αξεσουάρ", "8000", listOf(
            DrawerCategory("Υφασμάτινα και τσάντες", "8001")
        ))
    )

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
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text(
                            "Ψωνίστε Ανά Κατηγορία", 
                            modifier = Modifier.padding(16.dp), 
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        HorizontalDivider()
                    }
                    categories.forEach { category ->
                        item {
                            NavigationDrawerItem(
                                label = { Text(category.name, fontWeight = FontWeight.Bold) },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Routes.plpRoute(category = category.id))
                                }
                            )
                        }
                        items(category.subCategories) { sub ->
                            NavigationDrawerItem(
                                label = { Text("-- ${sub.name}", fontSize = 14.sp) },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Routes.plpRoute(category = sub.id))
                                },
                                modifier = Modifier.padding(start = 16.dp)
                            )
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                Column {
                    AppTopBar(
                        shopLabel = if (appState.activeShopId == "1") "SE" else "GR",
                        onMenuClick = { scope.launch { drawerState.open() } },
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
            bottomBar = { AppBottomNav(navController = navController) },
        ) { innerPadding ->
            Surface(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavHost(navController = navController, paddingValues = PaddingValues(0.dp))
            }
        }
    }
}

private fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
