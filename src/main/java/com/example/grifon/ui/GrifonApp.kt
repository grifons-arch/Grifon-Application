package com.example.grifon.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.clickable
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
import androidx.navigation.compose.currentBackStackEntryAsState
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
import androidx.core.os.LocaleListCompat

private data class DrawerCategory(
    val name: String,
    val id: String,
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

    var showLanguageDialog by remember { mutableStateOf(false) }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val showSearchBar = currentRoute == Routes.HOME || currentRoute?.startsWith("plp") == true

    // Κατηγορίες βάσει του δέντρου σας
    val categories = listOf(
        DrawerCategory("Κεραμικά", "4000"),
        DrawerCategory("Αγαλματίδια κ.α.", "4500"),
        DrawerCategory("Διακοσμητικά", "5000"),
        DrawerCategory("Για χρήση", "7500"),
        DrawerCategory("Χόμπι και παιχνίδια", "7000"),
        DrawerCategory("Αξεσουάρ", "8000"),
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

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text("Γλώσσα / Language") },
            text = {
                Column {
                    LanguageOption("English", "en") { showLanguageDialog = false }
                    LanguageOption("Ελληνικά", "el") { showLanguageDialog = false }
                    LanguageOption("Svenska", "sv") { showLanguageDialog = false }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text("OK")
                }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Κατηγορίες Προϊόντων", 
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
                                    Text(category.name, fontSize = 16.sp)
                                    Icon(Icons.Default.ChevronRight, contentDescription = null)
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
                    if (showSearchBar) {
                        AppSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            onScanClick = { navController.navigate(Routes.SCAN) }
                        )
                    }
                }
            },
            bottomBar = {
                AppBottomNav(
                    navController = navController,
                    onLanguageClick = { showLanguageDialog = true }
                )
            },
        ) { innerPadding ->
            // ΔΙΟΡΘΩΣΗ: Αφαίρεση του padding από το Surface για να λειτουργεί το Scroll
            Surface(
                modifier = Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding()),
                color = MaterialTheme.colorScheme.background
            ) {
                AppNavHost(
                    navController = navController,
                    paddingValues = innerPadding
                )
            }
        }
    }
}

@Composable
private fun LanguageOption(label: String, tag: String, onClick: () -> Unit) {
    Text(
        text = label,
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(tag)
                AppCompatDelegate.setApplicationLocales(appLocale)
                onClick()
            }
            .padding(vertical = 12.dp, horizontal = 8.dp),
        style = MaterialTheme.typography.bodyLarge
    )
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
