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
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
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
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.HomeViewModel
import com.example.grifon.core.UiState

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
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
                                        label = cat.label,
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
                                text = "Προτεινόμενα για εσάς",
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
                                        ProductCard(product = product, onClick = { onProductClick(product.id) })
                                    }
                                }
                            }
                        }
                    }

                    // 3. Τίτλος "Όλα τα Προϊόντα"
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = if (data.selectedCategoryId == null) "Όλα τα Προϊόντα" else "Προϊόντα Κατηγορίας",
                            style = MaterialTheme.typography.titleMedium.copy(color = grifonGold, fontWeight = FontWeight.Bold),
                            modifier = Modifier.padding(16.dp)
                        )
                    }

                    // 4. Πλέγμα με Όλα τα Προϊόντα
                    if (data.allProducts.isEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                Text("Δεν βρέθηκαν προϊόντα", color = Color.Gray)
                            }
                        }
                    } else {
                        items(data.allProducts, key = { it.id }) { product ->
                            Box(modifier = Modifier.padding(8.dp)) {
                                ProductCard(product = product, onClick = { onProductClick(product.id) })
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
fun NewsletterSection() {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFF001C46)).padding(24.dp)) {
        Text("Εγγραφείτε στο newsletter", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Λάβετε ενημερώσεις για νέα είδη.", color = Color.White.copy(0.7f), fontSize = 13.sp)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = "", onValueChange = {}, modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Email", color = Color.Gray) },
            trailingIcon = { Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White) },
            colors = OutlinedTextFieldDefaults.colors(unfocusedBorderColor = Color.White.copy(0.2f), focusedContainerColor = Color.White.copy(0.05f), unfocusedContainerColor = Color.White.copy(0.05f), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
            singleLine = true
        )
    }
}

@Composable
fun CookieConsentBanner() {
    Column(modifier = Modifier.fillMaxWidth().background(Color.Black.copy(0.95f)).padding(24.dp)) {
        Text("Χρησιμοποιούμε cookies για την καλύτερη εμπειρία σας.", color = Color.White, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {}, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF005A5F)), shape = RoundedCornerShape(4.dp)) {
            Text("ΑΠΟΔΕΧΟΜΑΙ", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun CategoryIconComponent(label: String, resId: Int, isSelected: Boolean, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(75.dp).clickable { onClick() }) {
        Box(modifier = Modifier.size(60.dp).clip(CircleShape).background(if (isSelected) Color(0xFFC5A059) else Color(0xFF1E1E1E)).border(if (isSelected) 2.dp else 0.dp, Color.White, CircleShape), contentAlignment = Alignment.Center) {
            Image(painter = painterResource(id = resId), contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        Text(text = label, fontSize = 11.sp, color = if (isSelected) Color(0xFFC5A059) else Color.White, modifier = Modifier.padding(top = 4.dp), maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
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
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.title, color = Color.White, fontSize = 13.sp, maxLines = 2, minLines = 2, lineHeight = 18.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text("${product.price} €", color = Color(0xFFC5A059), fontSize = 15.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
