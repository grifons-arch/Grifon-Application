package com.example.grifon.ui.screens.plp

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.PdpViewModel

@Composable
fun ProductDetailsScreen(viewModel: PdpViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val product = state.data
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
            ) {
                Card(modifier = Modifier.fillMaxSize()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = product.title, style = MaterialTheme.typography.titleLarge)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "${product.price} ${product.currency}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Brand: ${product.brand}")
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(text = "Rating: ${product.rating}")
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { viewModel.addToCart(product) }) {
                            Text(text = "Προσθήκη στο καλάθι")
                        }
                    }
                }
            }
        }
    }
}
