package com.example.grifon.core

import android.content.Context

enum class LoginText {
    Login,
    Email,
    Password,
    WelcomeBack,
    LoggedInSuccess,
    AccountSettings,
    LoginSubtitle,
    HidePassword,
    ShowPassword,
    LoginNoAccount,
    RegisterHere,
    LoginValidationError,
    LoginInvalidCredentials,
    LoginAccountInactive,
    LoginNetworkError,
    LoginFailed,
}

fun loginText(language: String, text: LoginText): String = when (text) {
    LoginText.Login -> when (language) {
        "sv" -> "Logga in"
        "en" -> "Login"
        else -> "Σύνδεση"
    }
    LoginText.Email -> when (language) {
        "sv" -> "E-post"
        "en" -> "Email"
        else -> "Email"
    }
    LoginText.Password -> when (language) {
        "sv" -> "Lösenord"
        "en" -> "Password"
        else -> "Κωδικός"
    }
    LoginText.WelcomeBack -> when (language) {
        "sv" -> "Välkommen!"
        "en" -> "Welcome!"
        else -> "Καλώς ήρθες!"
    }
    LoginText.LoggedInSuccess -> when (language) {
        "sv" -> "Du har loggat in i Grifon."
        "en" -> "You have successfully signed in to Grifon."
        else -> "Έχεις συνδεθεί επιτυχώς στο Grifon."
    }
    LoginText.AccountSettings -> when (language) {
        "sv" -> "Kontoinställningar"
        "en" -> "Account Settings"
        else -> "Ρυθμίσεις Λογαριασμού"
    }
    LoginText.LoginSubtitle -> when (language) {
        "sv" -> "Logga in för att fortsätta handla"
        "en" -> "Sign in to continue your shopping"
        else -> "Συνδεθείτε για να συνεχίσετε τις αγορές σας"
    }
    LoginText.HidePassword -> when (language) {
        "sv" -> "Dölj lösenord"
        "en" -> "Hide password"
        else -> "Απόκρυψη κωδικού"
    }
    LoginText.ShowPassword -> when (language) {
        "sv" -> "Visa lösenord"
        "en" -> "Show password"
        else -> "Εμφάνιση κωδικού"
    }
    LoginText.LoginNoAccount -> when (language) {
        "sv" -> "Har du inget konto? "
        "en" -> "Don't have an account? "
        else -> "Δεν έχετε λογαριασμό; "
    }
    LoginText.RegisterHere -> when (language) {
        "sv" -> "Registrera dig här"
        "en" -> "Register here"
        else -> "Εγγραφείτε εδώ"
    }
    LoginText.LoginValidationError -> when (language) {
        "sv" -> "Fyll i giltiga inloggningsuppgifter (lösenordet måste vara minst 6 tecken)"
        "en" -> "Please enter valid credentials (password must be at least 6 characters)"
        else -> "Παρακαλώ συμπληρώστε σωστά τα στοιχεία σας (κωδικός τουλάχιστον 6 χαρακτήρες)"
    }
    LoginText.LoginInvalidCredentials -> when (language) {
        "sv" -> "Fel e-post eller lösenord"
        "en" -> "Incorrect email or password"
        else -> "Λάθος email ή κωδικός"
    }
    LoginText.LoginAccountInactive -> when (language) {
        "sv" -> "Ditt konto är inaktivt"
        "en" -> "Your account is inactive"
        else -> "Ο λογαριασμός σας είναι ανενεργός"
    }
    LoginText.LoginNetworkError -> when (language) {
        "sv" -> "Det gick inte att ansluta till servern. Försök igen."
        "en" -> "Unable to connect to the server. Please try again."
        else -> "Δεν ήταν δυνατή η σύνδεση με τον server. Δοκιμάστε ξανά."
    }
    LoginText.LoginFailed -> when (language) {
        "sv" -> "Inloggningen misslyckades"
        "en" -> "Login failed"
        else -> "Αποτυχία σύνδεσης"
    }
}

fun loginText(context: Context, text: LoginText): String =
    loginText(AppLanguage.getStoredLanguage(context), text)
