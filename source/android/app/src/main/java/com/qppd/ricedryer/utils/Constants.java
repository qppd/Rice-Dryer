package com.qppd.ricedryer.utils;

public class Constants {
    // Firebase paths
    public static final String DEVICES_PATH = "devices";
    public static final String USERS_PATH = "users";
    public static final String PAIRING_PATH = "devicePairing";
    
    // Device status
    public static final String STATUS_ONLINE = "online";
    public static final String STATUS_OFFLINE = "offline";
    
    // Commands
    public static final String CMD_START = "START";
    public static final String CMD_STOP = "STOP";
    public static final String CMD_SET_TEMP = "SET_TEMP";
    public static final String CMD_MANUAL_SSR = "MANUAL_SSR";
    
    // Temperature ranges
    public static final float TEMP_MIN = 30.0f;
    public static final float TEMP_MAX = 60.0f;
    public static final float TEMP_OPTIMAL_MIN = 40.0f;
    public static final float TEMP_OPTIMAL_MAX = 50.0f;
    public static final float TEMP_WARNING = 55.0f;
    
    // Humidity ranges
    public static final float HUMIDITY_OPTIMAL_MIN = 40.0f;
    public static final float HUMIDITY_OPTIMAL_MAX = 60.0f;
    public static final float HUMIDITY_WARNING = 75.0f;
    
    // Time intervals
    public static final long DATA_REFRESH_INTERVAL = 5000; // 5 seconds
    public static final long OFFLINE_THRESHOLD = 30000; // 30 seconds
    
    // SharedPreferences keys
    public static final String PREFS_NAME = "RiceDryerPrefs";
    public static final String KEY_REMEMBER_ME = "remember_me";
    public static final String KEY_SELECTED_DEVICE = "selected_device";
    public static final String KEY_ONBOARDING_COMPLETE = "onboarding_complete";
    public static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    
    // Chart time ranges
    public static final int CHART_RANGE_24H = 24;
    public static final int CHART_RANGE_7D = 168;
    public static final int CHART_RANGE_30D = 720;
}
