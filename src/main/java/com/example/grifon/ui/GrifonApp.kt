package com.example.grifon.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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

    val categories = listOf(
        DrawerCategory("Κεραμικά", "4000", listOf(
            DrawerCategory("Διακοσμητικά Κεραμικά", "4025"),
            DrawerCategory("Φανάρια, Καντήλια", "4030")
        )),
        DrawerCategory("Αγαλματίδια κ.α.", "4500", listOf(
            DrawerCategory("Veronese", "4504"),
            DrawerCategory("Αλαβαστρίνα", "4510"),
            DrawerCategory("Μπρούτζινα", "4520")
        ))
    )

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    item {
                        Text("Κατηγορίες", modifier = Modifier.padding(16.dp), style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold))
                        HorizontalDivider()
                    }
                    categories.forEach { category ->
                        item {
                            Text(category.name, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        }
                        items(category.subCategories) { subCategory ->
                            NavigationDrawerItem(
                                label = { Text("-- ${subCategory.name}") },
                                selected = false,
                                onClick = { 
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Routes.plpRoute(category = subCategory.id))
                                }
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
                        onNotificationsClick = { navController.navigateToTopLevel(Routes.ACCOUNT) },
                        onCategoriesClick = { scope.launch { drawerState.open() } }
                    )
                    AppSearchBar(query = searchQuery, onQueryChange = { searchQuery = it }, onScanClick = { navController.navigate(Routes.SCAN) })
                }
            },
            bottomBar = { AppBottomNav(navController = navController) },
        ) { innerPadding ->
            AppNavHost(
                navController = navController, 
                paddingValues = innerPadding // Εφαρμογή του padding εδώ
            )
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
