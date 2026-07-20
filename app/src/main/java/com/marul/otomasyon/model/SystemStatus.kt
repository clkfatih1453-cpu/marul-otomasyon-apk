package com.marul.otomasyon.model

data class SystemStatus(
    val isOnline: Boolean = false,
    val isBleConnected: Boolean = false,
    val isMqttConnected: Boolean = false,
    val isLocked: Boolean = false,
    val hasSensorError: Boolean = false,
    val statusMessage: String = "Offline",
    val lastUpdate: Long = System.currentTimeMillis()
)

data class SensorData(
    // Besin çözeltisi
    val ph: Float = 7.0f,
    val ec: Float = 0.0f,
    // Ortam sensörleri
    val temperature: Float = 25.0f,      // Ortam sıcaklığı (°C)
    val humidity: Float = 0.0f,           // Nem (%)
    val waterTemperature: Float = 20.0f, // Su sıcaklığı (°C)
    // Depo
    val tankLevel: Int = 100,             // Su seviyesi (%)
    val waterAddedLiters: Float = 0.0f,  // Tanka eklenen su (L)
    // Akış ve dozaj toplamları
    val phFlowRate: Float = 0.0f,
    val fertilizerAFlowRate: Float = 0.0f,
    val fertilizerBFlowRate: Float = 0.0f,
    val fertAMlTotal: Float = 0.0f,      // A gübresi verilen toplam (ml)
    val fertBMlTotal: Float = 0.0f,      // B gübresi verilen toplam (ml)
    val acidMlTotal: Float = 0.0f,       // Asit verilen toplam (ml)
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
