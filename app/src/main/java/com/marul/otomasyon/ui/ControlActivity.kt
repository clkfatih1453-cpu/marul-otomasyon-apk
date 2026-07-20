package com.marul.otomasyon.ui

import android.os.Bundle
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.BlynkCallback
import com.marul.otomasyon.manager.BlynkManager
import com.marul.otomasyon.manager.SensorDataManager
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.launch

class ControlActivity : AppCompatActivity() {
    private lateinit var sensorDataManager: SensorDataManager
    private lateinit var blynkManager: BlynkManager
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

    // Observer'dan gelen güncellemeler listener'ı tekrar tetiklemesin
    private var isUpdatingFromData = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_control)

        sensorDataManager = SensorDataManager()
        settingsManager = SettingsManager(this)
        blynkManager = BlynkManager(settingsManager.getBlynkToken())

        setupUI()
        setupObservers()
        connectToBlynk()
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
                blynkManager.virtualWrite(Constants.V_PH_DOWN, if (isChecked) 1 else 0)
            }
        }

        switchFertilizerA.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(fertilizerA = isChecked)
                blynkManager.virtualWrite(Constants.V_GUBRE_A, if (isChecked) 1 else 0)
            }
        }

        switchFertilizerB.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(fertilizerB = isChecked)
                blynkManager.virtualWrite(Constants.V_GUBRE_B, if (isChecked) 1 else 0)
            }
        }

        switchCirculation.setOnCheckedChangeListener { _, isChecked ->
            if (!isUpdatingFromData) {
                sensorDataManager.setPumpStatus(circulation = isChecked)
                blynkManager.virtualWrite(Constants.V_CIRCULATION, if (isChecked) 1 else 0)
            }
        }

        btnReset.setOnClickListener {
            sensorDataManager.reset()
            blynkManager.virtualWrite(Constants.V_RESET, 1)
            Toast.makeText(this, "Sistem sıfırlandı", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            sensorDataManager.sensorData.collect { data ->
                txtPhValue.text = "pH: ${String.format("%.1f", data.ph)}"
                txtEcValue.text = "EC: ${String.format("%.1f", data.ec)}"
                txtTempValue.text = "${String.format("%.1f", data.temperature)}°C"
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
            blynkManager.connectionState.collect { connected ->
                val msg = if (connected) "🟢 Blynk Bağlı" else "🔴 Blynk Bağlantısı Kesildi"
                title = msg
            }
        }
    }

    private fun connectToBlynk() {
        blynkManager.connect(object : BlynkCallback {
            override fun onConnected() {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "Blynk Bağlandı", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDisconnected() {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "Blynk Bağlantısı Kesildi", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onDataReceived(pin: Int, value: String) {
                runOnUiThread { updateSensorData(pin, value) }
            }

            override fun onError(message: String) {
                runOnUiThread {
                    Toast.makeText(this@ControlActivity, "Hata: $message", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }

    private fun updateSensorData(pin: Int, value: String) {
        when (pin) {
            Constants.V_PH -> value.toFloatOrNull()?.let { sensorDataManager.updatePh(it) }
            Constants.V_EC -> value.toFloatOrNull()?.let { sensorDataManager.updateEc(it) }
            Constants.V_TEMP -> value.toFloatOrNull()?.let { sensorDataManager.updateTemperature(it) }
            Constants.V_TANK_LEVEL -> value.toFloatOrNull()?.let { sensorDataManager.updateTankLevel(it.toInt()) }
            Constants.V_FLOW_A -> value.toFloatOrNull()?.let { sensorDataManager.updateFertilizerAFlowRate(it) }
            Constants.V_FLOW_B -> value.toFloatOrNull()?.let { sensorDataManager.updateFertilizerBFlowRate(it) }
            Constants.V_FLOW_PH -> value.toFloatOrNull()?.let { sensorDataManager.updatePhFlowRate(it) }
            Constants.V_PH_DOWN -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(phDown = value == "1")
                isUpdatingFromData = false
            }
            Constants.V_GUBRE_A -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(fertilizerA = value == "1")
                isUpdatingFromData = false
            }
            Constants.V_GUBRE_B -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(fertilizerB = value == "1")
                isUpdatingFromData = false
            }
            Constants.V_CIRCULATION -> {
                isUpdatingFromData = true
                sensorDataManager.setPumpStatus(circulation = value == "1")
                isUpdatingFromData = false
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blynkManager.disconnect()
    }
}
