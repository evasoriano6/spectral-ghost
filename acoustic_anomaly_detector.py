
import numpy as np
import scipy.signal as signal
import scipy.fftpack as fft
import json
import time

# --- CONFIGURACIÓN DE AUDIO CIENTÍFICO ---
SAMPLE_RATE = 192000       # 192 kHz (Alta Fidelidad para Ultrasonidos)
BUFFER_SIZE = 4096         # Tamaño de ventana para análisis FFT
NOISE_LEARNING_FRAMES = 50 # Frames para perfil de ruido
INFRASOUND_LIMIT = 20      # Hz
ULTRASOUND_LIMIT = 20000   # Hz

class AcousticAnomalyDetector:
    def __init__(self):
        print(f"[SYS] INICIALIZANDO DSP ACÚSTICO | SR: {SAMPLE_RATE}Hz")
        self.noise_profile = None
        self.noise_frames_count = 0
        self.is_calibrated = False
        
    def simulate_audio_stream(self):
        """
        Genera un buffer de audio raw (float32).
        Simula ruido rosa ambiental + inyecciones de EVP ultrasónicas.
        """
        # 1. Ruido Rosa (Fondo)
        # Aproximación simple 1/f
        white = np.random.normal(0, 0.1, BUFFER_SIZE)
        b, a = signal.butter(1, 0.04) # Lowpass suave
        pink = signal.lfilter(b, a, white)
        
        stream = pink
        
        # 2. Inyección de ANOMALÍA (EVP Ultrasónico)
        # Señal modulada en 28kHz (Inaudible)
        if np.random.random() > 0.85:
            t = np.linspace(0, BUFFER_SIZE/SAMPLE_RATE, BUFFER_SIZE)
            carrier = np.sin(2 * np.pi * 28000 * t) # Portadora 28kHz
            modulator = np.sin(2 * np.pi * 50 * t)  # Modulación inteligente 50Hz
            evp_signal = carrier * modulator * 0.5
            stream += evp_signal
            
        return stream.astype(np.float32)

    def spectral_subtraction(self, audio_buffer):
        """
        Eliminación adaptativa de ruido estacionario.
        """
        # FFT
        spectrum = fft.fft(audio_buffer)
        magnitude = np.abs(spectrum)
        phase = np.angle(spectrum)
        
        # Aprendizaje de perfil de ruido (primeros frames)
        if self.noise_frames_count < NOISE_LEARNING_FRAMES:
            if self.noise_profile is None:
                self.noise_profile = magnitude
            else:
                self.noise_profile = 0.9 * self.noise_profile + 0.1 * magnitude
            self.noise_frames_count += 1
            return audio_buffer # Retornar original mientras calibra
        
        self.is_calibrated = True
        
        # Sustracción espectral
        # Mag_clean = Mag_raw - Noise_profile
        clean_magnitude = np.maximum(magnitude - (self.noise_profile * 1.5), 0.0)
        
        # Reconstrucción (IFFT)
        clean_spectrum = clean_magnitude * np.exp(1j * phase)
        clean_audio = np.real(fft.ifft(clean_spectrum))
        
        return clean_audio

    def analyze_modulation(self, audio_buffer):
        """
        Detecta patrones inteligentes mediante Entropía de Shannon.
        Baja entropía = Señal ordenada (posible voz/inteligencia).
        """
        # Normalizar distribución de energía
        energy = np.abs(audio_buffer)**2
        energy_sum = np.sum(energy)
        if energy_sum == 0: return 1.0 # Entropía máxima (incertidumbre)
        
        prob_dist = energy / energy_sum
        
        # Entropía de Shannon: H = -sum(p * log2(p))
        # Evitar log(0) con máscara
        mask = prob_dist > 0
        entropy = -np.sum(prob_dist[mask] * np.log2(prob_dist[mask]))
        
        # Normalizar entropía (0-1)
        max_entropy = np.log2(BUFFER_SIZE)
        normalized_entropy = entropy / max_entropy
        
        return normalized_entropy

    def isolate_evp(self, audio_buffer):
        """
        Pitch Shifting para hacer audible el ultrasonido.
        Toma banda alta (>20kHz) y la mezcla hacia abajo.
        """
        # Filtro Pasa-Altos (>20kHz)
        nyquist = SAMPLE_RATE / 2
        b, a = signal.butter(4, ULTRASOUND_LIMIT / nyquist, btype='high')
        ultrasound = signal.lfilter(b, a, audio_buffer)
        
        # Si hay energía significativa en ultrasonido...
        if np.max(np.abs(ultrasound)) > 0.05:
            # Downsampling simple (diezmar) para bajar tono
            # Factor 4: 28kHz -> 7kHz (Audible)
            shifted = signal.resample(ultrasound, len(ultrasound)//4)
            return True, np.mean(np.abs(shifted))
        
        return False, 0.0

    def run(self):
        print("[SYS] ESCUCHANDO EN ESPECTRO COMPLETO (1Hz - 96kHz)...")
        try:
            while True:
                # 1. Ingesta
                raw_audio = self.simulate_audio_stream()
                
                # 2. Limpieza
                clean_audio = self.spectral_subtraction(raw_audio)
                
                if not self.is_calibrated:
                    print(f"\r[CALIBRANDO] Perfil de ruido... {self.noise_frames_count}/{NOISE_LEARNING_FRAMES}", end="")
                    continue
                
                # 3. Análisis Inteligente
                entropy = self.analyze_modulation(clean_audio)
                has_evp, evp_energy = self.isolate_evp(clean_audio)
                
                # Clasificación
                is_anomaly = entropy < 0.85 or has_evp
                
                if is_anomaly:
                    output = {
                        "timestamp": time.time(),
                        "sensor": "MEMS_ARRAY_SIMULATED",
                        "metrics": {
                            "shannon_entropy": round(entropy, 3),
                            "ultrasound_energy": round(evp_energy, 4),
                            "evp_detected": bool(has_evp)
                        },
                        "alert": "INTELLIGENT_SIGNAL_PATTERN" if entropy < 0.8 else "ULTRASONIC_BURST"
                    }
                    print(json.dumps(output))
                
                time.sleep(1.0 / (SAMPLE_RATE/BUFFER_SIZE)) # Sincronizar con tiempo real
                
        except KeyboardInterrupt:
            print("\n[SYS] DETENIENDO ANÁLISIS ACÚSTICO...")

if __name__ == "__main__":
    detector = AcousticAnomalyDetector()
    detector.run()
