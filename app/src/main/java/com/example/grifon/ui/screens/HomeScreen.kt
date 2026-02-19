package com.example.grifon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.ui.CategoryShortcut
import com.example.grifon.ui.buildCategoryShortcuts
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.HomeViewModel


@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }
    var filtersOpen by remember { mutableStateOf(false) }

    val categoryIcons = remember(uiState) {
        val categories = (uiState as? UiState.Success)?.data?.categories ?: emptyList()
        buildCategoryShortcuts(categories)
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        when (val state = uiState) {
            UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(message = state.message)
            is UiState.Success -> {
                val data = state.data
                val products = data.popular
                val recommendedProducts = data.recent

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(bottom = 80.dp)
                ) {
                    // 1. Ενότητα Κατηγοριών με Συντόμευση Φίλτρου
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Κατηγορίες", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            IconButton(onClick = { filtersOpen = true }) {
                                Icon(Icons.Default.FilterList, contentDescription = "Φίλτρα", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
                            items(categoryIcons) { item ->
                                CategoryIconComponent(
                                    item = item,
                                    isSelected = data.selectedCategoryId == item.categoryId,
                                    onClick = { viewModel.selectCategory(item.categoryId) }
                                )
                            }
                        }
                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                    }

                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                        Text(
                            text = "Προτεινόμενα",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(recommendedProducts) { product ->
                                Box(modifier = Modifier.width(170.dp).height(180.dp)) {
                                    SmallProductCard(
                                        product = product,
                                        onClick = { onProductClick(product.id) },
                                        onImageClick = { zoomedImageUrl = product.imageUrl },
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Όλα τα προϊόντα",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )

                    products.chunked(3).forEach { rowProducts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowProducts.forEach { product ->
                                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                                    SmallProductCard(
                                        product = product,
                                        onClick = { onProductClick(product.id) },
                                        onImageClick = { zoomedImageUrl = product.imageUrl },
                                    )
                                }
                            }
                            repeat(3 - rowProducts.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }

    if (zoomedImageUrl != null) {
        ImageZoomDialog(model = zoomedImageUrl!!, onDismiss = { zoomedImageUrl = null })
    }
}

@Composable
fun CategoryIconComponent(item: CategoryShortcut, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5))
                .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = item.iconResId), 
                contentDescription = item.label, 
                contentScale = ContentScale.Crop, 
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.label, 
            fontSize = 10.sp, 
            textAlign = TextAlign.Center, 
            maxLines = 1, 
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun FeaturedProductCard(product: Product, overrideImageRes: Int? = null, onClick: () -> Unit, onImageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imgModifier = Modifier.fillMaxSize().padding(16.dp).clickable { onImageClick() }
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(product.imageUrl.ifEmpty { R.drawable.logo }).crossfade(true).build(), contentDescription = product.title, contentScale = ContentScale.Fit, modifier = imgModifier)
            
            Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.5f)).padding(8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = product.title, color = Color.White, fontSize = 14.sp, maxLines = 1)
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
fun SmallProductCard(product: Product, overrideImageRes: Int? = null, onClick: () -> Unit, onImageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imgModifier = Modifier.fillMaxSize().padding(8.dp).clickable { onImageClick() }
            AsyncImage(model = ImageRequest.Builder(LocalContext.current).data(product.imageUrl.ifEmpty { R.drawable.logo }).crossfade(true).build(), contentDescription = product.title, contentScale = ContentScale.Fit, modifier = imgModifier)
            
            Row(modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.4f)).padding(4.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = product.title, color = Color.White, fontSize = 9.sp, maxLines = 1, modifier = Modifier.weight(1f))
                Icon(Icons.Default.Favorite, contentDescription = null, tint = Color.White, modifier = Modifier.size(10.dp))
            }
        }
    }
}

@Composable
fun ImageZoomDialog(model: Any, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var scale by remember { mutableStateOf(1f) }
        val state = rememberTransformableState { zoomChange, _, _ -> scale *= zoomChange }
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
            AsyncImage(model = model, contentDescription = null, modifier = Modifier.fillMaxSize().graphicsLayer(scaleX = scale.coerceIn(1f, 5f), scaleY = scale.coerceIn(1f, 5f)).transformable(state = state), contentScale = ContentScale.Fit)
            IconButton(onClick = onDismiss, modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
            }
        }
    }
}
