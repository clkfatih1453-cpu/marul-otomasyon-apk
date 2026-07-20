package com.marul.otomasyon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.MqttCallback
import com.marul.otomasyon.manager.MqttManager
import com.marul.otomasyon.manager.SensorDataManager
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.launch

class ControlActivity : AppCompatActivity() {
    private lateinit var sensorDataManager: SensorDataManager
    private lateinit var mqttManager: MqttManager
    private lateinit var settingsManager: SettingsManager

    private lateinit var txtPhValue: TextView
    private lateinit var txtEcValue: TextView
    private lateinit var txtTempValue: TextView
    private lateinit var txtTankLevel: TextView
    private lateinit var progressTank: ProgressBar

    private lateinit var switchPhDown: Switch
    private lateinit var switchFertilizerA: Switch
    private lateinit var switchFertilizerB: Switch
    private lateinit var switchCirculation: Switch

    private var isUpdatingFromData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        sensorDataManager = SensorDataManager()
        settingsManager = SettingsManager(this)
        mqttManager = MqttManager()

        setupUI()
        setupObservers()
        connectToMqtt()
    }

    private fun setupUI() {
        txtPhValue = findViewById(R.id.txt_ph_value)
        txtEcValue = findViewById(R.id.txt_ec_value)
        txtTempValue = findViewById(R.id.txt_temp_value)
        txtTankLevel = findViewById(R.id.txt_tank_level)
        progressTank = findViewById(R.id.progress_tank)

        switchPhDown = findViewById(R.id.switch_ph_down)
        switchFertilizerA = findViewById(R.id.switch_fertilizer_a)
        switchFertilizerB = findViewById(R.id.switch_fertilizer_b)
        switchCirculation = findViewById(R.id.switch_circulation)

        val btnReset = findViewById<Button>(R.id.btn_reset)

        switchPhDown.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(phDown = isChecked)
                mqttManager.publish(Constants.TOPIC_CTRL_PH_DOWN, if (isChecked) "1" else "0")
            }
        }

        switchFertilizerA.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(fertilizerA = isChecked)
                mqttManager.publish(Constants.TOPIC_CTRL_FERT_A, if (isChecked) "1" else "0")
            }
        }

        switchFertilizerB.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(fertilizerB = isChecked)
                mqttManager.publish(Constants.TOPIC_CTRL_FERT_B, if (isChecked) "1" else "0")
            }
        }

        switchCirculation.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(circulation = isChecked)
                mqttManager.publish(Constants.TOPIC_CTRL_CIRC, if (isChecked) "1" else "0")
            }
        }

        btnReset.setOnClickListener {
            sensorDataManager.reset()
            mqttManager.publish(Constants.TOPIC_CTRL_RESET, "1")
            Toast.makeText(this, "Sistem sifirlandirildi", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            sensorDataManager.sensorData.collect { data ->
                txtPhValue.text = "pH: ${String.format("%.1f", data.ph)}"
                txtEcValue.text = "EC: ${String.format("%.1f", data.ec)}"
                txtTempValue.text = "${String.format("%.1f", data.temperature)}C"
                txtTankLevel.text = "${data.tankLevel}%"
                progressTank.progress = data.tankLevel
            }
        }

        lifecycleScope.launch {
            sensorDataManager.pumpStatus.collect { status ->
                isUpdatingFromData = true
                switchPhDown.isChecked = status.phDownRunning
                switchFertilizerA.isChecked = status.fertilizerARunning
                switchFertilizerB.isChecked = status.fertilizerBRunning
                switchCirculation.isChecked = status.circulationRunning
                isUpdatingFromData = false
            }
        }

        lifecycleScope.launch {
            mqttManager.connectionState.collect { connected ->
                val msg = if (connected) "Bagli" else "Baglanti Kesildi"
                title = "Marul Otomasyon - $msg"
            }
        }
    }

    private fun connectToMqtt() {
        val host = settingsManager.getMqttHost()
        val port = settingsManager.getMqttPort()

        // Broker ayarlanmadıysa Ayarlar ekranına yönlendir
        if (host == Constants.MQTT_DEFAULT_HOST || host.isBlank()) {
            Toast.makeText(this, "Lütfen MQTT broker IP'sini Ayarlar'dan girin", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }

        mqttManager.connect(host, port, object : MqttCallback {
            override fun onConnected() {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "MQTT Baglandi ($host:$port)", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "MQTT Baglantisi Kesildi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onMessageReceived(topic: String, payload: String) {
                runOnUiThread { updateSensorData(topic, payload) }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "Hata: $message", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateSensorData(topic: String, payload: String) {
        when (topic) {
            Constants.TOPIC_PH       -> payload.toFloatOrNull()?.let { sensorDataManager.updatePh(it) }
            Constants.TOPIC_EC       -> payload.toFloatOrNull()?.let { sensorDataManager.updateEc(it) }
            Constants.TOPIC_TEMP     -> payload.toFloatOrNull()?.let { sensorDataManager.updateTemperature(it) }
            Constants.TOPIC_TANK     -> payload.toFloatOrNull()?.let { sensorDataManager.updateTankLevel(it.toInt()) }
            Constants.TOPIC_FLOW_A   -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertilizerAFlowRate(it) }
            Constants.TOPIC_FLOW_B   -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertilizerBFlowRate(it) }
            Constants.TOPIC_FLOW_PH  -> payload.toFloatOrNull()?.let { sensorDataManager.updatePhFlowRate(it) }
            Constants.TOPIC_STATUS_PH_DOWN -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(phDown = payload == "1")
                isUpdatingFromData = false
            }
            Constants.TOPIC_STATUS_FERT_A -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(fertilizerA = payload == "1")
                isUpdatingFromData = false
            }
            Constants.TOPIC_STATUS_FERT_B -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(fertilizerB = payload == "1")
                isUpdatingFromData = false
            }
            Constants.TOPIC_STATUS_CIRC -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(circulation = payload == "1")
                isUpdatingFromData = false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ayarlar ekranından dönüldüğünde bağlantıyı yenile
        if (!mqttManager.isConnected()) {
            connectToMqtt()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}
