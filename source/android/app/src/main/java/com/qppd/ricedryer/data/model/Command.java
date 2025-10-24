package com.qppd.ricedryer.data.model;

public class Command {
    private String action;
    private float value;
    private long timestamp;
    private boolean acknowledged;

    public Command() {
    }

    public Command(String action, float value) {
        this.action = action;
        this.value = value;
        this.timestamp = System.currentTimeMillis();
        this.acknowledged = false;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public float getValue() {
        return value;
    }

    public void setValue(float value) {
        this.value = value;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isAcknowledged() {
        return acknowledged;
    }

    public void setAcknowledged(boolean acknowledged) {
        this.acknowledged = acknowledged;
    }
}
