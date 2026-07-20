package com.marul.otomasyon.ui

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.marul.otomasyon.R
import com.marul.otomasyon.manager.SettingsManager

class MainActivity : AppCompatActivity() {
    private lateinit var settingsManager: SettingsManager
    private lateinit var statusText: TextView

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            navigateToSetup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        settingsManager = SettingsManager(this)
        setupUI()
        checkPermissions()
    }

    private fun setupUI() {
        statusText = findViewById(R.id.status_text)
        val btnSetup = findViewById<Button>(R.id.btn_setup)
        val btnControl = findViewById<Button>(R.id.btn_control)
        val btnSettings = findViewById<Button>(R.id.btn_settings)

        val wifiConfig = settingsManager.getWifiConfig()
        statusText.text = if (wifiConfig.ssid.isNotEmpty()) {
            "WiFi: ${wifiConfig.ssid}\n${getString(R.string.status_online)}"
        } else {
            getString(R.string.status_offline)
        }

        btnSetup.setOnClickListener {
            navigateToSetup()
        }

        btnControl.setOnClickListener {
            startActivity(Intent(this, ControlActivity::class.java))
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
    }

    private fun checkPermissions() {
        val permissions = mutableListOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_NETWORK_STATE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions.add(Manifest.permission.BLUETOOTH_SCAN)
            permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val needsRequest = permissions.any {
            android.content.pm.PackageManager.PERMISSION_DENIED ==
            androidx.core.content.ContextCompat.checkSelfPermission(this, it)
        }

        if (needsRequest) {
            permissionLauncher.launch(permissions.toTypedArray())
        }
    }

    private fun navigateToSetup() {
        startActivity(Intent(this, SetupActivity::class.java))
    }
}
