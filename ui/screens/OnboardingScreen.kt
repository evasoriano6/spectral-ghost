
package com.spectral.ghost.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.spectral.ghost.ColorBackground
import com.spectral.ghost.ColorCyan
import com.spectral.ghost.ColorOrange
import com.spectral.ghost.ColorAlert
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ColorBackground)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            OnboardingPage(page = page, onComplete = onOnboardingComplete)
        }

        // Pager Indicators
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(4) { iteration ->
                val color = if (pagerState.currentPage == iteration) ColorCyan else Color.Gray
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .size(8.dp)
                        .background(color)
                )
            }
        }
    }
}

@Composable
fun OnboardingPage(page: Int, onComplete: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        when (page) {
            0 -> NoiseFloorValidationStep()
            1 -> SpatialMappingStep()
            2 -> LiabilityWarningStep()
            3 -> FinalBriefingStep(onComplete)
        }
    }
}

@Composable
fun NoiseFloorValidationStep() {
    var progress by remember { mutableStateOf(0f) }
    
    LaunchedEffect(Unit) {
        while (progress < 1f) {
            delay(50)
            progress += 0.01f
        }
    }

    Text("NOISE FLOOR VALIDATION", color = ColorCyan, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(8.dp))
    Text("Keep device stationary for calibration.", color = Color.Gray, fontSize = 12.sp)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Simulated FFT Graph
    Canvas(modifier = Modifier.fillMaxWidth().height(200.dp).border(1.dp, ColorCyan.copy(alpha=0.3f))) {
        val points = FloatArray(50) { Random.nextFloat() * size.height }
        val path = Path().apply {
            moveTo(0f, size.height)
            points.forEachIndexed { i, y ->
                val x = (i.toFloat() / points.size) * size.width
                lineTo(x, size.height - y)
            }
            lineTo(size.width, size.height)
        }
        drawPath(path, color = ColorCyan, style = Stroke(width = 2f))
        
        // Progress Line
        drawLine(
            color = ColorOrange,
            start = Offset(0f, size.height),
            end = Offset(size.width * progress, size.height),
            strokeWidth = 4f
        )
    }
    
    Spacer(modifier = Modifier.height(16.dp))
    Text(if (progress < 1f) "CALIBRATING: ${(progress * 100).toInt()}%" else "BASELINE ACQUIRED", color = ColorOrange)
}

@Composable
fun SpatialMappingStep() {
    Text("SPATIAL MAPPING", color = ColorCyan, fontSize = 18.sp, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(16.dp))
    Text("Scan room in slow circular motion.", color = Color.Gray, fontSize = 12.sp)
    
    Spacer(modifier = Modifier.height(32.dp))
    
    // Abstract Wireframe Sphere
    Canvas(modifier = Modifier.size(200.dp)) {
        drawCircle(ColorCyan, style = Stroke(width = 1f), radius = size.minDimension/2)
        drawLine(ColorCyan, Offset(0f, size.height/2), Offset(size.width, size.height/2))
        drawLine(ColorCyan, Offset(size.width/2, 0f), Offset(size.width/2, size.height))
    }
    
    Spacer(modifier = Modifier.height(32.dp))
    Text("BUILDING DIGITAL TWIN...", color = Color.Green, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
}

@Composable
fun LiabilityWarningStep() {
    Text("WARNING: SCIENTIFIC LIABILITY", color = ColorAlert, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    Spacer(modifier = Modifier.height(24.dp))
    
    Text(
        "SPECTRAL-01 detects real physical variations (Thermal, EMF, Acoustic).", 
        color = Color.White, textAlign = TextAlign.Center
    )
    Spacer(modifier = Modifier.height(16.dp))
    Text(
        "Not all anomalies are unknown. Verify electrical interference and drafts using the telemetry panels.", 
        color = Color.Gray, textAlign = TextAlign.Center
    )
    
    Spacer(modifier = Modifier.height(32.dp))
    Text("FALSE POSITIVES FILTER: ACTIVE", color = ColorOrange, fontFamily = FontFamily.Monospace)
}

@Composable
fun FinalBriefingStep(onComplete: () -> Unit) {
    Text("SYSTEM READY", color = ColorCyan, fontSize = 24.sp, fontFamily = FontFamily.Monospace)
    Spacer(modifier = Modifier.height(32.dp))
    
    Column(horizontalAlignment = Alignment.Start) {
        Text("► CONFIDENCE METER: Requires >80% for confirmation.", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("► EVIDENCE: Crypto-signed SHA-256 logs.", color = Color.Gray)
        Spacer(modifier = Modifier.height(8.dp))
        Text("► HAPTICS: Ti-Ti vibration = Anomaly.", color = Color.Gray)
    }
    
    Spacer(modifier = Modifier.height(48.dp))
    
    Button(
        onClick = onComplete,
        colors = ButtonDefaults.buttonColors(containerColor = ColorCyan),
        modifier = Modifier.fillMaxWidth().height(56.dp)
    ) {
        Text("INITIALIZE KERNEL", color = Color.Black, fontWeight = FontWeight.Bold)
    }
}
