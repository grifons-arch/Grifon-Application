package com.example.grifon

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.grifon.ui.theme.GrifonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GrifonTheme {
                GrifonHomeScreen()
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun GrifonHomeScreen() {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Image(
                            painter = painterResource(R.drawable.logo),
                            contentDescription = "Grifon logo",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.height(32.dp),
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Εξυπηρέτηση πελατών 0030 2810-821627 - info@grifon.gr",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(top = 24.dp, start = 16.dp, end = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            Image(
                painter = painterResource(R.drawable.logo),
                contentDescription = "Grifon logo",
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(180.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Box(modifier = Modifier.weight(1f))
        }
    }
}
