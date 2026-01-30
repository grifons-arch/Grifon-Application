package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.grifon.ui.theme.GrifonTheme

private data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
)

private object Routes {
    const val HOME = "home"
    const val CATEGORIES = "categories"
    const val CART = "cart"
    const val ACCOUNT = "account"
    const val PRODUCT = "product/{id}"
    const val PLP = "plp?query={query}"
    const val SETTINGS = "settings"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GrifonTheme {
                GrifonApp()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GrifonApp() {
    val navController = rememberNavController()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            Column {
                TopBarStickyLine()
                TopBarAccountNotice()
                TopBarCollapsibleLine(scrollBehavior = scrollBehavior)
            }
        },
        bottomBar = {
            GrifonBottomNav(navController = navController)
        },
    ) { paddingValues ->
        GrifonNavHost(
            navController = navController,
            paddingValues = paddingValues,
        )
    }
}

@Composable
private fun TopBarAccountNotice() {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val linkText = buildAnnotatedString {
        append("Για να μπορείτε να παραγγείλετε ή να δείτε τιμές, θα πρέπει πρώτα να ")
        pushStringAnnotation(tag = "register", annotation = "register")
        pushStyle(
            SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            ),
        )
        append("δημιουργήσετε ένα λογαριασμό")
        pop()
        pop()
        append(" Ή να ")
        pushStringAnnotation(tag = "login", annotation = "login")
        pushStyle(
            SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            ),
        )
        append("συνδεθείτε με τον υπάρχον λογαριασμό σας")
        pop()
        pop()
        append(".\n\n")
        append(
            "Αν δημιουργήσετε ένα νέο λογαριασμό, θα πρέπει πρώτα να υποβάλετε αίτηση για " +
                "λογαριασμό χονδρικής πώλησης. Αφού εγκρίνουμε το αίτημά σας, θα μπορείτε " +
                "να παραγγείλετε και να δείτε τιμές χονδρικής.",
        )
    }

    ClickableText(
        text = linkText,
        style = MaterialTheme.typography.bodySmall.copy(
            color = MaterialTheme.colorScheme.onSurface,
            letterSpacing = 0.sp,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 0.dp),
    ) { offset ->
        linkText.getStringAnnotations(tag = "register", start = offset, end = offset)
            .firstOrNull()
            ?.let {
                context.startActivity(
                    android.content.Intent(context, RegisterActivity::class.java),
                )
            }
        linkText.getStringAnnotations(tag = "login", start = offset, end = offset)
            .firstOrNull()
            ?.let {
                context.startActivity(
                    android.content.Intent(context, LoginActivity::class.java),
                )
            }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBarStickyLine() {
    TopAppBar(
        title = {
            Text(
                text = "Εξυπηρέτηση πελατών 0030 2810-821627 - info@grifon.gr",
                style = MaterialTheme.typography.bodySmall,
            )
        },
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TopBarCollapsibleLine(scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior) {
    LargeTopAppBar(
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.offset(y = (-10).dp),
            ) {
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "Grifon logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(32.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Grifon Shop",
                    style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 0.sp),
                )
            }
        },
        scrollBehavior = scrollBehavior,
    )
}

@Composable
private fun GrifonBottomNav(navController: NavHostController) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry.value?.destination
    val items = listOf(
        BottomNavItem(route = Routes.HOME, label = "Home", icon = Icons.Default.Home),
        BottomNavItem(route = Routes.CATEGORIES, label = "Categories", icon = Icons.Default.List),
        BottomNavItem(route = Routes.CART, label = "Cart", icon = Icons.Default.ShoppingCart),
        BottomNavItem(route = Routes.ACCOUNT, label = "Account", icon = Icons.Default.AccountCircle),
    )

    NavigationBar {
        items.forEach { item ->
            val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                    )
                },
                label = {
                    Text(text = item.label)
                },
            )
        }
    }
}

@Composable
private fun GrifonNavHost(
    navController: NavHostController,
    paddingValues: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.padding(paddingValues),
    ) {
        composable(Routes.HOME) {
            HomeScreen()
        }
        composable(Routes.CATEGORIES) {
            PlaceholderScreen(title = "Categories")
        }
        composable(Routes.CART) {
            PlaceholderScreen(title = "Cart")
        }
        composable(Routes.ACCOUNT) {
            PlaceholderScreen(title = "Account")
        }
        composable(
            route = Routes.PRODUCT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("id") ?: ""
            PlaceholderScreen(title = "Product $productId")
        }
        composable(
            route = Routes.PLP,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query").orEmpty()
            PlaceholderScreen(title = "PLP $query")
        }
        composable(Routes.SETTINGS) {
            PlaceholderScreen(title = "Settings")
        }
    }
}

@Composable
private fun HomeScreen() {
    val featuredProduct = "Προϊόν 1"
    val topProducts = listOf("Προϊόν 2", "Προϊόν 3", "Προϊόν 4")
    val recentProducts = listOf("Προϊόν 1", "Προϊόν 2", "Προϊόν 3")
    val headerColor = Color(0xFF4B1FA8)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(headerColor)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = {}) {
                        Icon(
                            imageVector = Icons.Default.Menu,
                            contentDescription = "Menu",
                            tint = Color.White,
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = "Home",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = Color.White,
                            )
                        }
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = Color.White,
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Image(
                        painter = painterResource(R.drawable.logo),
                        contentDescription = "Grifon logo",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.height(36.dp),
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Grifon",
                        style = MaterialTheme.typography.titleMedium.copy(
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                        ),
                    )
                }
            }
        }
        item {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                placeholder = { Text(text = "Αναζήτηση") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                    )
                },
                singleLine = true,
                readOnly = true,
            )
        }
        item {
            FeaturedProductCard(title = featuredProduct)
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                topProducts.forEach { label ->
                    SmallProductCard(
                        title = label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        item {
            Text(
                text = "Τελευταία επιλεγμένα",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                recentProducts.forEach { label ->
                    SmallProductCard(
                        title = label,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun FeaturedProductCard(title: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 180.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(48.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6B6B6B))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color.White,
                    )
                }
            }
        }
    }
}

@Composable
private fun SmallProductCard(
    title: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        tonalElevation = 1.dp,
        shadowElevation = 2.dp,
        shape = MaterialTheme.shapes.medium,
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Home,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF6B6B6B))
                    .padding(horizontal = 8.dp, vertical = 8.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = title,
                        color = Color.White,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Start,
                    )
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = "Favorite",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    val items = List(20) { index -> "$title item ${index + 1}" }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
        }
        items(items) { label ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp),
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(text = label)
            }
        }
    }
}
