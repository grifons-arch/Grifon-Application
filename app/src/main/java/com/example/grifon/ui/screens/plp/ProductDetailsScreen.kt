package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LocalCanDisplayPrices
import com.example.grifon.ui.screens.LocalIsLoggedIn
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.PdpViewModel

@Composable
fun ProductDetailsScreen(viewModel: PdpViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val isFavorite by viewModel.isFavorite.collectAsState()
    val canDisplayPrices = LocalCanDisplayPrices.current
    val isLoggedIn = LocalIsLoggedIn.current
    var showZoomDialog by remember { mutableStateOf(false) }
    var selectedImageIndex by remember { mutableIntStateOf(0) }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val product = state.data
            val scrollState = rememberScrollState()
            
            // Προετοιμασία λίστας εικόνων (αν η λίστα images είναι κενή, χρησιμοποιούμε το imageUrl)
            val allImages = if (product.images.isNotEmpty()) product.images else listOf(product.imageUrl)
            val pagerState = rememberPagerState(pageCount = { allImages.size })

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .background(Color.White)
            ) {
                // Slider Εικόνων
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(350.dp)
                ) {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(allImages[page].ifEmpty { R.drawable.logo })
                                .crossfade(true)
                                .build(),
                            contentDescription = product.title,
                            placeholder = painterResource(R.drawable.logo),
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                                .clickable { 
                                    selectedImageIndex = page
                                    showZoomDialog = true 
                                }
                        )
                    }

                    // Pager Indicators (Τελείες)
                    if (allImages.size > 1) {
                        Row(
                            Modifier
                                .height(50.dp)
                                .fillMaxWidth()
                                .align(Alignment.BottomCenter),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            repeat(allImages.size) { iteration ->
                                val color = if (pagerState.currentPage == iteration) Color.DarkGray else Color.LightGray
                                Box(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .size(8.dp)
                                )
                            }
                        }
                    }
                    
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                        color = Color.Black.copy(alpha = 0.3f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "${pagerState.currentPage + 1}/${allImages.size}",
                            color = Color.White,
                            style = MaterialTheme.typography.labelSmall,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }

                    if (isLoggedIn) {
                        IconButton(
                            onClick = { viewModel.toggleFavorite(product) },
                            modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = stringResource(R.string.favorite_products),
                                tint = if (isFavorite) Color(0xFFE05050) else Color.DarkGray
                            )
                        }
                    }
                }

                // Λεπτομέρειες Προϊόντος
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = product.title,
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.Black
                    )
                    
                    val reference = product.attributesMap["reference"] ?: ""
                    if (reference.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.product_code, reference),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (canDisplayPrices && product.price != null) {
                        Text(
                            text = "${product.price} €",
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.wholesale_prices_only),
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    if (canDisplayPrices && product.price != null) {
                        Button(
                            onClick = { viewModel.addToCart(product) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Text(text = stringResource(R.string.add_to_cart))
                        }
                    }
                }
            }

            // Zoom Dialog για την επιλεγμένη εικόνα
            if (showZoomDialog) {
                ImageZoomDialog(
                    imageUrl = allImages[selectedImageIndex],
                    onDismiss = { showZoomDialog = false }
                )
            }
        }
    }
}

@Composable
fun ImageZoomDialog(imageUrl: String, onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        var scale by remember { mutableStateOf(1f) }
        val state = rememberTransformableState { zoomChange, _, _ ->
            scale *= zoomChange
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            AsyncImage(
                model = imageUrl.ifEmpty { R.drawable.logo },
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale.coerceIn(1f, 5f),
                        scaleY = scale.coerceIn(1f, 5f)
                    )
                    .transformable(state = state),
                contentScale = ContentScale.Fit
            )

            IconButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.TopEnd).padding(16.dp)
            ) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close), tint = Color.White)
            }
        }
    }
}
