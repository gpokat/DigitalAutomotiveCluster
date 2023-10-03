package com.example.simplecluster

import android.car.VehicleAreaType.VEHICLE_AREA_TYPE_GLOBAL
import android.car.VehiclePropertyIds
import android.car.hardware.CarPropertyConfig
import android.car.hardware.CarPropertyValue
import android.car.hardware.property.CarPropertyManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment


class SensorsData : Fragment() {
    private val TAG = "SR.SENSORDATA"
    private lateinit var mCarPropertyManager: CarPropertyManager


    private var mPropertyIdMap = hashMapOf(
        Pair(VehiclePropertyIds.PERF_VEHICLE_SPEED, 0.0F), //dynamic
        Pair(VehiclePropertyIds.EV_BATTERY_LEVEL, 0.0F), //dynamic
        Pair(VehiclePropertyIds.ENGINE_RPM, 0.0F), //dynamic. protected by signature permissions
        Pair(VehiclePropertyIds.ENGINE_OIL_TEMP, 0.0F) //dynamic. protected by signature permissions
        //Pair(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, 0.0F) //static. see updateStaticSensorsData method
    )

    private lateinit var speedometer: Speedometer

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val mActivity = (activity as ClusterMainView)
        val view = inflater.inflate(R.layout.fragment_speed, container, false)
        mCarPropertyManager = mActivity.getCarPropertyManager()
        speedometer = view.findViewById(R.id.speedometer)
        updateStaticSensorsData()
        return view
    }

    override fun onResume() {
        registerCarPropertyCallback()
        super.onResume()
    }

    override fun onPause() {
        unregisterCarPropertyCallback()
        super.onPause()
    }

    override fun onStop() {
        unregisterCarPropertyCallback()
        super.onStop()
    }

    override fun onDestroy() {
        unregisterCarPropertyCallback()
        super.onDestroy()
    }

    private val mPropertyCallback = object : CarPropertyManager.CarPropertyEventCallback {
        override fun onChangeEvent(carPropertyValue: CarPropertyValue<*>) {
            val currentPropValue = ((carPropertyValue.value as Float))
            val propId = carPropertyValue.propertyId
            if (mPropertyIdMap[propId] != currentPropValue) {
                Log.i(TAG, "Prop $propId Changed: $currentPropValue")
                mPropertyIdMap[propId] = currentPropValue
                updateSensorsData(propId)
            }
        }

        override fun onErrorEvent(propId: Int, zone: Int) {
            Log.e(TAG, "PropertyCallbackError, propId: $propId zone: $zone")
        }
    }

    private fun updateSensorsData(propId: Int) {
        when (propId) {
            VehiclePropertyIds.PERF_VEHICLE_SPEED -> speedometer.setSpeed(mPropertyIdMap[propId]!!.toInt())
            VehiclePropertyIds.EV_BATTERY_LEVEL -> speedometer.setBatteryLevel(mPropertyIdMap[propId]!!)
            VehiclePropertyIds.ENGINE_RPM -> speedometer.setEngineRpm(mPropertyIdMap[propId]!!)
            VehiclePropertyIds.ENGINE_OIL_TEMP -> speedometer.setEngineOilTemp(mPropertyIdMap[propId]!!)
        }
    }

    private fun updateStaticSensorsData() {
        val batteryCapacity = mCarPropertyManager.getFloatProperty(VehiclePropertyIds.INFO_EV_BATTERY_CAPACITY, VEHICLE_AREA_TYPE_GLOBAL)
        Log.i(TAG, "INFO_EV_BATTERY_CAPACITY, propValue: $batteryCapacity")
        speedometer.setBatteryCapacity(batteryCapacity)
    }

    private fun registerCarPropertyCallback() {
        for (mapItem in mPropertyIdMap) {

            val sensorUpdateRate =
                if (mCarPropertyManager.getCarPropertyConfig(mapItem.key)!!.changeMode == CarPropertyConfig.VEHICLE_PROPERTY_CHANGE_MODE_ONCHANGE)
                    CarPropertyManager.SENSOR_RATE_ONCHANGE else CarPropertyManager.SENSOR_RATE_NORMAL
            mCarPropertyManager.registerCallback(
                mPropertyCallback,
                mapItem.key,
                sensorUpdateRate
            )
        }
    }

    private fun unregisterCarPropertyCallback() {
        mCarPropertyManager.unregisterCallback(mPropertyCallback)
    }
   }