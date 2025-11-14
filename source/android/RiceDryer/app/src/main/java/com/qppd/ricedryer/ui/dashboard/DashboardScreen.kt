package com.qppd.ricedryer.ui.dashboard

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateToCharts: () -> Unit,
    onNavigateToDevices: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.deviceData?.deviceInfo?.deviceName ?: "Rice Dryer") },
                actions = {
                    IconButton(onClick = onNavigateToCharts) {
                        Icon(Icons.Default.BarChart, contentDescription = "View Charts")
                    }
                    IconButton(onClick = onNavigateToDevices) {
                        Icon(Icons.Default.Settings, contentDescription = "Device Settings")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Error: ${uiState.error}",
                    color = MaterialTheme.colorScheme.error
                )
            }
        } else {
            DashboardContent(
                uiState = uiState,
                onStartDrying = { viewModel.startDrying() },
                onStopDrying = { viewModel.stopDrying() },
                onSetTemperature = { viewModel.setTemperature(it) },
                onSetHumidity = { viewModel.setHumidity(it) },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
    
    // Show command status snackbar
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.commandStatus) {
        when (val status = uiState.commandStatus) {
            is CommandStatus.Success -> {
                snackbarHostState.showSnackbar("Command sent successfully")
            }
            is CommandStatus.Error -> {
                snackbarHostState.showSnackbar("Error: ${status.message}")
            }
            else -> {}
        }
    }
}

@Composable
private fun DashboardContent(
    uiState: DashboardUiState,
    onStartDrying: () -> Unit,
    onStopDrying: () -> Unit,
    onSetTemperature: (Float) -> Unit,
    onSetHumidity: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val deviceData = uiState.deviceData ?: return
    val status = deviceData.status
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Status Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (status.dryingActive) 
                    MaterialTheme.colorScheme.primaryContainer 
                else 
                    MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (status.dryingActive) "DRYING ACTIVE" else "STANDBY",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Heater: ${if (status.heaterOn) "ON" else "OFF"} | Fan: ${if (status.fanOn) "ON" else "OFF"}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Icon(
                    imageVector = if (status.dryingActive) Icons.Default.PlayArrow else Icons.Default.StopCircle,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = if (status.dryingActive) 
                        MaterialTheme.colorScheme.primary 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Gauges Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Temperature Gauge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                CircularGauge(
                    value = status.temperature,
                    maxValue = 100f,
                    label = "Temperature",
                    unit = "°C",
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Target: ${status.setpointTemp}°C",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Humidity Gauge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                CircularGauge(
                    value = status.humidity,
                    maxValue = 100f,
                    label = "Humidity",
                    unit = "%",
                    color = MaterialTheme.colorScheme.secondary
                )
                Text(
                    text = "Target: ${status.setpointHumidity}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = onStartDrying,
                enabled = !status.dryingActive && uiState.commandStatus !is CommandStatus.Sending,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Start")
            }
            
            Button(
                onClick = onStopDrying,
                enabled = status.dryingActive && uiState.commandStatus !is CommandStatus.Sending,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.StopCircle, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Stop")
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Setpoint Controls
        SetpointControls(
            currentTemp = status.setpointTemp,
            currentHumidity = status.setpointHumidity,
            onTempChange = onSetTemperature,
            onHumidityChange = onSetHumidity,
            enabled = uiState.commandStatus !is CommandStatus.Sending
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Connection Status
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatusChip(
                label = "WiFi",
                isConnected = status.wifiConnected
            )
            StatusChip(
                label = "Firebase",
                isConnected = status.firebaseConnected
            )
        }
    }
}

@Composable
private fun CircularGauge(
    value: Float,
    maxValue: Float,
    label: String,
    unit: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    val animatedValue by animateFloatAsState(
        targetValue = value / maxValue,
        animationSpec = tween(durationMillis = 1000, easing = EaseOutCubic),
        label = "gauge_animation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(120.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                val center = Offset(size.width / 2, size.height / 2)
                
                // Background arc
                drawArc(
                    color = color.copy(alpha = 0.2f),
                    startAngle = 135f,
                    sweepAngle = 270f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
                
                // Value arc
                drawArc(
                    color = color,
                    startAngle = 135f,
                    sweepAngle = 270f * animatedValue,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    size = Size(radius * 2, radius * 2),
                    topLeft = Offset(center.x - radius, center.y - radius)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "%.1f".format(value),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge
        )
    }
}

@Composable
private fun SetpointControls(
    currentTemp: Float,
    currentHumidity: Float,
    onTempChange: (Float) -> Unit,
    onHumidityChange: (Float) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    var tempSlider by remember(currentTemp) { mutableStateOf(currentTemp) }
    var humiditySlider by remember(currentHumidity) { mutableStateOf(currentHumidity) }
    
    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Setpoints",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Temperature slider
            Text("Temperature: ${tempSlider.toInt()}°C")
            Slider(
                value = tempSlider,
                onValueChange = { tempSlider = it },
                onValueChangeFinished = { onTempChange(tempSlider) },
                valueRange = 30f..80f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Humidity slider
            Text("Humidity: ${humiditySlider.toInt()}%")
            Slider(
                value = humiditySlider,
                onValueChange = { humiditySlider = it },
                onValueChangeFinished = { onHumidityChange(humiditySlider) },
                valueRange = 10f..50f,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun StatusChip(
    label: String,
    isConnected: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = if (isConnected) 
            MaterialTheme.colorScheme.primaryContainer 
        else 
            MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .padding(2.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCircle(
                        color = if (isConnected) Color(0xFF4CAF50) else Color(0xFFF44336)
                    )
                }
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}
