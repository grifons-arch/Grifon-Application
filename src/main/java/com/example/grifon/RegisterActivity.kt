package com.example.grifon

import android.app.Activity
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }

    // Launcher for Google Places Autocomplete
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val place = Autocomplete.getPlaceFromIntent(result.data!!)
            registerViewModel.onPlaceSelected(place)
        } else if (result.resultCode == AutocompleteActivityMode.FULLSCREEN.toInt()) {
            // Handle error
            val status = Autocomplete.getStatusFromIntent(result.data!!)
            Toast.makeText(context, "Error: ${status.statusMessage}", Toast.LENGTH_SHORT).show()
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        RegistrationTextField(
            value = state.iban,
            onValueChange = registerViewModel::onIbanChange,
            placeholder = "IBAN",
        )

        SectionTitle(title = "Εταιρεία")
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

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SectionTitle(title = "Διεύθυνση +")
            TextButton(
                onClick = {
                    val fields = listOf(
                        Place.Field.ID,
                        Place.Field.NAME,
                        Place.Field.ADDRESS_COMPONENTS,
                        Place.Field.ADDRESS
                    )
                    val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                        .setCountry("GR") // Προαιρετικά περιορισμός στην Ελλάδα
                        .build(context)
                    launcher.launch(intent)
                }
            ) {
                Icon(Icons.Default.Map, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Αναζήτηση στο Χάρτη")
            }
        }

        RegistrationTextField(
            value = state.country,
            onValueChange = registerViewModel::onCountryChange,
            placeholder = "Χώρα (ISO, π.χ. GR) *",
        )
        RegistrationTextField(
            value = state.city,
            onValueChange = registerViewModel::onCityChange,
            placeholder = "Πόλη *",
        )
        RegistrationTextField(
            value = state.street,
            onValueChange = registerViewModel::onStreetChange,
            placeholder = "Οδός και Αριθμός *",
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        RegistrationTextField(
            value = state.emailConfirmation,
            onValueChange = registerViewModel::onEmailConfirmationChange,
            placeholder = "Επιβεβαίωση Email *",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        RegistrationTextField(
            value = state.password,
            onValueChange = registerViewModel::onPasswordChange,
            placeholder = "Κωδικός *",
            isPassword = true,
            isPasswordVisible = isPasswordVisible,
            onPasswordVisibilityChange = { isPasswordVisible = !isPasswordVisible },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
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
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )

        ConsentOption(
            checked = state.customerDataPrivacyAccepted,
            onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
            title = "Προστασία δεδομένων πελάτη",
            description = "Τα προσωπικά δεδομένα που παρέχετε χρησιμοποιούνται για την απάντηση " +
                "σε αιτήματα, την επεξεργασία παραγγελιών ή την παροχή πρόσβασης σε συγκεκριμένες " +
                "πληροφορίες. Έχετε το δικαίωμα να αλλάξετε και να διαγράψετε όλα τα προσωπικά " +
                "σας δεδομένα που βρίσκονται στη σελίδα \"Ο λογαριασμός μου\".",
            required = true,
        )
        ConsentOption(
            checked = state.newsletterOptIn,
            onCheckedChange = registerViewModel::onNewsletterOptInChange,
            title = "Εγγραφείτε στο ενημερωτικό δελτίο μας",
            description = "Μπορείτε να διακόψετε τη συνδρομή οποιαδήποτε στιγμή. " +
                "Για αυτόν τον σκοπό, παρακαλούμε βρείτε τα στοιχεία επικοινωνίας μας " +
                "στην νομική ειδοποίηση.",
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
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = "Αποθήκευση")
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
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    TextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = {
            Text(
                text = placeholder,
                style = MaterialTheme.typography.bodyMedium,
            )
        },
        modifier = Modifier.fillMaxWidth(),
        textStyle = MaterialTheme.typography.bodyMedium,
        shape = RoundedCornerShape(10.dp),
        visualTransformation = if (isPassword && !isPasswordVisible) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        trailingIcon = if (isPassword && onPasswordVisibilityChange != null) {
            {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (isPasswordVisible) {
                            Icons.Filled.VisibilityOff
                        } else {
                            Icons.Filled.Visibility
                        },
                        contentDescription = null,
                    )
                }
            }
        } else {
            null
        },
        keyboardOptions = keyboardOptions,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
            disabledIndicatorColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
            unfocusedPlaceholderColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

@Composable
private fun SocialTitleOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
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
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
        Column(
            modifier = Modifier.padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = if (required) "$title *" else title,
                style = MaterialTheme.typography.bodyMedium,
            )
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
