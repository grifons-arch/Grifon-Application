package com.example.grifon.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.User
import com.example.grifon.viewmodel.AccountViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel, 
    onSettings: () -> Unit,
    onRegister: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var showProfileDetails by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val account = state.data
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Header Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = if (account.loggedIn) "Καλώς ήρθες, ${account.userName}!" else "Ο Λογαριασμός μου", 
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                if (!account.loggedIn) {
                    // Login Form
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Σύνδεση", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            
                            if (account.loginError != null) {
                                Text(text = account.loginError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }

                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("Email") },
                                modifier = Modifier.fillMaxWidth(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                label = { Text("Κωδικός") },
                                modifier = Modifier.fillMaxWidth(),
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                singleLine = true,
                                trailingIcon = {
                                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                        Icon(imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff, contentDescription = null)
                                    }
                                }
                            )

                            Button(
                                onClick = { viewModel.login(email, password) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !account.isLoading
                            ) {
                                if (account.isLoading) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White, strokeWidth = 2.dp)
                                else Text("Login")
                            }

                            val registerText = buildAnnotatedString {
                                append("Αν δεν έχεις λογαριασμό δημιούργησε ")
                                pushStringAnnotation(tag = "register", annotation = "register")
                                withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary, textDecoration = TextDecoration.Underline)) {
                                    append("εδώ")
                                }
                                pop()
                            }
                            ClickableText(
                                text = registerText,
                                style = MaterialTheme.typography.bodyMedium,
                                onClick = { offset ->
                                    registerText.getStringAnnotations("register", offset, offset)
                                        .firstOrNull()?.let { onRegister() }
                                }
                            )
                        }
                    }
                } else {
                    // Menu Items
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column {
                            AccountMenuItem(title = "Οι παραγγελίες μου", icon = Icons.AutoMirrored.Filled.ListAlt) { }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            AccountMenuItem(title = "Οι διευθύνσεις μου", icon = Icons.Default.LocationOn) { }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            AccountMenuItem(title = "Wishlist (Αγαπημένα)", icon = Icons.Default.Favorite) { }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp)
                            AccountMenuItem(title = "Στοιχεία Λογαριασμού", icon = Icons.Default.AccountCircle) { 
                                showProfileDetails = !showProfileDetails
                            }
                        }
                    }

                    // Profile Details Section
                    if (showProfileDetails && account.userDetails != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Προσωπικά Στοιχεία", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    IconButton(onClick = { isEditing = true }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    }
                                }
                                
                                ProfileDetailRow("Όνομα", account.userDetails.firstName)
                                ProfileDetailRow("Επώνυμο", account.userDetails.lastName)
                                ProfileDetailRow("Email", account.userDetails.email)
                                ProfileDetailRow("Τηλέφωνο", account.userDetails.phone ?: "-")
                                ProfileDetailRow("Εταιρεία", account.userDetails.company ?: "-")
                                ProfileDetailRow("ΑΦΜ", account.userDetails.vatNumber ?: "-")
                                ProfileDetailRow("Newsletter", if (account.userDetails.newsletter) "Ενεργό" else "Ανενεργό")
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                OutlinedButton(
                                    onClick = { isEditing = true },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Επεξεργασία Στοιχείων")
                                }
                            }
                        }
                    }

                    if (isEditing && account.userDetails != null) {
                        EditProfileDialog(
                            user = account.userDetails,
                            onDismiss = { isEditing = false },
                            onSave = { updatedUser ->
                                viewModel.updateProfile(updatedUser)
                                isEditing = false
                            }
                        )
                    }

                    Button(
                        onClick = { viewModel.logout() }, 
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Αποσύνδεση")
                    }
                }

                OutlinedButton(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Settings")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileDialog(user: User, onDismiss: () -> Unit, onSave: (User) -> Unit) {
    var firstName by remember { mutableStateOf(user.firstName) }
    var lastName by remember { mutableStateOf(user.lastName) }
    var company by remember { mutableStateOf(user.company ?: "") }
    var vat by remember { mutableStateOf(user.vatNumber ?: "") }
    var newsletter by remember { mutableStateOf(user.newsletter) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Επεξεργασία Στοιχείων") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = firstName, onValueChange = { firstName = it }, label = { Text("Όνομα") })
                OutlinedTextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Επώνυμο") })
                OutlinedTextField(value = company, onValueChange = { company = it }, label = { Text("Εταιρεία") })
                OutlinedTextField(value = vat, onValueChange = { vat = it }, label = { Text("ΑΦΜ") })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = newsletter, onCheckedChange = { newsletter = it })
                    Text("Εγγραφή στο Newsletter")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(user.copy(firstName = firstName, lastName = lastName, company = company, vatNumber = vat, newsletter = newsletter))
            }) { Text("Αποθήκευση") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Ακύρωση") }
        }
    )
}

@Composable
fun ProfileDetailRow(label: String, value: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
        Text(text = value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp), thickness = 0.5.dp, color = Color.LightGray.copy(alpha = 0.5f))
    }
}

@Composable
fun AccountMenuItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray)
    }
}
