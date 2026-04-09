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
import com.example.grifon.domain.model.Category
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
        is UiState.Error -> ErrorScreen(message = stringResource(R.string.categories_load_error))
        is UiState.Success -> {
            val state = (uiState as UiState.Success<CategoriesState>).data
            val menuGroups = remember(state.categories) { categoryMenuGroups(state.categories) }
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
    onCategorySelected: (String) -> Unit,
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
                        onClick = { onCategorySelected(group.categoryId) },
                    )
                    HorizontalDivider(color = Color(0xFFE2E6EE))
                    group.children.forEachIndexed { index, child ->
                        CategoryLinkRow(
                            title = child.title,
                            emphasized = false,
                            onClick = { onCategorySelected(child.categoryId) },
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
)

private fun categoryMenuGroups(categories: List<Category>): List<CategoryMenuGroup> {
    val filteredCategories = categories.filter { it.id != "1" && it.id != "2" }
    val rootCandidates = filteredCategories.filter { it.parentId == "2" }
    val topLevelCategories = if (rootCandidates.isNotEmpty()) rootCandidates else filteredCategories.filter { it.parentId == null }
    val childrenByParent = filteredCategories.groupBy { it.parentId }

    val preferredOrder = listOf(
        "Κεραμικά",
        "Αγαλματίδια",
        "Διακοσμητικά",
        "Για χρήση",
        "Χόμπι και παιχνίδια",
        "Αξεσουάρ",
    )

    return topLevelCategories
        .filter { categoryIconRes(it) != null }
        .sortedWith(
            compareBy<Category> { category ->
                preferredOrder.indexOfFirst { keyword ->
                    normalizeCategoryText(category.name).contains(normalizeCategoryText(keyword))
                }.let { if (it == -1) Int.MAX_VALUE else it }
            }.thenBy { it.position ?: Int.MAX_VALUE }
             .thenBy { it.name.lowercase() }
        )
        .map { parent ->
            val children = childrenByParent[parent.id]
                .orEmpty()
                .sortedWith(compareBy<Category> { it.position ?: Int.MAX_VALUE }.thenBy { it.name.lowercase() })

            CategoryMenuGroup(
                id = parent.id,
                title = parent.name,
                categoryId = parent.id,
                iconRes = categoryIconRes(parent) ?: R.drawable.logo,
                children = children.map { child ->
                    CategoryMenuChild(
                        title = child.name,
                        categoryId = child.id,
                    )
                },
            )
        }
        .filter { it.children.isNotEmpty() || it.categoryId.isNotBlank() }
}

private fun categoryIconRes(category: Category): Int? {
    val normalized = normalizeCategoryText(listOfNotNull(category.name, category.slug).joinToString(" "))
    return when {
        normalized.contains("κεραμ") || normalized.contains("keram") -> R.drawable.kersmiks_diskodmhtiks
        normalized.contains("αγαλμ") || normalized.contains("figur") || normalized.contains("agalm") -> R.drawable.veroza
        normalized.contains("διακοσμ") || normalized.contains("decor") -> R.drawable.diakosmitika_keramikago
        normalized.contains("χρηση") || normalized.contains("for use") || normalized.contains("for-use") || normalized.contains("σαπουν") -> R.drawable.sapounia
        normalized.contains("παιχν") || normalized.contains("hobbies") || normalized.contains("games") || normalized.contains("chess") -> R.drawable.paixnidiarouytrina
        normalized.contains("αξεσ") || normalized.contains("accessor") || normalized.contains("bag") || normalized.contains("textile") -> R.drawable.yfasmatina
        else -> null
    }
}

private fun normalizeCategoryText(value: String): String {
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
