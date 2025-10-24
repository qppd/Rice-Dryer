package com.qppd.ricedryer.data.model;

public class Device {
    private String deviceId;
    private String deviceName;
    private String macAddress;
    private String firmwareVersion;
    private String hardwareVersion;
    private String pairedTo;
    private long pairedAt;
    private boolean online;
    private long lastUpdate;

    public Device() {
    }

    public Device(String deviceId, String deviceName) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getMacAddress() {
        return macAddress;
    }

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public String getFirmwareVersion() {
        return firmwareVersion;
    }

    public void setFirmwareVersion(String firmwareVersion) {
        this.firmwareVersion = firmwareVersion;
    }

    public String getHardwareVersion() {
        return hardwareVersion;
    }

    public void setHardwareVersion(String hardwareVersion) {
        this.hardwareVersion = hardwareVersion;
    }

    public String getPairedTo() {
        return pairedTo;
    }

    public void setPairedTo(String pairedTo) {
        this.pairedTo = pairedTo;
    }

    public long getPairedAt() {
        return pairedAt;
    }

    public void setPairedAt(long pairedAt) {
        this.pairedAt = pairedAt;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getLastUpdate() {
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }
}
