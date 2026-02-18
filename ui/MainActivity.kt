
package com.spectral.ghost.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import com.spectral.ghost.ui.theme.ColorBackground
import com.spectral.ghost.ui.theme.ColorCyan
import com.spectral.ghost.ui.theme.ColorOrange
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.spectral.ghost.ui.screens.SplashScreen
import com.spectral.ghost.ui.screens.MainDashboard
import com.spectral.ghost.ui.screens.OnboardingScreen
import com.spectral.ghost.ui.viewmodels.SpectralViewModel
import com.spectral.ghost.ui.viewmodels.SpectralUiState

// ... (Theme definitions remain same) ...

class MainActivity : ComponentActivity() {
    private val viewModel: SpectralViewModel by viewModels()

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                // Permissions granted, proceed to normal flow
            } else {
                // Handle denial: Show rationale or exit
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = androidx.core.splashscreen.SplashScreen.installSplashScreen()
        super.onCreate(savedInstanceState)
        
        checkPermissions()
        
        setContent {
            SpectralTheme {
                var showSplash by remember { mutableStateOf(true) }
                var showOnboarding by remember { mutableStateOf(true) }
                
                if (showSplash) {
                    SplashScreen(onBootComplete = { showSplash = false })
                } else if (showOnboarding) {
                    OnboardingScreen(onOnboardingComplete = { showOnboarding = false })
                } else {
                    MainDashboard(viewModel)
                }
            }
        }
    }

    private fun checkPermissions() {
        val requiredPermissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        
        val allGranted = requiredPermissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
        
        if (!allGranted) {
            requestPermissionLauncher.launch(requiredPermissions)
        }
    }
}

@Composable
fun SpectralTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = ColorBackground,
            primary = ColorCyan,
            secondary = ColorOrange
        ),
        content = content
    )
}

@Composable
fun SpectralDashboard(viewModel: SpectralViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize().background(ColorBackground)) {
        // 1. AR VIEWPORT (LAYER 0)
        ARViewport(modifier = Modifier.fillMaxSize())

        // 2. HUD OVERLAY (LAYER 1)
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP BAR
            TopStatusBar(uiState)

            Row(modifier = Modifier.weight(1f).fillMaxWidth()) {
                // LEFT TELEMETRY
                LeftTelemetryPanel(uiState, modifier = Modifier.weight(0.2f).fillMaxHeight())
                
                // CENTER FOCUS
                Box(modifier = Modifier.weight(0.6f)) {
                    // Reticle
                    TargetReticle()
                }

                // RIGHT ANALYSIS
                RightAnalysisPanel(uiState, modifier = Modifier.weight(0.2f).fillMaxHeight())
            }

            // BOTTOM CONTROLS
            ControlLayer(
                onScanToggle = { viewModel.toggleScan() },
                onExport = { viewModel.exportEvidence() },
                isScanning = uiState.isScanning
            )
        }
    }
}

// MARK: - COMPOSABLES

@Composable
fun ARViewport(modifier: Modifier = Modifier) {
    // Placeholder for ARSurface / GLSurfaceView
    // In production, this hosts the Camera2/ARCore stream
    Box(modifier = modifier.background(Color.Transparent)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.2f)) // Dimming for HUD contrast
            // Wireframe Grid Simulation
            val step = 100f
            for (x in 0..size.width.toInt() step step.toInt()) {
                drawLine(
                    color = ColorCyan.copy(alpha = 0.1f),
                    start = Offset(x.toFloat(), 0f),
                    end = Offset(x.toFloat(), size.height),
                    strokeWidth = 1f
                )
            }
        }
    }
}

@Composable
fun TopStatusBar(state: SpectralUiState) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .background(Color.Black.copy(alpha = 0.8f))
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("SPECTRAL-01 // MIL-SPEC", color = ColorCyan, fontFamily = FontFamily.Monospace)
        Text(
            text = state.systemStatus,
            color = if (state.fusionConfidence > 0.8f) ColorAlert else Color.Gray,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun LeftTelemetryPanel(state: SpectralUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.5f))) {
        TelemetryWidget("EMF SCANNER", state.thermalVariance, ColorOrange)
        Spacer(modifier = Modifier.height(10.dp))
        TelemetryWidget("ENTROPY", state.audioEnergy, ColorCyan)
        Spacer(modifier = Modifier.height(20.dp))
        
        Text("CONFIDENCE", color = Color.Gray, fontSize = 10.sp)
        Text(
            text = "${(state.fusionConfidence * 100).toInt()}%",
            color = if (state.fusionConfidence > 0.8f) ColorAlert else ColorCyan,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun RightAnalysisPanel(state: SpectralUiState, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(8.dp).background(Color.Black.copy(alpha = 0.5f))) {
        Text("WATERFALL", color = Color.Gray, fontSize = 10.sp)
        // Simulated Waterfall
        Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
            val brush = Brush.verticalGradient(listOf(Color.Transparent, ColorCyan))
            drawRect(brush = brush)
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Text("SoC TEMP", color = Color.Gray, fontSize = 10.sp)
        Text("${state.cpuTemp.toInt()}°C", color = ColorOrange, fontSize = 18.sp)
    }
}

@Composable
fun ControlLayer(onScanToggle: () -> Unit, onExport: () -> Unit, isScanning: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 20.dp)
            .height(80.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Button(
            onClick = onScanToggle,
            colors = ButtonDefaults.buttonColors(containerColor = if (isScanning) ColorAlert else ColorCyan)
        ) {
            Text(if (isScanning) "ABORT SCAN" else "ENGAGE SENSORS", color = Color.Black)
        }
        
        Button(
            onClick = onExport,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            border = androidx.compose.foundation.BorderStroke(1.dp, ColorCyan)
        ) {
            Text("EXPORT EVIDENCE", color = ColorCyan)
        }
    }
}

@Composable
fun TelemetryWidget(label: String, value: Float, color: Color) {
    Column {
        Text(label, color = Color.Gray, fontSize = 8.sp, fontFamily = FontFamily.Monospace)
        Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
            drawRect(color = Color.Gray.copy(alpha = 0.3f))
            drawRect(color = color, size = size.copy(width = size.width * value))
        }
        Text(String.format("%.3f", value), color = Color.White, fontSize = 12.sp)
    }
}

@Composable
fun TargetReticle() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val cx = size.width / 2
        val cy = size.height / 2
        val radius = 50.dp.toPx()
        
        drawCircle(color = ColorCyan.copy(alpha = 0.5f), radius = radius, style = Stroke(width = 2f))
        drawLine(color = ColorCyan, start = Offset(cx - radius - 10, cy), end = Offset(cx + radius + 10, cy), strokeWidth = 1f)
        drawLine(color = ColorCyan, start = Offset(cx, cy - radius - 10), end = Offset(cx, cy + radius + 10), strokeWidth = 1f)
    }
}
