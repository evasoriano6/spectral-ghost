
#include <aaudio/AAudio.h>
#include <android/log.h>
#include <complex>
#include <vector>
#include <algorithm>
#include <cmath>

#define TAG "AAudioEngine"

// FFT Utils (Simple Radix-2 for demo, in prod use KISS_FFT or Oboe)
const float PI = 3.14159265358979323846;

struct Complex {
    float real, imag;
};

void fft(std::vector<Complex>& a) {
    int n = a.size();
    if (n <= 1) return;

    std::vector<Complex> even(n / 2), odd(n / 2);
    for (int i = 0; 2 * i < n; i++) {
        even[i] = a[2 * i];
        odd[i] = a[2 * i + 1];
    }

    fft(even);
    fft(odd);

    for (int i = 0; 2 * i < n; i++) {
        float angle = -2 * PI * i / n;
        Complex w = {std::cos(angle), std::sin(angle)};
        Complex t = {w.real * odd[i].real - w.imag * odd[i].imag,
                     w.real * odd[i].imag + w.imag * odd[i].real};
        a[i] = {even[i].real + t.real, even[i].imag + t.imag};
        a[i + n / 2] = {even[i].real - t.real, even[i].imag - t.imag};
    }
}

class AudioEngine {
public:
    AAudioStream* stream = nullptr;
    int32_t sampleRate = 48000;
    std::vector<float> audioBuffer;
    bool isRecording = false;

    // Callback de audio de alta prioridad
    static aaudio_data_callback_result_t dataCallback(
            AAudioStream *stream,
            void *userData,
            void *audioData,
            int32_t numFrames) {
        
        float *buffer = static_cast<float *>(audioData);
        AudioEngine *engine = static_cast<AudioEngine *>(userData);

        // Copia segura para análisis FFT (Ring Buffer simplificado)
        // En prod usar Lock-Free Queue
        if (engine->isRecording) {
            engine->analyzeAudio(buffer, numFrames);
        }

        return AAUDIO_CALLBACK_RESULT_CONTINUE;
    }

    void start() {
        AAudioStreamBuilder *builder;
        AAudio_createStreamBuilder(&builder);
        AAudioStreamBuilder_setFormat(builder, AAUDIO_FORMAT_PCM_FLOAT);
        AAudioStreamBuilder_setPerformanceMode(builder, AAUDIO_PERFORMANCE_MODE_LOW_LATENCY);
        AAudioStreamBuilder_setDataCallback(builder, dataCallback, this);
        
        AAudioStreamBuilder_openStream(builder, &stream);
        AAudioStream_requestStart(stream);
        
        sampleRate = AAudioStream_getSampleRate(stream);
        isRecording = true;
        
        __android_log_print(ANDROID_LOG_INFO, TAG, "Stream started at %d Hz", sampleRate);
        AAudioStreamBuilder_delete(builder);
    }

    void stop() {
        if (stream) {
            isRecording = false;
            AAudioStream_requestStop(stream);
            AAudioStream_close(stream);
            stream = nullptr;
        }
    }

    // Análisis FFT optimizado (se ejecuta en hilo de audio, warning!)
    // Idealmente mover a hilo de worker
    void analyzeAudio(float* data, int32_t numFrames) {
        // Ventana de Hamming
        std::vector<Complex> fftData(numFrames);
        for (int i = 0; i < numFrames; i++) {
            float hamming = 0.54f - 0.46f * cos(2 * PI * i / (numFrames - 1));
            fftData[i] = {data[i] * hamming, 0};
        }
        
        fft(fftData);
        
        // Buscar ultrasonido (>18kHz)
        int binSize = sampleRate / numFrames;
        int ultrasoundStartBin = 18000 / binSize;
        
        float maxEnergy = 0.0f;
        for (int i = ultrasoundStartBin; i < numFrames/2; i++) {
            float energy = sqrt(fftData[i].real * fftData[i].real + fftData[i].imag * fftData[i].imag);
            if (energy > maxEnergy) maxEnergy = energy;
        }

        if (maxEnergy > 0.05f) { // Umbral de silencio
             __android_log_print(ANDROID_LOG_WARN, TAG, "ULTRASOUND DETECTED: Energy %f", maxEnergy);
        }
    }
    // Exposición de Métrica para Calibración (JNI)
    float getCurrentEnergy() {
        // Retorna la energía promedio del último buffer analizado
        // En prod: usar RingBuffer promedio móvil
        if (audioBuffer.empty()) return 0.0f;
        float sum = 0.0f;
        for (float val : audioBuffer) sum += val * val; // RMS Simplificado
        return sqrt(sum / audioBuffer.size()) * 100.0f; // Escala arbitraria 0-100
    }
};

extern "C" {
    AudioEngine* engine = new AudioEngine();

    void Java_com_spectral_ghost_NativeHypervisor_startAudioEngine(JNIEnv* env, jobject thiz) {
        engine->start();
    }
    
    void Java_com_spectral_ghost_NativeHypervisor_stopAudioEngine(JNIEnv* env, jobject thiz) {
        engine->stop();
    }

    jfloat Java_com_spectral_ghost_NativeHypervisor_getAudioEnergy(JNIEnv* env, jobject thiz) {
        return engine->getCurrentEnergy();
    }
}
