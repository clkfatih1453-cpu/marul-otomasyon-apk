package com.marul.otomasyon.manager

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.os.Build
import com.marul.otomasyon.util.Constants
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.UUID

interface BluetoothCallback {
    fun onDeviceFound(deviceName: String, deviceAddress: String)
    fun onConnected()
    fun onDisconnected()
    fun onDataReceived(data: String)
    fun onError(message: String)
}

class BluetoothManager(private val context: Context) {
    private val bluetoothAdapter: BluetoothAdapter?
    private val bluetoothLeScanner: BluetoothLeScanner?
    private var bluetoothGatt: BluetoothGatt? = null
    private var callback: BluetoothCallback? = null

    private val _scanState = MutableStateFlow<List<Pair<String, String>>>(emptyList())
    val scanState: StateFlow<List<Pair<String, String>>> = _scanState

    init {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
        bluetoothLeScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    fun isBluetoothSupported(): Boolean = bluetoothAdapter != null

    fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    fun startScan(callback: BluetoothCallback) {
        this.callback = callback
        _scanState.value = emptyList()
        bluetoothLeScanner?.startScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        bluetoothLeScanner?.stopScan(scanCallback)
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String) {
        val device = bluetoothAdapter?.getRemoteDevice(address)
        device?.connectGatt(context, false, gattCallback)
    }

    @SuppressLint("MissingPermission")
    fun disconnect() {
        bluetoothGatt?.disconnect()
        bluetoothGatt?.close()
        bluetoothGatt = null
    }

    @SuppressLint("MissingPermission")
    fun writeCharacteristic(uuid: String, value: String) {
        val characteristic = bluetoothGatt?.getService(UUID.fromString(Constants.SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(uuid))

        characteristic?.let {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                bluetoothGatt?.writeCharacteristic(it, value.toByteArray(Charsets.UTF_8), BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
            } else {
                @Suppress("DEPRECATION")
                it.value = value.toByteArray(Charsets.UTF_8)
                @Suppress("DEPRECATION")
                bluetoothGatt?.writeCharacteristic(it)
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun readCharacteristic(uuid: String) {
        val characteristic = bluetoothGatt?.getService(UUID.fromString(Constants.SERVICE_UUID))
            ?.getCharacteristic(UUID.fromString(uuid))
        
        characteristic?.let {
            bluetoothGatt?.readCharacteristic(it)
        }
    }

    private val scanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let {
                val deviceName = it.device.name ?: "Unknown"
                val deviceAddress = it.device.address
                
                if (deviceName.contains("ESP32", ignoreCase = true) || 
                    deviceName.contains("Marul", ignoreCase = true)) {
                    callback?.onDeviceFound(deviceName, deviceAddress)
                    
                    val currentDevices = _scanState.value.toMutableList()
                    if (!currentDevices.any { d -> d.second == deviceAddress }) {
                        currentDevices.add(Pair(deviceName, deviceAddress))
                        _scanState.value = currentDevices
                    }
                }
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            callback?.onError("Scan failed: $errorCode")
        }
    }

    private val gattCallback = object : BluetoothGattCallback() {
        @SuppressLint("MissingPermission")
        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            when (newState) {
                BluetoothGatt.STATE_CONNECTED -> {
                    bluetoothGatt = gatt
                    gatt?.discoverServices()
                    callback?.onConnected()
                }
                BluetoothGatt.STATE_DISCONNECTED -> {
                    callback?.onDisconnected()
                    bluetoothGatt = null
                }
            }
        }

        @SuppressLint("MissingPermission")
        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                gatt?.let {
                    val service = it.getService(UUID.fromString(Constants.SERVICE_UUID))
                    service?.let {
                        val characteristic = it.getCharacteristic(UUID.fromString(Constants.CHAR_DATA_UUID))
                        characteristic?.let {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                gatt.readCharacteristic(it)
                            }
                        }
                    }
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            @Suppress("DEPRECATION")
            super.onCharacteristicRead(gatt, characteristic, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                @Suppress("DEPRECATION")
                characteristic?.value?.let {
                    val data = String(it, Charsets.UTF_8)
                    callback?.onDataReceived(data)
                }
            }
        }

        // API 33+ için yeni callback
        override fun onCharacteristicRead(
            gatt: BluetoothGatt,
            characteristic: BluetoothGattCharacteristic,
            value: ByteArray,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, value, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                callback?.onDataReceived(String(value, Charsets.UTF_8))
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (status != BluetoothGatt.GATT_SUCCESS) {
                callback?.onError("Write failed: $status")
            }
        }
    }
}
