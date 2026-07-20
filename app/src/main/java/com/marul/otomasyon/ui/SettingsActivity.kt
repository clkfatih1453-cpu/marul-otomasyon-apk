package com.marul.otomasyon.ui

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants

class SettingsActivity : AppCompatActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var edtMqttHost: EditText
    private lateinit var edtMqttPort: EditText
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
        val config = settingsManager.getWifiConfig()
        edtPhMax.setText(config.phMax.toString())
        edtPhMin.setText(config.phMin.toString())
        edtEcMin.setText(config.ecMin.toString())
        edtDosageTime.setText(config.dosageTime.toString())
        edtMeasurementPeriod.setText(config.measurementPeriod.toString())
    }

    private fun saveSettings() {
        try {
            val host = edtMqttHost.text.toString().trim()
            val port = edtMqttPort.text.toString().toIntOrNull() ?: Constants.MQTT_DEFAULT_PORT
            val phMax = edtPhMax.text.toString().toFloat()
            val phMin = edtPhMin.text.toString().toFloat()
            val ecMin = edtEcMin.text.toString().toFloat()
            val dosageTime = edtDosageTime.text.toString().toLong()
            val measurementPeriod = edtMeasurementPeriod.text.toString().toLong()

            if (host.isBlank()) {
                Toast.makeText(this, "Broker IP boş olamaz", Toast.LENGTH_SHORT).show()
                return
            }
            if (phMax <= phMin) {
                Toast.makeText(this, "pH Max, pH Min'den büyük olmalıdır", Toast.LENGTH_SHORT).show()
                return
            }

            settingsManager.saveMqttHost(host)
            settingsManager.saveMqttPort(port)
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
}
