package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.FavoriteProduct
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.FavoritesViewModel

@Composable
fun FavoritesScreen(
    viewModel: FavoritesViewModel,
    onProductClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val data = state.data
            when {
                !data.isLoggedIn -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.favorites_login_required))
                    }
                }

                data.favorites.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(stringResource(R.string.favorites_empty))
                    }
                }

                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = stringResource(R.string.favorite_products),
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 8.dp)
                            )
                        }
                        items(data.favorites, key = { "${it.shopId}:${it.productId}" }) { favorite ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            ) {
                                ProductCard(
                                    product = favorite.toDisplayProduct(),
                                    isFavorite = true,
                                    onToggleFavorite = { viewModel.toggleFavorite(favorite) },
                                    onClick = { onProductClick(favorite.productId) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun FavoriteProduct.toDisplayProduct(): Product {
    return Product(
        id = productId,
        title = title,
        price = price,
        currency = currency,
        imageUrl = imageUrl,
        images = emptyList(),
        brand = brand,
        rating = 0.0,
        inStock = true,
        attributesMap = emptyMap(),
    )
}
