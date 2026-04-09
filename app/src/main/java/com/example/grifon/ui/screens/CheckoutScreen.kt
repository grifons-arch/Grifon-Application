package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.grifon.R
import com.example.grifon.core.UiEvent
import com.example.grifon.core.ShopConfig
import com.example.grifon.core.UiState
import com.example.grifon.viewmodel.CheckoutAddress
import com.example.grifon.viewmodel.CheckoutMethod
import com.example.grifon.viewmodel.CheckoutState
import com.example.grifon.viewmodel.CheckoutViewModel
import java.util.Locale
@Composable
fun CheckoutScreen(navController: NavHostController, viewModel: CheckoutViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
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
        is UiState.Success -> {
            CheckoutContent(
                state = state.data,
                onRecipientChange = viewModel::onRecipientChange,
                onPhoneChange = viewModel::onPhoneChange,
                onCompanyChange = viewModel::onCompanyChange,
                onStreetChange = viewModel::onStreetChange,
                onCityChange = viewModel::onCityChange,
                onPostalCodeChange = viewModel::onPostalCodeChange,
                onCountryChange = viewModel::onCountryChange,
                onPaymentMethodSelected = viewModel::onPaymentMethodSelected,
                onShippingMethodSelected = viewModel::onShippingMethodSelected,
                onConfirmCheckout = viewModel::confirmCheckout,
                onDismissConfirmation = viewModel::dismissConfirmation,
            )
        }
    }
}

@Composable
private fun CheckoutContent(
    state: CheckoutState,
    onRecipientChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
    onPaymentMethodSelected: (String) -> Unit,
    onShippingMethodSelected: (String) -> Unit,
    onConfirmCheckout: () -> Unit,
    onDismissConfirmation: () -> Unit,
) {
    state.confirmation?.let { confirmation ->
        AlertDialog(
            onDismissRequest = onDismissConfirmation,
            confirmButton = {
                Button(onClick = onDismissConfirmation) {
                    Text(text = stringResource(R.string.accept))
                }
            },
            title = { Text(text = stringResource(R.string.checkout_saved_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(R.string.checkout_saved_message))
                    Text(text = confirmation.message)
                    Text(text = "${stringResource(R.string.product_code, confirmation.reference)}")
                }
            },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            Text(
                text = stringResource(R.string.checkout_screen_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                text = stringResource(R.string.checkout_screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = stringResource(R.string.checkout_summary_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    CheckoutSummaryRow(
                        label = stringResource(R.string.checkout_items_count),
                        value = state.items.sumOf { it.qty }.toString(),
                    )
                    CheckoutSummaryRow(
                        label = stringResource(R.string.checkout_store_label),
                        value = ShopConfig.displayName(state.shopId),
                    )
                    CheckoutSummaryRow(
                        label = stringResource(R.string.checkout_access_label),
                        value = stringResource(
                            if (state.canViewPrices) {
                                R.string.checkout_access_wholesale_active
                            } else {
                                R.string.checkout_access_wholesale_required
                            }
                        ),
                    )
                    if (state.canViewPrices && state.total != null) {
                        CheckoutSummaryRow(
                            label = stringResource(R.string.checkout_total_label),
                            value = "${formatAmount(state.total)} ${state.currency.orEmpty()}".trim(),
                        )
                    }
                }
            }
        }
        if (state.items.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.checkout_products_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.items.forEach { item ->
                            CheckoutSummaryRow(
                                label = "${item.qty}x ${item.title}",
                                value = if (state.canViewPrices) {
                                    "${formatAmount(item.priceSnapshot)} ${item.currency}"
                                } else {
                                    stringResource(R.string.checkout_price_hidden)
                                },
                            )
                        }
                    }
                }
            }
        }
        item {
            CheckoutAddressCard(
                address = state.address,
                onRecipientChange = onRecipientChange,
                onPhoneChange = onPhoneChange,
                onCompanyChange = onCompanyChange,
                onStreetChange = onStreetChange,
                onCityChange = onCityChange,
                onPostalCodeChange = onPostalCodeChange,
                onCountryChange = onCountryChange,
            )
        }
        item {
            CheckoutMethodCard(
                title = stringResource(R.string.checkout_shipping_method_title),
                methods = state.shippingMethods,
                selectedCode = state.selectedShippingCode,
                onSelected = onShippingMethodSelected,
            )
        }
        item {
            CheckoutMethodCard(
                title = stringResource(R.string.checkout_payment_method_title),
                methods = state.paymentMethods,
                selectedCode = state.selectedPaymentCode,
                onSelected = onPaymentMethodSelected,
            )
        }
        if (state.notes.isNotEmpty()) {
            item {
                Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = stringResource(R.string.checkout_notes_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        state.notes.forEach { note ->
                            Text(
                                text = note,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onConfirmCheckout,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.isReadyToConfirm,
                ) {
                    Text(text = stringResource(R.string.checkout_confirm_order))
                }
                if (!state.canViewPrices) {
                    Text(
                        text = stringResource(R.string.wholesale_login_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (!state.address.isComplete) {
                    Text(
                        text = stringResource(R.string.checkout_missing_address),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else if (state.items.isEmpty()) {
                    Text(
                        text = stringResource(R.string.checkout_empty_cart),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun CheckoutAddressCard(
    address: CheckoutAddress,
    onRecipientChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onCompanyChange: (String) -> Unit,
    onStreetChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onPostalCodeChange: (String) -> Unit,
    onCountryChange: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = stringResource(R.string.checkout_shipping_address_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            OutlinedTextField(
                value = address.recipient,
                onValueChange = onRecipientChange,
                label = { Text(stringResource(R.string.checkout_recipient_label)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = address.email,
                onValueChange = {},
                label = { Text(stringResource(R.string.email)) },
                modifier = Modifier.fillMaxWidth(),
                enabled = false,
                singleLine = true,
            )
            OutlinedTextField(
                value = address.phone,
                onValueChange = onPhoneChange,
                label = { Text(stringResource(R.string.phone_placeholder_required)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = address.company,
                onValueChange = onCompanyChange,
                label = { Text(stringResource(R.string.company_placeholder_optional)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            OutlinedTextField(
                value = address.street,
                onValueChange = onStreetChange,
                label = { Text(stringResource(R.string.street_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = address.city,
                    onValueChange = onCityChange,
                    label = { Text(stringResource(R.string.city_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedTextField(
                    value = address.postalCode,
                    onValueChange = onPostalCodeChange,
                    label = { Text(stringResource(R.string.postal_code_placeholder)) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
            }
            OutlinedTextField(
                value = address.country,
                onValueChange = onCountryChange,
                label = { Text(stringResource(R.string.country_iso_placeholder)) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )
        }
    }
}

@Composable
private fun CheckoutMethodCard(
    title: String,
    methods: List<CheckoutMethod>,
    selectedCode: String?,
    onSelected: (String) -> Unit,
) {
    Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            methods.forEach { method ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RadioButton(
                        selected = method.code == selectedCode,
                        onClick = { if (method.available) onSelected(method.code) },
                        enabled = method.available,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = method.title,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (method.available) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                        )
                        if (!method.available) {
                            Text(
                                text = stringResource(R.string.checkout_method_unavailable),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckoutSummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

private fun formatAmount(amount: Double): String {
    return String.format(Locale.getDefault(), "%.2f", amount)
}
