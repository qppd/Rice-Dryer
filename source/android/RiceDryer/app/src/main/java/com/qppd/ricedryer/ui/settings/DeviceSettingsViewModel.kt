package com.qppd.ricedryer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.qppd.ricedryer.data.local.AppDatabase
import com.qppd.ricedryer.data.repository.DeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeviceSettingsUiState {
    object Idle : DeviceSettingsUiState()
    object Renaming : DeviceSettingsUiState()
    object Removing : DeviceSettingsUiState()
    data class Error(val message: String) : DeviceSettingsUiState()
    object Success : DeviceSettingsUiState()
}

class DeviceSettingsViewModel(
    private val deviceRepository: DeviceRepository,
    private val deviceId: String
) : ViewModel() {
    private val _uiState = MutableStateFlow<DeviceSettingsUiState>(DeviceSettingsUiState.Idle)
    val uiState: StateFlow<DeviceSettingsUiState> = _uiState.asStateFlow()

    fun renameDevice(newName: String) {
        _uiState.value = DeviceSettingsUiState.Renaming
        viewModelScope.launch {
            try {
                deviceRepository.renameDevice(deviceId, newName)
                _uiState.value = DeviceSettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = DeviceSettingsUiState.Error(e.message ?: "Rename failed")
            }
        }
    }

    fun removeDevice(userId: String) {
        _uiState.value = DeviceSettingsUiState.Removing
        viewModelScope.launch {
            try {
                deviceRepository.removeDeviceFromUser(userId, deviceId)
                _uiState.value = DeviceSettingsUiState.Success
            } catch (e: Exception) {
                _uiState.value = DeviceSettingsUiState.Error(e.message ?: "Remove failed")
            }
        }
    }

    companion object {
        fun factory(database: AppDatabase, deviceId: String) = object : androidx.lifecycle.ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DeviceSettingsViewModel(DeviceRepository(database), deviceId) as T
            }
        }
    }
}
