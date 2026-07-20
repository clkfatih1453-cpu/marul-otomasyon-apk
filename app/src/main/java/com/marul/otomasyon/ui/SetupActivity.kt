package com.marul.otomasyon.ui

import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.SimpleAdapter
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.BluetoothCallback
import com.marul.otomasyon.manager.BluetoothManager
import com.marul.otomasyon.manager.SettingsManager
import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

class SetupActivity : AppCompatActivity(), BluetoothCallback {
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var settingsManager: SettingsManager
    private lateinit var progressBar: ProgressBar
    private lateinit var deviceListView: ListView
    private lateinit var edtSsid: EditText
    private lateinit var edtPassword: EditText
    private lateinit var edtPhMax: EditText
    private lateinit var edtPhMin: EditText
    private lateinit var edtEcMin: EditText
    private lateinit var statusText: TextView

    private var selectedDeviceAddress: String = ""
    private val deviceList = mutableListOf<Map<String, String>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        bluetoothManager = BluetoothManager(this)
        settingsManager = SettingsManager(this)

        setupUI()
        loadSavedSettings()
        setupBluetooth()
    }

    @SuppressLint("MissingPermission")
    private fun setupUI() {
        progressBar = findViewById(R.id.progress_bar)
        deviceListView = findViewById(R.id.device_list)
        edtSsid = findViewById(R.id.edt_ssid)
        edtPassword = findViewById(R.id.edt_password)
        edtPhMax = findViewById(R.id.edt_ph_max)
        edtPhMin = findViewById(R.id.edt_ph_min)
        edtEcMin = findViewById(R.id.edt_ec_min)
        statusText = findViewById(R.id.status_text)

        val btnScan = findViewById<Button>(R.id.btn_scan)
        val btnConnect = findViewById<Button>(R.id.btn_connect)
        val btnSendWifi = findViewById<Button>(R.id.btn_send_wifi)

        btnScan.setOnClickListener {
            if (!bluetoothManager.isBluetoothEnabled()) {
                @Suppress("DEPRECATION")
                startActivity(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                return@setOnClickListener
            }
            if (bluetoothManager.isBluetoothSupported()) {
                deviceList.clear()
                progressBar.progress = 0
                statusText.text = getString(R.string.msg_scanning)
                bluetoothManager.startScan(this)
            } else {
                Toast.makeText(this, getString(R.string.error_bluetooth_not_available), Toast.LENGTH_SHORT).show()
            }
        }

        btnConnect.setOnClickListener {
            if (deviceList.isNotEmpty()) {
                selectedDeviceAddress = deviceList[deviceListView.checkedItemPosition]["address"] ?: ""
                if (selectedDeviceAddress.isNotEmpty()) {
                    bluetoothManager.stopScan()
                    bluetoothManager.connectToDevice(selectedDeviceAddress)
                    statusText.text = getString(R.string.msg_scanning)
                }
            }
        }

        btnSendWifi.setOnClickListener {
            val ssid = edtSsid.text.toString()
            val password = edtPassword.text.toString()
            val phMax = edtPhMax.text.toString().toFloatOrNull() ?: Constants.DEFAULT_PH_MAX
            val phMin = edtPhMin.text.toString().toFloatOrNull() ?: Constants.DEFAULT_PH_MIN
            val ecMin = edtEcMin.text.toString().toFloatOrNull() ?: Constants.DEFAULT_EC_MIN

            if (ssid.isNotEmpty() && password.isNotEmpty()) {
                sendWifiConfigToDevice(ssid, password, phMax, phMin, ecMin)
            } else {
                Toast.makeText(this, getString(R.string.error_invalid_wifi), Toast.LENGTH_SHORT).show()
            }
        }

        deviceListView.adapter = SimpleAdapter(
            this,
            deviceList,
            android.R.layout.simple_list_item_single_choice,
            arrayOf("name"),
            intArrayOf(android.R.id.text1)
        )
        deviceListView.choiceMode = ListView.CHOICE_MODE_SINGLE
    }

    private fun setupBluetooth() {
        if (!bluetoothManager.isBluetoothSupported()) {
            Toast.makeText(this, getString(R.string.error_bluetooth_not_available), Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadSavedSettings() {
        val config = settingsManager.getWifiConfig()
        edtSsid.setText(config.ssid)
        edtPassword.setText(config.password)
        edtPhMax.setText(config.phMax.toString())
        edtPhMin.setText(config.phMin.toString())
        edtEcMin.setText(config.ecMin.toString())
    }

    @SuppressLint("MissingPermission")
    private fun sendWifiConfigToDevice(ssid: String, password: String, phMax: Float, phMin: Float, ecMin: Float) {
        val savedConfig = settingsManager.getWifiConfig()
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                bluetoothManager.writeCharacteristic(Constants.CHAR_SSID_UUID, ssid)
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_PASSWORD_UUID, password)
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_PH_MAX_UUID, phMax.toString())
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_PH_MIN_UUID, phMin.toString())
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_EC_MIN_UUID, ecMin.toString())
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_DOSAGE_TIME_UUID, savedConfig.dosageTime.toString())
                delay(100)
                bluetoothManager.writeCharacteristic(Constants.CHAR_MEASUREMENT_PERIOD_UUID, savedConfig.measurementPeriod.toString())

                settingsManager.saveWifiConfig(
                    com.marul.otomasyon.model.WifiConfig(
                        ssid = ssid,
                        password = password,
                        phMax = phMax,
                        phMin = phMin,
                        ecMin = ecMin,
                        dosageTime = savedConfig.dosageTime,
                        measurementPeriod = savedConfig.measurementPeriod
                    )
                )

                runOnUiThread {
                    Toast.makeText(this@SetupActivity, getString(R.string.msg_saved), Toast.LENGTH_SHORT).show()
                    statusText.text = "WiFi Ayarları Gönderildi!\nCihaz Yeniden Başlatılıyor..."
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this@SetupActivity, e.message, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onDeviceFound(deviceName: String, deviceAddress: String) {
        runOnUiThread {
            deviceList.add(mapOf("name" to deviceName, "address" to deviceAddress))
            (deviceListView.adapter as SimpleAdapter).notifyDataSetChanged()
        }
    }

    override fun onConnected() {
        runOnUiThread {
            statusText.text = getString(R.string.msg_connected)
            Toast.makeText(this, getString(R.string.msg_connected), Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDisconnected() {
        runOnUiThread {
            statusText.text = getString(R.string.msg_disconnected)
        }
    }

    override fun onDataReceived(data: String) {
        runOnUiThread {
            statusText.text = "Veri: $data"
        }
    }

    override fun onError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothManager.disconnect()
    }
}
