package com.example.grifon.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.grifon.RegisterScreen
import com.example.grifon.ui.screens.AccountScreen
import com.example.grifon.ui.screens.CartScreen
import com.example.grifon.ui.screens.CheckoutScreen
import com.example.grifon.ui.screens.FavoritesScreen
import com.example.grifon.ui.screens.HomeScreen
import com.example.grifon.ui.screens.PayPalCheckoutScreen
import com.example.grifon.ui.screens.SettingsScreen
import com.example.grifon.ui.screens.StripeCheckoutScreen
import com.example.grifon.ui.screens.WholesaleApplicationScreen
import com.example.grifon.ui.screens.categories.CategoriesScreen
import com.example.grifon.ui.screens.plp.ProductDetailsScreen
import com.example.grifon.ui.screens.plp.ProductListScreen
import com.example.grifon.ui.screens.scan.ScanScreen
import com.example.grifon.viewmodel.PdpViewModel
import com.example.grifon.viewmodel.PlpViewModel
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

@Composable
fun AppNavHost(
    navController: androidx.navigation.NavHostController,
    paddingValues: PaddingValues,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = Modifier.padding(paddingValues),
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                viewModel = hiltViewModel(),
                onProductClick = { productId ->
                    navController.navigate(Routes.productRoute(productId))
                },
                onSearch = { query ->
                    navController.navigate(Routes.plpRoute(query = query))
                },
                onCategoryClick = { categoryId ->
                    navController.navigate(Routes.plpRoute(category = categoryId))
                },
                onOpenAccount = {
                    navController.navigate(Routes.ACCOUNT)
                },
                onOpenFavorites = {
                    navController.navigate(Routes.FAVORITES)
                },
                onOpenCart = {
                    navController.navigate(Routes.CART)
                },
            )
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(viewModel = hiltViewModel()) { categoryId ->
                navController.navigate(Routes.plpRoute(category = categoryId))
            }
        }
        composable(Routes.CART) {
            CartScreen(
                viewModel = hiltViewModel(),
                onCheckout = {
                    navController.navigate(Routes.CHECKOUT)
                },
            )
        }
        composable(Routes.CHECKOUT) {
            CheckoutScreen(navController = navController, viewModel = hiltViewModel())
        }
        composable(
            route = Routes.PAYPAL_CHECKOUT,
            arguments = listOf(
                navArgument("orderReference") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("approvalUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val orderReference = backStackEntry.arguments?.getString("orderReference")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            val approvalUrl = backStackEntry.arguments?.getString("approvalUrl")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            PayPalCheckoutScreen(
                navController = navController,
                orderReference = orderReference,
                approvalUrl = approvalUrl,
                viewModel = hiltViewModel(),
            )
        }
        composable(
            route = Routes.STRIPE_CHECKOUT,
            arguments = listOf(
                navArgument("orderReference") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("checkoutUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val orderReference = backStackEntry.arguments?.getString("orderReference")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            val checkoutUrl = backStackEntry.arguments?.getString("checkoutUrl")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            StripeCheckoutScreen(
                navController = navController,
                orderReference = orderReference,
                checkoutUrl = checkoutUrl,
                viewModel = hiltViewModel(),
            )
        }
        composable(Routes.FAVORITES) {
            FavoritesScreen(
                viewModel = hiltViewModel(),
                onProductClick = { productId ->
                    navController.navigate(Routes.productRoute(productId))
                }
            )
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(
                viewModel = hiltViewModel(),
                onSettings = {
                    navController.navigate(Routes.SETTINGS)
                },
                onRegister = {
                    navController.navigate(Routes.REGISTER)
                },
                onWholesaleApplication = {
                    navController.navigate(Routes.WHOLESALE_APPLICATION)
                },
            )
        }
        composable(Routes.REGISTER) {
            RegisterScreen()
        }
        composable(Routes.WHOLESALE_APPLICATION) {
            WholesaleApplicationScreen(viewModel = hiltViewModel())
        }
        composable(
            route = Routes.PLP,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            val category = backStackEntry.arguments?.getString("category")
                ?.let { URLDecoder.decode(it, StandardCharsets.UTF_8.toString()) }
                .orEmpty()
            val viewModel: PlpViewModel = hiltViewModel()
            LaunchedEffect(query, category) {
                viewModel.updateQuery(query)
                viewModel.updateCategory(category)
            }
            ProductListScreen(viewModel = viewModel) { productId ->
                navController.navigate(Routes.productRoute(productId))
            }
        }
        composable(
            route = Routes.PRODUCT,
            arguments = listOf(navArgument("id") { type = NavType.StringType }),
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("id").orEmpty()
            val viewModel: PdpViewModel = hiltViewModel()
            viewModel.setProductId(productId)
            ProductDetailsScreen(viewModel = viewModel)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(viewModel = hiltViewModel())
        }
        composable(Routes.SCAN) {
            ScanScreen(
                navController = navController,
                viewModel = hiltViewModel()
            )
        }
    }
}
