@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Product
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.ui.screens.LocalCanDisplayPrices
import com.example.grifon.ui.screens.LocalIsLoggedIn
import com.example.grifon.ui.screens.WholesaleLoginBanner
import com.example.grifon.ui.theme.GrifonGold
import com.example.grifon.viewmodel.PlpViewModel

@Composable
fun ProductListScreen(
    viewModel: PlpViewModel,
    onProductClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var filtersOpen by remember { mutableStateOf(false) }
    var sortOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        when (val state = uiState) {
            UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(message = state.message)
            is UiState.Success -> {
                val data = state.data
                val canDisplayPrices = LocalCanDisplayPrices.current
                val activeFilters = remember(data.filters) { data.filters.activeCount() }
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        FilterBar(
                            activeFilters = activeFilters,
                            onFiltersClick = { filtersOpen = true },
                            onSortClick = { sortOpen = true }
                        )
                    }
                    if (!canDisplayPrices) {
                        item(span = { GridItemSpan(2) }) {
                            WholesaleLoginBanner()
                        }
                    }
                    items(data.products) { product ->
                        ProductGridItem(
                            product = product,
                            isFavorite = data.favoriteIds.contains(product.id),
                            onToggleFavorite = { viewModel.toggleFavorite(product) },
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }
                if (filtersOpen) {
                    FiltersSheet(
                        currentFilters = data.filters,
                        availableFacets = data.availableFacets,
                        products = data.products,
                        onDismiss = { filtersOpen = false },
                        onApply = { newFilters ->
                            viewModel.updateFilters(newFilters)
                            filtersOpen = false
                        },
                    )
                }
                if (sortOpen) {
                    SortDialog(
                        currentSort = data.sortOption,
                        onDismiss = { sortOpen = false },
                        onSelect = { newSort ->
                            viewModel.updateSort(newSort)
                            sortOpen = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current
    Column(
        modifier = Modifier.fillMaxWidth().background(Color.White)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth().aspectRatio(1f),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl.ifEmpty { R.drawable.logo })
                        .crossfade(true).build(),
                    contentDescription = product.title,
                    placeholder = painterResource(R.drawable.logo),
                    error = painterResource(R.drawable.logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
                if (isLoggedIn) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(RoundedCornerShape(100))
                            .background(Color.White.copy(alpha = 0.96f))
                            .border(1.5.dp, GrifonGold, RoundedCornerShape(100))
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_products),
                            tint = if (isFavorite) Color(0xFFE05050) else Color(0xFF4A4A4A)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = product.title.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.Black)
        if (canDisplayPrices && product.price != null) {
            Text(text = "${product.price} €", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
        }
    }
}
