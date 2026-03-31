@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)

package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.grifon.R
import com.example.grifon.domain.model.CatalogFacet
import com.example.grifon.domain.model.CatalogFacetType
import com.example.grifon.domain.model.FilterState
import com.example.grifon.domain.model.Product
import com.example.grifon.domain.model.SortOption
import com.example.grifon.ui.screens.LocalCanDisplayPrices
import com.example.grifon.ui.theme.GrifonBlue
import java.util.Locale

@Composable
fun FilterBar(activeFilters: Int, onFiltersClick: () -> Unit, onSortClick: () -> Unit) {
    val locale = Locale.getDefault()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onFiltersClick,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (activeFilters > 0) Color(0xFF222222) else Color(0xFFD8D8D8)
            ),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = if (activeFilters > 0) Color(0xFFF7F7F7) else Color.White,
                contentColor = Color(0xFF222222)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    stringResource(R.string.filters).uppercase(locale),
                    fontWeight = FontWeight.Medium,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp
                )
                if (activeFilters > 0) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF222222), RoundedCornerShape(9.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeFilters.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
        OutlinedButton(
            onClick = onSortClick,
            shape = RoundedCornerShape(2.dp),
            modifier = Modifier
                .weight(1f)
                .height(42.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8D8D8)),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = Color(0xFF222222)
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
        ) {
            Text(
                stringResource(R.string.sort).uppercase(locale),
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                letterSpacing = 0.6.sp
            )
        }
    }
}

@Composable
fun FiltersSheet(
    currentFilters: FilterState,
    availableFacets: List<CatalogFacet>,
    products: List<Product>,
    onDismiss: () -> Unit,
    onApply: (FilterState) -> Unit,
) {
    val scrollState = rememberScrollState()
    var tempFilters by remember(currentFilters) { mutableStateOf(currentFilters) }
    val canDisplayPrices = LocalCanDisplayPrices.current
    val locale = Locale.getDefault()
    val useServerFacets = availableFacets.isNotEmpty()
    val serverBrandFacet = remember(availableFacets) { availableFacets.firstOrNull { it.type == CatalogFacetType.BRAND } }
    val serverColorFacet = remember(availableFacets) { availableFacets.firstOrNull { it.type == CatalogFacetType.COLOR } }
    val serverPriceFacet = remember(availableFacets) { availableFacets.firstOrNull { it.type == CatalogFacetType.PRICE } }
    val serverAvailabilityFacet = remember(availableFacets) { availableFacets.firstOrNull { it.type == CatalogFacetType.AVAILABILITY } }
    val brands = remember(products) { products.map { it.brand }.filter { it.isNotBlank() }.distinct().sorted() }
    val brandOptions = remember(products, brands, serverBrandFacet, useServerFacets) {
        if (useServerFacets) {
            serverBrandFacet?.options.orEmpty().map { option ->
                FilterOption(
                    key = option.key,
                    label = option.label,
                    count = option.count
                )
            }
        } else {
            brands.map { brand ->
                FilterOption(
                    key = brand,
                    label = brand,
                    count = products.count { it.brand == brand }
                )
            }
        }
    }
    val hasPricedProducts = remember(products) { products.any { it.price != null } }
    val priceVisible = if (useServerFacets) {
        canDisplayPrices && serverPriceFacet != null
    } else {
        canDisplayPrices && hasPricedProducts
    }
    val maxPrice = remember(products, serverPriceFacet, useServerFacets) {
        if (useServerFacets) {
            serverPriceFacet?.maxValue?.coerceAtLeast(10.0) ?: 500.0
        } else {
            products.mapNotNull { it.price }.maxOrNull()?.coerceAtLeast(10.0) ?: 500.0
        }
    }
    val colorOptions = remember(products, locale, serverColorFacet, useServerFacets) {
        if (useServerFacets) {
            serverColorFacet?.options.orEmpty().map { option ->
                FilterOption(
                    key = option.key,
                    label = option.label,
                    count = option.count
                )
            }
        } else {
            buildColorOptions(products, locale)
        }
    }
    val attributeSections = remember(products, locale, availableFacets, useServerFacets) {
        if (useServerFacets) {
            availableFacets
                .filter { it.type == CatalogFacetType.ATTRIBUTE }
                .map { facet ->
                    AttributeFilterSection(
                        key = facet.key,
                        title = facet.title,
                        options = facet.options.map { option ->
                            FilterOption(
                                key = option.key,
                                label = option.label,
                                count = option.count
                            )
                        }
                    )
                }
        } else {
            buildAttributeSections(products, locale)
        }
    }
    val activeSelections = remember(tempFilters) { tempFilters.activeCount() }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFFDFDFC)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 8.dp, end = 16.dp, top = 10.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                            contentDescription = stringResource(R.string.close),
                            tint = Color(0xFF5E5E5E)
                        )
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.filter_by),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                            ),
                            color = Color(0xFF181818)
                        )
                        if (activeSelections > 0) {
                            Text(
                                text = stringResource(R.string.selected_filters_count, activeSelections),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFF8A8A8A)
                            )
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFE1E1DE))

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .verticalScroll(scrollState)
                ) {
                    Spacer(Modifier.height(8.dp))

                    if (colorOptions.isNotEmpty()) {
                        ReferenceFilterSection(title = stringResource(R.string.filter_colors)) {
                            ColorSwatchRow(
                                options = colorOptions,
                                selectedValues = tempFilters.colors,
                                onToggle = { color ->
                                    val nextColors = if (tempFilters.colors.contains(color)) {
                                        tempFilters.colors - color
                                    } else {
                                        tempFilters.colors + color
                                    }
                                    tempFilters = tempFilters.copy(colors = nextColors)
                                }
                            )
                        }
                    }

                    if (brandOptions.isNotEmpty()) {
                        ReferenceFilterSection(title = stringResource(R.string.brand)) {
                            ReferenceFilterOptionList(
                                options = brandOptions,
                                selectedValues = tempFilters.brands,
                                onToggle = { brand ->
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

                    attributeSections.forEach { section ->
                        ReferenceFilterSection(title = section.title) {
                            ReferenceFilterOptionList(
                                options = section.options,
                                selectedValues = tempFilters.attributes[section.key].orEmpty(),
                                onToggle = { value ->
                                    val currentSelection = tempFilters.attributes[section.key].orEmpty()
                                    val nextSelection = if (currentSelection.contains(value)) {
                                        currentSelection - value
                                    } else {
                                        currentSelection + value
                                    }
                                    val nextAttributes = tempFilters.attributes.toMutableMap().apply {
                                        if (nextSelection.isEmpty()) remove(section.key) else put(section.key, nextSelection)
                                    }
                                    tempFilters = tempFilters.copy(attributes = nextAttributes)
                                }
                            )
                        }
                    }

                    if (priceVisible) {
                        val sliderRange = 0f..maxPrice.toFloat()
                        val selectedStart = tempFilters.priceRange.start.toFloat().coerceIn(sliderRange.start, sliderRange.endInclusive)
                        val selectedEnd = tempFilters.priceRange.endInclusive.toFloat().coerceIn(selectedStart, sliderRange.endInclusive)
                        ReferenceFilterSection(title = stringResource(R.string.filter_price)) {
                            RangeSlider(
                                value = selectedStart..selectedEnd,
                                onValueChange = { range ->
                                    tempFilters = tempFilters.copy(priceRange = range.start.toDouble()..range.endInclusive.toDouble())
                                },
                                valueRange = sliderRange,
                                modifier = Modifier.padding(horizontal = 4.dp),
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF0F0F0F),
                                    activeTrackColor = Color(0xFF0F0F0F),
                                    inactiveTrackColor = Color(0xFFD8D8D8)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                PriceEdgeLabel(label = stringResource(R.string.price_from), value = selectedStart)
                                PriceEdgeLabel(label = stringResource(R.string.price_to), value = selectedEnd)
                            }
                        }
                    }

                    if (useServerFacets) {
                        serverAvailabilityFacet?.options?.firstOrNull()?.let { option ->
                            ReferenceFilterSection(title = serverAvailabilityFacet.title) {
                                ReferenceFilterItemRow(
                                    label = option.label,
                                    count = option.count,
                                    selected = tempFilters.inStockOnly,
                                    onToggle = { tempFilters = tempFilters.copy(inStockOnly = !tempFilters.inStockOnly) }
                                )
                            }
                        }
                    } else {
                        ReferenceFilterSection(title = stringResource(R.string.filter_availability)) {
                            ReferenceFilterItemRow(
                                label = stringResource(R.string.in_stock),
                                count = products.count { it.inStock },
                                selected = tempFilters.inStockOnly,
                                onToggle = { tempFilters = tempFilters.copy(inStockOnly = !tempFilters.inStockOnly) }
                            )
                        }

                        ReferenceFilterSection(title = stringResource(R.string.filter_rating)) {
                            ReferenceFilterItemRow(
                                label = stringResource(R.string.rating_four_up),
                                count = products.count { it.rating >= 4.0 },
                                selected = tempFilters.ratingMin >= 4.0,
                                onToggle = {
                                    tempFilters = tempFilters.copy(ratingMin = if (tempFilters.ratingMin >= 4.0) 0.0 else 4.0)
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))
                }

                HorizontalDivider(color = Color(0xFFE1E1DE))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (activeSelections > 0) {
                        TextButton(onClick = { tempFilters = FilterState() }) {
                            Text(
                                text = stringResource(R.string.clear_filters),
                                color = Color(0xFF8A8A8A),
                                fontSize = 12.sp
                            )
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                    OutlinedButton(
                        onClick = { onApply(tempFilters) },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(2.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFD8D8D8)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = Color.White,
                            contentColor = Color(0xFF222222)
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 0.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.apply_filters).uppercase(locale),
                            fontSize = 9.sp,
                            letterSpacing = 0.8.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SortDialog(currentSort: SortOption, onDismiss: () -> Unit, onSelect: (SortOption) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(36.dp)
                    .height(3.dp)
                    .background(Color(0xFFD6D6D6), RoundedCornerShape(100))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.sort),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = Color(0xFF1E1E1E)
                )
                TextButton(onClick = onDismiss) {
                    Text(
                        text = stringResource(R.string.close),
                        color = Color(0xFF4B443B),
                        fontSize = 12.sp
                    )
                }
            }

            HorizontalDivider(color = Color(0xFFE6E6E6))

            SortOption.entries.forEach { option ->
                val selected = currentSort == option
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(option) }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SelectionBox(selected = selected)
                        Text(
                            text = sortLabel(option),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF1E1E1E)
                        )
                    }
                    if (selected) {
                        Text(
                            text = "•",
                            color = Color(0xFF1E1E1E),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                HorizontalDivider(color = Color(0xFFE6E6E6))
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PriceEdgeLabel(
    label: String,
    value: Float,
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFF8A8A8A)
        )
        Text(
            text = String.format(Locale.getDefault(), "%.0f €", value),
            style = MaterialTheme.typography.bodySmall,
            color = Color(0xFF1E1E1E)
        )
    }
}

@Composable
private fun sortLabel(option: SortOption): String = when (option) {
    SortOption.RELEVANCE -> stringResource(R.string.sort_relevance)
    SortOption.PRICE_LOW_HIGH -> stringResource(R.string.sort_price_low_high)
    SortOption.PRICE_HIGH_LOW -> stringResource(R.string.sort_price_high_low)
    SortOption.RATING -> stringResource(R.string.sort_rating)
}

@Composable
private fun ReferenceFilterSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    var expanded by rememberSaveable(title) { mutableStateOf(true) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(top = 7.dp, bottom = 7.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                ),
                color = Color(0xFF1A1A1A)
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFF9A9A9A)
            )
        }
        if (expanded) {
            content()
        }
        HorizontalDivider(color = Color(0xFFE1E1DE), modifier = Modifier.padding(top = 5.dp))
    }
}

@Composable
private fun ReferenceFilterItemRow(label: String, count: Int, selected: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            SelectionBox(selected = selected)
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                color = Color(0xFF1E1E1E)
            )
        }
        Text(
            text = "[$count]",
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFB9B9B9)
        )
    }
}

@Composable
private fun SelectionBox(selected: Boolean) {
    Box(
        modifier = Modifier
            .size(14.dp)
            .background(
                color = if (selected) Color(0xFF222222) else Color.White,
                shape = RoundedCornerShape(2.dp)
            )
            .border(1.dp, Color(0xFFB8B8B8), RoundedCornerShape(2.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Text(
                text = "✓",
                color = Color.White,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp)
            )
        }
    }
}

@Composable
private fun ColorSwatchRow(
    options: List<FilterOption>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        options.forEach { option ->
            val swatchColor = swatchColorFor(option.key)
            val selected = selectedValues.contains(option.key)
            Box(
                modifier = Modifier
                    .size(15.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(swatchColor)
                    .border(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) Color(0xFF111111) else Color(0xFFD0D0D0),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable { onToggle(option.key) }
            )
        }
    }
}

@Composable
private fun ReferenceFilterOptionList(
    options: List<FilterOption>,
    selectedValues: Set<String>,
    onToggle: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleOptions = if (expanded || options.size <= 8) options else options.take(8)

    Column {
        visibleOptions.forEach { option ->
            ReferenceFilterItemRow(
                label = option.label,
                count = option.count,
                selected = selectedValues.contains(option.key),
                onToggle = { onToggle(option.key) }
            )
        }

        if (options.size > 6) {
            TextButton(
                onClick = { expanded = !expanded },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = stringResource(if (expanded) R.string.show_less else R.string.show_more),
                    color = GrifonBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

data class FilterOption(
    val key: String,
    val label: String,
    val count: Int,
)

data class AttributeFilterSection(
    val key: String,
    val title: String,
    val options: List<FilterOption>,
)

fun buildColorOptions(products: List<Product>, locale: Locale): List<FilterOption> {
    return colorCatalog(locale).mapNotNull { descriptor ->
            val count = products.count { product ->
                val haystacks = buildList {
                    add(product.title)
                    add(product.brand)
                    addAll(product.attributesMap.values.flatten())
                    addAll(product.attributesMap.keys)
                }
                haystacks.any { value ->
                descriptor.aliases.any { alias -> value.contains(alias, ignoreCase = true) }
            }
        }

        if (count > 0) {
            FilterOption(
                key = descriptor.key,
                label = descriptor.label,
                count = count
            )
        } else {
            null
        }
    }
}

fun buildAttributeSections(products: List<Product>, locale: Locale): List<AttributeFilterSection> {
    val grouped = products
        .flatMap { product ->
            product.attributesMap.entries.flatMap { entry ->
                entry.value.map { value -> entry.key.trim() to value.trim() }
            }
        }
        .filter { (key, value) ->
            key.isNotBlank() &&
                value.isNotBlank() &&
                !key.equals("reference", ignoreCase = true) &&
                !key.equals("color", ignoreCase = true)
        }
        .groupBy({ it.first }, { it.second })

    return grouped
        .map { (key, values) ->
            val distinctValues = values.distinct().sorted()
            AttributeFilterSection(
                key = key,
                title = localizedAttributeTitle(key, locale),
                options = distinctValues.map { value ->
                    FilterOption(
                        key = value,
                        label = value,
                        count = values.count { it == value }
                    )
                }
            )
        }
        .sortedBy { it.title.lowercase(locale) }
}

private fun localizedAttributeTitle(key: String, locale: Locale): String {
    val normalized = key.trim().lowercase(locale)
    val isGreek = locale.language.equals("el", ignoreCase = true)
    val isSwedish = locale.language.equals("sv", ignoreCase = true)
    return when (normalized) {
        "ram" -> if (isGreek) "Μνήμη RAM" else if (isSwedish) "RAM-minne" else "RAM"
        "storage" -> if (isGreek) "Αποθηκευτικός χώρος" else if (isSwedish) "Lagring" else "Storage"
        "size" -> if (isGreek) "Μέγεθος" else if (isSwedish) "Storlek" else "Size"
        else -> key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

private fun localizedColorName(key: String, locale: Locale): String {
    val isGreek = locale.language.equals("el", ignoreCase = true)
    val isSwedish = locale.language.equals("sv", ignoreCase = true)
    return when (key) {
        "blue" -> if (isGreek) "Μπλε" else if (isSwedish) "Blå" else "Blue"
        "red" -> if (isGreek) "Κόκκινο" else if (isSwedish) "Röd" else "Red"
        "yellow" -> if (isGreek) "Κίτρινο" else if (isSwedish) "Gul" else "Yellow"
        "green" -> if (isGreek) "Πράσινο" else if (isSwedish) "Grön" else "Green"
        "black" -> if (isGreek) "Μαύρο" else if (isSwedish) "Svart" else "Black"
        "white" -> if (isGreek) "Λευκό" else if (isSwedish) "Vit" else "White"
        "gray" -> if (isGreek) "Γκρι" else if (isSwedish) "Grå" else "Gray"
        "pink" -> if (isGreek) "Ροζ" else if (isSwedish) "Rosa" else "Pink"
        "purple" -> if (isGreek) "Μωβ" else if (isSwedish) "Lila" else "Purple"
        "orange" -> if (isGreek) "Πορτοκαλί" else if (isSwedish) "Orange" else "Orange"
        "brown" -> if (isGreek) "Καφέ" else if (isSwedish) "Brun" else "Brown"
        "beige" -> if (isGreek) "Μπεζ" else if (isSwedish) "Beige" else "Beige"
        "gold" -> if (isGreek) "Χρυσό" else if (isSwedish) "Guld" else "Gold"
        "silver" -> if (isGreek) "Ασημί" else if (isSwedish) "Silver" else "Silver"
        "turquoise" -> if (isGreek) "Τυρκουάζ" else if (isSwedish) "Turkos" else "Turquoise"
        "multicolor" -> if (isGreek) "Πολύχρωμο" else if (isSwedish) "Flerfärgad" else "Multicolor"
        else -> key.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    }
}

private fun swatchColorFor(key: String): Color = when (key.lowercase(Locale.ROOT)) {
    "blue" -> Color(0xFF5A768F)
    "red" -> Color(0xFF8B4B55)
    "yellow" -> Color(0xFFD5B867)
    "green" -> Color(0xFF73806A)
    "black" -> Color(0xFF3C3C3C)
    "white" -> Color(0xFFF3F1EB)
    "gray" -> Color(0xFF9A9A9A)
    "pink" -> Color(0xFFD37C9A)
    "purple" -> Color(0xFF8F6BAF)
    "orange" -> Color(0xFFCF8A4A)
    "brown" -> Color(0xFF8B6A55)
    "beige" -> Color(0xFFD9C6AE)
    "gold" -> Color(0xFFC5A059)
    "silver" -> Color(0xFFB8BDC4)
    "turquoise" -> Color(0xFF3B9C98)
    "multicolor" -> Color(0xFFCC6B84)
    else -> Color(0xFFC7B5B5)
}

data class ColorDescriptor(
    val key: String,
    val label: String,
    val aliases: Set<String>,
)

fun colorCatalog(locale: Locale): List<ColorDescriptor> = listOf(
    colorDescriptor("white", locale, "white", "λευκ", "ασπρ", "vit"),
    colorDescriptor("gray", locale, "gray", "grey", "γκρι", "grå", "grafit"),
    colorDescriptor("black", locale, "black", "μαυρ", "svart"),
    colorDescriptor("red", locale, "red", "κόκ", "κοκκ", "röd", "bordo", "burgundy", "bordeaux"),
    colorDescriptor("pink", locale, "pink", "ροζ", "fuchsia", "φουξ", "rosa"),
    colorDescriptor("purple", locale, "purple", "μωβ", "λιλά", "lila", "violet"),
    colorDescriptor("beige", locale, "beige", "μπεζ", "sand", "εκρού", "ecru"),
    colorDescriptor("brown", locale, "brown", "καφέ", "brun", "camel", "tabac"),
    colorDescriptor("yellow", locale, "yellow", "κίτρ", "κιτρ", "gul", "mustard", "μουσταρδ"),
    colorDescriptor("orange", locale, "orange", "πορτοκαλ", "orange"),
    colorDescriptor("green", locale, "green", "πράσ", "πρασ", "grön", "khaki", "χακί", "olive"),
    colorDescriptor("blue", locale, "blue", "μπλε", "blå", "navy", "γαλάζ", "σιελ"),
    colorDescriptor("turquoise", locale, "turquoise", "τυρκ", "turkos", "petrol", "aqua"),
    colorDescriptor("silver", locale, "silver", "ασημ", "silver"),
    colorDescriptor("gold", locale, "gold", "χρυσ", "guld", "ochre", "ώχρα"),
    colorDescriptor("multicolor", locale, "multi", "multicolor", "πολύχρ", "flerfär")
)

fun colorDescriptor(key: String, locale: Locale, vararg aliases: String): ColorDescriptor =
    ColorDescriptor(
        key = key,
        label = localizedColorName(key, locale),
        aliases = aliases.map { it.lowercase(locale) }.toSet()
    )

fun FilterState.activeCount(): Int {
    var count = 0
    if (priceRange != 0.0..500.0) count += 1
    if (brands.isNotEmpty()) count += brands.size
    if (colors.isNotEmpty()) count += colors.size
    if (inStockOnly) count += 1
    if (ratingMin >= 4.0) count += 1
    if (attributes.isNotEmpty()) count += attributes.values.sumOf { it.size }
    return count
}
