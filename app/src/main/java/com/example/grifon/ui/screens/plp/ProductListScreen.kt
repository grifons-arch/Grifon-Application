@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.PlpViewModel

@Composable
fun ProductListScreen(
    viewModel: PlpViewModel,
    onProductClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var filtersOpen by remember { mutableStateOf(false) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        when (val state = uiState) {
            UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(message = state.message)
            is UiState.Success -> {
                val data = state.data
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        FilterBar(
                            filters = data.filters,
                            sortOption = data.sortOption,
                            onFiltersClick = { filtersOpen = true },
                            onSortSelected = viewModel::updateSort,
                            onToggleInStock = {
                                viewModel.updateFilters(data.filters.copy(inStockOnly = !data.filters.inStockOnly))
                            },
                            onToggleExpress = {
                                val expressSet = data.filters.deliveryOptions
                                val updated = if (expressSet.contains("express")) {
                                    expressSet - "express"
                                } else {
                                    expressSet + "express"
                                }
                                viewModel.updateFilters(data.filters.copy(deliveryOptions = updated))
                            },
                        )
                    }

                    item(span = { GridItemSpan(2) }) {
                        ActiveFiltersRow(filters = data.filters)
                    }

                    items(data.products) { product ->
                        ProductGridItem(
                            product = product,
                            onClick = { onProductClick(product.id) }
                        )
                    }
                }

                if (filtersOpen) {
                    FiltersSheet(
                        initialState = data.filters,
                        onDismiss = { filtersOpen = false },
                        onApply = { newFilters ->
                            viewModel.updateFilters(newFilters)
                            filtersOpen = false
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.White)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
            shape = RoundedCornerShape(4.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9)),
            onClick = onClick
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imageUrl.ifEmpty { R.drawable.logo })
                        .crossfade(true)
                        .build(),
                    contentDescription = product.title,
                    placeholder = painterResource(R.drawable.logo),
                    error = painterResource(R.drawable.logo),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = product.title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )

        val reference = product.attributesMap["reference"] ?: ""
        if (reference.isNotEmpty()) {
            Text(
                text = "Κωδικός: $reference",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = "${product.price} €",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun FilterBar(
    filters: FilterState,
    sortOption: SortOption,
    onFiltersClick: () -> Unit,
    onSortSelected: (SortOption) -> Unit,
    onToggleInStock: () -> Unit,
    onToggleExpress: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onFiltersClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
            Text(text = "Φίλτρα", fontSize = 12.sp)
        }
        FilterChip(
            selected = filters.inStockOnly,
            onClick = onToggleInStock,
            label = { Text(text = "Διαθέσιμα", fontSize = 11.sp) },
        )
        FilterChip(
            selected = filters.deliveryOptions.contains("express"),
            onClick = onToggleExpress,
            label = { Text(text = "Express", fontSize = 11.sp) },
        )
    }
}

@Composable
private fun ActiveFiltersRow(filters: FilterState) {
    // ... παραμένει το ίδιο
}

@Composable
private fun FiltersSheet(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
) {
    // ... παραμένει το ίδιο αλλά με σωστά imports αν χρειαστεί
    var range by remember { mutableStateOf(initialState.priceRange) }
    var inStock by remember { mutableStateOf(initialState.inStockOnly) }
    var saleOnly by remember { mutableStateOf(initialState.saleOnly) }
    var rating by remember { mutableStateOf(initialState.ratingMin) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = "Τιμή", style = MaterialTheme.typography.titleMedium)
            RangeSlider(
                value = range.start.toFloat()..range.endInclusive.toFloat(),
                onValueChange = { newRange ->
                    range = newRange.start.toDouble()..newRange.endInclusive.toDouble()
                },
                valueRange = 0f..1500f,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Διαθεσιμότητα", style = MaterialTheme.typography.titleMedium)
            FilterChip(selected = inStock, onClick = { inStock = !inStock }, label = { Text("Άμεσα διαθέσιμα") })
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onApply(FilterState()) }, modifier = Modifier.weight(1f)) {
                    Text("Καθαρισμός")
                }
                Button(onClick = {
                    onApply(initialState.copy(priceRange = range, inStockOnly = inStock, saleOnly = saleOnly))
                }, modifier = Modifier.weight(1f)) {
                    Text("Εφαρμογή")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
