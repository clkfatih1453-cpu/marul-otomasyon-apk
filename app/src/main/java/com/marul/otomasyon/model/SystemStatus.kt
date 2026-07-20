package com.marul.otomasyon.model

data class SystemStatus(
    val isOnline: Boolean = false,
    val isBleConnected: Boolean = false,
    val isBlynkConnected: Boolean = false,
    val isLocked: Boolean = false,
    val hasSensorError: Boolean = false,
    val statusMessage: String = "Offline",
    val lastUpdate: Long = System.currentTimeMillis()
)

data class SensorData(
    val ph: Float = 7.0f,
    val ec: Float = 0.0f,
    val temperature: Float = 25.0f,
    val tankLevel: Int = 100,
    val phFlowRate: Float = 0.0f,
    val fertilizerAFlowRate: Float = 0.0f,
    val fertilizerBFlowRate: Float = 0.0f,
    val timestamp: Long = System.currentTimeMillis()
)

data class WifiConfig(
    val ssid: String = "",
    val password: String = "",
    val phMax: Float = 6.2f,
    val phMin: Float = 5.5f,
    val ecMin: Float = 1.0f,
    val dosageTime: Long = 5000L,
    val measurementPeriod: Long = 120000L
)

data class PumpStatus(
    val phDownRunning: Boolean = false,
    val fertilizerARunning: Boolean = false,
    val fertilizerBRunning: Boolean = false,
    val circulationRunning: Boolean = true
)
