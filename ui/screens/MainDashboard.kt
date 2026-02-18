package com.spectral.ghost.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectral.ghost.ui.theme.SpectralColors
import com.spectral.ghost.ui.viewmodels.SpectralViewModel
import kotlinx.coroutines.delay
import kotlin.random.Random

@Composable
fun MainDashboard(viewModel: SpectralViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val calibrationProgress by viewModel.calibrationProgress.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent) // IMPORTANT: Camera Preview is behind this
    ) {
        // 1. ANOMALY GLITCH OVERLAY (Conditional)
        if (uiState.fusionConfidence > 0.8f) {
            GlitchOverlay()
        }

        // 2. HUD LAYOUT
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // HEADER
            HUDHeader(uiState.systemStatus)

            Spacer(modifier = Modifier.height(16.dp))

            Row(modifier = Modifier.weight(1f)) {
                // LEFT PANEL (Narrower 15%)
                Column(
                    modifier = Modifier
                        .weight(0.15f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(SpectralColors.SurfaceTransparent, Color.Transparent)
                            )
                        )
                        .padding(4.dp)
                ) {
                    HUDLabel("AUDIO (Hz)")
                    Spacer(modifier = Modifier.height(8.dp))
                    HUDValue(String.format("%.1f", uiState.audioEnergy * 100))
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    HUDLabel("EMF (µT)")
                    Spacer(modifier = Modifier.height(8.dp))
                    HUDValue(String.format("%.2f", uiState.thermalVariance))
                }

                // CENTER VIEWPORT (Clear path for AR)
                Box(modifier = Modifier.weight(0.7f).fillMaxHeight()) {
                     if (!uiState.isCalibrated) {
                         CalibrationOverlay(calibrationProgress)
                     }
                }

                // RIGHT PANEL (Narrower 15%)
                Column(
                    modifier = Modifier
                        .weight(0.15f)
                        .fillMaxHeight()
                        .background(
                            brush = Brush.verticalGradient(
                                listOf(SpectralColors.SurfaceTransparent, Color.Transparent)
                            )
                        )
                        .padding(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    HUDLabel("CONFIDENCE")
                    HUDValue("${(uiState.fusionConfidence * 100).toInt()}%", 
                        if (uiState.fusionConfidence > 0.8f) SpectralColors.Red else SpectralColors.Cyan)
                }
            }
            
            // BOTTOM STATUS
            Text(
                "LAT: 40.4168 | LON: -3.7038", // Placeholder GPS
                color = Color.Gray,
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
        }
    }
}

@Composable
fun GlitchOverlay() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        
        // Random Chromatic Aberration Lines
        for (i in 0..5) {
            val y = Random.nextFloat() * height
            drawLine(
                color = SpectralColors.Cyan.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = Random.nextFloat() * 30f
            )
            drawLine(
                color = SpectralColors.Red.copy(alpha = 0.3f),
                start = Offset(0f, y + 10),
                end = Offset(width, y + 10),
                strokeWidth = Random.nextFloat() * 20f
            )
        }
    }
}

@Composable
fun CalibrationOverlay(progress: Float) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "LEARNING NOISE PATTERNS...",
                color = SpectralColors.Cyan,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.width(200.dp),
                color = SpectralColors.Cyan,
                trackColor = SpectralColors.SurfaceTransparent
            )
            Text(
                "${(progress * 100).toInt()}%",
                color = SpectralColors.Cyan,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun HUDHeader(status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SpectralColors.SurfaceTransparent)
            .padding(8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("SPECTRAL-01 // v1.0", color = SpectralColors.Cyan, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
        Text(status, color = if (status.contains("ANOMALY")) SpectralColors.Red else SpectralColors.Green, fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
    }
}

@Composable
fun HUDLabel(text: String) {
    Text(text, color = Color.Gray, fontSize = 9.sp, fontFamily = FontFamily.Monospace)
}

@Composable
fun HUDValue(text: String, color: Color = SpectralColors.Cyan) {
    Text(text, color = color, fontSize = 20.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
}
