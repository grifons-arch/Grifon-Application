package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme

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
    
    // Δεδομένα Χωρών και Πόλεων
    val countries = listOf("Ελλάδα (GR)", "Σουηδία (SE)", "Κύπρος (CY)")
    val citiesMap = mapOf(
        "Ελλάδα (GR)" to listOf("Αθήνα", "Θεσσαλονίκη", "Ηράκλειο", "Χανιά", "Πάτρα", "Λάρισα"),
        "Σουηδία (SE)" to listOf("Στοκχόλμη", "Γκέτεμποργκ", "Μάλμε", "Ουψάλα"),
        "Κύπρος (CY)" to listOf("Λευκωσία", "Λεμεσός", "Λάρνακα", "Πάφος")
    )

    var countryExpanded by remember { mutableStateOf(false) }
    var cityExpanded by remember { mutableStateOf(false) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(text = "Εγγραφή", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold))

        // Προσωπικά Στοιχεία
        RegistrationTextField(value = state.firstName, onValueChange = registerViewModel::onFirstNameChange, placeholder = "Όνομα *")
        RegistrationTextField(value = state.lastName, onValueChange = registerViewModel::onLastNameChange, placeholder = "Επώνυμο *")
        RegistrationTextField(value = state.phone, onValueChange = registerViewModel::onPhoneChange, placeholder = "Τηλέφωνο")

        SectionTitle(title = "Διεύθυνση")
        
        // Επιλογή Χώρας (Dropdown)
        ExposedDropdownMenuBox(
            expanded = countryExpanded,
            onExpandedChange = { countryExpanded = !countryExpanded }
        ) {
            OutlinedTextField(
                value = state.country,
                onValueChange = {},
                readOnly = true,
                label = { Text("Χώρα *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = countryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
            ExposedDropdownMenu(
                expanded = countryExpanded,
                onDismissRequest = { countryExpanded = false }
            ) {
                countries.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            registerViewModel.onCountryChange(selection)
                            registerViewModel.onCityChange("") // Reset city on country change
                            countryExpanded = false
                        }
                    )
                }
            }
        }

        // Επιλογή Πόλης (Dropdown βασισμένο στη χώρα)
        val availableCities = citiesMap[state.country] ?: emptyList()
        ExposedDropdownMenuBox(
            expanded = cityExpanded,
            onExpandedChange = { if (availableCities.isNotEmpty()) cityExpanded = !cityExpanded }
        ) {
            OutlinedTextField(
                value = state.city,
                onValueChange = {},
                readOnly = true,
                label = { Text(if (state.country.isEmpty()) "Επιλέξτε πρώτα χώρα" else "Πόλη *") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cityExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
                enabled = availableCities.isNotEmpty(),
                colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
            )
            ExposedDropdownMenu(
                expanded = cityExpanded,
                onDismissRequest = { cityExpanded = false }
            ) {
                availableCities.forEach { selection ->
                    DropdownMenuItem(
                        text = { Text(selection) },
                        onClick = {
                            registerViewModel.onCityChange(selection)
                            cityExpanded = false
                        }
                    )
                }
            }
        }

        RegistrationTextField(value = state.street, onValueChange = registerViewModel::onStreetChange, placeholder = "Οδός και Αριθμός *")
        RegistrationTextField(value = state.postalCode, onValueChange = registerViewModel::onPostalCodeChange, placeholder = "Τ.Κ *")

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

        ConsentOption(
            checked = state.customerDataPrivacyAccepted,
            onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
            title = "Προστασία δεδομένων πελάτη *",
            required = true,
        )
        ConsentOption(
            checked = state.termsAndPrivacyAccepted,
            onCheckedChange = registerViewModel::onTermsAndPrivacyAcceptedChange,
            title = "Αποδέχομαι τους όρους χρήσης *",
            required = true,
        )

        Button(
            onClick = registerViewModel::onSubmit,
            enabled = state.isSubmitEnabled,
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            if (state.status is RegisterStatus.Loading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
            } else {
                Text(text = "Ολοκλήρωση Εγγραφής")
            }
        }

        // Status Messages
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
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(placeholder) },
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
        colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant, unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant)
    )
}

@Composable
private fun ConsentOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    required: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(text = title, style = MaterialTheme.typography.bodyMedium)
    }
}
