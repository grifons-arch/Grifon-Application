package com.example.grifon.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.grifon.viewmodel.ScanViewModel

@Composable
fun ScanScreen(viewModel: ScanViewModel) {
    val manualCode = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.start()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Barcode scan", style = MaterialTheme.typography.titleMedium)
                Text(text = "Κάμερα + ML Kit (stub)")
            }
        }
        OutlinedTextField(
            value = manualCode.value,
            onValueChange = { manualCode.value = it },
            label = { Text("Manual barcode") },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.submitManual(manualCode.value) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = "Αναζήτηση")
        }
    }
}
