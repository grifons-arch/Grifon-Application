package com.example.grifon

import android.app.Activity
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
private fun RegisterScreen(
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(ServiceLocator.provideRegisterUseCase()),
    ),
) {
    val state by registerViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
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
                
                place.addressComponents?.asList()?.forEach { component ->
                    val types = component.types
                    when {
                        types.contains("route") -> streetName = component.name
                        types.contains("street_number") -> streetNumber = component.name
                        types.contains("locality") -> registerViewModel.onCityChange(component.name)
                        types.contains("postal_code") -> registerViewModel.onPostalCodeChange(component.name)
                        types.contains("country") -> registerViewModel.onCountryChange(component.shortName ?: component.name)
                    }
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
                apiErrorMessage = "Σφάλμα Google: ${status.statusMessage}"
            }
        } catch (e: Exception) {
            apiErrorMessage = "Σφάλμα κατά την επεξεργασία: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Εγγραφή",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SocialTitleOption(
                label = "Κος",
                selected = state.socialTitle == "mr",
                onSelect = { registerViewModel.onSocialTitleChange("mr") },
            )
            Spacer(modifier = Modifier.width(12.dp))
            SocialTitleOption(
                label = "Κα",
                selected = state.socialTitle == "mrs",
                onSelect = { registerViewModel.onSocialTitleChange("mrs") },
            )
        }

        RegistrationTextField(
            value = state.firstName,
            onValueChange = registerViewModel::onFirstNameChange,
            placeholder = "Όνομα *",
        )
        RegistrationTextField(
            value = state.lastName,
            onValueChange = registerViewModel::onLastNameChange,
            placeholder = "Επώνυμο *",
        )
        RegistrationTextField(
            value = state.phone,
            onValueChange = registerViewModel::onPhoneChange,
            placeholder = "Τηλέφωνο",
        )
        RegistrationTextField(
            value = state.iban,
            onValueChange = registerViewModel::onIbanChange,
            placeholder = "IBAN",
        )

        SectionTitle(title = "Εταιρεία")
        
        ConsentOption(
            checked = state.wholesaleRequested,
            onCheckedChange = registerViewModel::onWholesaleRequestedChange,
            title = "Αίτηση για λογαριασμό Χονδρικής",
            description = "Επιλέξτε αν είστε επαγγελματίας και επιθυμείτε πρόσβαση σε τιμές χονδρικής."
        )

        RegistrationTextField(
            value = state.companyName,
            onValueChange = registerViewModel::onCompanyNameChange,
            placeholder = "Εταιρεία",
        )
        RegistrationTextField(
            value = state.vatNumber,
            onValueChange = registerViewModel::onVatNumberChange,
            placeholder = "Α.Φ.Μ",
        )

        SectionTitle(title = "Διεύθυνση +")

        Text(
            text = "Αναζήτηση διεύθυνσης στο χάρτη (κλικ εδώ)",
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
                            apiErrorMessage = "Λείπει το API Key"
                        } else {
                            val fields = listOf(Place.Field.ID, Place.Field.NAME, Place.Field.ADDRESS_COMPONENTS)
                            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.FULLSCREEN, fields)
                                .setCountries(listOf("GR")) // Νέος τρόπος περιορισμού χώρας
                                .build(context)
                            autocompleteLauncher.launch(intent)
                        }
                    } catch (e: Exception) {
                        apiErrorMessage = "Αποτυχία ανοίγματος χάρτη: ${e.message}"
                    }
                }
        )
        
        RegistrationTextField(
            value = state.street,
            onValueChange = registerViewModel::onStreetChange,
            placeholder = "Οδός και Αριθμός *",
        )

        apiErrorMessage?.let { message ->
            Text(text = message, color = Color.Red, style = MaterialTheme.typography.bodySmall)
        }

        RegistrationTextField(
            value = state.city,
            onValueChange = registerViewModel::onCityChange,
            placeholder = "Πόλη *",
        )
        RegistrationTextField(
            value = state.country,
            onValueChange = registerViewModel::onCountryChange,
            placeholder = "Χώρα (ISO, π.χ. GR) *",
        )
        RegistrationTextField(
            value = state.postalCode,
            onValueChange = registerViewModel::onPostalCodeChange,
            placeholder = "Τ.Κ *",
        )

        SectionTitle(title = "Άλλα στοιχεία")
        RegistrationTextField(
            value = state.email,
            onValueChange = registerViewModel::onEmailChange,
            placeholder = "Email *",
        )
        RegistrationTextField(
            value = state.emailConfirmation,
            onValueChange = registerViewModel::onEmailConfirmationChange,
            placeholder = "Επιβεβαίωση Email *",
        )
        RegistrationTextField(
            value = state.password,
            onValueChange = registerViewModel::onPasswordChange,
            placeholder = "Κωδικός *",
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
        )
        RegistrationTextField(
            value = state.passwordConfirmation,
            onValueChange = registerViewModel::onPasswordConfirmationChange,
            placeholder = "Επιβεβαίωση Κωδικού *",
            isPassword = true,
            isPasswordVisible = isPasswordConfirmationVisible,
            onPasswordVisibilityChange = {
                isPasswordConfirmationVisible = !isPasswordConfirmationVisible
            },
        )

        ConsentOption(
            checked = state.customerDataPrivacyAccepted,
            onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
            title = "Προστασία δεδομένων πελάτη",
            description = "Τα προσωπικά δεδομένα που παρέχετε χρησιμοποιούνται για την απάντηση " +
                "σε αιτήματα, την επεξεργασία παραγγελιών ή την παροχή πρόσβασης σε συγκεκριμένες " +
                "πληροφορίες.",
            required = true,
        )
        ConsentOption(
            checked = state.newsletterOptIn,
            onCheckedChange = registerViewModel::onNewsletterOptInChange,
            title = "Εγγραφείτε στο ενημερωτικό δελτίο μας",
        )
        ConsentOption(
            checked = state.termsAndPrivacyAccepted,
            onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange,
            title = "Αποδέχομαι τους όρους και την πολιτική απορρήτου",
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
            Text(text = "Αποθήκευση")
        }
        
        when (val status = state.status) {
            is RegisterStatus.Loading -> Text("Η αίτηση αποστέλλεται...", style = MaterialTheme.typography.bodySmall)
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
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
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
