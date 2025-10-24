#include "WiFiManagerCustom.h"

WiFiManagerCustom::WiFiManagerCustom() : lastReconnectAttempt(0) {
}

bool WiFiManagerCustom::begin(const char* apName, const char* apPassword) {
    // Configure WiFiManager
    wifiManager.setConfigPortalTimeout(180); // 3 minutes
    wifiManager.setConnectTimeout(30); // 30 seconds
    wifiManager.setAPStaticIPConfig(IPAddress(192,168,4,1), 
                                     IPAddress(192,168,4,1), 
                                     IPAddress(255,255,255,0));
    
    // Set custom HTML
    wifiManager.setCustomHeadElement("<style>body{background:#009688;}button{background:#FF5722;}</style>");
    
    // Try to connect with saved credentials or start config portal
    if (!wifiManager.autoConnect(apName, apPassword)) {
        Serial.println("Failed to connect and hit timeout");
        return false;
    }
    
    Serial.println("WiFi Connected!");
    Serial.print("IP Address: ");
    Serial.println(WiFi.localIP());
    
    return true;
}

bool WiFiManagerCustom::isConnected() {
    return WiFi.status() == WL_CONNECTED;
}

String WiFiManagerCustom::getLocalIP() {
    return WiFi.localIP().toString();
}

void WiFiManagerCustom::reconnect() {
    if (!isConnected()) {
        unsigned long currentMillis = millis();
        if (currentMillis - lastReconnectAttempt >= RECONNECT_INTERVAL) {
            lastReconnectAttempt = currentMillis;
            
            Serial.println("Attempting to reconnect to WiFi...");
            WiFi.disconnect();
            WiFi.reconnect();
        }
    }
}

void WiFiManagerCustom::reset() {
    wifiManager.resetSettings();
    Serial.println("WiFi settings reset");
}
