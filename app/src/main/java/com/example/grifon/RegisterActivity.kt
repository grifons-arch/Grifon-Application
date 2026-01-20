package com.example.grifon

import android.location.Geocoder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.ui.theme.GrifonTheme
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
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
private fun RegisterScreen(
    registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(ServiceLocator.provideRegisterUseCase()),
    ),
) {
    val state by registerViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val geocoder = remember { Geocoder(context, Locale.getDefault()) }
    val countryOptions = remember { countryOptions() }
    var countryExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var addressExpanded by remember { mutableStateOf(false) }
    var addressSuggestions by remember { mutableStateOf(emptyList<String>()) }
    var citySuggestions by remember { mutableStateOf(emptyList<String>()) }
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            registerViewModel.onGoogleAccountReceived(task.getResult(ApiException::class.java))
        } catch (exception: ApiException) {
            registerViewModel.onGoogleAccountError(
                "Η σύνδεση με Google απέτυχε. Δοκιμάστε ξανά.",
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Δημιουργία λογαριασμού") },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Συμπληρώστε τα παρακάτω στοιχεία για να υποβάλετε αίτηση εγγραφής.",
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                onClick = {
                    val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, options)
                    signInLauncher.launch(client.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Συνέχεια με Google")
            }
            state.googleAccountEmail?.let { email ->
                Text(
                    text = "Συνδεθήκατε ως $email.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            state.googleSignInError?.let { error ->
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Text(
                text = "Εναλλακτικά, μπορείτε να δημιουργήσετε λογαριασμό με τα στοιχεία σας.",
                style = MaterialTheme.typography.bodySmall,
            )
            SectionTitle(title = "Προσωπικά στοιχεία")
            OutlinedTextField(
                value = state.firstName,
                onValueChange = registerViewModel::onFirstNameChange,
                label = { Text(text = "Όνομα") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.lastName,
                onValueChange = registerViewModel::onLastNameChange,
                label = { Text(text = "Επώνυμο") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.phone,
                onValueChange = registerViewModel::onPhoneChange,
                label = { Text(text = "Τηλέφωνο") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(title = "Στοιχεία εταιρείας")
            OutlinedTextField(
                value = state.companyName,
                onValueChange = registerViewModel::onCompanyNameChange,
                label = { Text(text = "Επωνυμία") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.vatNumber,
                onValueChange = registerViewModel::onVatNumberChange,
                label = { Text(text = "ΑΦΜ") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.taxOffice,
                onValueChange = registerViewModel::onTaxOfficeChange,
                label = { Text(text = "ΔΟΥ") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(title = "Διεύθυνση")
            ExposedDropdownMenuBox(
                expanded = countryExpanded,
                onExpandedChange = { countryExpanded = !countryExpanded },
            ) {
                OutlinedTextField(
                    value = state.countryName,
                    onValueChange = {},
                    label = { Text(text = "Χώρα") },
                    readOnly = true,
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(
                            expanded = countryExpanded,
                        )
                    },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = countryExpanded,
                    onDismissRequest = { countryExpanded = false },
                ) {
                    countryOptions.forEach { country ->
                        androidx.compose.material3.DropdownMenuItem(
                            text = { Text(country.displayName) },
                            onClick = {
                                registerViewModel.onCountryChange(country.displayName, country.isoCode)
                                countryExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = cityExpanded && citySuggestions.isNotEmpty(),
                onExpandedChange = { cityExpanded = !cityExpanded },
            ) {
                OutlinedTextField(
                    value = state.city,
                    onValueChange = {
                        registerViewModel.onCityChange(it)
                        cityExpanded = true
                    },
                    label = { Text(text = "Πόλη") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = state.countryName.isNotBlank(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded)
                    },
                    supportingText = {
                        if (state.countryName.isBlank()) {
                            Text(text = "Επιλέξτε πρώτα χώρα.")
                        }
                    },
                )
                ExposedDropdownMenu(
                    expanded = cityExpanded && citySuggestions.isNotEmpty(),
                    onDismissRequest = { cityExpanded = false },
                ) {
                    citySuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(text = suggestion) },
                            onClick = {
                                registerViewModel.onCityChange(suggestion)
                                cityExpanded = false
                            },
                        )
                    }
                }
            }
            ExposedDropdownMenuBox(
                expanded = addressExpanded && addressSuggestions.isNotEmpty(),
                onExpandedChange = { addressExpanded = !addressExpanded },
            ) {
                OutlinedTextField(
                    value = state.address,
                    onValueChange = {
                        registerViewModel.onAddressChange(it)
                        addressExpanded = true
                    },
                    label = { Text(text = "Οδός & αριθμός") },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                    enabled = state.countryName.isNotBlank() && state.city.isNotBlank(),
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = addressExpanded)
                    },
                    supportingText = {
                        when {
                            state.countryName.isBlank() -> Text(text = "Επιλέξτε πρώτα χώρα.")
                            state.city.isBlank() -> Text(text = "Συμπληρώστε πρώτα πόλη.")
                        }
                    },
                )
                ExposedDropdownMenu(
                    expanded = addressExpanded && addressSuggestions.isNotEmpty(),
                    onDismissRequest = { addressExpanded = false },
                ) {
                    addressSuggestions.forEach { suggestion ->
                        DropdownMenuItem(
                            text = { Text(text = suggestion) },
                            onClick = {
                                registerViewModel.onAddressChange(suggestion)
                                addressExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = state.postalCode,
                onValueChange = registerViewModel::onPostalCodeChange,
                label = { Text(text = "Τ.Κ.") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(title = "Λεπτομέρειες λογαριασμού")
            OutlinedTextField(
                value = state.email,
                onValueChange = registerViewModel::onEmailChange,
                label = { Text(text = "Email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.confirmEmail,
                onValueChange = registerViewModel::onConfirmEmailChange,
                label = { Text(text = "Επιβεβαίωση email") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = registerViewModel::onPasswordChange,
                label = { Text(text = "Κωδικός") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.confirmPassword,
                onValueChange = registerViewModel::onConfirmPasswordChange,
                label = { Text(text = "Επιβεβαίωση κωδικού") },
                modifier = Modifier.fillMaxWidth(),
            )

            OutlinedTextField(
                value = state.notes,
                onValueChange = registerViewModel::onNotesChange,
                label = { Text(text = "Σχόλια") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.acceptTerms,
                    onCheckedChange = registerViewModel::onAcceptTermsChange,
                )
                Text(
                    text = "Αποδέχομαι τους όρους χρήσης.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.subscribeNewsletter,
                    onCheckedChange = registerViewModel::onSubscribeNewsletterChange,
                )
                Text(
                    text = "Θέλω να λαμβάνω ενημερώσεις και προσφορές.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = registerViewModel::onSubmit,
                enabled = state.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Υποβολή αίτησης")
            }
            when (val status = state.status) {
                is RegisterStatus.Loading -> {
                    Text(
                        text = "Η αίτηση αποστέλλεται...",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                is RegisterStatus.Success -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                is RegisterStatus.Error -> {
                    Text(
                        text = status.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
                RegisterStatus.Idle -> Unit
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Θα επικοινωνήσουμε μαζί σας μόλις εγκριθεί η αίτηση.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    LaunchedEffect(state.address, state.city, state.countryName) {
        val query = state.address.trim()
        if (query.length < 3 || state.countryName.isBlank() || state.city.isBlank()) {
            addressSuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        addressSuggestions = lookupAddressSuggestions(
            geocoder = geocoder,
            query = query,
            city = state.city,
            country = state.countryName,
        )
    }

    LaunchedEffect(state.city, state.countryName) {
        val query = state.city.trim()
        if (query.length < 2 || state.countryName.isBlank()) {
            citySuggestions = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        citySuggestions = lookupCitySuggestions(
            geocoder = geocoder,
            query = query,
            country = state.countryName,
        )
    }

    LaunchedEffect(state.countryName) {
        cityExpanded = false
        addressExpanded = false
        citySuggestions = emptyList()
        addressSuggestions = emptyList()
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    )
}

private data class CountryOption(
    val displayName: String,
    val isoCode: String,
)

private fun countryOptions(): List<CountryOption> {
    val locale = Locale("el")
    return Locale.getISOCountries()
        .map { iso ->
            CountryOption(
                displayName = Locale("", iso).getDisplayCountry(locale),
                isoCode = iso,
            )
        }
        .filter { it.displayName.isNotBlank() }
        .distinctBy { it.isoCode }
        .sortedBy { it.displayName }
}

private suspend fun lookupAddressSuggestions(
    geocoder: Geocoder,
    query: String,
    city: String,
    country: String,
): List<String> = withContext(Dispatchers.IO) {
    runCatching {
        geocoder.getFromLocationName("$query, $city, $country", 5)
            ?.mapNotNull { address -> address.getAddressLine(0) }
            ?.distinct()
            .orEmpty()
    }.getOrDefault(emptyList())
}

private suspend fun lookupCitySuggestions(
    geocoder: Geocoder,
    query: String,
    country: String,
): List<String> = withContext(Dispatchers.IO) {
    runCatching {
        geocoder.getFromLocationName("$query, $country", 5)
            ?.mapNotNull { address -> address.locality ?: address.subAdminArea }
            ?.distinct()
            .orEmpty()
    }.getOrDefault(emptyList())
}
