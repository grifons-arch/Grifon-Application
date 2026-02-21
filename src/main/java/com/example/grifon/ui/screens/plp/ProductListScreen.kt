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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Screen displaying a list of products with filtering and sorting options.
 */
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
                        ProductGridItem(
                            product = product, 
                            onClick = { onProductClick(product.id) },
                            onFavoriteClick = { /* TODO: Implement favorite toggle */ }
                        )
                    }
                }
                if (filtersOpen) {
                    FiltersSheet(
                        currentFilters = data.filters,
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
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
) {
    val scrollState = rememberScrollState()
    var tempFilters by remember { mutableStateOf(currentFilters) }
    val colors = mapOf(
        "Μπλε" to Color.Blue, "Κόκκινο" to Color.Red, "Κίτρινο" to Color.Yellow, 
        "Πράσινο" to Color.Green, "Μαύρο" to Color.Black, "Λευκό" to Color.White
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp).verticalScroll(scrollState)
        ) {
            Text("Φιλτράρισμα κατά", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))
            Spacer(Modifier.height(16.dp))

            FilterSectionTitle(title = "Χρωματισμοί")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                colors.forEach { (name, color) ->
                    ColorCircle(name, color, tempFilters.colors.contains(name)) {
                        val currentColors = tempFilters.colors
                        val nextColors = if (currentColors.contains(name)) currentColors - name else currentColors + name
                        tempFilters = tempFilters.copy(colors = nextColors)
                    }
                }
            }

            FilterSectionTitle(title = "Διαθεσιμότητα")
            FilterItemRow(
                label = "Σε απόθεμα", 
                count = 77, 
                selected = tempFilters.inStockOnly,
                onToggle = { tempFilters = tempFilters.copy(inStockOnly = !tempFilters.inStockOnly) }
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
    )
}

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

/**
 * A card representing a single product in the grid.
 * Displays the product image with an overlay containing the title, price, and favorite icon.
 */
@Composable
fun ProductGridItem(
    product: Product, 
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUrl.ifEmpty { R.drawable.logo })
                    .crossfade(true).build(),
                contentDescription = product.title,
                placeholder = painterResource(R.drawable.logo),
                error = painterResource(R.drawable.logo),
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(12.dp)
            )
            
            // Overlay μπάρα στο κάτω μέρος της εικόνας με όνομα, τιμή και καρδούλα
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.title.uppercase(), 
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), 
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${product.price} €", 
                        color = Color.White,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold, fontSize = 10.sp)
                    )
                }
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite, 
                        contentDescription = null, 
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
