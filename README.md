# grifon-eshop

/workspace/grifon-eshop$ /bin/bash -lc nl -ba README.md
1	# Grifon Eshop
     2	
     3	Σύντομη περίληψη των μέχρι τώρα εργασιών για το project Grifon eShop.
     4	
     5	## Τι έχουμε υλοποιήσει
     6	
     7	- **Android εφαρμογή (module `app`)**: η βάση της εφαρμογής πελάτη για προβολή περιεχομένου e-shop.
     8	- **Backend proxy (`backend/`)**: REST API που μεσολαβεί προς το PrestaShop Webservice API και προσφέρει endpoints για categories, προϊόντα, search και stock.
     9	- **Gateway υπηρεσία (`grifon-gateway/`)**: ασφαλές proxy για PrestaShop multishop με φιλτραρισμένα endpoints, έλεγχο τιμών ανά πελάτη και μηχανισμούς ασφάλειας.
    10	
    11	## Δομή αποθετηρίου
    12	
    13	- `app/` — Android client (Gradle project).
    14	- `backend/` — Node.js REST proxy για PrestaShop.
    15	- `grifon-gateway/` — Node.js gateway με multishop υποστήριξη.
    16	
    17	## Τεκμηρίωση
    18	
    19	Για αναλυτικές οδηγίες εκκίνησης και endpoints, δείτε τα README στα επιμέρους directories:
    20	- `backend/README.md`
    21	- `grifon-gateway/README.md`
    22	
    23	## How to test
    24	
    25	### Gateway (register)
    26	
    27	```bash
    28	curl -X POST http://localhost:3000/auth/register \
    29	  -H "Content-Type: application/json" \
    30	  -d '{
    31	    "email": "user@example.com",
    32	    "password": "secret123",
    33	    "firstName": "Γιώργος",
    34	    "lastName": "Παπαδόπουλος",
    35	    "countryIso": "GR"
    36	  }'
    37	```
    38	
    39	### Android flow
    40	
    41	1. Ρύθμισε το `API_BASE_URL` στο `app/build.gradle.kts` (π.χ. `http://10.0.2.2:3000/` για emulator).
    42	2. Εκκίνησε την εφαρμογή και πήγαινε στη φόρμα εγγραφής.
    43	3. Συμπλήρωσε τα πεδία και πάτησε **Υποβολή αίτησης** ώστε να σταλεί το αίτημα στο gateway.
