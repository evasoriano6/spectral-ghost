
package com.spectral.ghost.data.device.core

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager

class HapticFeedbackManager(context: Context) {

    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    fun triggerWarning() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Vibración suave y corta (Click)
            vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            vibrator.vibrate(50)
        }
    }

    fun triggerAnomalyDetected() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Patrón doble: Ti-Ti
            val timing = longArrayOf(0, 100, 50, 100)
            val amplitudes = intArrayOf(0, 200, 0, 200)
            vibrator.vibrate(VibrationEffect.createWaveform(timing, amplitudes, -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 100, 50, 100), -1)
        }
    }

    fun triggerCriticalAlert() {
       if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Patrón pesado y persistente: BROOOOM
            val timing = longArrayOf(0, 500, 100, 500)
            val amplitudes = intArrayOf(0, 255, 0, 255) // Max amplitud
            vibrator.vibrate(VibrationEffect.createWaveform(timing, amplitudes, -1))
        } else {
            vibrator.vibrate(longArrayOf(0, 500, 100, 500), -1)
        } 
    }
}
