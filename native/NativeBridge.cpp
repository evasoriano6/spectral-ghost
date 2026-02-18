
#include <jni.h>
#include <string>
#include "AudioEngine.cpp"
#include "ThermalFilter.cpp"

extern "C" JNIEXPORT jstring JNICALL
Java_com_spectral_ghost_data_native_NativeHypervisor_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "SPECTRAL-01 NATIVE CORE ACTIVE";
    return env->NewStringUTF(hello.c_str());
}

// MARK: - AUDIO ENGINE BINDINGS
extern "C" JNIEXPORT void JNICALL
Java_com_spectral_ghost_data_native_NativeHypervisor_startAudioEngine(JNIEnv* env, jobject thiz) {
    if (engine) engine->start();
}

extern "C" JNIEXPORT void JNICALL
Java_com_spectral_ghost_data_native_NativeHypervisor_stopAudioEngine(JNIEnv* env, jobject thiz) {
    if (engine) engine->stop();
}

// MARK: - VULKAN THERMAL FILTER BINDINGS
extern "C" JNIEXPORT void JNICALL
Java_com_spectral_ghost_data_native_NativeHypervisor_initVulkanFilter(JNIEnv* env, jobject thiz) {
    if (filter) filter->initVulkan();
}

extern "C" JNIEXPORT void JNICALL
Java_com_spectral_ghost_data_native_NativeHypervisor_processThermalFrame(JNIEnv* env, jobject thiz, jbyteArray data, jint w, jint h) {
    // Convertir jbyteArray a uint8_t* para C++
    jboolean isCopy;
    jbyte* buffer = env->GetByteArrayElements(data, &isCopy);
    
    if (filter) {
        filter->processFrame(reinterpret_cast<uint8_t*>(buffer), w, h);
    }
    
    env->ReleaseByteArrayElements(data, buffer, 0);
}
