package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isPasswordConfirmationVisible by remember { mutableStateOf(false) }

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

        RegistrationTextField(value = state.firstName, onValueChange = registerViewModel::onFirstNameChange, placeholder = "Όνομα *")
        RegistrationTextField(value = state.lastName, onValueChange = registerViewModel::onLastNameChange, placeholder = "Επώνυμο *")
        RegistrationTextField(value = state.phone, onValueChange = registerViewModel::onPhoneChange, placeholder = "Τηλέφωνο")
        RegistrationTextField(value = state.iban, onValueChange = registerViewModel::onIbanChange, placeholder = "IBAN")

        SectionTitle(title = "Εταιρεία")
        RegistrationTextField(value = state.companyName, onValueChange = registerViewModel::onCompanyNameChange, placeholder = "Εταιρεία")
        RegistrationTextField(value = state.vatNumber, onValueChange = registerViewModel::onVatNumberChange, placeholder = "Α.Φ.Μ")

        SectionTitle(title = "Διεύθυνση +")
        RegistrationTextField(value = state.country, onValueChange = registerViewModel::onCountryChange, placeholder = "Χώρα (ISO, π.χ. GR) *")
        RegistrationTextField(value = state.city, onValueChange = registerViewModel::onCityChange, placeholder = "Πόλη *")
        RegistrationTextField(value = state.street, onValueChange = registerViewModel::onStreetChange, placeholder = "Οδός και Αριθμός *")
        RegistrationTextField(value = state.postalCode, onValueChange = registerViewModel::onPostalCodeChange, placeholder = "Τ.Κ *")

        SectionTitle(title = "Άλλα στοιχεία")
        RegistrationTextField(value = state.email, onValueChange = registerViewModel::onEmailChange, placeholder = "Email *")
        RegistrationTextField(value = state.emailConfirmation, onValueChange = registerViewModel::onEmailConfirmationChange, placeholder = "Επιβεβαίωση Email *")
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
            onPasswordVisibilityChange = { isPasswordConfirmationVisible = !isPasswordConfirmationVisible },
        )

        ConsentOption(
            checked = state.customerDataPrivacyAccepted,
            onCheckedChange = registerViewModel::onCustomerDataPrivacyAcceptedChange,
            title = "Προστασία δεδομένων πελάτη",
            description = "Τα προσωπικά δεδομένα που παρέχετε χρησιμοποιούνται για την απάντηση σε αιτήματα...",
            required = true,
        )
        ConsentOption(
            checked = state.newsletterOptIn,
            onCheckedChange = registerViewModel::onNewsletterOptInChange,
            title = "Εγγραφείτε στο ενημερωτικό δελτίο μας",
            description = "Μπορείτε να διακόψετε τη συνδρομή οποιαδήποτε στιγμή.",
        )
        ConsentOption(
            checked = state.partnerOffersOptIn, // Διόρθωση: Σύνδεση με το σωστό πεδίο
            onCheckedChange = registerViewModel::onPartnerOffersOptInChange,
            title = "Λήψη προσφορών από τους συνεργάτες μας",
            description = "Επιλέξτε αν επιθυμείτε να λαμβάνετε ειδικές προσφορές.",
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
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp),
            shape = RoundedCornerShape(12.dp),
        ) {
            Text(text = "Αποθήκευση")
        }

        when (val status = state.status) {
            is RegisterStatus.Loading -> Text("Η αίτηση αποστέλλεται...", style = MaterialTheme.typography.bodySmall)
            is RegisterStatus.Success -> Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            is RegisterStatus.Error -> Text(status.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            RegisterStatus.Idle -> Unit
        }
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
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
        textStyle = MaterialTheme.typography.bodyMedium,
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
private fun ConsentOption(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    title: String,
    description: String? = null,
    required: Boolean = false,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Column(modifier = Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = if (required) "$title *" else title, style = MaterialTheme.typography.bodyMedium)
            if (!description.isNullOrBlank()) {
                Text(text = description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
