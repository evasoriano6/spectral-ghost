# SPECTRAL-01: Guía de Integración Móvil (iOS)

Este documento detalla cómo integrar el núcleo `ARFusionCore.swift` en un proyecto Xcode nativo.

## Requisitos Previos
- Xcode 14.0+
- Dispositivo iOS con Sensor LiDAR (iPhone 12 Pro/Max o superior)
- iOS 16.0+

## Pasos de Instalación

1. **Crear Proyecto:** Inicie un nuevo proyecto en Xcode seleccionando "Augmented Reality App".
2. **Importar Núcleo:** Arrastre el archivo `ARFusionCore.swift` a la carpeta principal del proyecto.
3. **Permisos (Info.plist):**
    Añada las siguientes claves para acceder a los sensores:
    ```xml
    <key>NSCameraUsageDescription</key>
    <string>Acceso requerido para análisis de espectro visual y térmico.</string>
    <key>NSMicrophoneUsageDescription</key>
    <string>Acceso requerido para análisis DSP de audio ambiente.</string>
    <key>NSLocationWhenInUseUsageDescription</key>
    <string>Requerido para el registro criptográfico de eventos.</string>
    ```

## Uso del API
En su `ViewController` o `ContentView` (SwiftUI):

```swift
let fusionCore = ARFusionCore()
fusionCore.isScanning = true

// Escuchar notificaciones de anomalías
// El núcleo imprime logs en consola: [ALERTA TÁCTICA]...
```

## Notas de Rendimiento
- El procesamiento de `ARFrame` se realiza en un hilo secundario (`.userInitiated`) para no bloquear la UI de 60fps.
- La simulación térmica y de audio en `ARFusionCore.swift` debe reemplazarse por los SDKs reales de FLIR y vDSP en producción.

---
*SPECTRAL-01 Mobile Architecture*
