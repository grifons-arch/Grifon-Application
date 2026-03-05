package com.example.grifon.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.HomeViewModel

data class CategoryDisplayItem(
    val name: String,
    val id: String,
    val imageRes: Int
)

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val bgColor = Color(0xFF090D14)

    Surface(modifier = Modifier.fillMaxSize(), color = bgColor) {
        when (val state = uiState) {
            UiState.Loading -> HomeLoadingIndicator()
            is UiState.Error -> HomeErrorMessage(message = state.message)
            is UiState.Success -> {
                val data = state.data
                val products = data.popular

                val mainCategories = listOf(
                    CategoryDisplayItem("Κεραμικά", "4000", R.drawable.kersmiks_diskodmhtiks),
                    CategoryDisplayItem("Αγαλματίδια", "4500", R.drawable.veroza),
                    CategoryDisplayItem("Διακοσμητικά", "5000", R.drawable.diakosmitika_keramikago),
                    CategoryDisplayItem("Για χρήση", "7500", R.drawable.sapounia),
                    CategoryDisplayItem("Χόμπι", "7000", R.drawable.paixnidiarouytrina),
                    CategoryDisplayItem("Αξεσουάρ", "8000", R.drawable.yfasmatina)
                )

                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    item(span = { GridItemSpan(3) }) {
                        WholesaleBanner()
                    }

                    item(span = { GridItemSpan(3) }) {
                        Column(modifier = Modifier.padding(bottom = 8.dp)) {
                            Text(
                                "Κύριες Κατηγορίες", 
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                            Spacer(Modifier.height(12.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                contentPadding = PaddingValues(horizontal = 16.dp)
                            ) {
                                item {
                                    CategoryCircleComponent(
                                        item = CategoryDisplayItem("Όλα", "0", R.drawable.logo),
                                        isSelected = data.selectedCategoryId == null,
                                        onClick = { viewModel.selectCategory(null) }
                                    )
                                }
                                items(mainCategories) { item ->
                                    CategoryCircleComponent(
                                        item = item,
                                        isSelected = data.selectedCategoryId == item.id,
                                        onClick = { viewModel.selectCategory(item.id) }
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            HorizontalDivider(color = Color(0xFF1A1F2B))
                        }
                    }

                    itemsIndexed(
                        items = products,
                        span = { index, _ -> if (index == 0) GridItemSpan(3) else GridItemSpan(1) }
                    ) { index, product ->
                        Box(modifier = Modifier.padding(4.dp)) {
                            if (index == 0) {
                                FeaturedProductCard(product, onClick = { onProductClick(product.id) })
                            } else {
                                SmallProductCard(product, onClick = { onProductClick(product.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WholesaleBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0A2A8C)),
        shape = RoundedCornerShape(4.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "Για να παραγγείλετε ή να δείτε τιμές, συνδεθείτε ή δημιουργήστε λογαριασμό.",
                color = Color.White,
                fontSize = 12.sp,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
fun CategoryCircleComponent(item: CategoryDisplayItem, isSelected: Boolean, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(70.dp).clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(58.dp)
                .clip(CircleShape)
                .background(if (isSelected) Color(0xFF3F51B5) else Color(0xFF121923))
                .border(if (isSelected) 2.dp else 0.dp, Color.White, CircleShape),
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
            color = if (isSelected) Color.White else Color(0xFFB6BDC9),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
fun FeaturedProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().height(220.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121923))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = product.imageUrl.ifEmpty { R.drawable.logo }, contentDescription = product.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(12.dp))
            Column(modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.7f)).fillMaxWidth().padding(8.dp)) {
                Text(text = product.title, color = Color.White, fontSize = 13.sp, maxLines = 1)
                Text(text = "${product.price}€", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
fun SmallProductCard(product: Product, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().aspectRatio(0.85f).clickable(onClick = onClick),
        shape = RoundedCornerShape(4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF121923))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(model = product.imageUrl.ifEmpty { R.drawable.logo }, contentDescription = product.title, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize().padding(8.dp))
            Column(modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.6f)).fillMaxWidth().padding(4.dp)) {
                Text(text = product.title, color = Color.White, fontSize = 9.sp, maxLines = 1)
                Text(text = "${product.price}€", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun HomeLoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Color.White)
    }
}

@Composable
fun HomeErrorMessage(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = message, color = Color.Red, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp))
    }
}
