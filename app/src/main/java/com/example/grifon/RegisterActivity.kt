package com.example.grifon

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.AppLanguage
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.CountryOption
import com.example.grifon.presentation.register.RegisterAddressCatalog
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import java.util.Locale

class RegisterActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.apply(AppLanguage.getStoredLanguage(this))
        super.onCreate(savedInstanceState)
        setContent {
            GrifonTheme {
                RegisterScreen()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun RegisterScreen(
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(ServiceLocator.provideRegisterUseCase()),
    ),
) {
    val state by registerViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val configuration = LocalConfiguration.current
    val locale = remember(configuration) {
        val locales = configuration.locales
        if (!locales.isEmpty) locales.get(0) else Locale.getDefault()
    }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = stringResource(R.string.register),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )

        RegistrationTextField(value = state.firstName, onValueChange = registerViewModel::onFirstNameChange, placeholder = "Όνομα *")
        RegistrationTextField(value = state.lastName, onValueChange = registerViewModel::onLastNameChange, placeholder = "Επώνυμο *")

        SectionTitle(title = "Διεύθυνση")

        // 1. Επιλογή Χώρας (Dropdown)
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded }
        ) {
            SocialTitleOption(
                label = stringResource(R.string.social_title_mr),
                selected = state.socialTitle == "mr",
                onSelect = { registerViewModel.onSocialTitleChange("mr") },
            )
            Spacer(modifier = Modifier.width(12.dp))
            SocialTitleOption(
                label = stringResource(R.string.social_title_mrs),
                selected = state.socialTitle == "mrs",
                onSelect = { registerViewModel.onSocialTitleChange("mrs") },
            )
            ExposedDropdownMenu(
                expanded = countryExpanded,
                onDismissRequest = { countryExpanded = false }
            ) {
                countries.keys.forEach { name ->
                    DropdownMenuItem(
                        text = { Text(name) },
                        onClick = {
                            registerViewModel.onCountryChange(name)
                            countryExpanded = false
                            addressSearchQuery = "" // Reset search when country changes
                        }
                    )
                }
            }
        }

        RegistrationTextField(
            value = state.firstName,
            onValueChange = registerViewModel::onFirstNameChange,
            placeholder = stringResource(R.string.first_name_placeholder),
            supportingText = if (state.wholesaleRequested) {
                stringResource(R.string.name_validation_hint)
            } else null,
        )
        RegistrationTextField(
            value = state.lastName,
            onValueChange = registerViewModel::onLastNameChange,
            placeholder = stringResource(R.string.last_name_placeholder),
            supportingText = if (state.wholesaleRequested) {
                stringResource(R.string.name_validation_hint)
            } else null,
        )

        ConsentOption(
            checked = state.wholesaleRequested,
            onCheckedChange = registerViewModel::onWholesaleRequestedChange,
            title = stringResource(R.string.wholesale_request_title),
            description = stringResource(R.string.wholesale_request_description)
        )

        if (state.wholesaleRequested) {
            SectionTitle(title = stringResource(R.string.company_section))
            RegistrationTextField(
                value = state.companyName,
                onValueChange = registerViewModel::onCompanyNameChange,
                placeholder = stringResource(R.string.company_placeholder_optional),
            )
            RegistrationTextField(
                value = state.vatNumber,
                onValueChange = registerViewModel::onVatNumberChange,
                placeholder = stringResource(R.string.identification_placeholder_optional),
            )
            SectionTitle(title = stringResource(R.string.address_section))
            SearchableCountryField(
                value = state.country,
                onValueChange = registerViewModel::onCountryChange,
                onOptionSelected = registerViewModel::onCountrySelected,
                options = countrySuggestions,
                placeholder = stringResource(R.string.country_iso_placeholder),
            )
            SearchableSuggestionField(
                value = state.city,
                onValueChange = registerViewModel::onCityChange,
                onOptionSelected = registerViewModel::onCityChange,
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
                onValueChange = registerViewModel::onStreetChange,
                onOptionSelected = registerViewModel::onStreetChange,
                options = streetSuggestions,
                placeholder = stringResource(R.string.street_placeholder),
                enabled = state.countryIso.isNotBlank() && state.city.isNotBlank(),
                supportingText = if (state.city.isBlank()) {
                    stringResource(R.string.select_city_first)
                } else {
                    null
                },
            )
            RegistrationTextField(
                value = state.postalCode,
                onValueChange = registerViewModel::onPostalCodeChange,
                placeholder = stringResource(R.string.postal_code_placeholder),
                keyboardType = KeyboardType.Text,
            )
            RegistrationTextField(
                value = state.email,
                onValueChange = registerViewModel::onEmailChange,
                placeholder = stringResource(R.string.email_placeholder),
            )
            RegistrationTextField(
                value = state.password,
                onValueChange = registerViewModel::onPasswordChange,
                placeholder = stringResource(R.string.password_placeholder_required),
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
            )
            ConsentOption(
                checked = state.customerDataPrivacyAccepted,
                onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
                title = stringResource(R.string.customer_data_title),
                description = stringResource(R.string.customer_data_description_full),
                required = true,
            )
            ConsentOption(
                checked = state.newsletterOptIn,
                onCheckedChange = registerViewModel::onNewsletterOptInChange,
                title = stringResource(R.string.newsletter_opt_in_title),
                description = stringResource(R.string.newsletter_opt_in_description),
            )
            ConsentOption(
                checked = state.termsAndPrivacyAccepted,
                onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange,
                title = stringResource(R.string.wholesale_terms_privacy_title),
                required = true,
            )
        } else {
            RegistrationTextField(
                value = state.email,
                onValueChange = registerViewModel::onEmailChange,
                placeholder = stringResource(R.string.email_placeholder),
            )
            RegistrationTextField(
                value = state.emailConfirmation,
                onValueChange = registerViewModel::onEmailConfirmationChange,
                placeholder = stringResource(R.string.email_confirmation_placeholder),
            )
            RegistrationTextField(
                value = state.password,
                onValueChange = registerViewModel::onPasswordChange,
                placeholder = stringResource(R.string.password_placeholder_required),
                isPassword = true,
                isPasswordVisible = isPasswordVisible,
                onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
            )
            RegistrationTextField(
                value = state.passwordConfirmation,
                onValueChange = registerViewModel::onPasswordConfirmationChange,
                placeholder = stringResource(R.string.password_confirmation_placeholder),
                isPassword = true,
                isPasswordVisible = isPasswordConfirmationVisible,
                onPasswordVisibilityChange = {
                    isPasswordConfirmationVisible = !isPasswordConfirmationVisible
                },
            )
            ConsentOption(
                checked = state.termsAndPrivacyAccepted,
                onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange,
                title = stringResource(R.string.terms_title_simple),
                required = true,
            )
            ConsentOption(
                checked = state.newsletterOptIn,
                onCheckedChange = registerViewModel::onNewsletterOptInChange,
                title = stringResource(R.string.newsletter_opt_in_title),
            )
        }

        Button(
            onClick = registerViewModel::onSubmit,
            enabled = state.isSubmitEnabled,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                contentColor = MaterialTheme.colorScheme.primary,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = stringResource(R.string.save))
        }
        
        when (val status = state.status) {
            is RegisterStatus.Loading -> Text(stringResource(R.string.submitting_request), style = MaterialTheme.typography.bodySmall)
            is RegisterStatus.Success -> Text(status.message, color = MaterialTheme.colorScheme.primary)
            is RegisterStatus.Error -> Text(status.message, color = MaterialTheme.colorScheme.error)
            RegisterStatus.Idle -> Unit
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    supportingText: String? = null,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onPasswordVisibilityChange != null) {
            {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                }
            }
        } else null,
        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
        supportingText = if (!supportingText.isNullOrBlank()) {
            { Text(text = supportingText, style = MaterialTheme.typography.bodySmall) }
        } else null,
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
            } else null,
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
private fun SocialTitleOption(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        RadioButton(selected = selected, onClick = onSelect)
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ConsentOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit, title: String, description: String? = null, required: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(top = 4.dp)) {
            Text(text = if (required) "$title *" else title, style = MaterialTheme.typography.bodyMedium)
            if (!description.isNullOrBlank()) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
