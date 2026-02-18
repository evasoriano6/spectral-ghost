
package com.spectral.ghost.domain

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import kotlin.math.abs
import kotlin.math.sqrt

// MARK: - MOTION VALIDATOR (IMU AUDIT)
class MotionValidator(context: Context) : SensorEventListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)

    // Estado del Dispositivo
    private var gravity = FloatArray(3)
    private var linearAcceleration = FloatArray(3) // Movimiento real sin gravedad
    private var rotationRate = FloatArray(3)
    
    // Configuración de Filtros
    private val alpha = 0.8f // Factor para filtro complementario
    private val motionThreshold = 0.15f // Sensibilidad de movimiento (m/s^2)
    private val correlationTolerance = 0.90f // 90% de similitud vectorial = Falso Positivo

    init {
        accelerometer?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
        gyroscope?.also { sensor ->
            sensorManager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        event?.let {
            when (it.sensor.type) {
                Sensor.TYPE_ACCELEROMETER -> {
                    // Filtro Complementario para aislar gravedad
                    gravity[0] = alpha * gravity[0] + (1 - alpha) * it.values[0]
                    gravity[1] = alpha * gravity[1] + (1 - alpha) * it.values[1]
                    gravity[2] = alpha * gravity[2] + (1 - alpha) * it.values[2]

                    // Aceleración lineal = Aceleración Total - Gravedad
                    linearAcceleration[0] = it.values[0] - gravity[0]
                    linearAcceleration[1] = it.values[1] - gravity[1]
                    linearAcceleration[2] = it.values[2] - gravity[2]
                }
                Sensor.TYPE_GYROSCOPE -> {
                    rotationRate = it.values.clone()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
        // No-op
    }

    /**
     * AUDITORÍA CRUZADA:
     * Verifica si una anomalía es válida o es producto del movimiento del operador.
     * 
     * @param anomalyVector Vector de movimiento de la anomalía [dx, dy, dz]
     * @return true si es una entidad independiente, false si es "Ruido de Movimiento"
     */
    fun isValidAnomaly(anomalyVector: FloatArray): Boolean {
        // 1. Estabilización: Si el dispositivo está "quieto", confiamos más en el sensor visual
        val deviceSpeed = vectorMagnitude(linearAcceleration)
        val deviceRotation = vectorMagnitude(rotationRate)
        
        if (deviceSpeed < motionThreshold && deviceRotation < 0.1f) {
            return true // El dispositivo está estable, la anomalía es real
        }

        // 2. Filtro de Correlación Vectorial
        // Normalizamos vectores para comparar dirección
        val devNorm = normalize(linearAcceleration)
        val anomNorm = normalize(anomalyVector)

        // Producto Punto: 1.0 = Misma dirección, -1.0 = Opuesta, 0.0 = Perpendicular
        val dotProduct = dot(devNorm, anomNorm)

        // Si la anomalía se mueve exactamente igual que el teléfono (>90% correlación),
        // es un reflejo o un "fantasma" del algoritmo de tracking.
        if (dotProduct > correlationTolerance) {
            return false // FALSO POSITIVO (Inercia pareidólica)
        }

        return true // La entidad se mueve independientemente
    }
    
    // MARK: - MATH UTILS
    private fun vectorMagnitude(v: FloatArray): Float {
        return sqrt(v[0]*v[0] + v[1]*v[1] + v[2]*v[2])
    }

    private fun normalize(v: FloatArray): FloatArray {
        val mag = vectorMagnitude(v)
        if (mag == 0f) return FloatArray(3)
        return floatArrayOf(v[0]/mag, v[1]/mag, v[2]/mag)
    }

    private fun dot(v1: FloatArray, v2: FloatArray): Float {
        return v1[0]*v2[0] + v1[1]*v2[1] + v1[2]*v2[2]
    }
    
    fun unregister() {
        sensorManager.unregisterListener(this)
    }
}
