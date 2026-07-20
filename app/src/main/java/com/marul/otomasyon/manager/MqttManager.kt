package com.marul.otomasyon.manager

import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.UUID

interface MqttCallback {
    fun onConnected()
    fun onDisconnected()
    fun onMessageReceived(topic: String, payload: String)
    fun onError(message: String)
}

class MqttManager {
    private var client: MqttClient? = null
    private var callback: MqttCallback? = null

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val subscribeTopics = listOf(
        Constants.TOPIC_PH, Constants.TOPIC_EC, Constants.TOPIC_TEMP,
        Constants.TOPIC_TANK, Constants.TOPIC_FLOW_A, Constants.TOPIC_FLOW_B,
        Constants.TOPIC_FLOW_PH, Constants.TOPIC_STATUS_PH_DOWN,
        Constants.TOPIC_STATUS_FERT_A, Constants.TOPIC_STATUS_FERT_B,
        Constants.TOPIC_STATUS_CIRC
    )

    fun connect(brokerHost: String, port: Int, callback: MqttCallback) {
        this.callback = callback
        if (brokerHost.isBlank()) {
            callback.onError("Broker IP adresi boş. Ayarlar ekranından girin.")
            return
        }
        val clientId = "marul-android-${UUID.randomUUID().toString().take(8)}"
        val brokerUri = "tcp://$brokerHost:$port"

        scope.launch {
            try {
                val mqttClient = MqttClient(brokerUri, clientId, MemoryPersistence())
                val options = MqttConnectOptions().apply {
                    isCleanSession = true
                    connectionTimeout = 10
                    keepAliveInterval = 30
                    isAutomaticReconnect = true
                }

                mqttClient.setCallback(object : MqttCallbackExtended {
                    override fun connectComplete(reconnect: Boolean, serverURI: String) {
                        _connectionState.value = true
                        subscribeAll(mqttClient)
                        if (reconnect) callback.onConnected()
                    }

                    override fun connectionLost(cause: Throwable?) {
                        _connectionState.value = false
                        callback.onDisconnected()
                    }

                    override fun messageArrived(topic: String, message: MqttMessage) {
                        callback.onMessageReceived(topic, message.toString())
                    }

                    override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                })

                mqttClient.connect(options)
                client = mqttClient
                _connectionState.value = true
                subscribeAll(mqttClient)
                callback.onConnected()
            } catch (e: Exception) {
                callback.onError("MQTT bağlantı hatası: ${e.message}")
            }
        }
    }

    private fun subscribeAll(mqttClient: MqttClient) {
        try {
            val topics = subscribeTopics.toTypedArray()
            val qos = IntArray(topics.size) { 0 }
            mqttClient.subscribe(topics, qos)
        } catch (e: Exception) {
            callback?.onError("Subscribe hatası: ${e.message}")
        }
    }

    fun publish(topic: String, payload: String) {
        if (!_connectionState.value) return
        scope.launch {
            try {
                val message = MqttMessage(payload.toByteArray()).apply { qos = 1 }
                client?.publish(topic, message)
            } catch (e: Exception) {
                callback?.onError("Yayın hatası: ${e.message}")
            }
        }
    }

    fun disconnect() {
        scope.launch {
            try {
                client?.disconnect()
            } catch (_: Exception) {}
            _connectionState.value = false
            callback?.onDisconnected()
        }
        callback = null
    }

    fun isConnected() = _connectionState.value
}
