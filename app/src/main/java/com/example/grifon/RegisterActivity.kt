package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
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
private fun RegisterScreen(registerViewModel: RegisterViewModel = viewModel()) {
    val state by registerViewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

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
                value = state.email,
                onValueChange = registerViewModel::onEmailChange,
                label = { Text(text = "Email") },
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
            OutlinedTextField(
                value = state.address,
                onValueChange = registerViewModel::onAddressChange,
                label = { Text(text = "Οδός & αριθμός") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.city,
                onValueChange = registerViewModel::onCityChange,
                label = { Text(text = "Πόλη") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = state.postalCode,
                onValueChange = registerViewModel::onPostalCodeChange,
                label = { Text(text = "Τ.Κ.") },
                modifier = Modifier.fillMaxWidth(),
            )

            SectionTitle(title = "Λεπτομέρειες λογαριασμού")
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

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Θα επικοινωνήσουμε μαζί σας μόλις εγκριθεί η αίτηση.",
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
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
