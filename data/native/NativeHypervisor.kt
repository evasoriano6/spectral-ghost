
package com.spectral.ghost.data.native

class NativeHypervisor {

    companion object {
        // Carga la librería dinámica 'native-lib.so' al iniciar
        init {
            System.loadLibrary("native-lib")
        }
    }

    // --- JNI EXTERNAL FUNCTION DECLARATIONS ---

    /**
     * Inicia el motor de audio de baja latencia (AAudio).
     * Muestreo: 48kHz / 96kHz según hardware.
     */
    external fun startAudioEngine()

    /**
     * Detiene el motor de audio y libera recursos.
     */
    external fun stopAudioEngine()

    /**
     * Inicializa la instancia de Vulkan y el pipeline de cómputo.
     */
    external fun initVulkanFilter()

    /**
     * Envía un frame de cámara RAW para análisis de ruido térmico en GPU.
     * @param data Buffer de bytes YUV_420_888
     * @param width Ancho de imagen
     * @param height Alto de imagen
     */
    external fun processThermalFrame(data: ByteArray, width: Int, height: Int)

    /**
     * Test de conectividad JNI.
     */
    external fun stringFromJNI(): String

    // --- CALIBRATION METRICS ---

    /**
     * Retorna la energía promedio del espectro de audio (RMS).
     */
    external fun getAudioEnergy(): Float

    /**
     * Retorna la varianza del ruido térmico del sensor de cámara.
     */
    external fun getThermalMetric(): Float
}
