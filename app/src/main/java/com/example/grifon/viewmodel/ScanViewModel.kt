package com.example.grifon.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.grifon.core.UiEvent
import com.example.grifon.data.repository.BarcodeScannerService
import com.example.grifon.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val scannerService: BarcodeScannerService,
) : ViewModel() {
    val events = MutableSharedFlow<UiEvent>()

    init {
        scannerService.scanResults
            .onEach { code ->
                events.emit(UiEvent.Navigate(Routes.plpRoute(query = code)))
            }
            .launchIn(viewModelScope)
    }

    fun start() = scannerService.startScanning()
    fun stop() = scannerService.stopScanning()
    fun submitManual(code: String) = scannerService.submitManualCode(code)
}
