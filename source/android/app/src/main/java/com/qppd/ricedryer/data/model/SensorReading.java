package com.qppd.ricedryer.data.model;

public class SensorReading {
    private float temperature;
    private float humidity;
    private float setpoint;
    private boolean ssrStatus;
    private boolean dryingActive;
    private boolean online;
    private long timestamp;

    public SensorReading() {
    }

    public float getTemperature() {
        return temperature;
    }

    public void setTemperature(float temperature) {
        this.temperature = temperature;
    }

    public float getHumidity() {
        return humidity;
    }

    public void setHumidity(float humidity) {
        this.humidity = humidity;
    }

    public float getSetpoint() {
        return setpoint;
    }

    public void setSetpoint(float setpoint) {
        this.setpoint = setpoint;
    }

    public boolean isSsrStatus() {
        return ssrStatus;
    }

    public void setSsrStatus(boolean ssrStatus) {
        this.ssrStatus = ssrStatus;
    }

    public boolean isDryingActive() {
        return dryingActive;
    }

    public void setDryingActive(boolean dryingActive) {
        this.dryingActive = dryingActive;
    }

    public boolean isOnline() {
        return online;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
}
