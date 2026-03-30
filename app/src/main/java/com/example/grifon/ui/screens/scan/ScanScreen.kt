package com.example.grifon.ui.screens.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.grifon.R
import com.example.grifon.core.UiEvent
import com.example.grifon.viewmodel.ScanViewModel

@Composable
fun ScanScreen(navController: NavHostController, viewModel: ScanViewModel) {
    val manualCode = remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.start()
        // Collect navigation events from the ViewModel
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.Navigate -> navController.navigate(event.route)
                is UiEvent.ShowSnackbar -> {
                    // Optionally show a snackbar if you have scaffold/snackbarHost
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(R.string.barcode_scan), style = MaterialTheme.typography.titleMedium)
                Text(text = stringResource(R.string.camera_mlkit_stub))
            }
        }
        OutlinedTextField(
            value = manualCode.value,
            onValueChange = { manualCode.value = it },
            label = { Text(stringResource(R.string.manual_barcode)) },
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = { viewModel.submitManual(manualCode.value) },
            modifier = Modifier.align(Alignment.End),
        ) {
            Text(text = stringResource(R.string.search))
        }
    }
}
