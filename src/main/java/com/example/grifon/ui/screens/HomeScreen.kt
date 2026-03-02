package com.example.grifon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
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
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.HomeViewModel

data class CategoryDisplayItem(
    val name: String,
    val searchName: String,
    val imageRes: Int
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
        when (val state = uiState) {
            UiState.Loading -> HomeLoadingIndicator()
            is UiState.Error -> HomeErrorMessage(message = state.message)
            is UiState.Success -> {
                val data = state.data
                val products = data.popular

                val mainCategories = listOf(
                    CategoryDisplayItem("ÎšÎµÏÎ±Î¼Î¹ÎºÎ¬", "Ceramics", R.drawable.kersmiks_diskodmhtiks),
                    CategoryDisplayItem("Î‘Î³Î±Î»Î¼Î±Ï„Î¯Î´Î¹Î±", "Statuettes", R.drawable.veroza),
                    CategoryDisplayItem("Î”Î¹Î±ÎºÎ¿ÏƒÎ¼Î·Ï„Î¹ÎºÎ¬", "Decorative", R.drawable.diakosmitika_keramikago),
                    CategoryDisplayItem("Î“Î¹Î± Ï‡ÏÎ®ÏƒÎ·", "For use", R.drawable.sapounia),
                    CategoryDisplayItem("Î§ÏŒÎ¼Ï€Î¹", "Hobbies", R.drawable.paixnidiarouytrina),
                    CategoryDisplayItem("Î‘Î¾ÎµÏƒÎ¿Ï…Î¬Ï", "Accessory", R.drawable.yfasmatina)
                )

                val displayCategories = remember(data.categories) {
                    val list = mutableListOf<Pair<CategoryDisplayItem, String?>>()
                    list.add(CategoryDisplayItem("ÎŒÎ»Î±", "Home", R.drawable.logo) to null)
                    
                    mainCategories.forEach { item ->
                        val cat = data.categories.find { 
                            it.name.contains(item.name, ignoreCase = true) || 
                            it.name.contains(item.searchName, ignoreCase = true) 
                        }
                        list.add(item to cat?.id)
                    }
                    list
                }

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item(span = { GridItemSpan(3) }) {
                        Column(modifier = Modifier.padding(vertical = 16.dp)) {
                            Text(
                                "ÎšÏÏÎ¹ÎµÏ‚ ÎšÎ±Ï„Î·Î³Î¿ÏÎ¯ÎµÏ‚", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                items(displayCategories) { (item, id) ->
                                    CategoryCircleComponent(
                                        item = item,
                                        isSelected = data.selectedCategoryId == id,
                                        onClick = { viewModel.selectCategory(id) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(16.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
                        }
                    }

                    if (products.isEmpty()) {
                        item(span = { GridItemSpan(3) }) {
                            Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                                Text("Î”ÎµÎ½ Î²ÏÎ­Î¸Î·ÎºÎ±Î½ Ï€ÏÎ¿ÏŠÏŒÎ½Ï„Î±")
                            }
                        }
                    }

                    itemsIndexed(
                        items = products,
                        span = { index, _ -> if (index == 0) GridItemSpan(3) else GridItemSpan(1) }
                    ) { index, product ->
                        Box(modifier = Modifier.padding(4.dp)) {
                            if (index == 0) {
                                FeaturedProductCard(product, onClick = { onProductClick(product.id) }) { zoomedImageUrl = product.imageUrl }
                            } else {
                                SmallProductCard(product, onClick = { onProductClick(product.id) }) { zoomedImageUrl = product.imageUrl }
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
fun CategoryCircleComponent(item: CategoryDisplayItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(75.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(65.dp)
                .clip(CircleShape)
                .background(if (isSelected) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF5F5F5))
                .border(if (isSelected) 2.dp else 0.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = item.imageRes),
                contentDescription = item.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = item.name,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun HomeLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

@Composable
fun HomeErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun FeaturedProductCard(product: Product, onClick: () -> Unit, onImageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(240.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = product.imageUrl.ifEmpty { R.drawable.logo }, contentDescription = product.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp).clickable { onImageClick() })
            Column(modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f)).fillMaxWidth().padding(8.dp)) {
                Text(text = product.title, color = Color.White, fontSize = 14.sp, maxLines = 2)
                Text(text = "${product.price}â‚¬", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun SmallProductCard(product: Product, onClick: () -> Unit, onImageClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.75f).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = product.imageUrl.ifEmpty { R.drawable.logo }, contentDescription = product.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp).clickable { onImageClick() })
            Column(modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f)).fillMaxWidth().padding(4.dp)) {
                Text(text = product.title, color = Color.White, fontSize = 10.sp, maxLines = 1)
                Text(text = "${product.price}â‚¬", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
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



