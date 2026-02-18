
#version 450
precision highp float;

// INPUTS FROM VERTEX SHADER
layout(location = 0) in vec2 v_UV;
layout(location = 1) in vec3 v_WorldPos;
layout(location = 2) in vec3 v_ViewDir; // Vector del ojo al fragmento

// OUTPUT
layout(location = 0) out vec4 outColor;

// UNIFORMS
layout(binding = 0) uniform sampler2D u_CameraTexture; // Feed de cámara RGB
layout(binding = 1) uniform UBO {
    float u_Time;           // Tiempo en segundos
    vec3  u_AnomalyCenter;  // Centro de la anomalía en World Space
    float u_AnomalyRadius;  // Radio de efecto
    float u_Intensity;      // Nivel de confianza (0.0 - 1.0)
    vec2  u_ScreenRes;      // Resolución de pantalla
} params;

// --- UTILS: GRADIENT NOISE (Simplex-like) ---
vec3 hash(vec3 p) {
    p = vec3(dot(p, vec3(127.1, 311.7, 74.7)),
             dot(p, vec3(269.5, 183.3, 246.1)),
             dot(p, vec3(113.5, 271.9, 124.6)));
    return -1.0 + 2.0 * fract(sin(p) * 43758.5453123);
}

float noise(vec3 p) {
    vec3 i = floor(p);
    vec3 f = fract(p);
    vec3 u = f * f * (3.0 - 2.0 * f);
    
    return mix(mix(mix(dot(hash(i + vec3(0.0, 0.0, 0.0)), f - vec3(0.0, 0.0, 0.0)),
                       dot(hash(i + vec3(1.0, 0.0, 0.0)), f - vec3(1.0, 0.0, 0.0)), u.x),
                   mix(dot(hash(i + vec3(0.0, 1.0, 0.0)), f - vec3(0.0, 1.0, 0.0)),
                       dot(hash(i + vec3(1.0, 1.0, 0.0)), f - vec3(1.0, 1.0, 0.0)), u.x), u.y),
               mix(mix(dot(hash(i + vec3(0.0, 0.0, 1.0)), f - vec3(0.0, 0.0, 1.0)),
                       dot(hash(i + vec3(1.0, 0.0, 1.0)), f - vec3(1.0, 0.0, 1.0)), u.x),
                   mix(dot(hash(i + vec3(0.0, 1.0, 1.0)), f - vec3(0.0, 1.0, 1.0)),
                       dot(hash(i + vec3(1.0, 1.0, 1.0)), f - vec3(1.0, 1.0, 1.0)), u.x), u.y), u.z);
}

// --- MAIN SHADER LOGIC ---
void main() {
    float distToCenter = distance(v_WorldPos, params.u_AnomalyCenter);
    
    // 1. MASKING: Solo renderizar dentro de la esfera de influencia
    float mask = 1.0 - smoothstep(params.u_AnomalyRadius * 0.8, params.u_AnomalyRadius, distToCenter);
    if (mask <= 0.01) discard; // Optimización (Early Z)

    // 2. LENS REFRACTION (Distorsión de calor)
    // Coordenadas de pantalla para muestrear la cámara
    vec2 screenUV = gl_FragCoord.xy / params.u_ScreenRes;
    
    // Generar ruido de turbulencia
    float turbulence = noise(v_WorldPos * 2.0 + vec3(0.0, params.u_Time * 1.5, 0.0));
    vec2 aberration = vec2(turbulence) * 0.02 * params.u_Intensity * mask;
    
    // Muestrear cámara con "Aberración Cromática" (RGB Split)
    float r = texture(u_CameraTexture, screenUV + aberration).r;
    float g = texture(u_CameraTexture, screenUV + aberration * 1.5).g; // Más offset en G
    float b = texture(u_CameraTexture, screenUV + aberration * 2.0).b; // Más offset en B
    vec3 cameraColor = vec3(r, g, b);

    // 3. DYNAMIC HEATMAP (Gradiente Espectral)
    // Mapear turbulencia a colores fríos (azul) o calientes (naranja/blanco)
    vec3 coldColor = vec3(0.0, 0.1, 0.3); // Deep Cyan
    vec3 hotColor = vec3(1.0, 0.4, 0.0);  // Radiometric Orange
    vec3 coreColor = vec3(1.0, 1.0, 1.0); // Punto cero
    
    vec3 heatmap = mix(coldColor, hotColor, smoothstep(-0.5, 0.5, turbulence));
    heatmap = mix(heatmap, coreColor, smoothstep(0.8, 1.0, turbulence));

    // 4. SCANLINE SWEEP (Instrumentación Militar)
    // Barrido vertical constante
    float scanline = sin(v_WorldPos.y * 30.0 - params.u_Time * 10.0);
    scanline = smoothstep(0.95, 1.0, scanline) * 0.8; // Línea fina y brillante
    
    // Rejilla estática (Grid)
    float grid = max(step(0.95, fract(v_WorldPos.x * 5.0)), step(0.95, fract(v_WorldPos.y * 5.0))) * 0.2;

    // 5. COMPOSICIÓN FINAL
    // Mezcla aditiva: Cámara Distorsionada + Heatmap + Gráficos
    vec3  finalRGB = cameraColor + (heatmap * 0.6 * mask * params.u_Intensity);
    finalRGB += vec3(0.0, 1.0, 1.0) * scanline * mask; // Scanline Cyan
    finalRGB += vec3(1.0, 1.0, 1.0) * grid * mask * 0.5;

    // Viñeta de borde suave para la burbuja
    float alpha = mask * (0.3 + 0.7 * params.u_Intensity);
    
    outColor = vec4(finalRGB, alpha);
}
