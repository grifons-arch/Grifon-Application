package com.example.grifon.ui

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import com.example.grifon.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun GrifonApp() {
    Log.d("CrashLog", "GrifonApp: Start")
    
    val navController = rememberNavController()
    val appViewModel: AppViewModel = hiltViewModel()
    val appState by appViewModel.state.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val rootCategories = remember(appState.categories) {
        if (appState.categories.isEmpty()) emptyList<Category>()
        else appState.categories.filter { it.parentId?.endsWith("_2") == true || it.parentId == null }
    }

    LaunchedEffect(searchQuery) {
        if (searchQuery.length >= 2) {
            snapshotFlow { searchQuery }
                .debounce(600)
                .distinctUntilChanged()
                .collect { query ->
                    navController.navigate(Routes.plpRoute(query = query)) {
                        popUpTo(Routes.HOME) { saveState = true }
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
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Μενού", 
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp), 
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                )
                HorizontalDivider()
                
                LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 16.dp)) {
                    item {
                        NavigationDrawerItem(
                            label = { Text("Αρχική Οθόνη", fontSize = 16.sp) },
                            selected = currentRoute == Routes.HOME,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.HOME)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Αγαπημένα", fontSize = 16.sp) },
                            selected = currentRoute == Routes.FAVORITES,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.FAVORITES)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    item {
                        NavigationDrawerItem(
                            label = { Text("Λογαριασμός", fontSize = 16.sp) },
                            selected = currentRoute == Routes.ACCOUNT || currentRoute == Routes.REGISTER,
                            onClick = {
                                scope.launch { drawerState.close() }
                                navController.navigateToTopLevel(Routes.ACCOUNT)
                            },
                            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                        )
                    }
                    
                    item {
                        Text(
                            "Κατηγορίες", 
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (rootCategories.isEmpty()) {
                        item {
                            Text("Φόρτωση...", modifier = Modifier.padding(horizontal = 24.dp), color = Color.Gray)
                        }
                    } else {
                        items(rootCategories) { root: Category ->
                            CategoryDrawerItem(
                                root = root,
                                allCategories = appState.categories,
                                onCategoryClick = { category ->
                                    scope.launch { drawerState.close() }
                                    navController.navigate(Routes.plpRoute(category = category.id))
                                }
                            )
                        }
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
                    onMenuClick = { scope.launch { drawerState.open() } },
                    onHomeClick = { 
                        searchQuery = "" 
                        navController.navigateToTopLevel(Routes.HOME) 
                    },
                    onCartClick = { navController.navigateToTopLevel(Routes.CART) },
                    onNotificationsClick = { navController.navigateToTopLevel(Routes.ACCOUNT) },
                    onFavoritesClick = { navController.navigateToTopLevel(Routes.FAVORITES) },
                    onBackClick = if (currentRoute != Routes.HOME) { { navController.navigateUp() } } else null,
                    categories = appState.categories,
                    onCategoryClick = { category ->
                        navController.navigate(Routes.plpRoute(category = category.id))
                    },
                    showSearch = true, // Πλέον πάντα ορατή
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

@Composable
fun CategoryDrawerItem(
    root: Category,
    allCategories: List<Category>,
    onCategoryClick: (Category) -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    val children = remember(allCategories, root.id) {
        allCategories.filter { it.parentId == root.id }
    }

    Column {
        NavigationDrawerItem(
            label = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(root.name, fontSize = 14.sp, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                    if (children.isNotEmpty()) {
                        Icon(if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null, modifier = Modifier.size(18.dp))
                    }
                }
            },
            selected = false,
            onClick = {
                if (children.isNotEmpty()) isExpanded = !isExpanded
                else onCategoryClick(root)
            },
            modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
        )
        
        if (isExpanded) {
            children.forEach { child ->
                NavigationDrawerItem(
                    label = { 
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(child.name, fontSize = 13.sp)
                        }
                    },
                    selected = false,
                    onClick = { onCategoryClick(child) },
                    modifier = Modifier.padding(start = 24.dp).padding(NavigationDrawerItemDefaults.ItemPadding)
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
