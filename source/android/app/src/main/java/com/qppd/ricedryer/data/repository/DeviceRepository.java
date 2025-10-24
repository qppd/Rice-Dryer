package com.qppd.ricedryer.data.repository;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;
import com.qppd.ricedryer.data.model.Command;
import com.qppd.ricedryer.data.model.Device;
import com.qppd.ricedryer.data.model.SensorReading;
import com.qppd.ricedryer.data.remote.FirebaseDataSource;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DeviceRepository {
    private static DeviceRepository instance;
    private final FirebaseDataSource dataSource;
    private final MutableLiveData<List<Device>> devicesLiveData;
    private final MutableLiveData<SensorReading> currentReadingLiveData;
    private final Map<String, ValueEventListener> deviceListeners;

    private DeviceRepository() {
        dataSource = FirebaseDataSource.getInstance();
        devicesLiveData = new MutableLiveData<>(new ArrayList<>());
        currentReadingLiveData = new MutableLiveData<>();
        deviceListeners = new HashMap<>();
    }

    public static synchronized DeviceRepository getInstance() {
        if (instance == null) {
            instance = new DeviceRepository();
        }
        return instance;
    }

    public LiveData<List<Device>> getDevicesLiveData() {
        return devicesLiveData;
    }

    public LiveData<SensorReading> getCurrentReadingLiveData() {
        return currentReadingLiveData;
    }

    public void loadUserDevices(String userId) {
        dataSource.getUserDevicesRef(userId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Device> devices = new ArrayList<>();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String deviceId = child.getKey();
                    String deviceName = child.child("deviceName").getValue(String.class);
                    
                    if (deviceId != null && deviceName != null) {
                        Device device = new Device(deviceId, deviceName);
                        device.setPairedAt(child.child("pairedAt").getValue(Long.class));
                        devices.add(device);
                        
                        // Load device info
                        loadDeviceInfo(device);
                    }
                }
                devicesLiveData.setValue(devices);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        });
    }

    private void loadDeviceInfo(Device device) {
        dataSource.getDeviceRef(device.getDeviceId()).child("deviceInfo")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        device.setMacAddress(snapshot.child("macAddress").getValue(String.class));
                        device.setFirmwareVersion(snapshot.child("firmwareVersion").getValue(String.class));
                        device.setHardwareVersion(snapshot.child("hardwareVersion").getValue(String.class));
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                    }
                });
    }

    public void listenToDeviceData(String deviceId) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                SensorReading reading = snapshot.getValue(SensorReading.class);
                if (reading != null) {
                    currentReadingLiveData.setValue(reading);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Handle error
            }
        };
        
        dataSource.getDeviceCurrentDataRef(deviceId).addValueEventListener(listener);
        deviceListeners.put(deviceId, listener);
    }

    public void stopListeningToDevice(String deviceId) {
        ValueEventListener listener = deviceListeners.remove(deviceId);
        if (listener != null) {
            dataSource.getDeviceCurrentDataRef(deviceId).removeEventListener(listener);
        }
    }

    public void sendCommand(String deviceId, Command command) {
        dataSource.getDeviceCommandsRef(deviceId).setValue(command);
    }

    public void pairDevice(String userId, String pairingCode, String deviceName, PairingCallback callback) {
        dataSource.getPairingCodeRef(pairingCode).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    callback.onError("Invalid pairing code");
                    return;
                }

                Boolean used = snapshot.child("used").getValue(Boolean.class);
                if (used != null && used) {
                    callback.onError("Pairing code already used");
                    return;
                }

                Long expiresAt = snapshot.child("expiresAt").getValue(Long.class);
                if (expiresAt != null && System.currentTimeMillis() > expiresAt) {
                    callback.onError("Pairing code expired");
                    return;
                }

                String deviceId = snapshot.child("deviceId").getValue(String.class);
                if (deviceId == null) {
                    callback.onError("Invalid device ID");
                    return;
                }

                // Mark pairing code as used
                snapshot.getRef().child("used").setValue(true);

                // Add device to user's devices
                Map<String, Object> deviceData = new HashMap<>();
                deviceData.put("deviceName", deviceName);
                deviceData.put("pairedAt", System.currentTimeMillis());
                deviceData.put("notifications", true);
                
                dataSource.getUserDevicesRef(userId).child(deviceId).setValue(deviceData);

                // Update device's pairedTo field
                dataSource.getDeviceRef(deviceId).child("deviceInfo").child("pairedTo").setValue(userId);
                dataSource.getDeviceRef(deviceId).child("deviceInfo").child("deviceName").setValue(deviceName);

                callback.onSuccess(deviceId);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                callback.onError(error.getMessage());
            }
        });
    }

    public interface PairingCallback {
        void onSuccess(String deviceId);
        void onError(String error);
    }
}
