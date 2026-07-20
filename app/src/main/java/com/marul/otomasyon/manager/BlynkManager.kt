package com.marul.otomasyon.manager

import com.blynk.blynkserver.core.BlockingIOProcessor
import com.blynk.blynkserver.transport.ClientTransport
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

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

    fun connect(callback: BlynkCallback) {
        this.callback = callback
        // Blynk bağlantısı gerçek uygulamada burada yapılır
        _connectionState.value = true
        callback.onConnected()
    }

    fun disconnect() {
        _connectionState.value = false
        callback?.onDisconnected()
    }

    fun isConnected(): Boolean = _connectionState.value

    fun virtualWrite(pin: Int, value: String) {
        // Blynk virtual pin'e değer yazma
        if (_connectionState.value) {
            // Gerçek implementasyon burada yapılır
        }
    }

    fun virtualWrite(pin: Int, value: Float) {
        virtualWrite(pin, value.toString())
    }

    fun virtualWrite(pin: Int, value: Int) {
        virtualWrite(pin, value.toString())
    }
}
