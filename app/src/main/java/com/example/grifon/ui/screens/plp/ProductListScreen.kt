@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
    var sortOpen by remember { mutableStateOf(false) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
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
                            onFiltersClick = { filtersOpen = true },
                            onSortClick = { sortOpen = true }
                        )
                    }
                    items(data.products) { product ->
                        ProductGridItem(product, onClick = { onProductClick(product.id) })
                    }
                }
                if (filtersOpen) {
                    FiltersSheet(
                        currentFilters = data.filters,
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
private fun FiltersSheet(
    currentFilters: FilterState,
    products: List<Product>,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
) {
    val scrollState = rememberScrollState()
    var tempFilters by remember(currentFilters) { mutableStateOf(currentFilters) }
    val brands = remember(products) { products.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted() }
    val maxPrice = remember(products) {
        products.maxOfOrNull { it.price }?.coerceAtLeast(10.0) ?: 500.0
    }
    val colorPalette = remember {
        mapOf(
            "Μπλε" to Color(0xFF0D47A1),
            "Κόκκινο" to Color(0xFFC62828),
            "Κίτρινο" to Color(0xFFFBC02D),
            "Πράσινο" to Color(0xFF2E7D32),
            "Μαύρο" to Color(0xFF111111),
            "Λευκό" to Color(0xFFF8F8F8),
            "Γκρι" to Color(0xFF757575)
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scrollState)
        ) {
            Text("Φιλτράρισμα κατά", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))

            FilterSectionTitle(title = "Τιμή")
            val sliderRange = 0f..maxPrice.toFloat()
            val selectedStart = tempFilters.priceRange.start.toFloat().coerceIn(sliderRange.start, sliderRange.endInclusive)
            val selectedEnd = tempFilters.priceRange.endInclusive.toFloat().coerceIn(selectedStart, sliderRange.endInclusive)
            RangeSlider(
                value = selectedStart..selectedEnd,
                onValueChange = { range ->
                    tempFilters = tempFilters.copy(priceRange = range.start.toDouble()..range.endInclusive.toDouble())
                },
                valueRange = sliderRange
            )
            Text(
                text = String.format("%.0f€ - %.0f€", selectedStart, selectedEnd),
                style = MaterialTheme.typography.bodyMedium
            )

            if (brands.isNotEmpty()) {
                FilterSectionTitle(title = "Brand")
                brands.forEach { brand ->
                    FilterItemRow(
                        label = brand,
                        count = products.count { it.brand == brand },
                        selected = tempFilters.brands.contains(brand),
                        onToggle = {
                            val next = if (tempFilters.brands.contains(brand)) {
                                tempFilters.brands - brand
                            } else {
                                tempFilters.brands + brand
                            }
                            tempFilters = tempFilters.copy(brands = next)
                        }
                    )
                }
            }

            FilterSectionTitle(title = "Χρωματισμοί")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                colorPalette.forEach { (name, color) ->
                    ColorCircle(name, color, tempFilters.colors.contains(name)) {
                        val nextColors = if (tempFilters.colors.contains(name)) tempFilters.colors - name else tempFilters.colors + name
                        tempFilters = tempFilters.copy(colors = nextColors)
                    }
                }
            }

            FilterSectionTitle(title = "Διαθεσιμότητα")
            FilterItemRow(
                label = "Σε απόθεμα", 
                count = products.count { it.inStock },
                selected = tempFilters.inStockOnly,
                onToggle = { tempFilters = tempFilters.copy(inStockOnly = !tempFilters.inStockOnly) }
            )

            FilterSectionTitle(title = "Αξιολόγηση")
            FilterItemRow(
                label = "4★ και άνω",
                count = products.count { it.rating >= 4.0 },
                selected = tempFilters.ratingMin >= 4.0,
                onToggle = {
                    tempFilters = tempFilters.copy(ratingMin = if (tempFilters.ratingMin >= 4.0) 0.0 else 4.0)
                }
            )

            Spacer(Modifier.height(24.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(onClick = { onApply(FilterState()) }, modifier = Modifier.weight(1f)) { Text("Καθαρισμός") }
                Button(onClick = { onApply(tempFilters) }, modifier = Modifier.weight(1f)) { Text("Εφαρμογή") }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ColorCircle(name: String, color: Color, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color)
            .border(2.dp, if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, CircleShape)
            .clickable { onClick() }
    ) {
        if (isSelected) {
            Text(
                text = "✓",
                color = if (color.luminance() > 0.5f) Color.Black else Color.White,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

// ... (τα υπόλοιπα composables παραμένουν ως έχουν)

@Composable
private fun FilterBar(onFiltersClick: () -> Unit, onSortClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(onClick = onFiltersClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))) {
            Text("Φίλτρα")
        }
        OutlinedButton(onClick = onSortClick, shape = RoundedCornerShape(20.dp), modifier = Modifier.weight(1f)) {
            Text("Ταξινόμηση")
        }
    }
}

@Composable
private fun SortDialog(currentSort: SortOption, onDismiss: () -> Unit, onSelect: (SortOption) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ταξινόμηση", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SortOption.entries.forEach { option ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable { onSelect(option) }.padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentSort == option, onClick = { onSelect(option) })
                        Spacer(Modifier.width(12.dp))
                        Text(option.label)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Ακύρωση") } }
    )
}

@Composable
private fun FilterSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        modifier = Modifier.padding(top = 16.dp, bottom = 8.dp)
    )
    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
}

@Composable
private fun FilterItemRow(label: String, count: Int, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onToggle() }.padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
        Text(text = count.toString(), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
    }
}

@Composable
fun ProductGridItem(product: Product, onClick: () -> Unit) {
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
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = product.title.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium), maxLines = 2, overflow = TextOverflow.Ellipsis, color = Color.Black)
        Text(text = "${product.price} €", style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary))
    }
}
