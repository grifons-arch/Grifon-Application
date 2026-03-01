@file:OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalMaterial3Api::class,
    ExperimentalLayoutApi::class
)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.example.grifon.domain.model.Category
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
                            activeFiltersCount = activeFiltersCount(data.filters, data.selectedCategoryId),
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
                        currentCategoryId = data.selectedCategoryId,
                        categories = data.categories,
                        products = data.products,
                        onDismiss = { filtersOpen = false },
                        onApply = { newFilters, selectedCategoryId ->
                            viewModel.updateFilters(newFilters)
                            viewModel.updateCategory(selectedCategoryId)
                            filtersOpen = false
                        }
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
    currentCategoryId: String,
    categories: List<Category>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onApply: (FilterState, String) -> Unit,
) {
    val scrollState = rememberScrollState()
    val availablePriceRange = remember(products) { products.toPriceBounds() }
    val availableBrands = remember(products) {
        products.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted()
    }
    val availableColors = remember(products) { extractColorOptions(products) }
    val availableAttributes = remember(products) { extractAttributeOptions(products) }

    var tempSelectedCategory by remember(currentCategoryId) { mutableStateOf(currentCategoryId) }
    var tempExpanded by remember(categories) {
        mutableStateOf(categories.filter { it.parentId == null }.map { it.id }.toSet())
    }

    var tempFilters by remember(currentFilters, products) {
        mutableStateOf(
            currentFilters.copy(
                priceRange = currentFilters.priceRange.clampTo(availablePriceRange)
            )
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .verticalScroll(scrollState)
        ) {
            Text(
                text = "Φιλτράρισμα κατά",
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(Modifier.height(12.dp))

            FilterSectionTitle(title = "Κατηγορίες")
            CategoryTreeFilter(
                categories = categories,
                selectedCategoryId = tempSelectedCategory,
                expanded = tempExpanded,
                onToggleExpanded = { categoryId ->
                    tempExpanded = if (tempExpanded.contains(categoryId)) tempExpanded - categoryId else tempExpanded + categoryId
                },
                onSelectCategory = { categoryId -> tempSelectedCategory = categoryId }
            )

            FilterSectionTitle(title = "Τιμή")
            Text(
                text = "${tempFilters.priceRange.start.toInt()}€ - ${tempFilters.priceRange.endInclusive.toInt()}€",
                style = MaterialTheme.typography.bodyMedium
            )
            RangeSlider(
                value = tempFilters.priceRange.start.toFloat()..tempFilters.priceRange.endInclusive.toFloat(),
                onValueChange = { range ->
                    tempFilters = tempFilters.copy(
                        priceRange = range.start.toDouble()..range.endInclusive.toDouble()
                    )
                },
                valueRange = availablePriceRange.start.toFloat()..availablePriceRange.endInclusive.toFloat()
            )

            if (availableBrands.isNotEmpty()) {
                FilterSectionTitle(title = "Μάρκες")
                MultiSelectChips(
                    options = availableBrands,
                    selected = tempFilters.brands,
                    onToggle = { brand ->
                        tempFilters = tempFilters.copy(brands = tempFilters.brands.toggle(brand))
                    }
                )
            }

            if (availableColors.isNotEmpty()) {
                FilterSectionTitle(title = "Χρώματα")
                ColorChips(
                    options = availableColors,
                    selected = tempFilters.colors,
                    onToggle = { color ->
                        tempFilters = tempFilters.copy(colors = tempFilters.colors.toggle(color))
                    }
                )
            }

            availableAttributes.forEach { (attributeKey, options) ->
                if (options.isNotEmpty()) {
                    FilterSectionTitle(title = attributeKey)
                    MultiSelectChips(
                        options = options,
                        selected = tempFilters.attributes[attributeKey] ?: emptySet(),
                        onToggle = { value ->
                            val current = tempFilters.attributes[attributeKey].orEmpty()
                            tempFilters = tempFilters.copy(
                                attributes = tempFilters.attributes + (attributeKey to current.toggle(value))
                            )
                        }
                    )
                }
            }

            FilterSectionTitle(title = "Διαθεσιμότητα")
            FilterItemRow(
                label = "Σε απόθεμα",
                selected = tempFilters.inStockOnly,
                onToggle = { tempFilters = tempFilters.copy(inStockOnly = !tempFilters.inStockOnly) }
            )

            Spacer(Modifier.height(24.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(
                    onClick = {
                        tempSelectedCategory = ""
                        onApply(FilterState(), "")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Καθαρισμός")
                }
                Button(
                    onClick = { onApply(tempFilters, tempSelectedCategory) },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Εφαρμογή")
                }
            }
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
private fun CategoryTreeFilter(
    categories: List<Category>,
    selectedCategoryId: String,
    expanded: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    if (categories.isEmpty()) {
        Text("Δεν βρέθηκαν κατηγορίες", style = MaterialTheme.typography.bodySmall)
        return
    }

    val categoryIds = categories.map { it.id }.toSet()
    val roots = categories
        .filter { it.parentId == null || !categoryIds.contains(it.parentId) }
        .sortedBy { it.name.lowercase() }

    Column(
        verticalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F5F5), RoundedCornerShape(6.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp)
    ) {
        CategoryLeafRow(
            label = "Home",
            level = 0,
            selected = selectedCategoryId.isBlank(),
            onClick = { onSelectCategory("") }
        )
        roots.forEach { root ->
            CategoryTreeNode(
                node = root,
                allCategories = categories,
                level = 0,
                selectedCategoryId = selectedCategoryId,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
                onSelectCategory = onSelectCategory
            )
        }
    }
}

@Composable
private fun CategoryTreeNode(
    node: Category,
    allCategories: List<Category>,
    level: Int,
    selectedCategoryId: String,
    expanded: Set<String>,
    onToggleExpanded: (String) -> Unit,
    onSelectCategory: (String) -> Unit,
) {
    val children = allCategories.filter { it.parentId == node.id }.sortedBy { it.name.lowercase() }
    val hasChildren = children.isNotEmpty()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectCategory(node.id) }
            .padding(start = (level * 14).dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = if (hasChildren) if (expanded.contains(node.id)) "⌄" else "›" else " ",
            modifier = Modifier
                .width(14.dp)
                .clickable(enabled = hasChildren) { onToggleExpanded(node.id) },
            color = Color(0xFF8A8A8A),
            fontSize = 12.sp
        )
        SelectionCircle(selected = selectedCategoryId == node.id, onClick = { onSelectCategory(node.id) })
        Text(
            text = node.name,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Color(0xFF4A4A4A),
            modifier = Modifier.padding(start = 6.dp)
        )
    }

    if (hasChildren && expanded.contains(node.id)) {
        children.forEach { child ->
            CategoryTreeNode(
                node = child,
                allCategories = allCategories,
                level = level + 1,
                selectedCategoryId = selectedCategoryId,
                expanded = expanded,
                onToggleExpanded = onToggleExpanded,
                onSelectCategory = onSelectCategory
            )
        }
    }
}

@Composable
private fun CategoryLeafRow(
    label: String,
    level: Int,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(start = (level * 14).dp, top = 1.dp, bottom = 1.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(" ", modifier = Modifier.width(14.dp))
        SelectionCircle(selected = selected, onClick = onClick)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
            color = Color(0xFF4A4A4A),
            modifier = Modifier.padding(start = 6.dp)
        )
    }
}

@Composable
private fun SelectionCircle(selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .border(1.dp, Color(0xFF909090), CircleShape)
            .background(if (selected) Color(0xFF6E8BB6) else Color.Transparent)
            .clickable { onClick() }
    )
}

@Composable
private fun FilterBar(activeFiltersCount: Int, onFiltersClick: () -> Unit, onSortClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onFiltersClick,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6200EE))
        ) {
            Text(if (activeFiltersCount == 0) "Φίλτρα" else "Φίλτρα ($activeFiltersCount)")
        }
        OutlinedButton(
            onClick = onSortClick,
            shape = RoundedCornerShape(20.dp),
            modifier = Modifier.weight(1f)
        ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
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
private fun FilterItemRow(label: String, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = selected, onCheckedChange = { onToggle() })
            Text(text = label, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun MultiSelectChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            FilterChip(
                selected = selected.contains(option),
                onClick = { onToggle(option) },
                label = { Text(option) }
            )
        }
    }
}

@Composable
private fun ColorChips(
    options: List<String>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { colorName ->
            val color = colorNameToValue(colorName)
            Surface(
                shape = RoundedCornerShape(50),
                color = if (selected.contains(colorName)) MaterialTheme.colorScheme.primaryContainer else Color.Transparent,
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .clickable { onToggle(colorName) }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(color)
                            .border(
                                width = if (color == Color.White) 1.dp else 0.dp,
                                color = Color.LightGray,
                                shape = CircleShape
                            )
                    )
                    Text(colorName)
                }
            }
        }
    }
}

private fun extractColorOptions(products: List<Product>): List<String> {
    val fromAttributes = products.flatMap { product ->
        product.attributesMap
            .filterKeys { key -> key.equals("color", ignoreCase = true) || key.contains("χρώ", ignoreCase = true) }
            .values
    }

    val fallback = listOf("Μπλε", "Κόκκινο", "Κίτρινο", "Πράσινο", "Μαύρο", "Λευκό")
    return (fromAttributes + fallback)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
        .sorted()
}

private fun extractAttributeOptions(products: List<Product>): Map<String, List<String>> {
    val ignoredKeys = setOf("color", "χρώμα")
    val grouped = mutableMapOf<String, MutableSet<String>>()

    products.forEach { product ->
        product.attributesMap.forEach { (key, value) ->
            if (key.isBlank() || value.isBlank()) return@forEach
            if (ignoredKeys.any { key.equals(it, ignoreCase = true) }) return@forEach
            grouped.getOrPut(key) { mutableSetOf() }.add(value)
        }
    }

    return grouped.mapValues { (_, values) -> values.sorted() }
}

private fun List<Product>.toPriceBounds(): ClosedFloatingPointRange<Double> {
    if (isEmpty()) return 0.0..500.0
    val min = minOf { it.price }
    val max = maxOf { it.price }
    return if (min == max) (0.0..(max + 1.0)) else (min..max)
}

private fun ClosedFloatingPointRange<Double>.clampTo(bounds: ClosedFloatingPointRange<Double>): ClosedFloatingPointRange<Double> {
    val startClamped = start.coerceIn(bounds.start, bounds.endInclusive)
    val endClamped = endInclusive.coerceIn(bounds.start, bounds.endInclusive)
    return if (startClamped <= endClamped) startClamped..endClamped else bounds
}

private fun Set<String>.toggle(value: String): Set<String> =
    if (contains(value)) this - value else this + value

private fun colorNameToValue(name: String): Color = when (name.trim().lowercase()) {
    "blue", "μπλε" -> Color(0xFF1976D2)
    "red", "κόκκινο" -> Color(0xFFD32F2F)
    "yellow", "κίτρινο" -> Color(0xFFFBC02D)
    "green", "πράσινο" -> Color(0xFF388E3C)
    "black", "μαύρο" -> Color.Black
    "white", "λευκό" -> Color.White
    else -> Color.LightGray
}

private fun activeFiltersCount(filters: FilterState, selectedCategoryId: String): Int {
    var count = 0
    if (selectedCategoryId.isNotBlank()) count++
    if (filters.priceRange != FilterState().priceRange) count++
    if (filters.inStockOnly) count++
    if (filters.brands.isNotEmpty()) count++
    if (filters.colors.isNotEmpty()) count++
    count += filters.attributes.count { it.value.isNotEmpty() }
    return count
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
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )
        Text(
            text = "${product.price} €",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        )
    }
}
