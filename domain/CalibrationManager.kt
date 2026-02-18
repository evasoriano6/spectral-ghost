
package com.spectral.ghost.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.math.pow
import kotlin.math.sqrt

// MARK: - CALIBRATION DATA
data class CalibrationProfile(
    val audioMean: Float = 0f,
    val audioStdDev: Float = 0f,
    val thermalMean: Float = 0f,
    val thermalStdDev: Float = 0f,
    val isCalibrated: Boolean = false
)

class CalibrationManager(private val hypervisor: NativeHypervisor) {
    
    private val _calibrationState = MutableStateFlow(CalibrationProfile())
    val calibrationState = _calibrationState.asStateFlow()
    
    // Configuración
    private val samplesToCollect = 100 // @ 100ms interval = 10 seconds
    private val sigmaMultiplier = 3.0f // 3 Sigma Rule (99.7%)
    
    // Buffers
    private val audioSamples = ArrayList<Float>()
    private val thermalSamples = ArrayList<Float>()

    private val _calibrationProgress = MutableStateFlow(0f)
    val calibrationProgress = _calibrationProgress.asStateFlow()

    suspend fun startLearningPhase() {
        println("[CALIBRATION] INICIANDO FASE DE APRENDIZAJE...")
        
        audioSamples.clear()
        thermalSamples.clear()
        
        // 1. Recolección de Datos (Baseline)
        for (i in 0 until samplesToCollect) {
            val audioE = hypervisor.getAudioEnergy()
            val thermalV = hypervisor.getThermalMetric()
            
            audioSamples.add(audioE)
            thermalSamples.add(thermalV)
            
            // Log de progreso
            val progress = (i + 1) / samplesToCollect.toFloat()
            _calibrationProgress.value = progress
            
            if (i % 10 == 0) {
                println("[CALIBRATION] Muestreo ${(progress * 100).toInt()}% completo.")
            }
            
            delay(100) // 100ms intervalo
        }
        
        // 2. Cálculo Estadístico
        val audioStats = calculateStats(audioSamples)
        val thermalStats = calculateStats(thermalSamples)
        
        // 3. Establecer Perfil
        _calibrationState.value = CalibrationProfile(
            audioMean = audioStats.first,
            audioStdDev = audioStats.second,
            thermalMean = thermalStats.first,
            thermalStdDev = thermalStats.second,
            isCalibrated = true
        )
        
        println("[CALIBRATION] PERFIL ESTABLECIDO:")
        println("   AUDIO: μ=${audioStats.first} σ=${audioStats.second}")
        println("   THERMAL: μ=${thermalStats.first} σ=${thermalStats.second}")
    }
    
    private fun calculateStats(data: List<Float>): Pair<Float, Float> {
        if (data.isEmpty()) return Pair(0f, 0f)
        
        val mean = data.average().toFloat()
        var sumSquaredDiff = 0.0
        
        for (x in data) {
            sumSquaredDiff += (x - mean).pow(2)
        }
        
        val stdDev = sqrt(sumSquaredDiff / data.size).toFloat()
        return Pair(mean, stdDev)
    }

    /**
     * Verifica si un valor actual es una anomalía estadística.
     * @param currentValue Valor actual del sensor
     * @param sensorType "AUDIO" o "THERMAL"
     * @return true si supera el umbral de 3 Sigma
     */
    fun isAnomaly(currentValue: Float, sensorType: String): Boolean {
        if (!_calibrationState.value.isCalibrated) return false
        
        val profile = _calibrationState.value
        val (mean, stdDev) = when (sensorType) {
            "AUDIO" -> Pair(profile.audioMean, profile.audioStdDev)
            "THERMAL" -> Pair(profile.thermalMean, profile.thermalStdDev)
            else -> return false
        }
        
        // Dynamic Threshold: μ + 3σ
        val threshold = mean + (stdDev * sigmaMultiplier)
        
        return currentValue > threshold
    }
}
