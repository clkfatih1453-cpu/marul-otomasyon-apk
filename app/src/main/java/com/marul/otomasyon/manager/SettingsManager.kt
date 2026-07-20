package com.marul.otomasyon.manager

import android.content.Context
import android.content.SharedPreferences
import com.marul.otomasyon.model.WifiConfig
import com.marul.otomasyon.util.Constants

class SettingsManager(context: Context) {
    private val preferences: SharedPreferences = context.getSharedPreferences("marul_prefs", Context.MODE_PRIVATE)

    fun saveWifiConfig(config: WifiConfig) {
        preferences.edit().apply {
            putString(Constants.PREF_SSID, config.ssid)
            putString(Constants.PREF_PASSWORD, config.password)
            putFloat(Constants.PREF_PH_MAX, config.phMax)
            putFloat(Constants.PREF_PH_MIN, config.phMin)
            putFloat(Constants.PREF_EC_MIN, config.ecMin)
            putLong(Constants.PREF_DOSAGE_TIME, config.dosageTime)
            putLong(Constants.PREF_MEASUREMENT_PERIOD, config.measurementPeriod)
            apply()
        }
    }

    fun getWifiConfig(): WifiConfig {
        return WifiConfig(
            ssid = preferences.getString(Constants.PREF_SSID, "") ?: "",
            password = preferences.getString(Constants.PREF_PASSWORD, "") ?: "",
            phMax = preferences.getFloat(Constants.PREF_PH_MAX, Constants.DEFAULT_PH_MAX),
            phMin = preferences.getFloat(Constants.PREF_PH_MIN, Constants.DEFAULT_PH_MIN),
            ecMin = preferences.getFloat(Constants.PREF_EC_MIN, Constants.DEFAULT_EC_MIN),
            dosageTime = preferences.getLong(Constants.PREF_DOSAGE_TIME, Constants.DEFAULT_DOSAGE_TIME),
            measurementPeriod = preferences.getLong(Constants.PREF_MEASUREMENT_PERIOD, Constants.DEFAULT_MEASUREMENT_PERIOD)
        )
    }

    fun saveMqttHost(host: String) {
        preferences.edit().putString(Constants.PREF_MQTT_HOST, host).apply()
    }

    fun getMqttHost(): String {
        return preferences.getString(Constants.PREF_MQTT_HOST, Constants.MQTT_DEFAULT_HOST) ?: Constants.MQTT_DEFAULT_HOST
    }

    fun saveMqttPort(port: Int) {
        preferences.edit().putInt(Constants.PREF_MQTT_PORT, port).apply()
    }

    fun getMqttPort(): Int {
        return preferences.getInt(Constants.PREF_MQTT_PORT, Constants.MQTT_DEFAULT_PORT)
    }

    fun updatePhMax(value: Float) {
        preferences.edit().putFloat(Constants.PREF_PH_MAX, value).apply()
    }

    fun updatePhMin(value: Float) {
        preferences.edit().putFloat(Constants.PREF_PH_MIN, value).apply()
    }

    fun updateEcMin(value: Float) {
        preferences.edit().putFloat(Constants.PREF_EC_MIN, value).apply()
    }

    fun updateDosageTime(value: Long) {
        preferences.edit().putLong(Constants.PREF_DOSAGE_TIME, value).apply()
    }

    fun updateMeasurementPeriod(value: Long) {
        preferences.edit().putLong(Constants.PREF_MEASUREMENT_PERIOD, value).apply()
    }

    fun clearAll() {
        preferences.edit().clear().apply()
    }
}
