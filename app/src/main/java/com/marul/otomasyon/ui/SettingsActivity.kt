package com.marul.otomasyon.ui

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants

class SettingsActivity : Activity() {
    private lateinit var settingsManager: SettingsManager
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
        edtPhMax = findViewById(R.id.edt_ph_max)
        edtPhMin = findViewById(R.id.edt_ph_min)
        edtEcMin = findViewById(R.id.edt_ec_min)
        edtDosageTime = findViewById(R.id.edt_dosage_time)
        edtMeasurementPeriod = findViewById(R.id.edt_measurement_period)

        val btnSave = findViewById<Button>(R.id.btn_save)
        btnSave.setOnClickListener {
            saveSettings()
        }
    }

    private fun loadSettings() {
        val config = settingsManager.getWifiConfig()
        edtPhMax.setText(config.phMax.toString())
        edtPhMin.setText(config.phMin.toString())
        edtEcMin.setText(config.ecMin.toString())
        edtDosageTime.setText(config.dosageTime.toString())
        edtMeasurementPeriod.setText(config.measurementPeriod.toString())
    }

    private fun saveSettings() {
        try {
            val phMax = edtPhMax.text.toString().toFloat()
            val phMin = edtPhMin.text.toString().toFloat()
            val ecMin = edtEcMin.text.toString().toFloat()
            val dosageTime = edtDosageTime.text.toString().toLong()
            val measurementPeriod = edtMeasurementPeriod.text.toString().toLong()

            if (phMax <= phMin) {
                Toast.makeText(this, "pH Max, pH Min'den büyük olmalıdır", Toast.LENGTH_SHORT).show()
                return
            }

            settingsManager.updatePhMax(phMax)
            settingsManager.updatePhMin(phMin)
            settingsManager.updateEcMin(ecMin)
            settingsManager.updateDosageTime(dosageTime)
            settingsManager.updateMeasurementPeriod(measurementPeriod)

            Toast.makeText(this, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Geçersiz giriş: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
