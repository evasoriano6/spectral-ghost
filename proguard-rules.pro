
# Add project specific ProGuard rules here.
# You can edit the include path and order by changing the proguardFiles
# directive in build.gradle.

# PRESERVE NATIVE JNI CLASSES
# Critical: If these are obfuscated, the C++ NativeBridge won't find the Java methods.
-keep class com.spectral.ghost.NativeHypervisor {
    native <methods>;
    public <methods>; 
}

# PRESERVE DATA CLASSES FOR SERIALIZATION
-keep class com.spectral.ghost.core.AnomalyEvent { *; }

# JETPACK COMPOSE & MATERIAL3 RULES
-keep class androidx.compose.material3.** { *; }

# ARCORE SPECIFIC
-keep class com.google.ar.core.** { *; }

# OPTIMIZATIONS
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose
