package com.example.grifon.data.repository

import kotlinx.coroutines.flow.Flow

interface BarcodeScannerService {
    val scanResults: Flow<String>
    fun startScanning()
    fun stopScanning()
    fun submitManualCode(code: String)
}
