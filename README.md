# Grifon Eshop

Σύντομη περίληψη των μέχρι τώρα εργασιών για το project Grifon eShop.

## Τι έχουμε υλοποιήσει μέχρι τώρα

- **Android εφαρμογή (module `app`)**: βασική δομή του client για περιήγηση στο e-shop και σύνδεση με το gateway.
- **Backend proxy (`backend/`)**: REST API που μεσολαβεί στο PrestaShop Webservice, με endpoints για κατηγορίες, προϊόντα, αναζήτηση και stock.
- **Gateway υπηρεσία (`grifon-gateway/`)**: ασφαλές proxy για PrestaShop multishop με φιλτραρισμένα endpoints, έλεγχο τιμών ανά πελάτη και μηχανισμούς ασφάλειας.
- **Ροή εγγραφής**: διαθέσιμο παράδειγμα `curl` για register και οδηγίες σύνδεσης του Android client με το gateway.

## Δομή αποθετηρίου

- `app/` — Android client (Gradle project).
- `backend/` — Node.js REST proxy για PrestaShop.
- `grifon-gateway/` — Node.js gateway με multishop υποστήριξη.

## Τεκμηρίωση

Για αναλυτικές οδηγίες εκκίνησης και endpoints, δείτε τα README στα επιμέρους directories:
- `backend/README.md`
- `grifon-gateway/README.md`

## How to test

### Gateway (register)

```bash
curl -X POST http://localhost:3000/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "password": "secret123",
    "firstName": "Γιώργος",
    "lastName": "Παπαδόπουλος",
    "countryIso": "GR"
  }'
```

### Android flow

1. Ρύθμισε το `API_BASE_URL` στο `app/build.gradle.kts` (π.χ. `http://10.0.2.2:3000/` για emulator).
2. Εκκίνησε την εφαρμογή και πήγαινε στη φόρμα εγγραφής.
3. Συμπλήρωσε τα πεδία και πάτησε **Υποβολή αίτησης** ώστε να σταλεί το αίτημα στο gateway.
