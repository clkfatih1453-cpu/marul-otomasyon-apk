package com.marul.otomasyon.ui

import android.app.AlertDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.MotionEvent
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
    private lateinit var txtHumidity: TextView
    private lateinit var txtWaterTemp: TextView
    private lateinit var txtTankLevel: TextView
    private lateinit var progressTank: ProgressBar
    private lateinit var txtWaterAdded: TextView
    private lateinit var txtFertAMl: TextView
    private lateinit var txtFertBMl: TextView
    private lateinit var txtAcidMl: TextView
    private lateinit var txtWaterAddedTotal: TextView
    private lateinit var btnDoseFertA: Button
    private lateinit var btnDoseFertB: Button
    private lateinit var btnDoseAcid: Button
    private lateinit var txtDoseFertA: TextView
    private lateinit var txtDoseFertB: TextView
    private lateinit var txtDoseAcid: TextView
    private lateinit var switchPhDown: Switch
    private lateinit var switchFertilizerA: Switch
    private lateinit var switchFertilizerB: Switch
    private lateinit var switchCirculation: Switch
    private lateinit var btnPumpTimer: Button
    private lateinit var txtPumpTimer: TextView
    private lateinit var btnLightTimer: Button
    private lateinit var txtLightTimer: TextView

    private var isUpdatingFromData = false
    private var currentFertATotal = 0f
    private var currentFertBTotal = 0f
    private var currentAcidTotal = 0f
    private var holdDoseA = false
    private var holdDoseB = false
    private var holdDoseAcid = false
    private var holdDoseAStartTotal = 0f
    private var holdDoseBStartTotal = 0f
    private var holdDoseAcidStartTotal = 0f

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
        txtPhValue         = findViewById(R.id.txt_ph_value)
        txtEcValue         = findViewById(R.id.txt_ec_value)
        txtTempValue       = findViewById(R.id.txt_temp_value)
        txtHumidity        = findViewById(R.id.txt_humidity)
        txtWaterTemp       = findViewById(R.id.txt_water_temp)
        txtTankLevel       = findViewById(R.id.txt_tank_level)
        progressTank       = findViewById(R.id.progress_tank)
        txtWaterAdded      = findViewById(R.id.txt_water_added)
        txtFertAMl         = findViewById(R.id.txt_fert_a_ml)
        txtFertBMl         = findViewById(R.id.txt_fert_b_ml)
        txtAcidMl          = findViewById(R.id.txt_acid_ml)
        txtWaterAddedTotal = findViewById(R.id.txt_water_added_total)
        btnDoseFertA       = findViewById(R.id.btn_dose_fert_a)
        btnDoseFertB       = findViewById(R.id.btn_dose_fert_b)
        btnDoseAcid        = findViewById(R.id.btn_dose_acid)
        txtDoseFertA       = findViewById(R.id.txt_dose_fert_a)
        txtDoseFertB       = findViewById(R.id.txt_dose_fert_b)
        txtDoseAcid        = findViewById(R.id.txt_dose_acid)
        switchPhDown       = findViewById(R.id.switch_ph_down)
        switchFertilizerA  = findViewById(R.id.switch_fertilizer_a)
        switchFertilizerB  = findViewById(R.id.switch_fertilizer_b)
        switchCirculation  = findViewById(R.id.switch_circulation)
        btnPumpTimer       = findViewById(R.id.btn_pump_timer)
        txtPumpTimer       = findViewById(R.id.txt_pump_timer)
        btnLightTimer      = findViewById(R.id.btn_light_timer)
        txtLightTimer      = findViewById(R.id.txt_light_timer)

        txtPumpTimer.text = settingsManager.getPumpTimer()
        txtLightTimer.text = settingsManager.getLightTimer()

        switchPhDown.setOnCheckedChangeListener { _, c ->
            if (!isUpdatingFromData) { sensorDataManager.setPumpStatus(phDown = c); mqttManager.publish(Constants.TOPIC_CTRL_PH_DOWN, if (c) "1" else "0") }
        }
        switchFertilizerA.setOnCheckedChangeListener { _, c ->
            if (!isUpdatingFromData) { sensorDataManager.setPumpStatus(fertilizerA = c); mqttManager.publish(Constants.TOPIC_CTRL_FERT_A, if (c) "1" else "0") }
        }
        switchFertilizerB.setOnCheckedChangeListener { _, c ->
            if (!isUpdatingFromData) { sensorDataManager.setPumpStatus(fertilizerB = c); mqttManager.publish(Constants.TOPIC_CTRL_FERT_B, if (c) "1" else "0") }
        }
        switchCirculation.setOnCheckedChangeListener { _, c ->
            if (!isUpdatingFromData) { sensorDataManager.setPumpStatus(circulation = c); mqttManager.publish(Constants.TOPIC_CTRL_CIRC, if (c) "1" else "0") }
        }

        setupHoldDoseButton(
            button = btnDoseFertA,
            label = txtDoseFertA,
            name = "Gubre A",
            topic = Constants.TOPIC_CTRL_FERT_A,
            startTotal = { holdDoseAStartTotal = currentFertATotal; holdDoseA = true },
            stopTotal = { holdDoseA = false },
            currentDelta = { (currentFertATotal - holdDoseAStartTotal).coerceAtLeast(0f) }
        )
        setupHoldDoseButton(
            button = btnDoseFertB,
            label = txtDoseFertB,
            name = "Gubre B",
            topic = Constants.TOPIC_CTRL_FERT_B,
            startTotal = { holdDoseBStartTotal = currentFertBTotal; holdDoseB = true },
            stopTotal = { holdDoseB = false },
            currentDelta = { (currentFertBTotal - holdDoseBStartTotal).coerceAtLeast(0f) }
        )
        setupHoldDoseButton(
            button = btnDoseAcid,
            label = txtDoseAcid,
            name = "Asit",
            topic = Constants.TOPIC_CTRL_PH_DOWN,
            startTotal = { holdDoseAcidStartTotal = currentAcidTotal; holdDoseAcid = true },
            stopTotal = { holdDoseAcid = false },
            currentDelta = { (currentAcidTotal - holdDoseAcidStartTotal).coerceAtLeast(0f) }
        )

        btnPumpTimer.setOnClickListener  { showTimerDialog("Devirdaim Pompasi", Constants.TOPIC_CTRL_PUMP_TMR, txtPumpTimer, Constants.PREF_PUMP_TIMER) }
        btnLightTimer.setOnClickListener { showTimerDialog("Spektrum Isik", Constants.TOPIC_CTRL_LIGHT_TMR, txtLightTimer, Constants.PREF_LIGHT_TIMER) }

        val btnReset = findViewById<Button>(R.id.btn_reset)
        btnReset.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Sistemi Sifirla")
                .setMessage("ESP32 yeniden baslatilacak. Emin misiniz?")
                .setPositiveButton("Evet") { _, _ ->
                    sensorDataManager.reset()
                    mqttManager.publish(Constants.TOPIC_CTRL_RESET, "1")
                    Toast.makeText(this, "Sistem sifirlaniyor...", Toast.LENGTH_SHORT).show()
                }
                .setNegativeButton("Iptal", null).show()
        }
    }

    private fun setupHoldDoseButton(
        button: Button,
        label: TextView,
        name: String,
        topic: String,
        startTotal: () -> Unit,
        stopTotal: () -> Unit,
        currentDelta: () -> Float
    ) {
        var pressing = false
        button.setOnTouchListener { _, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    pressing = true
                    startTotal()
                    mqttManager.publish(topic, "1")
                    label.text = "Verilen: 0.0 ml"
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (pressing) {
                        pressing = false
                        stopTotal()
                        mqttManager.publish(topic, "0")
                        val ml = currentDelta()
                        label.text = "Verilen: %.1f ml".format(ml)
                        Toast.makeText(this, "$name: %.1f ml verildi".format(ml), Toast.LENGTH_SHORT).show()
                    }
                    true
                }
                else -> false
            }
        }
    }

    private fun showTimerDialog(name: String, topic: String, label: TextView, prefKey: String) {
        var sh = 0; var sm = 0
        TimePickerDialog(this, { _, h, m ->
            sh = h; sm = m
            TimePickerDialog(this, { _, eh, em ->
                val timer = "%02d:%02d - %02d:%02d".format(sh, sm, eh, em)
                label.text = timer
                mqttManager.publish(topic, timer)
                settingsManager.saveTimer(prefKey, timer)
                Toast.makeText(this, name + " zamanlama: " + timer, Toast.LENGTH_SHORT).show()
            }, 22, 0, true).apply { setTitle(name + " - Bitis Saati") }.show()
        }, 6, 0, true).apply { setTitle(name + " - Baslangic Saati") }.show()
    }

    private fun setupObservers() {
        lifecycleScope.launch {
            sensorDataManager.sensorData.collect { d ->
                txtPhValue.text   = "%.1f".format(d.ph)
                txtEcValue.text   = "%.2f".format(d.ec)
                txtTempValue.text = "%.1f\u00b0C".format(d.temperature)
                txtHumidity.text  = "%.0f%%".format(d.humidity)
                txtWaterTemp.text = "%.1f\u00b0C".format(d.waterTemperature)
                txtTankLevel.text = d.tankLevel.toString() + "%"
                progressTank.progress = d.tankLevel
                txtWaterAdded.text = "Eklenen su: %.1f L".format(d.waterAddedLiters)

                val tankColor = when {
                    d.tankLevel > 60 -> Color.parseColor("#4CAF50")
                    d.tankLevel > 30 -> Color.parseColor("#FFA726")
                    else             -> Color.parseColor("#EF5350")
                }
                progressTank.progressTintList = ColorStateList.valueOf(tankColor)

                val phColor = when {
                    d.ph in 5.5f..6.2f -> Color.parseColor("#2E7D32")
                    d.ph > 0f          -> Color.parseColor("#E65100")
                    else               -> Color.parseColor("#999999")
                }
                txtPhValue.setTextColor(phColor)

                currentFertATotal = d.fertAMlTotal
                currentFertBTotal = d.fertBMlTotal
                currentAcidTotal = d.acidMlTotal
                txtFertAMl.text        = "%.1f ml".format(d.fertAMlTotal)
                txtFertBMl.text        = "%.1f ml".format(d.fertBMlTotal)
                txtAcidMl.text         = "%.1f ml".format(d.acidMlTotal)
                txtWaterAddedTotal.text = "%.1f L".format(d.waterAddedLiters)

                if (holdDoseA) txtDoseFertA.text = "Verilen: %.1f ml".format((currentFertATotal - holdDoseAStartTotal).coerceAtLeast(0f))
                if (holdDoseB) txtDoseFertB.text = "Verilen: %.1f ml".format((currentFertBTotal - holdDoseBStartTotal).coerceAtLeast(0f))
                if (holdDoseAcid) txtDoseAcid.text = "Verilen: %.1f ml".format((currentAcidTotal - holdDoseAcidStartTotal).coerceAtLeast(0f))
            }
        }

        lifecycleScope.launch {
            sensorDataManager.pumpStatus.collect { s ->
                isUpdatingFromData = true
                switchPhDown.isChecked      = s.phDownRunning
                switchFertilizerA.isChecked = s.fertilizerARunning
                switchFertilizerB.isChecked = s.fertilizerBRunning
                switchCirculation.isChecked = s.circulationRunning
                isUpdatingFromData = false
            }
        }

        lifecycleScope.launch {
            mqttManager.connectionState.collect { connected ->
                title = if (connected) "Inci Tarim - Bagli" else "Inci Tarim - Baglanti Yok"
            }
        }
    }

    private fun connectToMqtt() {
        val host = settingsManager.getMqttHost()
        val port = settingsManager.getMqttPort()
        val brokerId = settingsManager.getMqttBrokerId()
        val mqttUrl = settingsManager.getMqttUrl()
        val provider = settingsManager.getMqttProvider()
        val mqttUsername = settingsManager.getMqttUsername()
        val mqttPassword = settingsManager.getMqttPassword()
        val mqttUseTls = settingsManager.getMqttUseTls()
        if ((host == Constants.MQTT_DEFAULT_HOST || host.isBlank()) && mqttUrl.isBlank()) {
            Toast.makeText(this, "Lutfen MQTT broker IP veya MQTT URL girin", Toast.LENGTH_LONG).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        mqttManager.connect(host, port, object : MqttCallback {
            override fun onConnected()  { runOnUiThread { Toast.makeText(this@ControlActivity, "Baglandi", Toast.LENGTH_SHORT).show() } }
            override fun onDisconnected() { runOnUiThread { Toast.makeText(this@ControlActivity, "Baglanti kesildi", Toast.LENGTH_SHORT).show() } }
            override fun onMessageReceived(topic: String, payload: String) { runOnUiThread { updateSensorData(topic, payload) } }
            override fun onError(message: String) { runOnUiThread { Toast.makeText(this@ControlActivity, "Hata: $message", Toast.LENGTH_SHORT).show() } }
        }, brokerId, mqttUrl, provider, mqttUsername, mqttPassword, mqttUseTls)
    }

    private fun updateSensorData(topic: String, payload: String) {
        when (topic) {
            Constants.TOPIC_PH         -> payload.toFloatOrNull()?.let { sensorDataManager.updatePh(it) }
            Constants.TOPIC_EC         -> payload.toFloatOrNull()?.let { sensorDataManager.updateEc(it) }
            Constants.TOPIC_TEMP       -> payload.toFloatOrNull()?.let { sensorDataManager.updateTemperature(it) }
            Constants.TOPIC_HUMIDITY   -> payload.toFloatOrNull()?.let { sensorDataManager.updateHumidity(it) }
            Constants.TOPIC_WATER_TEMP -> payload.toFloatOrNull()?.let { sensorDataManager.updateWaterTemperature(it) }
            Constants.TOPIC_TANK       -> payload.toFloatOrNull()?.let { sensorDataManager.updateTankLevel(it.toInt()) }
            Constants.TOPIC_WATER_ADDED-> payload.toFloatOrNull()?.let { sensorDataManager.updateWaterAdded(it) }
            Constants.TOPIC_FLOW_A     -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertilizerAFlowRate(it) }
            Constants.TOPIC_FLOW_B     -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertilizerBFlowRate(it) }
            Constants.TOPIC_FLOW_PH    -> payload.toFloatOrNull()?.let { sensorDataManager.updatePhFlowRate(it) }
            Constants.TOPIC_FERT_A_ML  -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertAMl(it) }
            Constants.TOPIC_FERT_B_ML  -> payload.toFloatOrNull()?.let { sensorDataManager.updateFertBMl(it) }
            Constants.TOPIC_ACID_ML    -> payload.toFloatOrNull()?.let { sensorDataManager.updateAcidMl(it) }
            Constants.TOPIC_STATUS_PH_DOWN -> { isUpdatingFromData = true; sensorDataManager.setPumpStatus(phDown = payload == "1"); isUpdatingFromData = false }
            Constants.TOPIC_STATUS_FERT_A  -> { isUpdatingFromData = true; sensorDataManager.setPumpStatus(fertilizerA = payload == "1"); isUpdatingFromData = false }
            Constants.TOPIC_STATUS_FERT_B  -> { isUpdatingFromData = true; sensorDataManager.setPumpStatus(fertilizerB = payload == "1"); isUpdatingFromData = false }
            Constants.TOPIC_STATUS_CIRC    -> { isUpdatingFromData = true; sensorDataManager.setPumpStatus(circulation = payload == "1"); isUpdatingFromData = false }
        }
    }

    override fun onResume() {
        super.onResume()
        if (!mqttManager.isConnected()) connectToMqtt()
    }

    override fun onDestroy() {
        super.onDestroy()
        mqttManager.disconnect()
    }
}