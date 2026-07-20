package com.marul.otomasyon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants
import java.net.URI
import java.net.URISyntaxException

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var edtMqttHost: EditText
    private lateinit var edtMqttPort: EditText
    private lateinit var edtMqttBrokerId: EditText
    private lateinit var edtMqttProvider: EditText
    private lateinit var edtMqttUrl: EditText
    private lateinit var edtMqttWsUrl: EditText
    private lateinit var edtMqttUsername: EditText
    private lateinit var edtMqttPassword: EditText
    private lateinit var switchMqttTls: Switch
    private lateinit var edtPhMax: EditText
    private lateinit var edtPhMin: EditText
    private lateinit var edtEcMin: EditText
    private lateinit var edtDosageTime: EditText
    private lateinit var edtMeasurementPeriod: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        settingsManager = SettingsManager(this)
        setupUI()
        loadSettings()
    }

    private fun setupUI() {
        edtMqttHost = findViewById(R.id.edt_mqtt_host)
        edtMqttPort = findViewById(R.id.edt_mqtt_port)
        edtMqttBrokerId = findViewById(R.id.edt_mqtt_broker_id)
        edtMqttProvider = findViewById(R.id.edt_mqtt_provider)
        edtMqttUrl = findViewById(R.id.edt_mqtt_url)
        edtMqttWsUrl = findViewById(R.id.edt_mqtt_ws_url)
        edtMqttUsername = findViewById(R.id.edt_mqtt_username)
        edtMqttPassword = findViewById(R.id.edt_mqtt_password)
        switchMqttTls = findViewById(R.id.switch_mqtt_tls)
        edtPhMax = findViewById(R.id.edt_ph_max)
        edtPhMin = findViewById(R.id.edt_ph_min)
        edtEcMin = findViewById(R.id.edt_ec_min)
        edtDosageTime = findViewById(R.id.edt_dosage_time)
        edtMeasurementPeriod = findViewById(R.id.edt_measurement_period)

        val btnSave = findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            saveSettings()
        }

        findViewById<Button>(R.id.btn_esp32_config).setOnClickListener {
            startActivity(Intent(this, BtProvisionActivity::class.java))
        }
    }

    private fun loadSettings() {
        edtMqttHost.setText(settingsManager.getMqttHost())
        edtMqttPort.setText(settingsManager.getMqttPort().toString())
        edtMqttBrokerId.setText(settingsManager.getMqttBrokerId())
        edtMqttProvider.setText(settingsManager.getMqttProvider())
        edtMqttUrl.setText(settingsManager.getMqttUrl())
        edtMqttWsUrl.setText(settingsManager.getMqttWsUrl())
        edtMqttUsername.setText(settingsManager.getMqttUsername())
        edtMqttPassword.setText(settingsManager.getMqttPassword())
        switchMqttTls.isChecked = settingsManager.getMqttUseTls()
        val config = settingsManager.getWifiConfig()
        edtPhMax.setText(config.phMax.toString())
        edtPhMin.setText(config.phMin.toString())
        edtEcMin.setText(config.ecMin.toString())
        edtDosageTime.setText(config.dosageTime.toString())
        edtMeasurementPeriod.setText(config.measurementPeriod.toString())
    }

    private fun saveSettings() {
        try {
            var host = edtMqttHost.text.toString().trim()
            var port = edtMqttPort.text.toString().toIntOrNull() ?: Constants.MQTT_DEFAULT_PORT
            val brokerId = edtMqttBrokerId.text.toString().trim()
            val provider = edtMqttProvider.text.toString().trim()
            val mqttUrl = edtMqttUrl.text.toString().trim()
            val mqttWsUrl = edtMqttWsUrl.text.toString().trim()
            val mqttUser = edtMqttUsername.text.toString().trim()
            val mqttPass = edtMqttPassword.text.toString()
            var useTls = switchMqttTls.isChecked
            val phMax = edtPhMax.text.toString().toFloat()
            val phMin = edtPhMin.text.toString().toFloat()
            val ecMin = edtEcMin.text.toString().toFloat()
            val dosageTime = edtDosageTime.text.toString().toLong()
            val measurementPeriod = edtMeasurementPeriod.text.toString().toLong()

            if (mqttUrl.isNotBlank()) {
                val endpoint = parseMqttEndpoint(mqttUrl)
                if (endpoint != null) {
                    host = endpoint.first
                    port = endpoint.second
                    if (endpoint.third) useTls = true
                }
            }
            host = normalizeBrokerHost(host, provider)
            if (host.endsWith(".hivemq.cloud")) {
                useTls = true
                if (port == Constants.MQTT_DEFAULT_PORT) port = 8883
            }
            if (host.isBlank()) {
                Toast.makeText(this, "Broker IP veya MQTT URL gerekli", Toast.LENGTH_SHORT).show()
                return
            }
            if (useTls && port <= 0) {
                Toast.makeText(this, "TLS için geçerli port girin (öneri: 8883)", Toast.LENGTH_SHORT).show()
                return
            }
            if (useTls && mqttUser.isBlank()) {
                Toast.makeText(this, "HiveMQ Cloud için kullanıcı adı gerekli", Toast.LENGTH_SHORT).show()
                return
            }
            if (useTls && mqttPass.isBlank()) {
                Toast.makeText(this, "HiveMQ Cloud için şifre gerekli", Toast.LENGTH_SHORT).show()
                return
            }
            if (phMax <= phMin) {
                Toast.makeText(this, "pH Max, pH Min'den büyük olmalıdır", Toast.LENGTH_SHORT).show()
                return
            }

            settingsManager.saveMqttHost(host)
            settingsManager.saveMqttPort(port)
            settingsManager.saveMqttBrokerId(brokerId)
            settingsManager.saveMqttProvider(provider)
            settingsManager.saveMqttUrl(mqttUrl)
            settingsManager.saveMqttWsUrl(mqttWsUrl)
            settingsManager.saveMqttUsername(mqttUser)
            settingsManager.saveMqttPassword(mqttPass)
            settingsManager.saveMqttUseTls(useTls)
            settingsManager.updatePhMax(phMax)
            settingsManager.updatePhMin(phMin)
            settingsManager.updateEcMin(ecMin)
            settingsManager.updateDosageTime(dosageTime)
            settingsManager.updateMeasurementPeriod(measurementPeriod)

            Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
            finish()
        } catch (e: Exception) {
            Toast.makeText(this, "Geçersiz giriş: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun parseMqttEndpoint(url: String): Triple<String, Int, Boolean>? {
        return try {
            val normalized = if (url.contains("://")) url else "mqtt://$url"
            val uri = URI(normalized)
            val host = uri.host?.trim()?.lowercase().orEmpty()
            if (host.isBlank()) return null
            val port = if (uri.port > 0) uri.port else Constants.MQTT_DEFAULT_PORT
            val scheme = uri.scheme?.lowercase().orEmpty()
            val tls = scheme == "ssl" || scheme == "mqtts" || scheme == "tls"
            Triple(host, port, tls)
        } catch (_: URISyntaxException) {
            null
        }
    }

    private fun normalizeBrokerHost(rawHost: String, provider: String): String {
        var host = rawHost.trim().lowercase()
        if (host.contains("://")) {
            host = parseMqttEndpoint(host)?.first ?: host
        }
        host = host.substringBefore("/").trim()
        if (host == "hivemq.com" || host == "www.hivemq.com") return "broker.hivemq.com"
        if (provider.contains("hivemq", ignoreCase = true) && host == "hivemq.com") return "broker.hivemq.com"
        return host
    }
}
