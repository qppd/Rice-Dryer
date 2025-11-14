package com.qppd.ricedryer.ui.devices

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.local.CachedDevice
import com.qppd.ricedryer.data.model.DeviceData
import com.qppd.ricedryer.data.repository.DeviceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class DeviceListUiState(
    val devices: List<DeviceData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val userDeviceIds: List<String> = emptyList()
)

class DeviceListViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val auth = FirebaseAuth.getInstance()
    private val userId = auth.currentUser?.uid ?: ""
    
    private val _uiState = MutableStateFlow(DeviceListUiState())
    val uiState: StateFlow<DeviceListUiState> = _uiState.asStateFlow()
    
    init {
        loadDevices()
    }
    
    private fun loadDevices() {
        if (userId.isEmpty()) {
            _uiState.update { it.copy(isLoading = false, error = "User not authenticated") }
            return
        }
        
        viewModelScope.launch {
            deviceRepository.getUserDevices(userId)
                .catch { exception ->
                    _uiState.update { it.copy(
                        isLoading = false,
                        error = exception.message
                    )}
                }
                .collectLatest { deviceIds ->
                    _uiState.update { it.copy(userDeviceIds = deviceIds) }
                    
                    if (deviceIds.isEmpty()) {
                        _uiState.update { it.copy(
                            devices = emptyList(),
                            isLoading = false
                        )}
                    } else {
                        // Load data for each device
                        loadDeviceData(deviceIds)
                    }
                }
        }
    }
    
    private fun loadDeviceData(deviceIds: List<String>) {
        viewModelScope.launch {
            val deviceFlows = deviceIds.map { deviceId ->
                deviceRepository.getDeviceData(deviceId)
            }
            
            combine(deviceFlows) { devices ->
                devices.filterNotNull()
            }.catch { exception ->
                _uiState.update { it.copy(
                    isLoading = false,
                    error = exception.message
                )}
            }.collect { devices ->
                _uiState.update { it.copy(
                    devices = devices,
                    isLoading = false,
                    error = null
                )}
            }
        }
    }
    
    fun refresh() {
        _uiState.update { it.copy(isLoading = true) }
        loadDevices()
    }
    
    companion object {
        fun factory(database: AppDatabase) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DeviceListViewModel(DeviceRepository(database)) as T
            }
        }
    }
}
