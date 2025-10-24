#ifndef WIFI_MANAGER_CUSTOM_H
#define WIFI_MANAGER_CUSTOM_H

#include <WiFi.h>
#include <WiFiManager.h>

class WiFiManagerCustom {
public:
    WiFiManagerCustom();
    bool begin(const char* apName, const char* apPassword);
    bool isConnected();
    String getLocalIP();
    void reconnect();
    void reset();
    
private:
    WiFiManager wifiManager;
    unsigned long lastReconnectAttempt;
    static const unsigned long RECONNECT_INTERVAL = 5000;
};

#endif
