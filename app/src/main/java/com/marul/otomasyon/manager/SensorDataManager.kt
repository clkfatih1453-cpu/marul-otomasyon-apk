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

    fun updatePh(v: Float)              { _sensorData.value = _sensorData.value.copy(ph = v) }
    fun updateEc(v: Float)              { _sensorData.value = _sensorData.value.copy(ec = v) }
    fun updateTemperature(v: Float)     { _sensorData.value = _sensorData.value.copy(temperature = v) }
    fun updateHumidity(v: Float)        { _sensorData.value = _sensorData.value.copy(humidity = v) }
    fun updateWaterTemperature(v: Float){ _sensorData.value = _sensorData.value.copy(waterTemperature = v) }
    fun updateTankLevel(v: Int)         { _sensorData.value = _sensorData.value.copy(tankLevel = v.coerceIn(0, 100)) }
    fun updateWaterAdded(v: Float)      { _sensorData.value = _sensorData.value.copy(waterAddedLiters = v) }
    fun updatePhFlowRate(v: Float)      { _sensorData.value = _sensorData.value.copy(phFlowRate = v) }
    fun updateFertilizerAFlowRate(v: Float) { _sensorData.value = _sensorData.value.copy(fertilizerAFlowRate = v) }
    fun updateFertilizerBFlowRate(v: Float) { _sensorData.value = _sensorData.value.copy(fertilizerBFlowRate = v) }
    fun updateFertAMl(v: Float)         { _sensorData.value = _sensorData.value.copy(fertAMlTotal = v) }
    fun updateFertBMl(v: Float)         { _sensorData.value = _sensorData.value.copy(fertBMlTotal = v) }
    fun updateAcidMl(v: Float)          { _sensorData.value = _sensorData.value.copy(acidMlTotal = v) }

    fun setPumpStatus(
        phDown: Boolean = _pumpStatus.value.phDownRunning,
        fertilizerA: Boolean = _pumpStatus.value.fertilizerARunning,
        fertilizerB: Boolean = _pumpStatus.value.fertilizerBRunning,
        circulation: Boolean = _pumpStatus.value.circulationRunning
    ) {
        _pumpStatus.value = PumpStatus(phDown, fertilizerA, fertilizerB, circulation)
    }

    fun reset() {
        _sensorData.value = SensorData()
        _pumpStatus.value = PumpStatus()
    }
}
