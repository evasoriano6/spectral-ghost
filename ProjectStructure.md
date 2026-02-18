# SPECTRAL-01 Project Structure

This document outlines the architectural organization of the SPECTRAL-01 Android application. The project follows a Clean Architecture approach with a focus on separation of concerns, scalability, and stability.

## Directory Layout

### `ghost/data/`
**Purpose**: Handles data persistence, hardware interfaces, and external data sources.
*   `native/`: JNI wrappers and native interface definitions (e.g., `NativeHypervisor.kt`).
*   `device/`: Hardware abstraction for device-specific features (e.g., `HapticFeedbackManager.kt`).
*   `EvidenceManager.kt`: Manages recording and storage of forensic evidence.
*   `SubscriptionManager.kt`: Handles Google Play Billing interactions.
*   `EvidenceExporter.kt`: Utilities for exporting data.

### `ghost/domain/`
**Purpose**: Encapsulates core business logic and use cases, independent of UI and Framework.
*   `CalibrationManager.kt`: Logic for sensor calibration and anomaly detection thresholds.
*   `MotionValidator.kt`: Validates device movement patterns.
*   `AndroidDualDetection.kt`: Logic for dual-camera/sensor fusion.

### `ghost/ui/`
**Purpose**: Manages the user interface and user interaction.
*   `MainActivity.kt`: The single Activity entry point.
*   `screens/`: Composable screens.
    *   `SplashScreen.kt`: Technical boot sequence.
    *   `MainDashboard.kt`: The primary tactical interface (formerly `TacticalDashboard.kt`).
    *   `OnboardingScreen.kt`: Initial user setup.
*   `components/`: Reusable UI widgets (e.g., `RadarPulse`, `TelemetryWidget`).
*   `viewmodels/`: State holders and logic connectors.
    *   `SpectralViewModel.kt`: Connects Domain/Data layers to the UI.
*   `theme/`: Theme definitions (Colors, Type, etc.).

### `ghost/native/`
**Purpose**: Contains C++ source code and build scripts for the NDK integration.
*   `CMakeLists.txt`: Build configuration for the native library.
*   `NativeBridge.cpp`: JNI interface implementation.
*   `AudioEngine.cpp`: Low-latency audio processing.
*   `ThermalFilter.cpp`: Vulkan compute shaders for thermal simulation.

### `ghost/di/`
**Purpose**: Dependency Injection configuration.
*   *(Placeholder)*: Future Hilt/Koin modules.

## Key Implementation Details

*   **Namespace**: `com.spectral.ghost`
*   **Layering**: `UI` -> `ViewModel` -> `Domain` -> `Data` -> `Native/Device`
*   **Native Ops**: All JNI operations are routed through `com.spectral.ghost.data.native.NativeHypervisor`.

## Migration Notes
When adding new features:
1.  **Logic**: add to `domain/`.
2.  **Data**: add to `data/`.
3.  **UI**: add to `ui/`.
4.  **Native**: add to `native/` and update `CMakeLists.txt`.
