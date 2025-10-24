package com.qppd.ricedryer.data.remote;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class FirebaseDataSource {
    private static FirebaseDataSource instance;
    private final FirebaseDatabase database;
    private final DatabaseReference devicesRef;
    private final DatabaseReference usersRef;
    private final DatabaseReference pairingRef;

    private FirebaseDataSource() {
        database = FirebaseDatabase.getInstance();
        devicesRef = database.getReference("devices");
        usersRef = database.getReference("users");
        pairingRef = database.getReference("devicePairing");
    }

    public static synchronized FirebaseDataSource getInstance() {
        if (instance == null) {
            instance = new FirebaseDataSource();
        }
        return instance;
    }

    public DatabaseReference getDevicesRef() {
        return devicesRef;
    }

    public DatabaseReference getDeviceRef(String deviceId) {
        return devicesRef.child(deviceId);
    }

    public DatabaseReference getDeviceCurrentDataRef(String deviceId) {
        return devicesRef.child(deviceId).child("current");
    }

    public DatabaseReference getDeviceHistoryRef(String deviceId) {
        return devicesRef.child(deviceId).child("history");
    }

    public DatabaseReference getDeviceCommandsRef(String deviceId) {
        return devicesRef.child(deviceId).child("commands");
    }

    public DatabaseReference getUsersRef() {
        return usersRef;
    }

    public DatabaseReference getUserRef(String userId) {
        return usersRef.child(userId);
    }

    public DatabaseReference getUserDevicesRef(String userId) {
        return usersRef.child(userId).child("devices");
    }

    public DatabaseReference getPairingRef() {
        return pairingRef;
    }

    public DatabaseReference getPairingCodeRef(String code) {
        return pairingRef.child(code);
    }
}
