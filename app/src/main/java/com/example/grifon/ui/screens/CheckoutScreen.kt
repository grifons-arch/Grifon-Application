package com.example.grifon.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.grifon.core.UiEvent
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.LocalOrder
import com.example.grifon.viewmodel.CheckoutMethod
import com.example.grifon.viewmodel.CheckoutState
import com.example.grifon.viewmodel.CheckoutViewModel
import com.example.grifon.viewmodel.OrdersViewModel
import java.util.Locale

@Composable
fun CheckoutScreen(
    navController: NavHostController,
    viewModel: CheckoutViewModel,
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(viewModel, navController) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ShowSnackbar -> Unit
            }
        }
    }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> CheckoutContent(
            state = state.data,
            viewModel = viewModel,
            onBack = { navController.popBackStack() },
        )
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutState,
    viewModel: CheckoutViewModel,
    onBack: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = "Checkout",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }

        state.notes.forEach { note ->
            item {
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item {
            CheckoutAddressFields(state, viewModel)
        }

        item {
            CheckoutMethodSection(
                title = "Shipping",
                methods = state.shippingMethods,
                selectedCode = state.selectedShippingCode,
                onSelect = viewModel::onShippingMethodSelected,
            )
        }

        item {
            CheckoutMethodSection(
                title = "Payment",
                methods = state.paymentMethods,
                selectedCode = state.selectedPaymentCode,
                onSelect = viewModel::onPaymentMethodSelected,
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Items: ${state.items.sumOf { it.qty }}")
                    Text("Total: ${state.total?.let { formatAmount(it) } ?: "-"} ${state.currency.orEmpty()}")
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = onBack, modifier = Modifier.weight(1f)) {
                    Text("Back")
                }
                Button(
                    onClick = viewModel::confirmCheckout,
                    enabled = state.isReadyToConfirm,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Place order")
                }
            }
        }
    }

    state.confirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = viewModel::dismissConfirmation,
            title = { Text(confirmation.reference) },
            text = { Text(confirmation.message) },
            confirmButton = {
                TextButton(onClick = viewModel::dismissConfirmation) {
                    Text("OK")
                }
            },
        )
    }
}

@Composable
private fun CheckoutAddressFields(
    state: CheckoutState,
    viewModel: CheckoutViewModel,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Address", style = MaterialTheme.typography.titleMedium)
        OutlinedTextField(
            value = state.address.recipient,
            onValueChange = viewModel::onRecipientChange,
            label = { Text("Recipient") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.phone,
            onValueChange = viewModel::onPhoneChange,
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.company,
            onValueChange = viewModel::onCompanyChange,
            label = { Text("Company") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.street,
            onValueChange = viewModel::onStreetChange,
            label = { Text("Street") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.city,
            onValueChange = viewModel::onCityChange,
            label = { Text("City") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.postalCode,
            onValueChange = viewModel::onPostalCodeChange,
            label = { Text("Postal code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
        OutlinedTextField(
            value = state.address.country,
            onValueChange = viewModel::onCountryChange,
            label = { Text("Country") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
        )
    }
}

@Composable
private fun CheckoutMethodSection(
    title: String,
    methods: List<CheckoutMethod>,
    selectedCode: String?,
    onSelect: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        methods.forEach { method ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(
                    selected = method.code == selectedCode,
                    enabled = method.available,
                    onClick = { onSelect(method.code) },
                )
                Text(
                    text = method.title,
                    color = if (method.available) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}

@Composable
fun PayPalCheckoutScreen(
    navController: NavHostController,
    orderReference: String,
    approvalUrl: String,
    viewModel: CheckoutViewModel,
) {
    ExternalCheckoutScreen(
        title = "PayPal checkout",
        orderReference = orderReference,
        url = approvalUrl,
        navController = navController,
    )
}

@Composable
fun StripeCheckoutScreen(
    navController: NavHostController,
    orderReference: String,
    checkoutUrl: String,
    viewModel: CheckoutViewModel,
) {
    ExternalCheckoutScreen(
        title = "Stripe checkout",
        orderReference = orderReference,
        url = checkoutUrl,
        navController = navController,
    )
}

@Composable
private fun ExternalCheckoutScreen(
    title: String,
    orderReference: String,
    url: String,
    navController: NavHostController,
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Order: $orderReference")
        Button(
            onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            },
            enabled = url.isNotBlank(),
        ) {
            Text("Open payment")
        }
        OutlinedButton(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }
}

@Composable
fun OrdersScreen(viewModel: OrdersViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val orders = state.data
            if (orders.isEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("No orders yet.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(orders, key = { it.orderReference }) { order ->
                        OrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderCard(order: LocalOrder) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(order.orderReference, style = MaterialTheme.typography.titleMedium)
            Text("${formatAmount(order.totalAmount)} ${order.currency}")
            Text("Payment: ${order.paymentStatus}")
            Text("Order: ${order.orderStatus}")
            Spacer(modifier = Modifier.height(4.dp))
            Text("Items: ${order.items.sumOf { it.qty }}")
        }
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}
