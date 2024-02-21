/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.manager

import android.content.Context
import android.hardware.GeomagneticField
import android.hardware.Sensor
import android.hardware.Sensor.*
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.*
import android.location.Location
import android.os.Build.VERSION.SDK_INT
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.SystemClock
import android.os.WorkSource
import android.util.Log
import android.view.Surface
import android.view.WindowManager
import androidx.core.content.getSystemService
import androidx.core.location.LocationCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.DeviceOrientation
import com.google.android.gms.location.DeviceOrientationRequest
import com.google.android.gms.location.IDeviceOrientationListener
import com.google.android.gms.location.internal.ClientIdentity
import com.google.android.gms.location.internal.DeviceOrientationRequestInternal
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.microg.gms.location.formatDuration
import org.microg.gms.utils.WorkSourceUtil
import java.io.PrintWriter
import kotlin.math.*

class DeviceOrientationManager(private val context: Context, override val lifecycle: Lifecycle) : LifecycleOwner, SensorEventListener, IBinder.DeathRecipient {
    private var lock = Mutex(false)
    private var started: Boolean = false
    private var sensors: Set<Sensor>? = null
    private var handlerThread: HandlerThread? = null
    private val requests = mutableMapOf<IBinder, DeviceOrientationRequestHolder>()

    suspend fun add(clientIdentity: ClientIdentity, request: DeviceOrientationRequestInternal, listener: IDeviceOrientationListener) {
        listener.asBinder().linkToDeath(this, 0)
        lock.withLock {
            requests[listener.asBinder()] = DeviceOrientationRequestHolder(clientIdentity, request.request, listener)
            updateStatus()
        }
    }

    suspend fun remove(clientIdentity: ClientIdentity, listener: IDeviceOrientationListener) {
        listener.asBinder().unlinkToDeath(this, 0)
        lock.withLock {
            requests.remove(listener.asBinder())
            updateStatus()
        }
    }

    private fun SensorManager.registerListener(sensor: Sensor, handler: Handler) {
        if (SDK_INT >= 19) {
            registerListener(this@DeviceOrientationManager, sensor, SAMPLING_PERIOD_US, MAX_REPORT_LATENCY_US, handler)
        } else {
            registerListener(this@DeviceOrientationManager, sensor, SAMPLING_PERIOD_US, handler)
        }
    }

    private fun updateStatus() {
        if (requests.isNotEmpty() && !started) {
            try {
                val sensorManager = context.getSystemService<SensorManager>() ?: return
                val sensors = mutableSetOf<Sensor>()
                if (SDK_INT >= 33) {
                    sensorManager.getDefaultSensor(TYPE_HEADING)?.let { sensors.add(it) }
                }
                if (sensors.isEmpty()) {
                    sensorManager.getDefaultSensor(TYPE_ROTATION_VECTOR)?.let { sensors.add(it) }
                }
                if (sensors.isEmpty()) {
                    sensors.add(sensorManager.getDefaultSensor(TYPE_MAGNETIC_FIELD) ?: return)
                    sensors.add(sensorManager.getDefaultSensor(TYPE_ACCELEROMETER) ?: return)
                }
                handlerThread = HandlerThread("DeviceOrientation")
                handlerThread!!.start()
                val handler = Handler(handlerThread!!.looper)
                for (sensor in sensors) {
                    sensorManager.registerListener(sensor, handler)
                }
                this.sensors = sensors
                started = true
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        } else if (requests.isEmpty() && started) {
            stop()
        }
    }

    override fun binderDied() {
        lifecycleScope.launchWhenStarted {
            val toRemove = requests.keys.filter { !it.isBinderAlive }.toList()
            for (binder in toRemove) {
                requests.remove(binder)
            }
            updateStatus()
        }
    }

    private var location: Location? = null
    fun onLocationChanged(location: Location) {
        this.location = location
        updateHeading()
    }

    private var accelerometerValues = FloatArray(3)
    private var accelerometerRealtimeNanos = 0L
    private fun handleAccelerometerEvent(event: SensorEvent) {
        event.values.copyInto(accelerometerValues)
        accelerometerRealtimeNanos = event.timestamp
        updateAzimuth()
    }

    private var magneticFieldValues = FloatArray(3)
    private var magneticRealtimeNanos = 0L
    private fun handleMagneticEvent(event: SensorEvent) {
        event.values.copyInto(magneticFieldValues)
        magneticRealtimeNanos = event.timestamp
        updateAzimuth()
    }

    private var azimuths = FloatArray(5)
    private var azimuthIndex = 0
    private var hadAzimuths = false
    private var azimuth = Float.NaN
    private var azimuthRealtimeNanos = 0L
    private var azimuthAccuracy = Float.NaN
    private fun updateAzimuth() {
        if (accelerometerRealtimeNanos == 0L || magneticRealtimeNanos == 0L) return
        var r = FloatArray(9)
        val i = FloatArray(9)
        if (getRotationMatrix(r, i, accelerometerValues, magneticFieldValues)) {
            r = remapForOrientation(r)
            val values = FloatArray(3)
            getOrientation(r, values)
            azimuths[azimuthIndex] = values[0]
            if (azimuthIndex == azimuths.size - 1) {
                azimuthIndex = 0
                hadAzimuths = true
            } else {
                azimuthIndex++
            }
            var sumSin = 0.0
            var sumCos = 0.0
            for (j in 0 until (if (hadAzimuths) azimuths.size else azimuthIndex)) {
                sumSin = sin(azimuths[j].toDouble())
                sumCos = cos(azimuths[j].toDouble())
            }
            azimuth = Math.toDegrees(atan2(sumSin, sumCos)).toFloat()
            azimuthRealtimeNanos = max(accelerometerRealtimeNanos, magneticRealtimeNanos)
            updateHeading()
        }
    }

    private fun remapForOrientation(r: FloatArray): FloatArray {
        val display = context.getSystemService<WindowManager>()?.defaultDisplay
        fun remap(x: Int, y: Int) = FloatArray(9).also { remapCoordinateSystem(r, x, y, it) }
        return when (display?.rotation) {
            Surface.ROTATION_90 -> remap(AXIS_Y, AXIS_MINUS_X)
            Surface.ROTATION_180 -> remap(AXIS_MINUS_X, AXIS_MINUS_Y)
            Surface.ROTATION_270 -> remap(AXIS_MINUS_Y, AXIS_X)
            else -> r
        }
    }

    private fun handleRotationVectorEvent(event: SensorEvent) {
        val v = FloatArray(3)
        event.values.copyInto(v, endIndex = 3)
        var r = FloatArray(9)
        getRotationMatrixFromVector(r, v)
        r = remapForOrientation(r)
        val values = FloatArray(3)
        getOrientation(r, values)
        azimuth = Math.toDegrees(values[0].toDouble()).toFloat()
        azimuthRealtimeNanos = event.timestamp
        if (SDK_INT >= 18 && values.size >= 5 && values[4] != -1f) {
            azimuthAccuracy = Math.toDegrees(values[4].toDouble()).toFloat()
        }
        updateHeading()
    }

    private var heading = Float.NaN
    private var headingAccuracy = Float.NaN
    private var headingRealtimeNanos = 0L
    private fun updateHeading() {
        if (!azimuth.isNaN()) {
            if (location == null) {
                heading = azimuth
                headingAccuracy = azimuthAccuracy.takeIf { !it.isNaN() } ?: 90.0f
                headingRealtimeNanos = azimuthRealtimeNanos
            } else {
                heading = azimuth + location!!.run { GeomagneticField(latitude.toFloat(), longitude.toFloat(), altitude.toFloat(), time).declination }
                headingAccuracy = azimuthAccuracy.takeIf { !it.isNaN() } ?: 45.0f
                headingRealtimeNanos = max(LocationCompat.getElapsedRealtimeNanos(location!!), azimuthRealtimeNanos)
            }
            updateDeviceOrientation()
        }
    }

    private fun handleHeadingEvent(event: SensorEvent) {
        heading = event.values[0]
        headingAccuracy = event.values[1]
        headingRealtimeNanos = event.timestamp
        updateDeviceOrientation()
    }

    private fun updateDeviceOrientation() {
        val deviceOrientation = DeviceOrientation()
        deviceOrientation.headingDegrees = heading
        deviceOrientation.headingErrorDegrees = headingAccuracy
        deviceOrientation.elapsedRealtimeNanos = headingRealtimeNanos
        lifecycleScope.launchWhenStarted {
            processNewDeviceOrientation(deviceOrientation)
        }
    }

    override fun onSensorChanged(event: SensorEvent) {
        when (event.sensor.type) {
            TYPE_ACCELEROMETER -> handleAccelerometerEvent(event)
            TYPE_MAGNETIC_FIELD -> handleMagneticEvent(event)
            TYPE_ROTATION_VECTOR -> handleRotationVectorEvent(event)
            TYPE_HEADING -> handleHeadingEvent(event)
            else -> return
        }
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        if (sensor.type == TYPE_ROTATION_VECTOR) {
            azimuthAccuracy = when (accuracy) {
                SENSOR_STATUS_ACCURACY_LOW -> 45.0f
                SENSOR_STATUS_ACCURACY_MEDIUM -> 30.0f
                SENSOR_STATUS_ACCURACY_HIGH -> 15.0f
                else -> Float.NaN
            }
        }
    }

    fun stop() {
        if (SDK_INT >= 18) handlerThread?.looper?.quitSafely()
        else handlerThread?.looper?.quit()
        context.getSystemService<SensorManager>()?.unregisterListener(this)
        started = false
    }

    fun dump(writer: PrintWriter) {
        writer.println("Current device orientation request (started=$started, sensors=${sensors?.map { it.name }})")
        for (request in requests.values.toList()) {
            writer.println("- ${request.workSource} (pending: ${request.updatesPending.let { if (it == Int.MAX_VALUE) "\u221e" else "$it" }} ${request.timePendingMillis.formatDuration()})")
        }
    }

    suspend fun processNewDeviceOrientation(deviceOrientation: DeviceOrientation) {
        lock.withLock {
            val toRemove = mutableSetOf<IBinder>()
            for ((binder, holder) in requests) {
                try {
                    holder.processNewDeviceOrientation(deviceOrientation)
                } catch (e: Exception) {
                    toRemove.add(binder)
                }
            }
            for (binder in toRemove) {
                requests.remove(binder)
            }
            if (toRemove.isNotEmpty()) {
                updateStatus()
            }
        }
    }

    companion object {
        const val SAMPLING_PERIOD_US = 20_000
        const val MAX_REPORT_LATENCY_US = 200_000

        private class DeviceOrientationRequestHolder(
            private val clientIdentity: ClientIdentity,
            private val request: DeviceOrientationRequest,
            private val listener: IDeviceOrientationListener,
        ) {
            private var updates = 0
            private var lastOrientation: DeviceOrientation? = null

            val updatesPending: Int
                get() = request.numUpdates - updates
            val timePendingMillis: Long
                get() = request.expirationTime - SystemClock.elapsedRealtime()
            val workSource = WorkSource().also { WorkSourceUtil.add(it, clientIdentity.uid, clientIdentity.packageName) }

            fun processNewDeviceOrientation(deviceOrientation: DeviceOrientation) {
                if (timePendingMillis < 0) throw RuntimeException("duration limit reached (expired at ${request.expirationTime}, now is ${SystemClock.elapsedRealtime()})")
                if (lastOrientation != null && abs(lastOrientation!!.headingDegrees - deviceOrientation.headingDegrees) < Math.toDegrees(request.smallestAngleChangeRadians.toDouble())) return
                if (lastOrientation == deviceOrientation) return
                listener.onDeviceOrientationChanged(deviceOrientation)
                if (request.numUpdates != Int.MAX_VALUE) updates++
                if (updatesPending <= 0) throw RuntimeException("max updates reached")
            }
        }
    }
}