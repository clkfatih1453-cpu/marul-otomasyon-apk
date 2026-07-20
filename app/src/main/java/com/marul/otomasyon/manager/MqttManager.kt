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
import java.net.URI
import java.net.URISyntaxException
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
        Constants.TOPIC_HUMIDITY, Constants.TOPIC_WATER_TEMP,
        Constants.TOPIC_TANK, Constants.TOPIC_WATER_ADDED,
        Constants.TOPIC_FLOW_A, Constants.TOPIC_FLOW_B, Constants.TOPIC_FLOW_PH,
        Constants.TOPIC_FERT_A_ML, Constants.TOPIC_FERT_B_ML, Constants.TOPIC_ACID_ML,
        Constants.TOPIC_STATUS_PH_DOWN, Constants.TOPIC_STATUS_FERT_A,
        Constants.TOPIC_STATUS_FERT_B, Constants.TOPIC_STATUS_CIRC
    )

    fun connect(
        brokerHost: String,
        port: Int,
        callback: MqttCallback,
        brokerId: String = "",
        mqttUrl: String = "",
        provider: String = ""
    ) {
        this.callback = callback
        val endpoint = resolveEndpoint(brokerHost, port, mqttUrl, provider)
        if (endpoint == null) {
            callback.onError("Broker IP adresi boş. Ayarlar ekranından girin.")
            return
        }
        val (resolvedHost, resolvedPort) = endpoint
        val normalizedBrokerId = brokerId.trim().replace("\\s+".toRegex(), "-")
        val clientId = if (normalizedBrokerId.isNotBlank()) {
            "marul-android-$normalizedBrokerId"
        } else {
            "marul-android-${UUID.randomUUID().toString().take(8)}"
        }
        val brokerUri = "tcp://$resolvedHost:$resolvedPort"

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
                        // Yalnızca otomatik yeniden bağlanmada bildir
                        // (ilk bağlanma aşağıdaki try bloğunda ele alınır)
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
                // connect() döndükten sonra zaten connectComplete çağrılmıştır
                client = mqttClient
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

    private fun resolveEndpoint(
        brokerHost: String,
        port: Int,
        mqttUrl: String,
        provider: String
    ): Pair<String, Int>? {
        var host = brokerHost.trim()
        var resolvedPort = if (port > 0) port else Constants.MQTT_DEFAULT_PORT

        if (mqttUrl.isNotBlank()) {
            parseMqttEndpoint(mqttUrl)?.let {
                host = it.first
                resolvedPort = it.second
            }
        }

        if (host.contains("://")) {
            parseMqttEndpoint(host)?.let {
                host = it.first
                resolvedPort = it.second
            }
        }

        host = host.substringBefore("/").trim().lowercase()
        if (host.isBlank()) return null

        // HiveMQ için kullanıcıların sık yaptığı "hivemq.com" girişini çalışan public broker'a çevir.
        if (host == "hivemq.com") {
            host = "broker.hivemq.com"
            if (resolvedPort <= 0) resolvedPort = 1883
        }
        if (provider.contains("hivemq", ignoreCase = true) && host == "www.hivemq.com") {
            host = "broker.hivemq.com"
        }

        return host to resolvedPort
    }

    private fun parseMqttEndpoint(raw: String): Pair<String, Int>? {
        return try {
            val normalized = if (raw.contains("://")) raw else "mqtt://$raw"
            val uri = URI(normalized)
            val host = uri.host?.trim()?.lowercase().orEmpty()
            if (host.isBlank()) return null
            val port = if (uri.port > 0) uri.port else Constants.MQTT_DEFAULT_PORT
            host to port
        } catch (_: URISyntaxException) {
            null
        }
    }
}
