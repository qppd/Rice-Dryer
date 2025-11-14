package com.qppd.ricedryer.ui.charts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.model.SensorReading
import com.qppd.ricedryer.data.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ChartsUiState(
    val readings: List<SensorReading> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val timeRange: TimeRange = TimeRange.LAST_HOUR
)

enum class TimeRange(val label: String, val milliseconds: Long) {
    LAST_HOUR("Last Hour", 60 * 60 * 1000),
    LAST_6_HOURS("Last 6 Hours", 6 * 60 * 60 * 1000),
    LAST_24_HOURS("Last 24 Hours", 24 * 60 * 60 * 1000),
    LAST_WEEK("Last Week", 7 * 24 * 60 * 60 * 1000)
}

class ChartsViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(ChartsUiState())
    val uiState: StateFlow<ChartsUiState> = _uiState.asStateFlow()
    
    init {
        loadReadings()
    }
    
    private fun loadReadings() {
        viewModelScope.launch {
            deviceRepository.getDeviceHistory(deviceId, 500)
                .catch { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = exception.message
                    )}
                }
                .collect { readings ->
                    _uiState.update { it.copy(
                        readings = readings,
                        isLoading = false,
                        error = null
                    )}
                }
        }
    }
    
    fun setTimeRange(timeRange: TimeRange) {
        _uiState.update { it.copy(timeRange = timeRange) }
    }
    
    fun getFilteredReadings(): List<SensorReading> {
        val currentState = _uiState.value
        val allReadings = currentState.readings
        
        if (allReadings.isEmpty()) return emptyList()
        
        // ESP32 now uses NTP timestamps (Unix timestamps in milliseconds)
        val cutoffTime = System.currentTimeMillis() - currentState.timeRange.milliseconds
        
        return allReadings.filter { it.timestamp >= cutoffTime }
    }
    
    companion object {
        fun factory(database: AppDatabase, deviceId: String) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return ChartsViewModel(DeviceRepository(database), deviceId) as T
            }
        }
    }
}
