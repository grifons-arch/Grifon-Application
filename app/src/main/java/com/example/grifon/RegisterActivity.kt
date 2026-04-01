package com.example.grifon

import android.app.Activity
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.AppLanguage
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.CountryOption
import com.example.grifon.presentation.register.RegisterAddressCatalog
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode
import java.util.Locale

class RegisterActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrapContext(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        AppLanguage.apply(AppLanguage.getStoredLanguage(this))
        super.onCreate(savedInstanceState)
        // Αρχικοποίηση στην αρχή της Activity για σιγουριά
        if (BuildConfig.MAPS_API_KEY.isNotEmpty() && !Places.isInitialized()) {
            Places.initialize(applicationContext, BuildConfig.MAPS_API_KEY)
        }
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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    LaunchedEffect(context) {
        if (BuildConfig.MAPS_API_KEY.isNotEmpty() && !Places.isInitialized()) {
            Places.initialize(context.applicationContext, BuildConfig.MAPS_API_KEY)
        }
    }
    val locale = remember(context) {
        val locales = context.resources.configuration.locales
        if (locales.isEmpty) Locale.getDefault() else locales[0]
    }
    val countries = remember(locale) { RegisterAddressCatalog.countriesFor(locale) }
    val citySuggestions = remember(state.countryIso, state.city) {
        RegisterAddressCatalog.citySuggestions(state.countryIso, state.city)
    }
    val streetSuggestions = remember(state.countryIso, state.city, state.street) {
        RegisterAddressCatalog.streetSuggestions(state.countryIso, state.city, state.street)
    }
    var apiErrorMessage by remember { mutableStateOf<String?>(null) }

    // Launcher για το Google Autocomplete Intent
    val autocompleteLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        try {
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val place = Autocomplete.getPlaceFromIntent(result.data!!)
                
                var streetName = ""
                var streetNumber = ""
                var selectedCity = ""
                var selectedPostalCode = ""
                var selectedCountryIso = ""
                var selectedCountryName = ""
                
                place.addressComponents?.asList()?.forEach { component ->
                    val types = component.types
                    when {
                        types.contains("route") -> streetName = component.name
                        types.contains("street_number") -> streetNumber = component.name
                        types.contains("locality") -> selectedCity = component.name
                        types.contains("postal_code") -> selectedPostalCode = component.name
                        types.contains("country") -> {
                            selectedCountryIso = component.shortName.orEmpty()
                            selectedCountryName = component.name
                        }
                    }
                }

                val resolvedCountry = RegisterAddressCatalog.resolveCountry(
                    selectedCountryIso.ifBlank { selectedCountryName },
                    locale,
                )
                when {
                    resolvedCountry != null -> registerViewModel.onCountrySelected(resolvedCountry)
                    selectedCountryName.isNotBlank() -> registerViewModel.onCountryChange(selectedCountryName)
                }

                if (selectedCity.isNotBlank()) {
                    registerViewModel.onCityChange(selectedCity)
                }
                if (selectedPostalCode.isNotBlank()) {
                    registerViewModel.onPostalCodeChange(selectedPostalCode)
                }
                
                val fullStreet = if (streetNumber.isNotEmpty()) "$streetName $streetNumber" else streetName
                if (fullStreet.isNotEmpty()) {
                    registerViewModel.onStreetChange(fullStreet)
                } else {
                    registerViewModel.onStreetChange(place.name ?: "")
                }
                apiErrorMessage = null
            } else if (result.resultCode == 2) { // 2 είναι η τιμή του Autocomplete.RESULT_ERROR
                val status = Autocomplete.getStatusFromIntent(result.data!!)
                apiErrorMessage = context.getString(R.string.google_error, status.statusMessage.orEmpty())
            }
        } catch (e: Exception) {
            apiErrorMessage = context.getString(R.string.processing_error, e.message.orEmpty())
        }
    }

    val companyRegistrationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        registerViewModel.onCompanyRegistrationFileSelected(resolveDocumentName(context, uri))
    }

    val invoiceLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        registerViewModel.onInvoiceFileSelected(resolveDocumentName(context, uri))
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
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
        }

        RegistrationTextField(
            value = state.firstName,
            onValueChange = registerViewModel::onFirstNameChange,
            placeholder = stringResource(R.string.first_name_placeholder),
        )
        RegistrationTextField(
            value = state.lastName,
            onValueChange = registerViewModel::onLastNameChange,
            placeholder = stringResource(R.string.last_name_placeholder),
        )
        if (!state.wholesaleRequested) {
            RegistrationTextField(
                value = state.phone,
                onValueChange = registerViewModel::onPhoneChange,
                placeholder = stringResource(R.string.phone_placeholder),
            )
        }
        RegistrationTextField(
            value = state.iban,
            onValueChange = registerViewModel::onIbanChange,
            placeholder = stringResource(R.string.iban),
        )

        SectionTitle(title = stringResource(R.string.company_section))
        
        ConsentOption(
            checked = state.wholesaleRequested,
            onCheckedChange = registerViewModel::onWholesaleRequestedChange,
            title = stringResource(R.string.wholesale_request_title),
            description = stringResource(R.string.wholesale_request_description)
        )

        RegistrationTextField(
            value = state.companyName,
            onValueChange = registerViewModel::onCompanyNameChange,
            placeholder = if (state.wholesaleRequested) {
                stringResource(R.string.company_placeholder_required)
            } else {
                stringResource(R.string.company_placeholder)
            },
        )
        RegistrationTextField(
            value = state.vatNumber,
            onValueChange = registerViewModel::onVatNumberChange,
            placeholder = if (state.wholesaleRequested) {
                stringResource(R.string.vat_placeholder_required)
            } else {
                stringResource(R.string.vat_placeholder)
            },
        )

        SectionTitle(title = stringResource(R.string.address_section))

        Text(
            text = stringResource(R.string.search_address_on_map),
            style = MaterialTheme.typography.bodyMedium.copy(
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline
            ),
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clickable {
                    try {
                        if (BuildConfig.MAPS_API_KEY.isEmpty()) {
                            apiErrorMessage = context.getString(R.string.missing_api_key)
                        } else {
                            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS)
                            val builder = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                            if (state.countryIso.isNotBlank()) {
                                builder.setCountries(listOf(state.countryIso))
                            }
                            val intent = builder.build(context)
                            autocompleteLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        apiErrorMessage = context.getString(R.string.open_map_failed, e.message.orEmpty())
                    }
                }
        )
        
        RegistrationDropdownField(
            value = state.country,
            placeholder = stringResource(R.string.country_iso_placeholder),
            options = countries,
            onOptionSelected = registerViewModel::onCountrySelected,
        )
        RegistrationAutoCompleteField(
            value = state.city,
            onValueChange = registerViewModel::onCityChange,
            placeholder = stringResource(R.string.city_placeholder),
            suggestions = citySuggestions,
            enabled = state.countryIso.isNotBlank(),
        )
        RegistrationAutoCompleteField(
            value = state.street,
            onValueChange = registerViewModel::onStreetChange,
            placeholder = stringResource(R.string.street_placeholder),
            suggestions = streetSuggestions,
            enabled = state.countryIso.isNotBlank() && state.city.isNotBlank(),
        )

        apiErrorMessage?.let { message ->
            Text(text = message, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        RegistrationTextField(
            value = state.postalCode,
            onValueChange = registerViewModel::onPostalCodeChange,
            placeholder = stringResource(R.string.postal_code_placeholder),
        )

        if (state.wholesaleRequested) {
            RegistrationTextField(
                value = state.addressCoordinates,
                onValueChange = registerViewModel::onAddressCoordinatesChange,
                placeholder = stringResource(R.string.address_coordinates_placeholder),
            )

            SectionTitle(title = stringResource(R.string.contact_person_section))
            RegistrationTextField(
                value = state.contactPersonFullName,
                onValueChange = registerViewModel::onContactPersonFullNameChange,
                placeholder = stringResource(R.string.contact_person_full_name_placeholder),
            )
            RegistrationTextField(
                value = state.phone,
                onValueChange = registerViewModel::onPhoneChange,
                placeholder = stringResource(R.string.phone_placeholder_required),
            )

            SectionTitle(title = stringResource(R.string.supporting_documents_section))
            DocumentPickerField(
                label = stringResource(R.string.company_registration_document_label),
                selectedFileName = state.companyRegistrationFileName,
                onPickClick = { companyRegistrationLauncher.launch(arrayOf("application/pdf", "image/*")) },
            )
            DocumentPickerField(
                label = stringResource(R.string.invoice_document_label),
                selectedFileName = state.invoiceFileName,
                onPickClick = { invoiceLauncher.launch(arrayOf("application/pdf", "image/*")) },
            )
        }

        SectionTitle(title = stringResource(R.string.other_details_section))
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
            checked = state.customerDataPrivacyAccepted,
            onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
            title = stringResource(R.string.customer_data_title),
            description = stringResource(R.string.customer_data_description),
            required = true,
        )
        ConsentOption(
            checked = state.newsletterOptIn,
            onCheckedChange = registerViewModel::onNewsletterOptInChange,
            title = stringResource(R.string.newsletter_opt_in_title),
        )
        ConsentOption(
            checked = state.termsAndPrivacyAccepted,
            onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange,
            title = stringResource(R.string.terms_privacy_title),
            required = true,
        )

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
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

@Composable
private fun RegistrationTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    enabled: Boolean = true,
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
) {
    TextField(
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
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationDropdownField(
    value: String,
    placeholder: String,
    options: List<CountryOption>,
    onOptionSelected: (CountryOption) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        TextField(
            value = value,
            onValueChange = {},
            readOnly = true,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(text = option.displayName) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RegistrationAutoCompleteField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suggestions: List<String>,
    enabled: Boolean = true,
) {
    var expanded by remember { mutableStateOf(false) }
    val visibleSuggestions = remember(suggestions) { suggestions.distinct().take(8) }
    val showMenu = enabled && expanded && visibleSuggestions.isNotEmpty()

    ExposedDropdownMenuBox(
        expanded = showMenu,
        onExpandedChange = {
            if (enabled && visibleSuggestions.isNotEmpty()) {
                expanded = !expanded
            }
        },
    ) {
        TextField(
            value = value,
            onValueChange = {
                onValueChange(it)
                expanded = true
            },
            enabled = enabled,
            placeholder = { Text(text = placeholder, style = MaterialTheme.typography.bodyMedium) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(MenuAnchorType.PrimaryEditable),
            shape = RoundedCornerShape(10.dp),
            trailingIcon = {
                if (visibleSuggestions.isNotEmpty()) {
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMenu)
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        )

        ExposedDropdownMenu(
            expanded = showMenu,
            onDismissRequest = { expanded = false },
        ) {
            visibleSuggestions.forEach { suggestion ->
                DropdownMenuItem(
                    text = { Text(text = suggestion) },
                    onClick = {
                        onValueChange(suggestion)
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

@Composable
private fun DocumentPickerField(
    label: String,
    selectedFileName: String?,
    onPickClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
        )
        OutlinedButton(
            onClick = onPickClick,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
        ) {
            Text(
                text = if (selectedFileName.isNullOrBlank()) {
                    stringResource(R.string.choose_file)
                } else {
                    stringResource(R.string.replace_file)
                }
            )
        }
        Text(
            text = if (selectedFileName.isNullOrBlank()) {
                stringResource(R.string.no_file_selected)
            } else {
                stringResource(R.string.selected_file_value, selectedFileName)
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun resolveDocumentName(context: Context, uri: Uri?): String? {
    if (uri == null) return null

    val projection = arrayOf(OpenableColumns.DISPLAY_NAME)
    context.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (index >= 0 && cursor.moveToFirst()) {
            return cursor.getString(index)
        }
    }

    return uri.lastPathSegment?.substringAfterLast('/')
}
