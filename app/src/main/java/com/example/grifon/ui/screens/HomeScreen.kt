package com.example.grifon.ui.screens

import android.graphics.Paint as AndroidPaint
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.AppLanguage
import com.example.grifon.domain.model.Product
import com.example.grifon.ui.theme.GrifonGold
import com.example.grifon.viewmodel.HomeViewModel
import com.example.grifon.core.UiState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val canDisplayPrices = LocalCanDisplayPrices.current
    val grifonDark = Color(0xFF121212)
    val grifonGold = Color(0xFFC5A059)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = grifonDark
    ) {
        when (val state = uiState) {
            UiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = grifonGold)
                }
            }
            is UiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(state.message, color = Color.Red, modifier = Modifier.padding(16.dp))
                }
            }
            is UiState.Success -> {
                val data = state.data
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    // 1. Κατηγορίες (Κυκλάκια)
                    item(span = { GridItemSpan(2) }) {
                        Column(modifier = Modifier.background(Color.Black)) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listItems(viewModel.staticCategoryIcons) { cat ->
                                    CategoryIconComponent(
                                        label = homeCategoryLabel(cat.categoryId),
                                        resId = cat.resId,
                                        isSelected = data.selectedCategoryId == cat.categoryId,
                                        onClick = { viewModel.selectCategory(cat.categoryId) }
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.White.copy(0.1f))
                        }
                    }

                    // 2. Τίτλος "Προτεινόμενα" (Οριζόντια)
                    if (data.featuredProducts.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Text(
                                text = stringResource(R.string.featured_for_you),
                                style = MaterialTheme.typography.titleMedium.copy(color = grifonGold, fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        item(span = { GridItemSpan(2) }) {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                listItems(data.featuredProducts) { product ->
                                    Box(modifier = Modifier.width(160.dp)) {
                                        ProductCard(
                                            product = product,
                                            isFavorite = data.favoriteIds.contains(product.id),
                                            onToggleFavorite = { viewModel.toggleFavorite(product) },
                                            onClick = { onProductClick(product.id) }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 3. Τίτλος "Όλα τα Προϊόντα"
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = if (data.selectedCategoryId == null) {
                                stringResource(R.string.all_products)
                            } else {
                                stringResource(R.string.category_products)
                            },
                            style = MaterialTheme.typography.titleMedium.copy(color = grifonGold, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    if (!canDisplayPrices) {
                        item(span = { GridItemSpan(2) }) {
                            WholesaleLoginBanner(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    // 4. Πλέγμα με Όλα τα Προϊόντα
                    if (data.allProducts.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text(stringResource(R.string.no_products_found), color = Color.Gray)
                            }
                        }
                    } else {
                        items(data.allProducts, key = { it.id }) { product ->
                            Box(modifier = Modifier.padding(8.dp)) {
                                ProductCard(
                                    product = product,
                                    isFavorite = data.favoriteIds.contains(product.id),
                                    onToggleFavorite = { viewModel.toggleFavorite(product) },
                                    onClick = { onProductClick(product.id) }
                                )
                            }
                        }
                    }

                    // 5. Newsletter
                    item(span = { GridItemSpan(2) }) {
                        Spacer(modifier = Modifier.height(24.dp))
                        NewsletterSection()
                    }

                    // 6. Cookie Consent
                    item(span = { GridItemSpan(2) }) {
                        CookieConsentBanner()
                    }
                }
            }
        }
    }
}

@Composable
fun WholesaleLoginBanner(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2116)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.wholesale_prices_only),
                color = Color(0xFFC5A059),
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
            )
            Text(
                text = stringResource(R.string.wholesale_login_hint),
                color = Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
fun NewsletterSection() {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF001C46)).padding(24.dp)) {
        Text(stringResource(R.string.newsletter_title), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(stringResource(R.string.newsletter_subtitle), color = Color.White.copy(0.7f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.email), color = Color.Gray) },
            trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(0.2f), focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.05f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            singleLine = true
        )
    }
}

@Composable
fun CookieConsentBanner() {
    Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.95f)).padding(24.dp)) {
        Text(stringResource(R.string.cookie_message), color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005A5F)), shape = RoundedCornerShape(4.dp)) {
            Text(stringResource(R.string.accept), color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryIconComponent(label: String, resId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(132.dp)
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier.size(118.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularRainbowText(
                text = label,
                modifier = Modifier.fillMaxSize(),
                isSelected = isSelected
            )
            Box(
                modifier = Modifier
                    .size(74.dp)
                    .clip(CircleShape)
                    .background(if (isSelected) Color(0xFFC5A059) else Color(0xFF1E1E1E))
                    .border(if (isSelected) 2.dp else 1.dp, Color.White.copy(alpha = if (isSelected) 1f else 0.55f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(id = resId),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
private fun CircularRainbowText(
    text: String,
    modifier: Modifier = Modifier,
    isSelected: Boolean,
) {
    val density = androidx.compose.ui.platform.LocalDensity.current
    val rainbow = remember {
        listOf(
            Color(0xFFFF6B6B),
            Color(0xFFFFA94D),
            Color(0xFFFFE066),
            Color(0xFF69DB7C),
            Color(0xFF4DABF7),
            Color(0xFF9775FA),
            Color(0xFFF06595),
        )
    }
    Canvas(modifier = modifier) {
        val centerX = size.width / 2f
        val centerY = size.height / 2f
        val radius = size.minDimension / 2f - 10.dp.toPx()
        val displayText = text.uppercase()
        val visibleChars = displayText.count { !it.isWhitespace() }.coerceAtLeast(1)
        val sweep = (visibleChars * 14f).coerceIn(150f, 300f)
        val startAngle = -90f - sweep / 2f
        val step = if (displayText.length <= 1) 0f else sweep / (displayText.length - 1)
        val textSizePx = with(density) { if (isSelected) 10.5.sp.toPx() else 10.sp.toPx() }
        val paint = AndroidPaint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.LEFT
            textSize = textSizePx
            typeface = android.graphics.Typeface.create(android.graphics.Typeface.SANS_SERIF, android.graphics.Typeface.BOLD)
            alpha = if (isSelected) 255 else 235
        }

        drawIntoCanvas { canvas ->
            val nativeCanvas = canvas.nativeCanvas
            displayText.forEachIndexed { index, char ->
                val angle = startAngle + step * index
                val radians = angle * (PI / 180f).toFloat()
                val x = centerX + cos(radians) * radius
                val y = centerY + sin(radians) * radius
                if (!char.isWhitespace()) {
                    val glyph = char.toString()
                    val charWidth = paint.measureText(glyph)
                    paint.color = rainbow[index % rainbow.size].toArgb()
                    nativeCanvas.save()
                    nativeCanvas.rotate(angle + 90f, x, y)
                    nativeCanvas.drawText(glyph, x - charWidth / 2f, y + textSizePx / 3.2f, paint)
                    nativeCanvas.restore()
                }
            }
        }
    }
}

@Composable
private fun homeCategoryLabel(categoryId: String?): String {
    return when (AppLanguage.currentLanguage()) {
        "sv" -> when (categoryId) {
            null -> "Alla Produkter"
            "4000" -> "Keramik"
            "4500" -> "Figuriner"
            "5000" -> "Dekor"
            "7500" -> "Bruksföremål"
            "7000" -> "Hobby"
            "8000" -> "Accessoarer"
            else -> "Kategori"
        }
        "en" -> when (categoryId) {
            null -> "All Products"
            "4000" -> "Ceramics"
            "4500" -> "Figurines"
            "5000" -> "Decor"
            "7500" -> "Everyday Use"
            "7000" -> "Hobbies"
            "8000" -> "Accessories"
            else -> "Category"
        }
        else -> when (categoryId) {
            null -> "Όλα τα Προϊόντα"
            "4000" -> "Κεραμικά"
            "4500" -> "Φιγούρες"
            "5000" -> "Διακόσμηση"
            "7500" -> "Είδη Χρήσης"
            "7000" -> "Χόμπι"
            "8000" -> "Αξεσουάρ"
            else -> "Κατηγορία"
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
) {
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column {
            Box(modifier = Modifier.fillMaxWidth().height(160.dp).background(Color.White.copy(0.02f))) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(product.imageUrl).crossfade(true).build(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().padding(12.dp),
                    contentScale = ContentScale.Fit
                )
                if (isLoggedIn) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.94f))
                            .border(1.5.dp, GrifonGold, CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = stringResource(R.string.favorite_products),
                            tint = if (isFavorite) Color(0xFFE05050) else Color(0xFF3B3120)
                        )
                    }
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.title, color = Color.White, fontSize = 13.sp, maxLines = 2, minLines = 2, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                if (canDisplayPrices && product.price != null) {
                    Text("${product.price} €", color = Color(0xFFC5A059), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
