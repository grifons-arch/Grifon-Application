package com.example.grifon

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient

class RegisterActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Αρχικοποίηση Google Places SDK
        // ΠΡΟΣΟΧΗ: Αντικαταστήστε το "YOUR_API_KEY" με το πραγματικό σας κλειδί Google Maps API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, "YOUR_API_KEY")
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
    val context = LocalContext.current
    val placesClient = remember { Places.createClient(context) }
    
    var isPasswordVisible by remember { mutableStateOf(false) }
    var addressSearchQuery by remember { mutableStateOf("") }
    var predictions by remember { mutableStateOf<List<AutocompletePrediction>>(emptyList()) }

    // Λογική αναζήτησης Google Places
    LaunchedEffect(addressSearchQuery) {
        if (addressSearchQuery.length > 2) {
            val request = FindAutocompletePredictionsRequest.builder()
                .setQuery(addressSearchQuery)
                .build()
            placesClient.findAutocompletePredictions(request)
                .addOnSuccessListener { response ->
                    predictions = response.autocompletePredictions
                }
                .addOnFailureListener { predictions = emptyList() }
        } else {
            predictions = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Εγγραφή", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        RegistrationTextField(value = state.firstName, onValueChange = registerViewModel::onFirstNameChange, placeholder = "Όνομα *")
        RegistrationTextField(value = state.lastName, onValueChange = registerViewModel::onLastNameChange, placeholder = "Επώνυμο *")
        RegistrationTextField(value = state.phone, onValueChange = registerViewModel::onPhoneChange, placeholder = "Τηλέφωνο")

        SectionTitle(title = "Διεύθυνση (Google Maps)")
        
        // Πεδίο Αναζήτησης Διεύθυνσης Google
        OutlinedTextField(
            value = addressSearchQuery,
            onValueChange = { addressSearchQuery = it },
            label = { Text("Αναζητήστε Χώρα, Πόλη ή Οδό *") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            shape = RoundedCornerShape(10.dp)
        )

        // Λίστα Αποτελεσμάτων Google
        if (predictions.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column {
                    predictions.forEach { prediction ->
                        ListItem(
                            headlineContent = { Text(prediction.getPrimaryText(null).toString()) },
                            supportingContent = { Text(prediction.getSecondaryText(null).toString()) },
                            leadingContent = { Icon(Icons.Default.LocationOn, contentDescription = null) },
                            modifier = Modifier.clickable {
                                val placeId = prediction.placeId
                                val fields = listOf(Place.Field.ADDRESS_COMPONENTS)
                                val request = FetchPlaceRequest.newInstance(placeId, fields)
                                
                                placesClient.fetchPlace(request).addOnSuccessListener { response ->
                                    val place = response.place
                                    var streetName = ""
                                    var streetNumber = ""
                                    
                                    place.addressComponents?.asList()?.forEach { component ->
                                        when {
                                            component.types.contains("country") -> registerViewModel.onCountryChange(component.name)
                                            component.types.contains("locality") -> registerViewModel.onCityChange(component.name)
                                            component.types.contains("route") -> streetName = component.name
                                            component.types.contains("street_number") -> streetNumber = component.name
                                        }
                                    }
                                    registerViewModel.onStreetChange("$streetName $streetNumber".trim())
                                    addressSearchQuery = "" // Clear search after selection
                                    predictions = emptyList()
                                }
                            }
                        )
                        HorizontalDivider()
                    }
                }
            }
        }

        // Αυτόματα συμπληρωμένα πεδία από Google
        RegistrationTextField(value = state.country, onValueChange = registerViewModel::onCountryChange, placeholder = "Χώρα", readOnly = true)
        RegistrationTextField(value = state.city, onValueChange = registerViewModel::onCityChange, placeholder = "Πόλη", readOnly = true)
        RegistrationTextField(value = state.street, onValueChange = registerViewModel::onStreetChange, placeholder = "Οδός και Αριθμός", readOnly = true)
        
        // Χειροκίνητος Τ.Κ.
        RegistrationTextField(value = state.postalCode, onValueChange = registerViewModel::onPostalCodeChange, placeholder = "Τ.Κ (Πληκτρολογήστε χειροκίνητα) *")

        SectionTitle(title = "Στοιχεία Σύνδεσης")
        RegistrationTextField(value = state.email, onValueChange = registerViewModel::onEmailChange, placeholder = "Email *")
        RegistrationTextField(
            value = state.password,
            onValueChange = registerViewModel::onPasswordChange,
            placeholder = "Κωδικός *",
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
        )

        ConsentOption(checked = state.customerDataPrivacyAccepted, onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange, title = "Προστασία δεδομένων πελάτη *", required = true)
        ConsentOption(checked = state.termsAndPrivacyAccepted, onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange, title = "Αποδέχομαι τους όρους χρήσης *", required = true)

        Button(
            onClick = registerViewModel::onSubmit,
            enabled = state.isSubmitEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.status is RegisterStatus.Loading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            else Text(text = "Ολοκλήρωση Εγγραφής")
        }

        if (state.status is RegisterStatus.Error) {
            Text((state.status as RegisterStatus.Error).message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
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
    isPassword: Boolean = false,
    isPasswordVisible: Boolean = false,
    onPasswordVisibilityChange: (() -> Unit)? = null,
    readOnly: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(placeholder) },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        readOnly = readOnly,
        visualTransformation = if (isPassword && !isPasswordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = if (isPassword && onPasswordVisibilityChange != null) {
            {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(imageVector = if (isPasswordVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, contentDescription = null)
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = if (readOnly) Color.LightGray.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = if (readOnly) Color.LightGray.copy(alpha = 0.1f) else MaterialTheme.colorScheme.surfaceVariant
        )
    )
}

@Composable
private fun ConsentOption(checked: Boolean, onCheckedChange: (Boolean) -> Unit, title: String, required: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
