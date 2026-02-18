package com.spectral.ghost.ui.screens

import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onBootComplete: () -> Unit) {
    val context = LocalContext.current
    var bootLogs by remember { mutableStateOf(listOf<String>()) }
    var isLidarActive by remember { mutableStateOf(false) }

    // Radar Pulse Animation
    val infiniteTransition = rememberInfiniteTransition(label = "RadarLoop")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "PulseValue"
    )

    // Log Sequence Logic
    LaunchedEffect(Unit) {
        val sequence = listOf(
            "[SYS] KERNEL_VULKAN_INIT: OK",
            "[SYS] HW_HANDSHAKE: SENSOR_ARRAY_DETECTED",
            "[AUDIO] SAMPLING_RATE: 96000Hz (ULTRA-HIGH)",
            "[LIDAR] DEPTH_GRID_INITIALIZING... [WAIT]",
            "[CAL] SENSOR_FUSION: SYNCING XYZ_VECTORS...",
            "[READY] SPECTRAL-01: SYSTEM NOMINAL"
        )

        // Hardware Check (Simulate concurrent check)
        val hasLidar = context.packageManager.hasSystemFeature("android.hardware.sensor.lidar")
        isLidarActive = hasLidar
        
        sequence.forEachIndexed { index, log ->
            if (log.contains("[LIDAR]")) {
                val lidarStatus = if (hasLidar) "[OK]" else "[VIRTUAL_DEPTH: ENABLED]"
                bootLogs = bootLogs + log.replace("[WAIT]", lidarStatus)
            } else {
                bootLogs = bootLogs + log
            }
            delay(200) // Progressive delay
        }

        delay(500) // Hold "SYSTEM NOMINAL" briefly
        onBootComplete()
    }

    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000)), // Pure Black OLED
        contentAlignment = Alignment.Center
    ) {
        // RADAR VISUALIZATION
        Canvas(modifier = Modifier.size(300.dp)) {
            val center = Offset(size.width / 2, size.height / 2)
            val maxRadius = size.minDimension / 2
            
            // Static Grid
            drawCircle(Color.DarkGray, maxRadius, style = Stroke(1.dp.toPx()))
            drawCircle(Color.DarkGray, maxRadius * 0.66f, style = Stroke(1.dp.toPx()))
            drawCircle(Color.DarkGray, maxRadius * 0.33f, style = Stroke(1.dp.toPx()))
            drawLine(Color.DarkGray, Offset(center.x, 0f), Offset(center.x, size.height), 1.dp.toPx())
            drawLine(Color.DarkGray, Offset(0f, center.y), Offset(size.width, center.y), 1.dp.toPx())
            
            // Active Pulse
            drawCircle(
                color = Color(0xFF00F0FF).copy(alpha = 1f - pulseRadius),
                radius = maxRadius * pulseRadius,
                style = Stroke(width = 3.dp.toPx())
            )
        }

        // LOGGING TERMINAL
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(32.dp)
                .fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier.height(200.dp),
                reverseLayout = true
            ) {
                items(bootLogs.reversed()) { log ->
                    Text(
                        text = log,
                        color = if (log.contains("NOMINAL")) Color.Green else Color(0xFF00F0FF), // Cyan
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
        }
    }
}
