package com.example.grifon.ui.screens.categories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
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
import com.example.grifon.viewmodel.CategoriesState
import com.example.grifon.viewmodel.CategoriesViewModel

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
            val state = (uiState as UiState.Success<CategoriesState>).data
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Text(text = "Κατηγορίες", style = MaterialTheme.typography.titleLarge)
                }
                if (state.categories.isEmpty()) {
                    item {
                        Text(
                            text = "Δεν βρέθηκαν κατηγορίες για το ενεργό κατάστημα.",
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    items(state.categories, key = { it.id }) { category ->
                        CategoryCard(
                            title = category.name,
                            onClick = { onCategorySelected(category.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryCard(title: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .clickable(onClick = onClick),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
        }
    }
}
