@file:OptIn(ExperimentalFoundationApi::class)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.stickyHeader
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.SortOption
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.PlpViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductListScreen(
    viewModel: PlpViewModel,
    onProductClick: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    var filtersOpen by remember { mutableStateOf(false) }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val data = state.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 80.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                stickyHeader {
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
                item {
                    ActiveFiltersRow(filters = data.filters)
                }
                items(data.products) { product ->
                    ProductCard(
                        title = product.title,
                        subtitle = "${product.price} ${product.currency}",
                        onClick = { onProductClick(product.id) },
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(onClick = onFiltersClick) {
            Text(text = "Φίλτρα")
        }
        OutlinedButton(onClick = { onSortSelected(SortOption.PRICE_LOW_HIGH) }) {
            Text(text = "Ταξινόμηση")
        }
        FilterChip(
            selected = filters.inStockOnly,
            onClick = onToggleInStock,
            label = { Text(text = "Διαθέσιμα") },
        )
        FilterChip(
            selected = filters.deliveryOptions.contains("express"),
            onClick = onToggleExpress,
            label = { Text(text = "Express") },
        )
    }
}

@Composable
private fun ActiveFiltersRow(filters: FilterState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (filters.brands.isNotEmpty()) {
            filters.brands.forEach { brand ->
                FilterChip(selected = true, onClick = {}, label = { Text(text = "$brand ✕") })
            }
        }
        if (filters.priceRange.start > 0.0 || filters.priceRange.endInclusive < 500.0) {
            FilterChip(
                selected = true,
                onClick = {},
                label = { Text(text = "${filters.priceRange.start.toInt()}-${filters.priceRange.endInclusive.toInt()}€ ✕") },
            )
        }
    }
}

@Composable
private fun FiltersSheet(
    initialState: FilterState,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
) {
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
            Text(text = "Brand", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = initialState.brands.contains("Nike"), onClick = {}, label = { Text("Nike") })
                FilterChip(selected = initialState.brands.contains("Acme"), onClick = {}, label = { Text("Acme") })
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Διαθεσιμότητα", style = MaterialTheme.typography.titleMedium)
            FilterChip(selected = inStock, onClick = { inStock = !inStock }, label = { Text("Άμεσα διαθέσιμα") })
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Rating", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(3.0, 4.0, 4.5).forEach { value ->
                    FilterChip(
                        selected = rating == value,
                        onClick = { rating = value },
                        label = { Text("$value+") },
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = "Προσφορές", style = MaterialTheme.typography.titleMedium)
            FilterChip(selected = saleOnly, onClick = { saleOnly = !saleOnly }, label = { Text("Σε έκπτωση") })
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = {
                    onApply(FilterState())
                }, modifier = Modifier.weight(1f)) {
                    Text("Καθαρισμός")
                }
                Button(onClick = {
                    onApply(
                        initialState.copy(
                            priceRange = range,
                            inStockOnly = inStock,
                            ratingMin = rating,
                            saleOnly = saleOnly,
                        )
                    )
                }, modifier = Modifier.weight(1f)) {
                    Text("Εφαρμογή")
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun ProductCard(title: String, subtitle: String, onClick: () -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        onClick = onClick,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall)
        }
    }
}
