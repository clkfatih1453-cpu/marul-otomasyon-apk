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
import com.marul.otomasyon.manager.BlynkManager
import com.marul.otomasyon.manager.SensorDataManager
import com.marul.otomasyon.manager.SettingsManager
import kotlinx.coroutines.flow.collect
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
            sensorDataManager.setPumpStatus(phDown = isChecked)
        }

        switchFertilizerA.setOnCheckedChangeListener { _, isChecked ->
            sensorDataManager.setPumpStatus(fertilizerA = isChecked)
        }

        switchFertilizerB.setOnCheckedChangeListener { _, isChecked ->
            sensorDataManager.setPumpStatus(fertilizerB = isChecked)
        }

        switchCirculation.setOnCheckedChangeListener { _, isChecked ->
            sensorDataManager.setPumpStatus(circulation = isChecked)
        }

        btnReset.setOnClickListener {
            sensorDataManager.reset()
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
                switchPhDown.isChecked = status.phDownRunning
                switchFertilizerA.isChecked = status.fertilizerARunning
                switchFertilizerB.isChecked = status.fertilizerBRunning
                switchCirculation.isChecked = status.circulationRunning
            }
        }
    }

    private fun connectToBlynk() {
        blynkManager.connect(object : com.marul.otomasyon.manager.BlynkCallback {
            override fun onConnected() {
                Toast.makeText(this@ControlActivity, "Blynk Bağlandı", Toast.LENGTH_SHORT).show()
            }

            override fun onDisconnected() {
                Toast.makeText(this@ControlActivity, "Blynk Bağlantısı Kesildi", Toast.LENGTH_SHORT).show()
            }

            override fun onDataReceived(pin: Int, value: String) {
                updateSensorData(pin, value)
            }

            override fun onError(message: String) {
                Toast.makeText(this@ControlActivity, "Hata: $message", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateSensorData(pin: Int, value: String) {
        val floatValue = value.toFloatOrNull() ?: return
        when (pin) {
            0 -> sensorDataManager.updatePh(floatValue)
            1 -> sensorDataManager.updateEc(floatValue)
            2 -> sensorDataManager.updateTemperature(floatValue)
            11 -> sensorDataManager.updateTankLevel(floatValue.toInt())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        blynkManager.disconnect()
    }
}
