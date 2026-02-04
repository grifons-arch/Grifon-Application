package com.example.grifon.data.fake

import com.example.grifon.data.repository.BarcodeScannerService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

class FakeBarcodeScannerService : BarcodeScannerService {
    private val _results = MutableSharedFlow<String>()
    override val scanResults: Flow<String> = _results

    override fun startScanning() {
        // No-op for fake service.
    }

    override fun stopScanning() {
        // No-op for fake service.
    }

    override fun submitManualCode(code: String) {
        _results.tryEmit(code)
    }
}
