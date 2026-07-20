package com.marul.otomasyon.manager

import com.marul.otomasyon.model.PumpStatus
import com.marul.otomasyon.model.SensorData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class SensorDataManager {
    private val _sensorData = MutableStateFlow(SensorData())
    val sensorData: StateFlow<SensorData> = _sensorData

    private val _pumpStatus = MutableStateFlow(PumpStatus())
    val pumpStatus: StateFlow<PumpStatus> = _pumpStatus

    fun updatePh(value: Float) {
        _sensorData.value = _sensorData.value.copy(ph = value)
    }

    fun updateEc(value: Float) {
        _sensorData.value = _sensorData.value.copy(ec = value)
    }

    fun updateTemperature(value: Float) {
        _sensorData.value = _sensorData.value.copy(temperature = value)
    }

    fun updateTankLevel(value: Int) {
        _sensorData.value = _sensorData.value.copy(tankLevel = value.coerceIn(0, 100))
    }

    fun updatePhFlowRate(value: Float) {
        _sensorData.value = _sensorData.value.copy(phFlowRate = value)
    }

    fun updateFertilizerAFlowRate(value: Float) {
        _sensorData.value = _sensorData.value.copy(fertiilizerAFlowRate = value)
    }

    fun updateFertilizerBFlowRate(value: Float) {
        _sensorData.value = _sensorData.value.copy(fertilizerBFlowRate = value)
    }

    fun setPumpStatus(
        phDown: Boolean = _pumpStatus.value.phDownRunning,
        fertilizerA: Boolean = _pumpStatus.value.fertilizerARunning,
        fertilizerB: Boolean = _pumpStatus.value.fertilizerBRunning,
        circulation: Boolean = _pumpStatus.value.circulationRunning
    ) {
        _pumpStatus.value = PumpStatus(
            phDownRunning = phDown,
            fertilizerARunning = fertilizerA,
            fertilizerBRunning = fertilizerB,
            circulationRunning = circulation
        )
    }

    fun reset() {
        _sensorData.value = SensorData()
        _pumpStatus.value = PumpStatus()
    }
}
