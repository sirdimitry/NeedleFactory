package com.needlefactory.android

import android.app.Activity
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import com.needlefactory.presentation.NeedleLighting

/** Low-rate optional lighting input. Never changes physics, search cover or gameplay state. */
class NeedleTiltSensor(private val activity: Activity, private val lighting: NeedleLighting) : SensorEventListener {
    private val manager = activity.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val matrix = FloatArray(9)
    private var active: Sensor? = null

    fun start() {
        stop()
        val sensorManager = manager ?: return
        for (type in intArrayOf(Sensor.TYPE_ROTATION_VECTOR, Sensor.TYPE_GAME_ROTATION_VECTOR,
            Sensor.TYPE_GRAVITY, Sensor.TYPE_ACCELEROMETER)) {
            val candidate = sensorManager.getDefaultSensor(type) ?: continue
            if (sensorManager.registerListener(this, candidate, 30_000)) {
                active = candidate
                if (BuildConfig.DEBUG) Log.i("NeedleTilt", "lighting sensor=${candidate.name} type=$type requestedHz=33")
                return
            }
        }
        if (BuildConfig.DEBUG) Log.i("NeedleTilt", "No lighting sensor; neutral static metal reflection")
    }

    fun stop() {
        manager?.unregisterListener(this)
        active = null
        lighting.submit(0f, 0f)
    }

    @Suppress("DEPRECATION")
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor != active) return
        val x: Float
        val y: Float
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR || event.sensor.type == Sensor.TYPE_GAME_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(matrix, event.values)
            // R transforms device into world coordinates; row Z projects world-up onto device X/Y.
            x = matrix[6]; y = matrix[7]
        } else {
            x = event.values[0] / SensorManager.GRAVITY_EARTH
            y = event.values[1] / SensorManager.GRAVITY_EARTH
        }
        val rotation = if (Build.VERSION.SDK_INT >= 30) activity.display?.rotation ?: 0
            else activity.windowManager.defaultDisplay.rotation
        lighting.submit(x, y, rotation)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
