package com.qppd.ricedryer.ui.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.model.DeviceData
import com.qppd.ricedryer.data.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DashboardUiState(
    val deviceData: DeviceData? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val commandStatus: CommandStatus = CommandStatus.Idle
)

sealed class CommandStatus {
    object Idle : CommandStatus()
    object Sending : CommandStatus()
    object Success : CommandStatus()
    data class Error(val message: String) : CommandStatus()
}

class DashboardViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: String
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()
    
    init {
        observeDeviceData()
    }
    
    private fun observeDeviceData() {
        viewModelScope.launch {
            deviceRepository.getDeviceData(deviceId)
                .catch { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = exception.message
                    )}
                }
                .collect { deviceData ->
                    _uiState.update { it.copy(
                        deviceData = deviceData,
                        isLoading = false,
                        error = null
                    )}
                }
        }
    }
    
    fun startDrying() {
        sendCommand("START")
    }
    
    fun stopDrying() {
        sendCommand("STOP")
    }
    
    fun setTemperature(temperature: Float) {
        sendCommand("SET_TEMP", temperature)
    }
    
    fun setHumidity(humidity: Float) {
        sendCommand("SET_HUMIDITY", humidity)
    }
    
    private fun sendCommand(command: String, value: Float = 0f) {
        viewModelScope.launch {
            _uiState.update { it.copy(commandStatus = CommandStatus.Sending) }
            
            val result = deviceRepository.sendCommand(deviceId, command, value)
            
            _uiState.update { it.copy(
                commandStatus = result.fold(
                    onSuccess = { CommandStatus.Success },
                    onFailure = { CommandStatus.Error(it.message ?: "Command failed") }
                )
            )}
            
            // Reset command status after a delay
            kotlinx.coroutines.delay(2000)
            _uiState.update { it.copy(commandStatus = CommandStatus.Idle) }
        }
    }
    
    fun resetCommandStatus() {
        _uiState.update { it.copy(commandStatus = CommandStatus.Idle) }
    }
    
    companion object {
        fun factory(database: AppDatabase, deviceId: String) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(DeviceRepository(database), deviceId) as T
            }
        }
    }
}
