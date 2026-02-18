
import numpy as np
import cv2
import time
import json
from dataclasses import dataclass

# --- CONFIGURACIÓN TERMOGRÁFICA ---
TEMP_AMBIENT_AVG = 22.0      # °C (Referencia dinámica)
GRADIENT_THRESHOLD = 2.0     # °C (Caída mínima para considerar anomalía)
EMISSIVITY = 0.95            # Piel humana/Ropa
BOLTZMANN_CONST = 5.67e-8    # W/m^2*K^4
FRAME_RATE = 9               # Hz (Típico en cámaras térmicas de consumo)

@dataclass
class ThermalEntity:
    id: int
    centroid: tuple
    temp_avg: float
    area_px: int
    watts: float
    age: int = 0
    confidence: float = 0.0

class KalmanFilter2D:
    def __init__(self):
        # Estado [x, y, dx, dy]
        self.state = np.zeros(4, dtype=np.float32)
        self.transition = np.eye(4, dtype=np.float32)
        self.measurement = np.eye(2, 4, dtype=np.float32)
        self.cov = np.eye(4, dtype=np.float32) * 100
        
        # Física simple
        dt = 1.0 / FRAME_RATE
        self.transition[0, 2] = dt
        self.transition[1, 3] = dt

    def predict(self):
        self.state = np.dot(self.transition, self.state)
        self.cov = np.dot(np.dot(self.transition, self.cov), self.transition.T) + np.eye(4) * 0.1
        return self.state[:2]

    def update(self, measurement):
        m = np.array(measurement, dtype=np.float32)
        y = m - np.dot(self.measurement, self.state)
        s = np.dot(np.dot(self.measurement, self.cov), self.measurement.T) + np.eye(2) * 1.0
        k = np.dot(np.dot(self.cov, self.measurement.T), np.linalg.inv(s))
        self.state = self.state + np.dot(k, y)
        self.cov = np.dot(np.eye(4) - np.dot(k, self.measurement), self.cov)

class ThermalAnomalyDetector:
    def __init__(self):
        print("[SYS] INICIALIZANDO NÚCLEO RADIOMÉTRICO (LWIR)...")
        self.active_tracks = {}
        self.next_id = 1
        
    def simulate_radiometric_frame(self, width=320, height=240):
        """
        Genera una matriz de temperatura (float32) raw.
        Simula ruido de sensor y una 'zona fría' móvil.
        """
        # Fondo: Ruido gaussiano alrededor de T_amb
        frame = np.random.normal(TEMP_AMBIENT_AVG, 0.5, (height, width)).astype(np.float32)
        
        # Simular GRADIENTES TÉRMICOS (Paredes, fuentes de calor)
        # ... (omitido para brevedad, enfocado en anomalía)

        # ANOMALÍA TÉRMICA ("Punto Frío")
        # Generamos una zona que es 5°C más fría que el ambiente
        if np.random.random() > 0.6:
            cx, cy = int(width/2 + np.sin(time.time())*50), int(height/2 + np.cos(time.time())*30)
            y, x = np.ogrid[-cy:height-cy, -cx:width-cx]
            mask = x*x + y*y <= 20*20 # Radio 20px
            frame[mask] -= 5.0 # Delta T negativo brutal
            
        return frame

    def process_frame(self, thermal_matrix):
        """
        Algoritmo principal de detección.
        """
        # 1. Detección de Gradientes Negativos (Region Growing simplificado con Threshold)
        # Buscamos píxeles significativamente más fríos que el promedio local/global
        global_avg = np.mean(thermal_matrix)
        cold_mask = (thermal_matrix < (global_avg - GRADIENT_THRESHOLD)).astype(np.uint8) * 255
        
        # 2. Segmentación Morfológica
        kernel = np.ones((3,3), np.uint8)
        cold_mask = cv2.morphologyEx(cold_mask, cv2.MORPH_OPEN, kernel) # Limpiar ruido
        
        contours, _ = cv2.findContours(cold_mask, cv2.RETR_EXTERNAL, cv2.CHAIN_APPROX_SIMPLE)
        
        current_detections = []
        
        for cnt in contours:
            area = cv2.contourArea(cnt)
            if area < 50: continue # Descartar ruido térmico pequeño
            
            M = cv2.moments(cnt)
            if M["m00"] == 0: continue
            cx = int(M["m10"] / M["m00"])
            cy = int(M["m01"] / M["m00"])
            
            # Obtener temperatura promedio de la región
            mask_single = np.zeros_like(cold_mask)
            cv2.drawContours(mask_single, [cnt], -1, 1, -1)
            mean_temp = cv2.mean(thermal_matrix, mask=mask_single)[0]
            
            # 3. Cálculo de Transferencia de Calor (Stefan-Boltzmann)
            # P = ε * σ * A * (T_amb^4 - T_obj^4)
            # Asumimos T_amb global y área aproximada en m^2 (suponiendo distancia 2m, FOV estándar)
            # Factor de escala pixel -> m2 (muy aproximado para demo)
            area_m2 = area * (0.005 * 0.005) 
            
            t_amb_k = global_avg + 273.15
            t_obj_k = mean_temp + 273.15
            
            watts = EMISSIVITY * BOLTZMANN_CONST * area_m2 * (t_amb_k**4 - t_obj_k**4)
            
            current_detections.append({
                "centroid": (cx, cy),
                "temp": mean_temp,
                "area": area,
                "watts": watts
            })
            
        return self.update_tracks(current_detections)

    def update_tracks(self, detections):
        """
        Asociación de datos y Filtrado de Kalman.
        """
        # Pasos simplificados de asociación por distancia Euclidiana
        # En prod: Algoritmo Húngaro (Munkres)
        
        tracking_results = []
        
        # Predicción de tracks existentes
        for tid, track in self.active_tracks.items():
            track['kf'].predict()
            track['matched'] = False
            
        # Matching
        for det in detections:
            best_id = -1
            best_dist = 50.0 # Pixel threshold
            
            for tid, track in self.active_tracks.items():
                pred_pos = track['kf'].state[:2]
                dist = np.linalg.norm(np.array(det['centroid']) - pred_pos)
                if dist < best_dist:
                    best_dist = dist
                    best_id = tid
            
            if best_id != -1:
                # Update track existente
                self.active_tracks[best_id]['kf'].update(det['centroid'])
                self.active_tracks[best_id]['age'] += 1
                self.active_tracks[best_id]['data'] = det
                self.active_tracks[best_id]['matched'] = True
            else:
                # Nuevo track
                new_kf = KalmanFilter2D()
                new_kf.state[:2] = det['centroid']
                self.active_tracks[self.next_id] = {
                    'kf': new_kf,
                    'age': 1,
                    'data': det,
                    'matched': True
                }
                self.next_id += 1
        
        # Limpieza y formateo
        active_ids = list(self.active_tracks.keys())
        for tid in active_ids:
            track = self.active_tracks[tid]
            if not track['matched']:
                # Perdió rastro (persistence decay)
                del self.active_tracks[tid]
                continue
                
            if track['age'] > 5: # Filtro de persistencia mínima
                # Clasificación de "Entidad Clase 1" si se mueve contra gradiente natural (simulado aqui por persistencia)
                ent_type = "CLASS_1_ENTITY" if track['data']['watts'] > 0.5 else "THERMAL_POCKET"
                
                tracking_results.append({
                    "id": tid,
                    "type": ent_type,
                    "metrics": {
                        "temp_avg_c": round(track['data']['temp'], 1),
                        "heat_absorption_w": round(track['data']['watts'], 4),
                        "position": track['data']['centroid']
                    }
                })
                
        return tracking_results

    def run(self):
        print(f"[SYS] ESCANEO TÉRMICO ACTIVO | REF: {TEMP_AMBIENT_AVG}°C | UMBRAL: -{GRADIENT_THRESHOLD}°C")
        try:
            while True:
                # 1. Ingesta
                frame = self.simulate_radiometric_frame()
                
                # 2. Procesamiento
                anomalies = self.process_frame(frame)
                
                # 3. Salida
                if anomalies:
                    output = {
                        "timestamp": time.time(),
                        "sensor": "LWIR_SIMULATED",
                        "detected_anomalies": anomalies
                    }
                    print(json.dumps(output))
                
                time.sleep(1.0 / FRAME_RATE)
                
        except KeyboardInterrupt:
            print("[SYS] APAGANDO SENSOR TÉRMICO...")

if __name__ == "__main__":
    detector = ThermalAnomalyDetector()
    detector.run()
