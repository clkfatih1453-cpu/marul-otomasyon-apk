package com.marul.otomasyon.manager

import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

interface BlynkCallback {
    fun onConnected()
    fun onDisconnected()
    fun onDataReceived(pin: Int, value: String)
    fun onError(message: String)
}

class BlynkManager(private val authToken: String) {
    private var callback: BlynkCallback? = null
    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var pollingJob: Job? = null

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    // Sensör okuma pinleri (polling)
    private val sensorPins = listOf(
        Constants.V_PH, Constants.V_EC, Constants.V_TEMP,
        Constants.V_STATUS, Constants.V_TANK_LEVEL,
        Constants.V_FLOW_A, Constants.V_FLOW_B, Constants.V_FLOW_PH
    )

    fun connect(callback: BlynkCallback) {
        this.callback = callback
        if (authToken.isBlank()) {
            callback.onError("Blynk token boş. Ayarlar ekranından token girin.")
            return
        }
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("${Constants.BLYNK_BASE_URL}/isHardwareConnected?token=$authToken")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                response.close()
                if (response.isSuccessful) {
                    _connectionState.value = true
                    callback.onConnected()
                    startPolling()
                } else {
                    callback.onError("Blynk bağlantı hatası: ${response.code}")
                }
            } catch (e: Exception) {
                callback.onError("Bağlantı hatası: ${e.message}")
            }
        }
    }

    fun disconnect() {
        stopPolling()
        _connectionState.value = false
        callback?.onDisconnected()
        callback = null
    }

    fun isConnected(): Boolean = _connectionState.value

    fun virtualWrite(pin: Int, value: String) {
        if (authToken.isBlank() || !_connectionState.value) return
        scope.launch {
            try {
                val request = Request.Builder()
                    .url("${Constants.BLYNK_BASE_URL}/update?token=$authToken&v$pin=$value")
                    .get()
                    .build()
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    callback?.onError("Yazma hatası: ${response.code}")
                }
                response.close()
            } catch (e: Exception) {
                callback?.onError("Yazma hatası: ${e.message}")
            }
        }
    }

    fun virtualWrite(pin: Int, value: Float) = virtualWrite(pin, value.toString())
    fun virtualWrite(pin: Int, value: Int) = virtualWrite(pin, value.toString())

    private fun startPolling() {
        pollingJob?.cancel()
        pollingJob = scope.launch {
            while (isActive && _connectionState.value) {
                try {
                    for (pin in sensorPins) {
                        if (!isActive) break
                        val request = Request.Builder()
                            .url("${Constants.BLYNK_BASE_URL}/get?token=$authToken&v$pin")
                            .get()
                            .build()
                        val response = client.newCall(request).execute()
                        if (response.isSuccessful) {
                            val raw = response.body?.string()?.trim() ?: ""
                            // Blynk cevabı ["değer"] formatında döner
                            val parsed = raw.removePrefix("[\"").removeSuffix("\"]").trim()
                            if (parsed.isNotEmpty() && parsed != "null") {
                                callback?.onDataReceived(pin, parsed)
                            }
                        }
                        response.close()
                        delay(150)
                    }
                } catch (e: Exception) {
                    if (_connectionState.value) {
                        _connectionState.value = false
                        callback?.onDisconnected()
                    }
                    break
                }
                delay(Constants.BLYNK_POLL_INTERVAL_MS)
            }
        }
    }

    private fun stopPolling() {
        pollingJob?.cancel()
        pollingJob = null
    }
}
