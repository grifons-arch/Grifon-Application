package com.example.grifon.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grifon.R
import com.example.grifon.core.AppLanguage
import com.example.grifon.core.LoginText
import com.example.grifon.viewmodel.AccountViewModel
import com.example.grifon.core.UiState
import com.example.grifon.core.loginText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    viewModel: AccountViewModel,
    onSettings: () -> Unit,
    onRegister: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val email by viewModel.email.collectAsState()
    val password by viewModel.password.collectAsState()
    val loginError by viewModel.loginError.collectAsState()

    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val account = state.data
            
            if (account.loggedIn) {
                // ΟΘΟΝΗ ΟΤΑΝ Ο ΧΡΗΣΤΗΣ ΕΙΝΑΙ ΣΥΝΔΕΔΕΜΕΝΟΣ
                LoggedInContent(
                    onSettings = onSettings,
                    onLogout = viewModel::logout,
                )
            } else {
                // ΟΘΟΝΗ LOGIN
                LoginContent(
                    email = email,
                    password = password,
                    error = loginError,
                    onEmailChange = viewModel::onEmailChange,
                    onPasswordChange = viewModel::onPasswordChange,
                    onLoginClick = viewModel::login,
                    onRegisterClick = onRegister,
                )
            }
        }
    }
}

@Composable
fun LoggedInContent(
    onSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val language = AppLanguage.currentLanguage()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(loginText(language, LoginText.WelcomeBack), style = MaterialTheme.typography.headlineSmall)
                Text(loginText(language, LoginText.LoggedInSuccess))
            }
        }

        Button(onClick = onSettings, modifier = Modifier.fillMaxWidth()) {
            Text(loginText(language, LoginText.AccountSettings))
        }

        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth()) {
            Text(stringResource(R.string.logout))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginContent(
    email: String,
    password: String,
    error: String?,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit
) {
    val scrollState = rememberScrollState()
    var isPasswordVisible by remember { mutableStateOf(false) }
    val language = AppLanguage.currentLanguage()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = loginText(language, LoginText.Login),
            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = loginText(language, LoginText.LoginSubtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text(loginText(language, LoginText.Email)) },
            placeholder = { Text("example@mail.com") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Mail, contentDescription = null) },
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text(loginText(language, LoginText.Password)) },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (isPasswordVisible) {
                            loginText(language, LoginText.HidePassword)
                        } else {
                            loginText(language, LoginText.ShowPassword)
                        }
                    )
                }
            },
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        if (error != null) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp, start = 4.dp).fillMaxWidth(),
                textAlign = TextAlign.Start
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(loginText(language, LoginText.Login), fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(32.dp))

        val registerText = buildAnnotatedString {
            append(loginText(language, LoginText.LoginNoAccount))
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                append(loginText(language, LoginText.RegisterHere))
            }
        }

        ClickableText(
            text = registerText,
            onClick = { onRegisterClick() },
            style = MaterialTheme.typography.bodyMedium.copy(textAlign = TextAlign.Center)
        )
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
