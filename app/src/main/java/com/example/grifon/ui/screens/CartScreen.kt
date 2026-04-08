package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.CartItem
import com.example.grifon.viewmodel.CartViewModel
import java.util.Locale

@Composable
fun CartScreen(
    viewModel: CartViewModel,
    onCheckout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val cart = state.data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = cart.items, key = { it.productId }) { item ->
                    CartItemCard(
                        item = item,
                        canViewPrices = cart.canViewPrices,
                        onRemove = { viewModel.removeItem(item) },
                    )
                }
                item {
                    if (cart.canViewPrices && cart.total != null) {
                        Text(
                            text = stringResource(
                                R.string.total_amount_with_currency,
                                formatAmount(cart.total),
                                cart.currency.orEmpty(),
                            ),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(onClick = onCheckout) {
                            Text(text = stringResource(R.string.checkout))
                        }
                    } else {
                        Text(
                            text = stringResource(R.string.wholesale_prices_only),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CartItemCard(
    item: CartItem,
    canViewPrices: Boolean,
    onRemove: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(item.imageUrl.ifEmpty { R.drawable.logo })
                    .crossfade(true)
                    .build(),
                contentDescription = item.title,
                placeholder = painterResource(R.drawable.logo),
                error = painterResource(R.drawable.logo),
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(92.dp),
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.product_code, item.productCode),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stringResource(R.string.quantity_value, item.qty),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (canViewPrices) {
                    Text(
                        text = stringResource(
                            R.string.price_amount_with_currency,
                            formatAmount(item.priceSnapshot),
                            item.currency,
                        ),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }

            Box {
                OutlinedButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = stringResource(R.string.remove_from_cart),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.remove_from_cart))
                }
            }
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}
