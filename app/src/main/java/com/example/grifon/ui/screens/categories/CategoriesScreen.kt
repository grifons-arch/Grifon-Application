package com.example.grifon.ui.screens.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.grifon.R
import com.example.grifon.core.UiState
import com.example.grifon.ui.screens.ErrorScreen
import com.example.grifon.ui.screens.LoadingScreen
import com.example.grifon.viewmodel.CategoriesState
import com.example.grifon.viewmodel.CategoriesViewModel

@Composable
fun CategoriesScreen(
    viewModel: CategoriesViewModel,
    onCategorySelected: (String, String) -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    when (uiState) {
        UiState.Loading -> LoadingScreen()
        is UiState.Error -> ErrorScreen(message = stringResource(R.string.categories_load_error))
        is UiState.Success -> {
            val state = (uiState as UiState.Success<CategoriesState>).data
            val menuGroups = remember { categoryMenuGroups() }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.categories),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "Ψωνίστε ανά κατηγορία με υποκατηγορίες όπως στο vertical menu του site.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF6A7280),
                        )
                    }
                }
                if (menuGroups.isEmpty() && state.categories.isEmpty()) {
                    item {
                        Text(
                            text = stringResource(R.string.categories_empty),
                            modifier = Modifier.padding(top = 16.dp),
                            style = MaterialTheme.typography.bodyLarge,
                        )
                    }
                } else {
                    items(menuGroups, key = { it.id }) { group ->
                        CategoryGroupCard(
                            group = group,
                            expanded = state.expanded.contains(group.id),
                            onToggle = { viewModel.toggle(group.id) },
                            onCategorySelected = onCategorySelected,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryGroupCard(
    group: CategoryMenuGroup,
    expanded: Boolean,
    onToggle: () -> Unit,
    onCategorySelected: (String, String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color(0xFFEAF0FF), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(id = group.iconRes),
                        contentDescription = group.title,
                        tint = Color.Unspecified,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Text(
                        text = group.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${group.children.size} υποκατηγορίες",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFF6A7280),
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = Color(0xFF001489),
                )
            }

            if (expanded) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CategoryLinkRow(
                        title = "Όλα τα ${group.title}",
                        emphasized = true,
                        onClick = { onCategorySelected(group.categoryId, "") },
                    )
                    HorizontalDivider(color = Color(0xFFE2E6EE))
                    group.children.forEachIndexed { index, child ->
                        CategoryLinkRow(
                            title = child.title,
                            emphasized = false,
                            onClick = { onCategorySelected(child.categoryId, child.query) },
                        )
                        if (index != group.children.lastIndex) {
                            HorizontalDivider(color = Color(0xFFE2E6EE))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryLinkRow(
    title: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            style = if (emphasized) {
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (emphasized) Color(0xFF001489) else Color(0xFF1C2433),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = null,
            tint = Color(0xFF001489),
            modifier = Modifier.size(18.dp),
        )
    }
}

private data class CategoryMenuGroup(
    val id: String,
    val title: String,
    val categoryId: String,
    val iconRes: Int,
    val children: List<CategoryMenuChild>,
)

private data class CategoryMenuChild(
    val title: String,
    val categoryId: String,
    val query: String = "",
)

private fun categoryMenuGroups(): List<CategoryMenuGroup> {
    return listOf(
        CategoryMenuGroup(
            id = "4000",
            title = "Κεραμικά",
            categoryId = "4000",
            iconRes = R.drawable.kersmiks_diskodmhtiks,
            children = listOf(
                CategoryMenuChild("Διακοσμητικά Κεραμικά", "4000", "Διακοσμητικά Κεραμικά"),
                CategoryMenuChild("Φανάρια, Καντήλια", "4000", "Φανάρια, Καντήλια"),
            ),
        ),
        CategoryMenuGroup(
            id = "4500",
            title = "Αγαλματίδια κ.α.",
            categoryId = "4500",
            iconRes = R.drawable.veroza,
            children = listOf(
                CategoryMenuChild("Βερονέζ", "4500", "Βερονέζ"),
                CategoryMenuChild("Αλαβάστρινα", "4500", "Αλαβάστρινα"),
                CategoryMenuChild("Μπρούτζινα", "4500", "Μπρούτζινα"),
                CategoryMenuChild("Πολυεστερικά", "4500", "Πολυεστερικά"),
                CategoryMenuChild("Γύψινα, Πωρόλιθος, Μαρμάρινα", "4500", "Γύψινα Πωρόλιθος Μαρμάρινα"),
            ),
        ),
        CategoryMenuGroup(
            id = "5000",
            title = "Διακοσμητικά",
            categoryId = "5000",
            iconRes = R.drawable.diakosmitika_keramikago,
            children = listOf(
                CategoryMenuChild("Φανάρια, Καντήλια", "5000", "Φανάρια Καντήλια"),
                CategoryMenuChild("Φωτιστικά", "5000", "Φωτιστικά"),
                CategoryMenuChild("Ρολόγια", "5000", "Ρολόγια"),
                CategoryMenuChild("Επιτραπέζια", "5000", "Επιτραπέζια"),
            ),
        ),
        CategoryMenuGroup(
            id = "7500",
            title = "Για χρήση",
            categoryId = "7500",
            iconRes = R.drawable.sapounia,
            children = listOf(
                CategoryMenuChild("Κουζίνας κ υαλικά", "7500", "Κουζίνας υαλικά"),
                CategoryMenuChild("Σαπούνια", "7500", "Σαπούνια"),
            ),
        ),
        CategoryMenuGroup(
            id = "7000",
            title = "Χόμπι και παιχνίδια",
            categoryId = "7000",
            iconRes = R.drawable.paixnidiarouytrina,
            children = listOf(
                CategoryMenuChild("Τάβλι, Σκάκι", "7000", "Τάβλι Σκάκι"),
                CategoryMenuChild("Παιχνίδια, Λούτρινα", "7000", "Παιχνίδια Λούτρινα"),
            ),
        ),
        CategoryMenuGroup(
            id = "8000",
            title = "Αξεσουάρ",
            categoryId = "8000",
            iconRes = R.drawable.yfasmatina,
            children = listOf(
                CategoryMenuChild("Υφασμάτινα και τσάντες", "8000", "Υφασμάτινα τσάντες"),
            ),
        ),
    )
}
