Βήματα που θα ακολουθήσουμε:
1.
Π ροσθήκη Βιβλιοθήκης: Πρόσθεσα ήδη την com.google.android.libraries.places:places στο libs.versions.toml και στο build.gradle.kts.
2.
API Key: Θα χρειαστείς ένα Google Maps API Key. Θα πρέπει να το ορίσεις στο local.properties ως GOOGLE_MAPS_API_KEY=YOUR_KEY.
3.
Ενημέρωση UI: Θα προσθέσουμε ένα κουμπί "Αναζήτηση Διεύθυνσης" στην οθόνη εγγραφής που θα ανοίγει το Autocomplete της Google.
4.
Parsing: Όταν ο χρήστης επιλέγει μια διεύθυνση, η εφαρμογή θα "σπάει" την απάντηση της Google και θα γεμίζει αυτόματα τα πεδία: Χώρα, Πόλη, Οδός, ΤΚ.