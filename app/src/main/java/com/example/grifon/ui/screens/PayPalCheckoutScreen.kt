package com.example.grifon.ui.screens

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.PayPalCheckoutViewModel

private const val PayPalReturnUrlPrefix = "https://grifon.app/paypal/return"
private const val PayPalCancelUrlPrefix = "https://grifon.app/paypal/cancel"

@Composable
fun PayPalCheckoutScreen(
    navController: NavHostController,
    orderReference: String,
    approvalUrl: String,
    viewModel: PayPalCheckoutViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(orderReference, approvalUrl) {
        viewModel.start(orderReference, approvalUrl)
    }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            if (state.data.completed) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "PayPal payment completed",
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(text = state.data.message)
                            Text(text = "Order reference: ${state.data.orderReference}")
                        }
                    }
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = {
                            navController.popBackStack()
                            navController.popBackStack()
                        }
                    ) {
                        Text(text = "Return to checkout")
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = "PayPal approval",
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Text(text = state.data.message)
                        }
                    }
                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        val url = request?.url?.toString().orEmpty()
                                        if (url.startsWith(PayPalReturnUrlPrefix)) {
                                            viewModel.complete(orderReference)
                                            return true
                                        }
                                        if (url.startsWith(PayPalCancelUrlPrefix)) {
                                            viewModel.markCancelled()
                                            return true
                                        }
                                        return false
                                    }

                                    override fun onPageStarted(
                                        view: WebView?,
                                        url: String?,
                                        favicon: Bitmap?
                                    ) {
                                        super.onPageStarted(view, url, favicon)
                                        val currentUrl = url.orEmpty()
                                        if (currentUrl.startsWith(PayPalReturnUrlPrefix)) {
                                            viewModel.complete(orderReference)
                                        } else if (currentUrl.startsWith(PayPalCancelUrlPrefix)) {
                                            viewModel.markCancelled()
                                        }
                                    }
                                }
                                loadUrl(state.data.approvalUrl)
                            }
                        },
                        update = { webView ->
                            if (webView.url.isNullOrBlank()) {
                                webView.loadUrl(state.data.approvalUrl)
                            }
                        }
                    )
                }
            }
        }
    }
}
