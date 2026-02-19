package com.example.grifon.ui.screens.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.CategoriesViewModel

data class CategorySection(
    val title: String,
    val subcategories: List<String>,
)

private val categorySections = listOf(
    CategorySection("Κεραμικά", listOf("Διακοσμητικά Κεραμικά", "Φανάρια, Καντήλια")),
    CategorySection(
        "Αγαλματίδια κ.α.",
        listOf("Βερονέζ", "Αλαβάστρινα", "Μπρούτζινα", "Πολυεστερικά", "Γύψινα, Πωρόλιθος, Μαρμάρινα"),
    ),
    CategorySection("Διακοσμητικά", listOf("Φανάρια, Καντήλια", "Φωτιστικά", "Ρολόγια", "Επιτραπέζια")),
    CategorySection("Για χρήση", listOf("Κουζίνας κ υαλικά", "Σαπούνια")),
    CategorySection("Χόμπι και παιχνίδια", listOf("Τάβλι, Σκάκι", "Παιχνίδια, Λούτρινα")),
    CategorySection("Αξεσουάρ", listOf("Υφασμάτινα και τσάντες")),
)

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onCategorySelected: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    when (uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = "Αδυναμία φόρτωσης κατηγοριών")
        is UiState.Success -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Text(text = "Κατηγορίες", style = MaterialTheme.typography.titleLarge)
                }
                items(categorySections) { section ->
                    CategorySectionCard(section = section)
                }
            }
        }
    }
}

@Composable
private fun CategorySectionCard(section: CategorySection) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = section.title, style = MaterialTheme.typography.titleMedium)
            section.subcategories.forEach { subcategory ->
                Text(
                    text = "- $subcategory",
                    modifier = Modifier.padding(start = 16.dp, top = 6.dp),
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}
