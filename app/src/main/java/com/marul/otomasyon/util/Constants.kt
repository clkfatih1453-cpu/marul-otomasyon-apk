package com.marul.otomasyon.util

object Constants {
    // Blynk Configuration
    const val BLYNK_TEMPLATE_ID = "TMPL6e0eGKAVX"
    const val BLYNK_TEMPLATE_NAME = "marul otomasyon"
    const val BLYNK_AUTH_TOKEN = "8Cj2uteylTLTPjiwbkFyCn_0assoU6Nm"
    const val BLYNK_BASE_URL = "https://blynk.cloud/external/api"
    const val BLYNK_POLL_INTERVAL_MS = 3000L

    // BLE UUIDs
    const val SERVICE_UUID = "0000180A-0000-1000-8000-00805f9b34fb"
    const val CHAR_SSID_UUID = "00000001-0000-1000-8000-00805f9b34fb"
    const val CHAR_PASSWORD_UUID = "00000002-0000-1000-8000-00805f9b34fb"
    const val CHAR_PH_MAX_UUID = "00000003-0000-1000-8000-00805f9b34fb"
    const val CHAR_PH_MIN_UUID = "00000004-0000-1000-8000-00805f9b34fb"
    const val CHAR_EC_MIN_UUID = "00000005-0000-1000-8000-00805f9b34fb"
    const val CHAR_DATA_UUID = "00000006-0000-1000-8000-00805f9b34fb"
    const val CHAR_DOSAGE_TIME_UUID = "00000007-0000-1000-8000-00805f9b34fb"
    const val CHAR_MEASUREMENT_PERIOD_UUID = "00000008-0000-1000-8000-00805f9b34fb"
    const val BLE_CCCD_UUID = "00002902-0000-1000-8000-00805f9b34fb"

    // Virtual Pins (Blynk)
    const val V_PH = 0
    const val V_EC = 1
    const val V_TEMP = 2
    const val V_STATUS = 3
    const val V_PH_DOWN = 4
    const val V_GUBRE_A = 5
    const val V_RESET = 6
    const val V_FLOW_A = 7
    const val V_FLOW_B = 8
    const val V_FLOW_PH = 9
    const val V_GUBRE_B = 10
    const val V_TANK_LEVEL = 11
    const val V_CIRCULATION = 12

    // Default Values
    const val DEFAULT_PH_MAX = 6.2f
    const val DEFAULT_PH_MIN = 5.5f
    const val DEFAULT_EC_MIN = 1.0f
    const val DEFAULT_DOSAGE_TIME = 5000L
    const val DEFAULT_MEASUREMENT_PERIOD = 120000L

    // Preferences Keys
    const val PREF_SSID = "wifi_ssid"
    const val PREF_PASSWORD = "wifi_password"
    const val PREF_PH_MAX = "ph_max"
    const val PREF_PH_MIN = "ph_min"
    const val PREF_EC_MIN = "ec_min"
    const val PREF_DOSAGE_TIME = "dosage_time"
    const val PREF_MEASUREMENT_PERIOD = "measurement_period"
    const val PREF_BLYNK_TOKEN = "blynk_token"
}
