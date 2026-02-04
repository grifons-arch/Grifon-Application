package com.example.grifon.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.padding
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.grifon.ui.screens.AccountScreen
import com.example.grifon.ui.screens.CartScreen
import com.example.grifon.ui.screens.HomeScreen
import com.example.grifon.ui.screens.SettingsScreen
import com.example.grifon.ui.screens.categories.CategoriesScreen
import com.example.grifon.ui.screens.plp.ProductDetailsScreen
import com.example.grifon.ui.screens.plp.ProductListScreen
import com.example.grifon.ui.screens.scan.ScanScreen
import com.example.grifon.viewmodel.PdpViewModel
import com.example.grifon.viewmodel.PlpViewModel

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
            HomeScreen(viewModel = hiltViewModel())
        }
        composable(Routes.CATEGORIES) {
            CategoriesScreen(viewModel = hiltViewModel()) { categoryId ->
                navController.navigate(Routes.plpRoute(category = categoryId))
            }
        }
        composable(Routes.CART) {
            CartScreen(viewModel = hiltViewModel())
        }
        composable(Routes.ACCOUNT) {
            AccountScreen(viewModel = hiltViewModel()) {
                navController.navigate(Routes.SETTINGS)
            }
        }
        composable(
            route = Routes.PLP,
            arguments = listOf(
                navArgument("query") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
                navArgument("category") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val query = backStackEntry.arguments?.getString("query").orEmpty()
            val category = backStackEntry.arguments?.getString("category").orEmpty()
            val viewModel: PlpViewModel = hiltViewModel()
            viewModel.updateQuery(query)
            viewModel.updateCategory(category)
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
            ScanScreen(viewModel = hiltViewModel())
        }
    }
}
