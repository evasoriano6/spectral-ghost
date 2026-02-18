
import numpy as np
import open3d as o3d
import time
import json
import sys

# --- CONFIGURACIÓN INDUSTRIAL ---
VOXEL_SIZE = 0.05          # 5cm para reducción de ruido inicial
RANSAC_DISTANCE = 0.02     # Umbral de distancia para el plano del suelo
DBSCAN_EPS = 0.3           # 30cm de radio de búsqueda para clustering
DBSCAN_MIN_POINTS = 50     # Mínimo de puntos para considerar una entidad válida
ACQUISITION_RATE = 30      # Hz

class LidarAnomalyDetector:
    def __init__(self):
        print("[SYS] INICIALIZANDO CORE LIDAR...")
        self.reference_plane = None
        self.is_calibrated = False
        
    def simulate_acquisition(self):
        """
        Simula la entrada de un sensor LiDAR/ToF.
        Genera niebla de ruido + plano de suelo + entidades anómalas ocasionales.
        """
        # 1. Generar suelo (Plano XY)
        xx, yy = np.meshgrid(np.linspace(-3, 3, 100), np.linspace(-3, 3, 100))
        zz = np.zeros_like(xx) + np.random.normal(0, 0.01, xx.shape) # Ruido de sensor
        floor_points = np.stack([xx.flatten(), yy.flatten(), zz.flatten()], axis=1)
        
        # 2. Generar ruido ambiental disperso (polvo/electrónica)
        noise_points = np.random.uniform(-3, 3, (200, 3))
        noise_points[:, 2] = np.random.uniform(0, 2, 200) # Altura random
        
        # 3. Generar ENTIDAD ANÓMALA (Ocasional)
        if np.random.random() > 0.7:
            # Esfera densa flotando a 1.5m
            theta = np.random.uniform(0, 2*np.pi, 300)
            phi = np.random.uniform(0, np.pi, 300)
            r = 0.3 # 30cm radio
            
            ex = r * np.sin(phi) * np.cos(theta) + np.random.normal(0, 0.02, 300)
            ey = r * np.sin(phi) * np.sin(theta) + np.random.normal(0, 0.02, 300)
            ez = r * np.cos(phi) + 1.5 + np.random.normal(0, 0.02, 300)
            
            entity_points = np.stack([ex, ey, ez], axis=1)
            all_points = np.vstack([floor_points, noise_points, entity_points])
        else:
            all_points = np.vstack([floor_points, noise_points])
            
        pcd = o3d.geometry.PointCloud()
        pcd.points = o3d.utility.Vector3dVector(all_points)
        return pcd

    def process_frame(self, pcd):
        """
        Pipeline de procesamiento principal.
        """
        start_time = time.time()
        
        # 1. Downsampling (Optimización)
        pcd_down = pcd.voxel_down_sample(voxel_size=VOXEL_SIZE)
        
        # 2. Segmentación de Fondo (RANSAC)
        # Asumimos que el plano más grande es el suelo/paredes
        plane_model, inliers = pcd_down.segment_plane(distance_threshold=RANSAC_DISTANCE,
                                                     ransac_n=3,
                                                     num_iterations=1000)
        
        # Separar inliers (fondo) de outliers (posibles entidades)
        inlier_cloud = pcd_down.select_by_index(inliers)
        outlier_cloud = pcd_down.select_by_index(inliers, invert=True)
        
        # 3. Clustering de Anomalías (DBSCAN)
        # Agrupamos los puntos restantes para ver si forman estructuras coherentes
        with o3d.utility.VerbosityContextManager(o3d.utility.VerbosityLevel.Debug) as cm:
            labels = np.array(outlier_cloud.cluster_dbscan(eps=DBSCAN_EPS, 
                                                          min_points=DBSCAN_MIN_POINTS, 
                                                          print_progress=False))

        max_label = labels.max()
        anomalies = []
        
        if max_label >= 0:
            # Hay clusters detectados
            print(f"[DETECCION] {max_label + 1} entidades potenciales identificadas.")
            
            for i in range(max_label + 1):
                # Extraer puntos del cluster
                cluster_indices = np.where(labels == i)[0]
                cluster_pcd = outlier_cloud.select_by_index(cluster_indices)
                points = np.asarray(cluster_pcd.points)
                
                # Calcular Métricas Físicas
                center = cluster_pcd.get_center()
                bbox = cluster_pcd.get_axis_aligned_bounding_box()
                extent = bbox.get_extent()
                volume_cm3 = (extent[0] * extent[1] * extent[2]) * 1e6
                
                # Densidad Relativa (Mean Point Spacing aproximado)
                # En un sensor real, comparamos puntos esperados vs recibidos
                density = len(points) / (volume_cm3 / 1e6 + 1e-6) # pts/m3
                
                # Inconsistencia Óptica (Simulada)
                # Si la densidad es alta pero no hay objeto visual...
                confidence = min(0.99, (len(points) / 300) * 0.8 + 0.1) 
                
                anomaly_data = {
                    "id": i,
                    "centroid": {"x": round(center[0], 2), "y": round(center[1], 2), "z": round(center[2], 2)},
                    "volume_cm3": round(volume_cm3, 2),
                    "density_score": round(density, 2),
                    "confidence": round(confidence, 2),
                    "type": "UNKNOWN_MASS" if confidence > 0.7 else "NOISE_CLUSTER"
                }
                anomalies.append(anomaly_data)
        
        process_time = (time.time() - start_time) * 1000
        return anomalies, process_time

    def run(self):
        print("[SYS] SISTEMA ACTIVO. ESCANEANDO...")
        try:
            while True:
                # Simular frame
                pcd_frame = self.simulate_acquisition()
                
                # Procesar
                results, latentcy = self.process_frame(pcd_frame)
                
                # Salida JSON
                output = {
                    "timestamp": time.time(),
                    "latency_ms": round(latentcy, 1),
                    "anomalies": results
                }
                
                if results:
                    print(json.dumps(output, indent=2))
                
                time.sleep(1/ACQUISITION_RATE)
                
        except KeyboardInterrupt:
            print("[SYS] FINALIZANDO...")

if __name__ == "__main__":
    detector = LidarAnomalyDetector()
    detector.run()
