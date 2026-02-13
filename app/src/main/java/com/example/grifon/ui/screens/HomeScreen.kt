package com.example.grifon.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.grifon.domain.model.Product
import com.example.grifon.viewmodel.HomeViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.White
    ) {
        when (val state = uiState) {
            UiState.Loading -> LoadingScreen()
            is UiState.Error -> ErrorScreen(message = state.message)
            is UiState.Success -> {
                val products = state.data.popular + state.data.recent
                
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    item(span = { GridItemSpan(2) }) {
                        Text(
                            text = "Προτεινόμενα Προϊόντα",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    items(products.distinctBy { it.id }) { product ->
                        ProductGridItem(product = product)
                    }
                }
            }
        }
    }
}

@Composable
fun ProductGridItem(product: Product) {
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
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
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
                    modifier = Modifier.fillMaxSize().padding(8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = product.title.uppercase(),
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 16.sp
            ),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            color = Color.Black
        )

        val reference = product.attributesMap["reference"] ?: ""
        if (reference.isNotEmpty()) {
            Text(
                text = "Κωδικός: $reference",
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = 11.sp,
                    color = Color.Gray
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }

        Text(
            text = "${product.price} €",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}
