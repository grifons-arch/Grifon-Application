package com.example.grifon.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grifon.R
import com.example.grifon.domain.model.Category

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    shopLabel: String,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCartClick: () -> Unit,
    onNotificationsClick: () -> Unit,
    categories: List<Category> = emptyList(),
    onCategoryClick: (Category) -> Unit = {},
    showSearch: Boolean = false,
    searchQuery: String = "",
    onSearchQueryChange: (String) -> Unit = {},
    onScanClick: () -> Unit = {}
) {
    val purpleColor = Color(0xFF6200EE)
    var showCategoryMenu by remember { mutableStateOf(false) }
    var isSearchExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(purpleColor)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Μενού", tint = Color.White)
            }
            
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showSearch) {
                    IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                        Icon(
                            if (isSearchExpanded) Icons.Default.Close else Icons.Default.Search, 
                            contentDescription = "Search", 
                            tint = Color.White
                        )
                    }
                }

                IconButton(onClick = onHomeClick) {
                    Icon(Icons.Default.Home, contentDescription = "Home", tint = Color.White)
                }

                Box {
                    IconButton(onClick = { showCategoryMenu = true }) {
                        Icon(Icons.Default.Dashboard, contentDescription = "Κατηγορίες", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        if (categories.isEmpty()) {
                            DropdownMenuItem(
                                text = { Text("Φόρτωση κατηγοριών...") },
                                onClick = { showCategoryMenu = false }
                            )
                        } else {
                            categories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.name) },
                                    onClick = {
                                        showCategoryMenu = false
                                        onCategoryClick(category)
                                    }
                                )
                            }
                        }
                    }
                }

                IconButton(onClick = onNotificationsClick) {
                    Icon(Icons.Default.Notifications, contentDescription = "Notifications", tint = Color.White)
                }
                IconButton(onClick = onCartClick) {
                    Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = Color.White)
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(
            visible = showSearch && isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            AppSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onScanClick = onScanClick,
                onClearClick = { onSearchQueryChange("") }
            )
        }

        // Logo Section (Hidden when search is expanded to save space if needed, or kept)
        if (!(showSearch && isSearchExpanded)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.logo),
                        contentDescription = "Grifon Logo",
                        modifier = Modifier.height(40.dp),
                        contentScale = ContentScale.Fit
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "GRIFON ($shopLabel)",
                        color = Color.White,
                        style = MaterialTheme.typography.headlineSmall,
                        letterSpacing = 2.sp
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit,
    onClearClick: () -> Unit = {}
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF5F5F5),
        tonalElevation = 2.dp
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Αναζήτηση προϊόντων...", color = Color.Gray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                Row {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = onClearClick) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.Gray)
                        }
                    }
                    IconButton(onClick = onScanClick) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan", tint = Color.Gray)
                    }
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
            singleLine = true
        )
    }
}
