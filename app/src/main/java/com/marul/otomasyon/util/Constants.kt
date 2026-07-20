package com.marul.otomasyon.util

object Constants {
    // MQTT Broker Varsayılanları
    const val MQTT_DEFAULT_HOST = "192.168.1.100"
    const val MQTT_DEFAULT_PORT = 1883

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

    // ── MQTT Sensör Konuları (ESP32 → Android) ──────────────────────────────
    const val TOPIC_PH          = "marul/sensor/ph"
    const val TOPIC_EC          = "marul/sensor/ec"
    const val TOPIC_TEMP        = "marul/sensor/temp"
    const val TOPIC_HUMIDITY    = "marul/sensor/humidity"
    const val TOPIC_WATER_TEMP  = "marul/sensor/watertemp"
    const val TOPIC_TANK        = "marul/sensor/tank"
    const val TOPIC_WATER_ADDED = "marul/sensor/water_added"
    const val TOPIC_FLOW_A      = "marul/sensor/flow_a"
    const val TOPIC_FLOW_B      = "marul/sensor/flow_b"
    const val TOPIC_FLOW_PH     = "marul/sensor/flow_ph"
    const val TOPIC_FERT_A_ML   = "marul/sensor/fert_a_ml"
    const val TOPIC_FERT_B_ML   = "marul/sensor/fert_b_ml"
    const val TOPIC_ACID_ML     = "marul/sensor/acid_ml"

    // ── MQTT Durum Konuları (ESP32 pompa geri bildirimi) ────────────────────
    const val TOPIC_STATUS_PH_DOWN = "marul/status/ph_down"
    const val TOPIC_STATUS_FERT_A  = "marul/status/fert_a"
    const val TOPIC_STATUS_FERT_B  = "marul/status/fert_b"
    const val TOPIC_STATUS_CIRC    = "marul/status/circ"

    // ── MQTT Kontrol Konuları (Android → ESP32) ─────────────────────────────
    const val TOPIC_CTRL_PH_DOWN   = "marul/control/ph_down"
    const val TOPIC_CTRL_FERT_A    = "marul/control/fert_a"
    const val TOPIC_CTRL_FERT_B    = "marul/control/fert_b"
    const val TOPIC_CTRL_CIRC      = "marul/control/circ"
    const val TOPIC_CTRL_RESET     = "marul/control/reset"
    const val TOPIC_CTRL_DOSE_A    = "marul/control/dose_a"    // format: "50" (ml)
    const val TOPIC_CTRL_DOSE_B    = "marul/control/dose_b"
    const val TOPIC_CTRL_DOSE_ACID = "marul/control/dose_acid"
    const val TOPIC_CTRL_PUMP_TMR  = "marul/control/pump_timer"  // format: "08:00-20:00"
    const val TOPIC_CTRL_LIGHT_TMR = "marul/control/light_timer"

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
    const val PREF_MQTT_HOST = "mqtt_host"
    const val PREF_MQTT_PORT = "mqtt_port"
    const val PREF_PUMP_TIMER = "pump_timer"
    const val PREF_LIGHT_TIMER = "light_timer"
}
