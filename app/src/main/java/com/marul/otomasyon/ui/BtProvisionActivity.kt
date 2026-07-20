package com.marul.otomasyon.ui

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.SettingsManager
import kotlinx.coroutines.*
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

class BtProvisionActivity : AppCompatActivity() {

    // SPP UUID - tüm Classic BT serial uygulamalarında aynı
    private val SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    private val TARGET_NAME = "InciTarim-Config"

    private lateinit var txtStatus: TextView
    private lateinit var viewIndicator: View
    private lateinit var btnConnect: Button
    private lateinit var btnSend: Button
    private lateinit var edtSsid: EditText
    private lateinit var edtPass: EditText
    private lateinit var edtMqttIp: EditText
    private lateinit var txtLog: TextView

    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var inputStream: InputStream? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val btAdapter: BluetoothAdapter? by lazy {
        BluetoothAdapter.getDefaultAdapter()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bt_provision)
        supportActionBar?.title = "ESP32 Yapılandır"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        txtStatus    = findViewById(R.id.txt_bt_status)
        viewIndicator= findViewById(R.id.view_bt_indicator)
        btnConnect   = findViewById(R.id.btn_bt_connect)
        btnSend      = findViewById(R.id.btn_send_config)
        edtSsid      = findViewById(R.id.edt_wifi_ssid)
        edtPass      = findViewById(R.id.edt_wifi_pass)
        edtMqttIp    = findViewById(R.id.edt_mqtt_ip)
        txtLog       = findViewById(R.id.txt_log)

        // MQTT IP'yi ayarlardan otomatik doldur
        val sm = SettingsManager(this)
        val savedMqtt = sm.getMqttHost()
        if (savedMqtt.isNotBlank()) edtMqttIp.setText(savedMqtt)

        btnConnect.setOnClickListener { connectBluetooth() }
        btnSend.setOnClickListener    { sendConfig() }
    }

    @SuppressLint("MissingPermission")
    private fun connectBluetooth() {
        if (!hasBtPermission()) {
            requestBtPermission()
            return
        }
        if (btAdapter == null || !btAdapter!!.isEnabled) {
            toast("Bluetooth kapalı, lütfen açın")
            return
        }

        // Eşleşmiş cihazlarda "InciTarim-Config" ara
        val paired: Set<BluetoothDevice> = btAdapter!!.bondedDevices ?: emptySet()
        val device = paired.firstOrNull { it.name == TARGET_NAME }

        if (device == null) {
            // Eşleşmiş değil — kullanıcıya listele
            showDevicePicker(paired)
            return
        }
        doConnect(device)
    }

    @SuppressLint("MissingPermission")
    private fun showDevicePicker(devices: Set<BluetoothDevice>) {
        if (devices.isEmpty()) {
            toast("Eşleşmiş BT cihazı yok. Önce telefondan '$TARGET_NAME' ile eşleşin.")
            return
        }
        val names = devices.map { it.name ?: it.address }.toTypedArray()
        val devList = devices.toList()
        AlertDialog.Builder(this)
            .setTitle("Cihaz Seç")
            .setItems(names) { _, i -> doConnect(devList[i]) }
            .show()
    }

    @SuppressLint("MissingPermission")
    private fun doConnect(device: BluetoothDevice) {
        setStatus("Bağlanıyor: ${device.name}…", "#FFA000")
        btnConnect.isEnabled = false
        scope.launch {
            try {
                btAdapter?.cancelDiscovery()
                socket = device.createRfcommSocketToServiceRecord(SPP_UUID)
                socket!!.connect()
                outputStream = socket!!.outputStream
                inputStream  = socket!!.inputStream
                withContext(Dispatchers.Main) {
                    setStatus("Bağlandı: ${device.name}", "#4CAF50")
                    btnSend.isEnabled = true
                    btnConnect.text = "Bağlantıyı Kes"
                    btnConnect.isEnabled = true
                    btnConnect.setOnClickListener { disconnect() }
                }
                listenForData()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setStatus("Bağlantı hatası: ${e.message}", "#F44336")
                    btnConnect.isEnabled = true
                    appendLog("HATA: ${e.message}")
                }
            }
        }
    }

    private fun listenForData() {
        scope.launch {
            val buf = ByteArray(256)
            var line = ""
            try {
                while (socket?.isConnected == true) {
                    val n = inputStream?.read(buf) ?: break
                    if (n <= 0) continue
                    val chunk = String(buf, 0, n)
                    line += chunk
                    while (line.contains('\n')) {
                        val idx = line.indexOf('\n')
                        val response = line.substring(0, idx).trim()
                        line = line.substring(idx + 1)
                        withContext(Dispatchers.Main) {
                            appendLog("← $response")
                            if (response.startsWith("SAVED")) {
                                toast("✅ Kaydedildi! ESP32 yeniden başlatılıyor…")
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    private fun sendConfig() {
        val ssid = edtSsid.text.toString().trim()
        val pass = edtPass.text.toString()
        val mqtt = edtMqttIp.text.toString().trim()

        if (ssid.isBlank() || pass.isBlank() || mqtt.isBlank()) {
            toast("Tüm alanları doldurun")
            return
        }

        scope.launch {
            try {
                sendLine("SSID:$ssid")
                delay(300)
                sendLine("PASS:$pass")
                delay(300)
                sendLine("MQTT:$mqtt")
                delay(300)
                sendLine("SAVE")
                withContext(Dispatchers.Main) {
                    appendLog("→ SSID:$ssid")
                    appendLog("→ PASS:****")
                    appendLog("→ MQTT:$mqtt")
                    appendLog("→ SAVE")
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { appendLog("Gönderim hatası: ${e.message}") }
            }
        }
    }

    private fun sendLine(cmd: String) {
        outputStream?.write("$cmd\n".toByteArray())
        outputStream?.flush()
    }

    private fun disconnect() {
        scope.launch {
            try { socket?.close() } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                setStatus("Bağlı değil", "#9E9E9E")
                btnSend.isEnabled = false
                btnConnect.text = "Bağlan"
                btnConnect.setOnClickListener { connectBluetooth() }
            }
        }
    }

    private fun setStatus(msg: String, color: String) {
        txtStatus.text = msg
        viewIndicator.backgroundTintList =
            android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor(color))
    }

    private fun appendLog(line: String) {
        val current = txtLog.text.toString()
        val updated = if (current == "—") line else "$current\n$line"
        txtLog.text = updated
    }

    private fun toast(msg: String) =
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

    private fun hasBtPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) ==
                    PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestBtPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.BLUETOOTH_SCAN),
                101
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            connectBluetooth()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    override fun onDestroy() {
        scope.cancel()
        try { socket?.close() } catch (_: Exception) {}
        super.onDestroy()
    }
}
