package com.example.grifon.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.grifon.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    shopLabel: String,
    onMenuClick: () -> Unit,
    onHomeClick: () -> Unit,
    onCartClick: () -> Unit,
    onNotificationsClick: () -> Unit
) {
    val headerBackground = Color(0xFF0A0E14)
    val announcementColor = Color(0xFF0A2A8C)
    val dividerColor = Color(0xFF1A1F2B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(headerBackground)
            .statusBarsPadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(announcementColor)
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "To place an order or see prices, create an account or sign in.",
                color = Color.White,
                style = MaterialTheme.typography.bodySmall,
                lineHeight = 18.sp,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFFC8D6FF),
                modifier = Modifier.size(16.dp)
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Greek ($shopLabel)",
                color = Color(0xFFB6BDC9),
                style = MaterialTheme.typography.labelLarge
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color(0xFFB6BDC9)
            )
        }

        HorizontalDivider(color = dividerColor)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onMenuClick) {
                Icon(
                    Icons.Default.Menu,
                    contentDescription = stringResource(R.string.menu_description),
                    tint = Color.White
                )
            }

            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Grifon Logo",
                modifier = Modifier
                    .height(34.dp)
                    .weight(1f),
                contentScale = ContentScale.Fit
            )

            IconButton(onClick = onHomeClick) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = stringResource(R.string.home_description),
                    tint = Color.White
                )
            }
            IconButton(onClick = onNotificationsClick) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = stringResource(R.string.notifications_description),
                    tint = Color.White
                )
            }
            IconButton(onClick = onCartClick) {
                Icon(
                    Icons.Default.ShoppingCart,
                    contentDescription = stringResource(R.string.cart_description),
                    tint = Color.White
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onScanClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 8.dp), // Fixed padding parameters
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF131821)
    ) {
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFF2A313D), RoundedCornerShape(8.dp)),
            placeholder = { Text(stringResource(R.string.search_placeholder), color = Color(0xFF7F8794)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF7F8794)) },
            trailingIcon = {
                IconButton(onClick = onScanClick) {
                    Icon(
                        Icons.Default.QrCodeScanner,
                        contentDescription = stringResource(R.string.scan_description),
                        tint = Color(0xFF7F8794)
                    )
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color.White
            ),
            singleLine = true
        )
    }
}
