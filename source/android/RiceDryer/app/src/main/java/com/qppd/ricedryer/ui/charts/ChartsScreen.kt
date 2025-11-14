package com.qppd.ricedryer.ui.charts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsScreen(
    viewModel: ChartsViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Charts & Analytics") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
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
            ChartsContent(
                uiState = uiState,
                onTimeRangeChange = { viewModel.setTimeRange(it) },
                getFilteredReadings = { viewModel.getFilteredReadings() },
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun ChartsContent(
    uiState: ChartsUiState,
    onTimeRangeChange: (TimeRange) -> Unit,
    getFilteredReadings: () -> List<com.qppd.ricedryer.data.model.SensorReading>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Time Range Selector
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Time Range",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeRange.values().forEach { range ->
                        FilterChip(
                            selected = uiState.timeRange == range,
                            onClick = { onTimeRangeChange(range) },
                            label = { Text(range.label) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Temperature Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Temperature (°C)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val filteredReadings = remember(uiState.timeRange, uiState.readings) {
                    getFilteredReadings()
                }
                
                if (filteredReadings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data available")
                    }
                } else {
                    TemperatureChart(readings = filteredReadings)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Humidity Chart
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Humidity (%)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                val filteredReadings = remember(uiState.timeRange, uiState.readings) {
                    getFilteredReadings()
                }
                
                if (filteredReadings.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No data available")
                    }
                } else {
                    HumidityChart(readings = filteredReadings)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Statistics Card
        val filteredReadings = remember(uiState.timeRange, uiState.readings) {
            getFilteredReadings()
        }
        
        if (filteredReadings.isNotEmpty()) {
            StatisticsCard(readings = filteredReadings)
        }
    }
}

@Composable
private fun TemperatureChart(readings: List<com.qppd.ricedryer.data.model.SensorReading>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    override fun getFormattedValue(value: Float): String {
                        // Show relative time based on entry index
                        val index = value.toInt()
                        val minutes = index * 0.5 // Assuming 30 second intervals
                        return "${minutes.toInt()}m"
                    }
                }
                
                axisLeft.setDrawGridLines(true)
                axisRight.isEnabled = false
                
                legend.isEnabled = true
            }
        },
        update = { chart ->
            // Use index as X value instead of timestamp for better display
            val tempEntries = readings.mapIndexed { index, reading -> 
                Entry(index.toFloat(), reading.temperature) 
            }
            val setpointEntries = readings.mapIndexed { index, reading -> 
                Entry(index.toFloat(), reading.setpointTemp) 
            }
            
            val tempDataSet = LineDataSet(tempEntries, "Actual").apply {
                color = android.graphics.Color.rgb(33, 150, 243)
                setCircleColor(android.graphics.Color.rgb(33, 150, 243))
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
            }
            
            val setpointDataSet = LineDataSet(setpointEntries, "Target").apply {
                color = android.graphics.Color.rgb(255, 152, 0)
                setCircleColor(android.graphics.Color.rgb(255, 152, 0))
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
            }
            
            chart.data = LineData(tempDataSet, setpointDataSet)
            chart.invalidate()
        }
    )
}

@Composable
private fun HumidityChart(readings: List<com.qppd.ricedryer.data.model.SensorReading>) {
    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                description.isEnabled = false
                setTouchEnabled(true)
                isDragEnabled = true
                setScaleEnabled(true)
                setPinchZoom(true)
                setDrawGridBackground(false)
                
                xAxis.position = XAxis.XAxisPosition.BOTTOM
                xAxis.setDrawGridLines(false)
                xAxis.valueFormatter = object : ValueFormatter() {
                    private val dateFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
                    override fun getFormattedValue(value: Float): String {
                        // ESP32 now uses NTP timestamps (Unix time in milliseconds)
                        return dateFormat.format(Date(value.toLong()))
                    }
                }
                
                axisLeft.setDrawGridLines(true)
                axisRight.isEnabled = false
                
                legend.isEnabled = true
            }
        },
        update = { chart ->
            val humidityEntries = readings.map { Entry(it.timestamp.toFloat(), it.humidity) }
            val setpointEntries = readings.map { Entry(it.timestamp.toFloat(), it.setpointHumidity) }
            
            val humidityDataSet = LineDataSet(humidityEntries, "Actual").apply {
                color = android.graphics.Color.rgb(76, 175, 80)
                setCircleColor(android.graphics.Color.rgb(76, 175, 80))
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
            }
            
            val setpointDataSet = LineDataSet(setpointEntries, "Target").apply {
                color = android.graphics.Color.rgb(255, 152, 0)
                setCircleColor(android.graphics.Color.rgb(255, 152, 0))
                lineWidth = 2f
                circleRadius = 3f
                setDrawValues(false)
                enableDashedLine(10f, 5f, 0f)
            }
            
            chart.data = LineData(humidityDataSet, setpointDataSet)
            chart.invalidate()
        }
    )
}

@Composable
private fun StatisticsCard(readings: List<com.qppd.ricedryer.data.model.SensorReading>) {
    val avgTemp = readings.map { it.temperature }.average().toFloat()
    val minTemp = readings.minOfOrNull { it.temperature } ?: 0f
    val maxTemp = readings.maxOfOrNull { it.temperature } ?: 0f
    
    val avgHumidity = readings.map { it.humidity }.average().toFloat()
    val minHumidity = readings.minOfOrNull { it.humidity } ?: 0f
    val maxHumidity = readings.maxOfOrNull { it.humidity } ?: 0f
    
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Statistics",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Temperature", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Avg: %.1f°C".format(avgTemp), style = MaterialTheme.typography.bodySmall)
                    Text("Min: %.1f°C".format(minTemp), style = MaterialTheme.typography.bodySmall)
                    Text("Max: %.1f°C".format(maxTemp), style = MaterialTheme.typography.bodySmall)
                }
                
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Humidity", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Avg: %.1f%%".format(avgHumidity), style = MaterialTheme.typography.bodySmall)
                    Text("Min: %.1f%%".format(minHumidity), style = MaterialTheme.typography.bodySmall)
                    Text("Max: %.1f%%".format(maxHumidity), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
