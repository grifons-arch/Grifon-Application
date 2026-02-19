package com.example.grifon.ui

import com.example.grifon.R
import com.example.grifon.domain.model.Category

/**
 * Mapping top-level PrestaShop categories to app labels and icons.
 */
data class CategoryShortcut(
    val label: String,
    val categoryId: String?,
    val iconResId: Int,
)

private data class CategoryRule(
    val keywords: List<String>,
    val label: String,
    val iconResId: Int,
    val fallbackId: String,
)

private val categoryRules = listOf(
    CategoryRule(listOf("ceramic", "κεραμ"), "Κεραμικά", R.drawable.kersmiks_diskodmhtiks, "3"),
    CategoryRule(listOf("statuette", "figur", "αλαβάστ", "αγαλμ"), "Αγαλματίδια", R.drawable.veroza, "4"),
    CategoryRule(listOf("decorative", "διακοσμ"), "Διακοσμητικά", R.drawable.diakosmitika_keramikago, "5"),
    CategoryRule(listOf("hobbies", "toys", "παιχν"), "Παιχνίδια", R.drawable.paixnidiarouytrina, "7"),
    CategoryRule(listOf("for use", "kitchen", "soap", "towel", "χρήση", "σαπο"), "Για χρήση", R.drawable.sapounia, "8"),
    CategoryRule(listOf("accessory", "fabric", "bag", "αξεσου", "υφασμ"), "Αξεσουάρ", R.drawable.yfasmatina, "9"),
)

fun buildCategoryShortcuts(categories: List<Category>): List<CategoryShortcut> {
    val normalized = categories.map { it to normalize(it.name) }

    val mapped = categoryRules.map { rule ->
        val matched = normalized.firstOrNull { (_, name) ->
            rule.keywords.any { keyword -> name.contains(normalize(keyword)) }
        }?.first

        CategoryShortcut(
            label = rule.label,
            categoryId = matched?.id ?: rule.fallbackId,
            iconResId = rule.iconResId,
        )
    }

    return listOf(CategoryShortcut("Όλα", null, R.drawable.logo)) + mapped
}

private fun normalize(value: String): String {
    return value
        .lowercase()
        .replace("ά", "α")
        .replace("έ", "ε")
        .replace("ή", "η")
        .replace("ί", "ι")
        .replace("ό", "ο")
        .replace("ύ", "υ")
        .replace("ώ", "ω")
        .replace("ϊ", "ι")
        .replace("ΐ", "ι")
        .replace("ϋ", "υ")
        .replace("ΰ", "υ")
        .trim()
}
