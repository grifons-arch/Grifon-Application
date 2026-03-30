package com.example.grifon.presentation.register

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

data class CountryOption(
    val isoCode: String,
    val displayName: String,
)

object RegisterAddressCatalog {
    private val countriesByLanguageTag = ConcurrentHashMap<String, List<CountryOption>>()

    private val countryAliases = mapOf(
        "GR" to setOf("greece", "hellas", "ellada", "ελλάδα", "ελλαδα"),
        "SE" to setOf("sweden", "sverige", "σουηδία", "σουηδια"),
        "CY" to setOf("cyprus", "κύπρος", "κυπρος"),
        "DE" to setOf("germany", "deutschland", "γερμανία", "γερμανια"),
        "IT" to setOf("italy", "italia", "ιταλία", "ιταλια"),
        "FR" to setOf("france", "γαλλία", "γαλλια"),
        "ES" to setOf("spain", "espana", "españa", "ισπανία", "ισπανια"),
        "GB" to setOf("united kingdom", "uk", "great britain", "england", "ηνωμένο βασίλειο", "ηνωμενο βασιλειο"),
        "US" to setOf("united states", "usa", "us", "america", "ηνωμένες πολιτείες", "ηνωμενες πολιτειες"),
    )

    private val citySuggestionsByCountry = mapOf(
        "GR" to listOf("Αθήνα", "Θεσσαλονίκη", "Πάτρα", "Ηράκλειο", "Λάρισα", "Βόλος", "Ιωάννινα", "Χανιά"),
        "SE" to listOf("Stockholm", "Göteborg", "Malmö", "Uppsala", "Västerås", "Örebro", "Linköping"),
        "CY" to listOf("Λευκωσία", "Λεμεσός", "Λάρνακα", "Πάφος", "Αμμόχωστος"),
        "DE" to listOf("Berlin", "Hamburg", "Munich", "Cologne", "Frankfurt", "Stuttgart"),
        "IT" to listOf("Rome", "Milan", "Naples", "Turin", "Florence", "Bologna"),
        "FR" to listOf("Paris", "Marseille", "Lyon", "Toulouse", "Nice"),
        "ES" to listOf("Madrid", "Barcelona", "Valencia", "Seville", "Bilbao"),
        "GB" to listOf("London", "Manchester", "Birmingham", "Liverpool", "Glasgow"),
        "US" to listOf("New York", "Los Angeles", "Chicago", "Houston", "Miami"),
    )

    private val streetSuggestionsByCountryAndCity = mapOf(
        "GR" to mapOf(
            "Αθήνα" to listOf("Ερμού 1", "Σταδίου 10", "Πανεπιστημίου 20", "Αθηνάς 15", "Σόλωνος 35"),
            "Θεσσαλονίκη" to listOf("Τσιμισκή 12", "Εγνατία 54", "Μητροπόλεως 18", "Λεωφόρος Νίκης 27"),
            "Πάτρα" to listOf("Ρήγα Φεραίου 8", "Κορίνθου 30", "Αγίου Ανδρέου 55"),
            "Ηράκλειο" to listOf("25ης Αυγούστου 11", "Δικαιοσύνης 5", "Ίδης 18"),
            "Λάρισα" to listOf("Κύπρου 14", "Παπαναστασίου 32", "Μεγάλου Αλεξάνδρου 9"),
            "Βόλος" to listOf("Δημητριάδος 88", "Ιάσονος 21", "Αργοναυτών 14"),
            "Ιωάννινα" to listOf("Αβέρωφ 6", "Δωδώνης 37", "Πυρσινέλλα 10"),
            "Χανιά" to listOf("Χάληδων 22", "Σκαλίδη 9", "Κισσάμου 40"),
        ),
        "SE" to mapOf(
            "Stockholm" to listOf("Drottninggatan 12", "Sveavägen 25", "Kungsgatan 8", "Hornsgatan 44"),
            "Göteborg" to listOf("Avenyn 10", "Kungsgatan 18", "Vasagatan 6"),
            "Malmö" to listOf("Södra Förstadsgatan 14", "Baltzarsgatan 20", "Stortorget 3"),
            "Uppsala" to listOf("Kungsgatan 32", "Drottninggatan 6", "Svartbäcksgatan 18"),
        ),
        "CY" to mapOf(
            "Λευκωσία" to listOf("Λεωφόρος Μακαρίου 25", "Στασικράτους 18", "Λήδρας 40"),
            "Λεμεσός" to listOf("Ανεξαρτησίας 55", "Μακαρίου Γ 77", "28ης Οκτωβρίου 14"),
            "Λάρνακα" to listOf("Ερμού 20", "Αρτέμιδος 11", "Ζήνωνος Κιτιέως 45"),
        ),
        "DE" to mapOf(
            "Berlin" to listOf("Friedrichstrasse 100", "Unter den Linden 40", "Kurfürstendamm 25"),
            "Hamburg" to listOf("Mönckebergstrasse 19", "Jungfernstieg 8", "Spitalerstrasse 12"),
            "Munich" to listOf("Leopoldstrasse 35", "Kaufingerstrasse 14", "Sendlinger Strasse 9"),
        ),
        "IT" to mapOf(
            "Rome" to listOf("Via del Corso 18", "Via Nazionale 45", "Via Cola di Rienzo 72"),
            "Milan" to listOf("Corso Buenos Aires 22", "Via Torino 15", "Via Dante 9"),
            "Florence" to listOf("Via Roma 11", "Via de' Tornabuoni 20", "Borgo San Lorenzo 8"),
        ),
        "FR" to mapOf(
            "Paris" to listOf("Rue de Rivoli 10", "Boulevard Saint-Germain 45", "Avenue des Champs-Élysées 60"),
            "Lyon" to listOf("Rue de la République 18", "Cours Lafayette 22", "Avenue Jean Jaurès 9"),
            "Marseille" to listOf("La Canebière 30", "Rue Paradis 14", "Boulevard Longchamp 12"),
        ),
        "ES" to mapOf(
            "Madrid" to listOf("Gran Via 18", "Calle de Alcalá 44", "Paseo de la Castellana 72"),
            "Barcelona" to listOf("Passeig de Gràcia 25", "La Rambla 40", "Carrer de Balmes 18"),
            "Valencia" to listOf("Carrer de Colón 17", "Avinguda del Port 28", "Gran Via del Marqués del Túria 8"),
        ),
        "GB" to mapOf(
            "London" to listOf("Oxford Street 120", "Baker Street 221B", "King's Road 55", "Piccadilly 40"),
            "Manchester" to listOf("Deansgate 80", "Market Street 32", "Oxford Road 18"),
            "Birmingham" to listOf("New Street 25", "Corporation Street 14", "Broad Street 60"),
        ),
        "US" to mapOf(
            "New York" to listOf("5th Avenue 350", "Broadway 1500", "Madison Avenue 200"),
            "Los Angeles" to listOf("Sunset Boulevard 1200", "Hollywood Boulevard 6800", "Wilshire Boulevard 350"),
            "Chicago" to listOf("Michigan Avenue 500", "Wacker Drive 233", "State Street 120"),
        ),
    )

    fun countriesFor(locale: Locale): List<CountryOption> {
        val languageTag = locale.toLanguageTag()
        return countriesByLanguageTag.getOrPut(languageTag) {
            Locale.getISOCountries()
                .mapNotNull { countryIso ->
                    val displayName = Locale("", countryIso).getDisplayCountry(locale).trim()
                    if (displayName.isBlank()) null else CountryOption(countryIso, displayName)
                }
                .sortedBy { normalizeKey(it.displayName) }
        }
    }

    fun resolveCountry(query: String, locale: Locale): CountryOption? {
        val rawQuery = query.trim()
        if (rawQuery.isBlank()) return null

        if (rawQuery.length == 2) {
            val isoCode = rawQuery.uppercase(Locale.ROOT)
            if (Locale.getISOCountries().contains(isoCode)) {
                return CountryOption(
                    isoCode = isoCode,
                    displayName = Locale("", isoCode).getDisplayCountry(locale),
                )
            }
        }

        val normalizedQuery = normalizeKey(rawQuery)
        countriesFor(locale).firstOrNull { normalizeKey(it.displayName) == normalizedQuery }?.let { return it }
        countriesFor(Locale.ENGLISH).firstOrNull { normalizeKey(it.displayName) == normalizedQuery }?.let {
            return CountryOption(it.isoCode, Locale("", it.isoCode).getDisplayCountry(locale))
        }

        val aliasedIso = countryAliases.entries.firstOrNull { (_, aliases) ->
            aliases.any { alias -> normalizeKey(alias) == normalizedQuery }
        }?.key ?: return null

        return CountryOption(
            isoCode = aliasedIso,
            displayName = Locale("", aliasedIso).getDisplayCountry(locale),
        )
    }

    fun citySuggestions(countryIso: String, query: String = ""): List<String> {
        return filterSuggestions(citySuggestionsByCountry[countryIso].orEmpty(), query)
    }

    fun streetSuggestions(countryIso: String, city: String, query: String = ""): List<String> {
        val normalizedCity = normalizeKey(city)
        val streets = streetSuggestionsByCountryAndCity[countryIso]
            .orEmpty()
            .entries
            .firstOrNull { normalizeKey(it.key) == normalizedCity }
            ?.value
            .orEmpty()

        return filterSuggestions(streets, query)
    }

    private fun filterSuggestions(suggestions: List<String>, query: String): List<String> {
        val normalizedQuery = normalizeKey(query)
        return suggestions.filter { suggestion ->
            normalizedQuery.isBlank() || normalizeKey(suggestion).contains(normalizedQuery)
        }
    }

    private fun normalizeKey(value: String): String {
        val normalized = Normalizer.normalize(value.trim(), Normalizer.Form.NFD)
        return normalized
            .replace("\\p{M}+".toRegex(), "")
            .lowercase(Locale.ROOT)
    }
}
