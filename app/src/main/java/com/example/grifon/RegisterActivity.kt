package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.grifon.core.ServiceLocator
import com.example.grifon.presentation.register.RegisterStatus
import com.example.grifon.presentation.register.RegisterViewModel
import com.example.grifon.presentation.register.RegisterViewModelFactory
import com.example.grifon.ui.theme.GrifonTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

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
                title = { Text(text = "Εγγραφή") },
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
                text = buildAnnotatedString {
                    append("Έχετε ήδη λογαριασμό; ")
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append("Συνδεθείτε αντί αυτού!")
                    }
                },
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle(title = "Προσφώνηση")
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SocialTitleOption(
                    label = "Κος.",
                    selected = state.socialTitle == "mr",
                    onSelect = { registerViewModel.onSocialTitleChange("mr") },
                )
                Spacer(modifier = Modifier.width(12.dp))
                SocialTitleOption(
                    label = "Κα.",
                    selected = state.socialTitle == "mrs",
                    onSelect = { registerViewModel.onSocialTitleChange("mrs") },
                )
            }

            OutlinedTextField(
                value = state.firstName,
                onValueChange = registerViewModel::onFirstNameChange,
                label = { Text(text = "Όνομα *") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(text = "Επιτρέπονται μόνο γράμματα και τελεία (.), ακολουθούμενη από κενό.")
                },
            )
            OutlinedTextField(
                value = state.lastName,
                onValueChange = registerViewModel::onLastNameChange,
                label = { Text(text = "Επώνυμο *") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = {
                    Text(text = "Επιτρέπονται μόνο γράμματα και τελεία (.), ακολουθούμενη από κενό.")
                },
            )
            OutlinedTextField(
                value = state.companyName,
                onValueChange = registerViewModel::onCompanyNameChange,
                label = { Text(text = "Εταιρεία") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(text = "Προαιρετικό") },
            )
            OutlinedTextField(
                value = state.vatNumber,
                onValueChange = registerViewModel::onVatNumberChange,
                label = { Text(text = "Ενδοκοινοτικό ΑΦΜ") },
                modifier = Modifier.fillMaxWidth(),
                supportingText = { Text(text = "Προαιρετικό") },
            )
            OutlinedTextField(
                value = state.email,
                onValueChange = registerViewModel::onEmailChange,
                label = { Text(text = "Email *") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.password,
                onValueChange = registerViewModel::onPasswordChange,
                label = { Text(text = "Κωδικός *") },
                modifier = Modifier.fillMaxWidth(),
                visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    TextButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                        Text(text = if (isPasswordVisible) "ΑΠΟΚΡΥΨΗ" else "ΕΜΦΑΝΙΣΗ")
                    }
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "*",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 4.dp),
                )
                Checkbox(
                    checked = state.acceptPrivacy,
                    onCheckedChange = registerViewModel::onAcceptPrivacyChange,
                )
            }
            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "Απόρρητο δεδομένων πελατών",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    text = "Τα προσωπικά δεδομένα που παρέχετε χρησιμοποιούνται για να απαντήσουμε " +
                        "σε αιτήματα, να επεξεργαστούμε παραγγελίες ή να επιτρέψουμε πρόσβαση σε " +
                        "συγκεκριμένες πληροφορίες. Έχετε δικαίωμα να αλλάξετε και να διαγράψετε " +
                        "κάθε προσωπική πληροφορία που βρίσκεται στη σελίδα \"Ο λογαριασμός μου\".",
                    style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
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
                Column {
                    Text(
                        text = "Εγγραφείτε στο newsletter μας",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = "Μπορείτε να διακόψετε τη συνδρομή οποιαδήποτε στιγμή. " +
                            "Για αυτόν τον σκοπό, βρείτε τα στοιχεία επικοινωνίας μας στο νομικό σημείωμα.",
                        style = MaterialTheme.typography.bodySmall.copy(fontStyle = FontStyle.Italic),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = state.acceptTerms,
                    onCheckedChange = registerViewModel::onAcceptTermsChange,
                )
                Text(
                    text = "Αποδέχομαι τους όρους και την πολιτική απορρήτου *",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            Button(
                onClick = registerViewModel::onSubmit,
                enabled = state.isSubmitEnabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "ΑΠΟΘΗΚΕΥΣΗ")
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
                text = "Ή συνδεθείτε με",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.fillMaxWidth(),
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
                Text(text = "Google")
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
