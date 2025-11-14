package com.qppd.ricedryer.ui.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.model.DeviceInfo
import com.qppd.ricedryer.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class PairDeviceUiState {
    object Idle : PairDeviceUiState()
    object Loading : PairDeviceUiState()
    data class Success(val deviceInfo: DeviceInfo) : PairDeviceUiState()
    data class Error(val message: String) : PairDeviceUiState()
}

class PairDeviceViewModel(
    private val deviceRepository: DeviceRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow<PairDeviceUiState>(PairDeviceUiState.Idle)
    val uiState: StateFlow<PairDeviceUiState> = _uiState.asStateFlow()
    
    private val auth = FirebaseAuth.getInstance()
    
    fun pairDevice(deviceId: String, pairingCode: String) {
        val userId = auth.currentUser?.uid
        
        if (userId == null) {
            _uiState.value = PairDeviceUiState.Error("User not authenticated")
            return
        }
        
        if (deviceId.isBlank() || pairingCode.length != 6) {
            _uiState.value = PairDeviceUiState.Error("Invalid device ID or pairing code")
            return
        }
        
        viewModelScope.launch {
            _uiState.value = PairDeviceUiState.Loading
            
            val result = deviceRepository.pairDevice(userId, deviceId, pairingCode)
            
            _uiState.value = result.fold(
                onSuccess = { deviceInfo ->
                    PairDeviceUiState.Success(deviceInfo)
                },
                onFailure = { exception ->
                    PairDeviceUiState.Error(exception.message ?: "Pairing failed")
                }
            )
        }
    }
    
    fun resetState() {
        _uiState.value = PairDeviceUiState.Idle
    }
    
    companion object {
        fun factory(database: AppDatabase) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return PairDeviceViewModel(DeviceRepository(database)) as T
            }
        }
    }
}
