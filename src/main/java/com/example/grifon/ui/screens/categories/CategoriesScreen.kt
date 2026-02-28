package com.example.grifon.ui.screens.categories

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.core.UiState
import com.example.grifon.domain.model.Category
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.CategoriesViewModel

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onCategorySelected: (String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = state.message)
        is UiState.Success -> {
            val categories = state.data.categories
            val expanded = state.data.expanded
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            ) {
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        QuickLinkChip(label = "Προσφορές") { onCategorySelected("offers") }
                        QuickLinkChip(label = "Δημοφιλή") { onCategorySelected("popular") }
                        QuickLinkChip(label = "Brands") { onCategorySelected("brands") }
                        QuickLinkChip(label = "Πρόσφατα") { onCategorySelected("recent") }
                    }
                }
                item {
                    Text(text = "Κατηγορίες", style = MaterialTheme.typography.titleMedium)
                }
                items(categories.filter { it.parentId == null }) { category ->
                    CategoryNode(
                        category = category,
                        categories = categories,
                        expanded = expanded,
                        level = 0,
                        onToggle = viewModel::toggle,
                        onCategorySelected = onCategorySelected,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickLinkChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(text = label) },
        colors = AssistChipDefaults.assistChipColors(),
    )
}

@Composable
private fun CategoryNode(
    category: Category,
    categories: List<Category>,
    expanded: Set<String>,
    level: Int,
    onToggle: (String) -> Unit,
    onCategorySelected: (String) -> Unit,
) {
    val children = categories.filter { it.parentId == category.id }
    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = (level * 16).dp)
                .clickable {
                    if (children.isEmpty()) {
                        onCategorySelected(category.id)
                    } else {
                        onToggle(category.id)
                    }
                },
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = category.name)
                if (children.isNotEmpty()) {
                    Text(text = if (expanded.contains(category.id)) "−" else "+")
                }
            }
        }
        if (expanded.contains(category.id)) {
            children.forEach { child ->
                CategoryNode(
                    category = child,
                    categories = categories,
                    expanded = expanded,
                    level = level + 1,
                    onToggle = onToggle,
                    onCategorySelected = onCategorySelected,
                )
            }
        }
    }
}
