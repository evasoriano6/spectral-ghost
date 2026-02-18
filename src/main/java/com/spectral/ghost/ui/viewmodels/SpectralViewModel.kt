
package com.spectral.ghost.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.spectral.ghost.data.native.NativeHypervisor
import com.spectral.ghost.domain.CalibrationManager
import com.spectral.ghost.domain.MotionValidator
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random

// MARK: - UI STATE
data class SpectralUiState(
    val isScanning: Boolean = false,
    val isCalibrated: Boolean = false,
    val audioEnergy: Float = 0f,
    val thermalVariance: Float = 0f,
    val fusionConfidence: Float = 0f, // 0.0 - 1.0
    val systemStatus: String = "SYSTEM IDLE",
    val cpuTemp: Float = 45.0f // Temperatura simulada del SoC
)

class SpectralViewModel : ViewModel() {

    // Dependencies
    private val hypervisor = NativeHypervisor()
    private val calibrationManager = CalibrationManager(hypervisor)
    // MotionValidator requires Context, typically handled in Activity or DI. 
    // For this architecture, we assume it's injected or passed. 
    // Here we'll simulate the validation logic or assume it updates a shared state.

    // State
    private val _uiState = MutableStateFlow(SpectralUiState())
    val uiState = _uiState.asStateFlow()

    val calibrationProgress = calibrationManager.calibrationProgress.asStateFlow()

    init {
        // ...
    }

    private fun startTelemetryLoop() {
        viewModelScope.launch {
            while (true) {
                // ... metrics ...
                val audioE = hypervisor.getAudioEnergy()
                val thermalV = hypervisor.getThermalMetric()
                
                // 2. Calibration Check
                val isCalibrated = calibrationManager.calibrationState.value.isCalibrated
                val isAudioAnomaly = calibrationManager.isAnomaly(audioE, "AUDIO")
                val isThermalAnomaly = calibrationManager.isAnomaly(thermalV, "THERMAL")
                
                // 3. Fusion Logic
                var confidence = 0.0f
                if (isCalibrated) {
                     if (isAudioAnomaly) confidence += 0.4f
                     if (isThermalAnomaly) confidence += 0.4f
                }
                
                // 4. Update UI State
                val statusText = if (!isCalibrated) {
                    "CALIBRATING SENSORS..."
                } else if (confidence > 0.7f) {
                    "ANOMALY DETECTED"
                } else {
                    "SCANNING SECTOR"
                }

                _uiState.value = _uiState.value.copy(
                    audioEnergy = audioE,
                    thermalVariance = thermalV,
                    fusionConfidence = confidence,
                    isCalibrated = isCalibrated,
                    systemStatus = statusText,
                    cpuTemp = 40.0f + (Random.nextFloat() * 5.0f)
                )
                
                delay(16) // ~60 FPS
            }
        }
    }

    fun startCalibration() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(systemStatus = "CALIBRATING SENSORS...")
            calibrationManager.startLearningPhase()
            _uiState.value = _uiState.value.copy(systemStatus = "CALIBRATION COMPLETE")
        }
    }

    fun toggleScan() {
        val current = _uiState.value.isScanning
        _uiState.value = _uiState.value.copy(isScanning = !current)
        if (!current && !calibrationManager.calibrationState.value.isCalibrated) {
            startCalibration()
        }
    }

    fun exportEvidence() {
        // Lógica de exportación y hashing SHA-256
        println("[EVIDENCE] Exporting secured data packet...")
    }

    override fun onCleared() {
        super.onCleared()
        hypervisor.stopAudioEngine()
    }
}
