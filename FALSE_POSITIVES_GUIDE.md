# Guía de Filtrado Científico: Eliminación de Falsos Positivos

Para mantener el rigor de una aplicación de **Grado Militar**, es imperativo distinguir entre anomalías genuinas y artefactos ambientales. Esta guía detalla los filtros implementados en el `ARFusionCore`.

## 1. Fenómenos Visuales (LiDAR / RGB)

### Pareidolia Digital (Reconocimiento Erróneo)
- **Problema:** El cerebro humano (y las redes neuronales mal entrenadas) tienden a ver rostros en patrones aleatorios de luz y sombra.
- **Filtro ARFusion:**
    - **Persistencia Temporal:** Una anomalía debe mantenerse en la misma coordenada `(x,y,z)` por al menos **30 frames** (0.5s) para ser considerada real.
    - **Solidez Volumétrica:** El módulo LiDAR descarta nubes de puntos con una densidad < 50 puntos/$cm^3$. Si es transparente al láser, es humo o polvo.

### Reflejos Especulares (Espejos/Vidre)
- **Problema:** Los sensores LiDAR rebotan en espejos, creando "habitaciones fantasma" duplicadas.
- **Filtro ARFusion:**
    - **Análisis de Intensidad de Retorno:** Los rebotes en espejos tienen una firma de intensidad específica. Si `intensity < 0.1` en una superficie plana vertical, se marca como superficie especular y se ignora lo que haya "detrás".

## 2. Fenómenos Térmicos (LWIR)

### Reflejos Térmicos
- **Problema:** Los vidrios y metales pulidos reflejan el calor del cuerpo del operador, creando un "fantasma térmico" que se mueve con él.
- **Filtro ARFusion:**
    - **Vector de Movimiento:** Si la anomalía térmica se mueve en *perfecta sincronía* con el acelerómetro del dispositivo (vector de cámara), se etiqueta como `SELF_REFLECTION` y se descarta.

### Inercia Térmica (Huellas de Calor)
- **Problema:** Alguien se sentó en un sofá y se fue. El sofá sigue caliente.
- **Filtro ARFusion:**
    - **Decaimiento Logarítmico:** Las huellas de calor estáticas se enfrían predeciblemente. Una entidad real mantiene o *aumenta* su delta térmico, o se mueve. Si es estática y se enfría según la Ley de Newton, es una huella.

## 3. Fenómenos Acústicos (EVP/Ultrasonido)

### Ruido Eléctrico (EMF/GSM)
- **Problema:** Los cables de alta tensión y las señales 5G inducen zumbidos en los micrófonos (50Hz/60Hz y sus armónicos).
- **Filtro ARFusion:**
    - **Notch Filter Adaptativo:** El sistema escanea la red eléctrica local. Si detecta un pico estable en 50/60Hz, aplica un filtro de corte automático.
    - **Patrón de Ráfaga:** Las señales digitales (Wi-Fi/4G) tienen encabezados de paquete muy claros. Si el análisis de entropía detecta modulación QAM/PSK, se etiqueta como `INTERFERENCIA_RF`.

### Aliasing (Falsos Graves)
- **Problema:** Frecuencias ultrasónicas mal muestreadas aparecen como sonidos graves extraños.
- **Filtro ARFusion:**
    - **Filtro Anti-Aliasing (Hardware):** Se exige hardware de 192kHz. Si el dispositivo no lo soporta, el software corta digitalmente todo lo que supere Nyquist (`SR/2`) para evitar "fantasmas de audio".

---
*Este documento es parte del estándar operativo de SPECTRAL-01.*
