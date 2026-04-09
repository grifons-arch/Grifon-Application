package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.grifon.R
import com.example.grifon.presentation.register.CountryOption
import com.example.grifon.presentation.register.RegisterAddressCatalog
import com.example.grifon.viewmodel.WholesaleApplicationStatus
import com.example.grifon.viewmodel.WholesaleApplicationViewModel
import java.util.Locale

@Composable
fun WholesaleApplicationScreen(
    viewModel: WholesaleApplicationViewModel,
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        val locales = configuration.locales
        if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
    }
    val countrySuggestions = remember(locale, state.country) {
        RegisterAddressCatalog.countrySuggestions(locale, state.country).take(12)
    }
    val citySuggestions = remember(state.countryIso, state.city) {
        RegisterAddressCatalog.citySuggestions(state.countryIso, state.city).take(12)
    }
    val streetSuggestions = remember(state.countryIso, state.city, state.street) {
        RegisterAddressCatalog.streetSuggestions(state.countryIso, state.city, state.street).take(12)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.wholesale_application_screen_title),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )

        Text(
            text = stringResource(R.string.wholesale_application_screen_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        if (!state.isEligible) {
            OutlinedCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = stringResource(R.string.wholesale_application_already_approved),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            SectionTitle(stringResource(R.string.contact_person_section))
            FormTextField(
                value = state.firstName,
                onValueChange = viewModel::onFirstNameChange,
                placeholder = stringResource(R.string.first_name_placeholder),
            )
            FormTextField(
                value = state.lastName,
                onValueChange = viewModel::onLastNameChange,
                placeholder = stringResource(R.string.last_name_placeholder),
            )
            FormTextField(
                value = state.contactPersonFullName,
                onValueChange = viewModel::onContactPersonFullNameChange,
                placeholder = stringResource(R.string.contact_person_full_name_placeholder),
            )
            FormTextField(
                value = state.email,
                onValueChange = viewModel::onEmailChange,
                placeholder = stringResource(R.string.email_placeholder),
            )
            FormTextField(
                value = state.phone,
                onValueChange = viewModel::onPhoneChange,
                placeholder = stringResource(R.string.phone_placeholder_required),
            )

            SectionTitle(stringResource(R.string.company_section))
            FormTextField(
                value = state.company,
                onValueChange = viewModel::onCompanyChange,
                placeholder = stringResource(R.string.company_placeholder_required),
            )
            FormTextField(
                value = state.vatNumber,
                onValueChange = viewModel::onVatNumberChange,
                placeholder = stringResource(R.string.vat_placeholder_required),
            )

            SectionTitle(stringResource(R.string.address_section))
            SearchableCountryField(
                value = state.country,
                onValueChange = viewModel::onCountryChange,
                onOptionSelected = viewModel::onCountrySelected,
                options = countrySuggestions,
                placeholder = stringResource(R.string.country_iso_placeholder),
            )
            SearchableSuggestionField(
                value = state.city,
                onValueChange = viewModel::onCityChange,
                onOptionSelected = viewModel::onCityChange,
                options = citySuggestions,
                placeholder = stringResource(R.string.city_placeholder),
                enabled = state.countryIso.isNotBlank(),
                supportingText = if (state.countryIso.isBlank()) {
                    stringResource(R.string.select_country_first)
                } else {
                    null
                },
            )
            SearchableSuggestionField(
                value = state.street,
                onValueChange = viewModel::onStreetChange,
                onOptionSelected = viewModel::onStreetChange,
                options = streetSuggestions,
                placeholder = stringResource(R.string.street_placeholder),
                enabled = state.countryIso.isNotBlank() && state.city.isNotBlank(),
                supportingText = if (state.city.isBlank()) {
                    stringResource(R.string.select_city_first)
                } else {
                    null
                },
            )
            FormTextField(
                value = state.postalCode,
                onValueChange = viewModel::onPostalCodeChange,
                placeholder = stringResource(R.string.postal_code_placeholder),
            )
            FormTextField(
                value = state.addressCoordinates,
                onValueChange = viewModel::onAddressCoordinatesChange,
                placeholder = stringResource(R.string.address_coordinates_placeholder),
            )

            SectionTitle(stringResource(R.string.supporting_documents_section))
            FormTextField(
                value = state.companyRegistrationFileName,
                onValueChange = viewModel::onCompanyRegistrationChange,
                placeholder = stringResource(R.string.company_registration_document_label),
            )
            FormTextField(
                value = state.invoiceFileName,
                onValueChange = viewModel::onInvoiceChange,
                placeholder = stringResource(R.string.invoice_document_label),
            )

            ConsentOption(
                checked = state.customerDataPrivacyAccepted,
                onCheckedChange = viewModel::onCustomerDataPrivacyAcceptedChange,
                title = stringResource(R.string.customer_data_title),
                description = stringResource(R.string.customer_data_description_full),
                required = true,
            )
            ConsentOption(
                checked = state.termsAndPrivacyAccepted,
                onCheckedChange = viewModel::onTermsAndPrivacyAcceptedChange,
                title = stringResource(R.string.wholesale_terms_privacy_title),
                required = true,
            )
            ConsentOption(
                checked = state.newsletter,
                onCheckedChange = viewModel::onNewsletterChange,
                title = stringResource(R.string.newsletter_opt_in_title),
                description = stringResource(R.string.newsletter_opt_in_description),
            )

            Button(
                onClick = viewModel::submit,
                enabled = state.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                ),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text(stringResource(R.string.wholesale_application_submit))
            }

            when (val status = state.status) {
                is WholesaleApplicationStatus.Loading ->
                    Text(stringResource(R.string.submitting_request), style = MaterialTheme.typography.bodySmall)
                is WholesaleApplicationStatus.Success ->
                    Text(status.message, color = MaterialTheme.colorScheme.primary)
                is WholesaleApplicationStatus.Error ->
                    Text(status.message, color = MaterialTheme.colorScheme.error)
                WholesaleApplicationStatus.Idle -> Unit
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        supportingText = if (!supportingText.isNullOrBlank()) {
            { Text(text = supportingText, style = MaterialTheme.typography.bodySmall) }
        } else {
            null
        },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchableCountryField(
    value: String,
    onValueChange: (String) -> Unit,
    onOptionSelected: (CountryOption) -> Unit,
    options: List<CountryOption>,
    placeholder: String,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val shouldShowMenu = expanded && options.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = shouldShowMenu,
        onExpandedChange = { expanded = !expanded && enabled },
    ) {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = enabled
            },
            enabled = enabled,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = shouldShowMenu)
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        ExposedDropdownMenu(
            expanded = shouldShowMenu,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SearchableSuggestionField(
    value: String,
    onValueChange: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    options: List<String>,
    placeholder: String,
    enabled: Boolean = true,
    supportingText: String? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val shouldShowMenu = expanded && enabled && options.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = shouldShowMenu,
        onExpandedChange = { expanded = !expanded && enabled },
    ) {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = enabled
            },
            enabled = enabled,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = shouldShowMenu)
            },
            supportingText = if (!supportingText.isNullOrBlank()) {
                { Text(text = supportingText, style = MaterialTheme.typography.bodySmall) }
            } else {
                null
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        ExposedDropdownMenu(
            expanded = shouldShowMenu,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun ConsentOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String? = null,
    required: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text(text = if (required) "$title *" else title, style = MaterialTheme.typography.bodyMedium)
            if (!description.isNullOrBlank()) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
