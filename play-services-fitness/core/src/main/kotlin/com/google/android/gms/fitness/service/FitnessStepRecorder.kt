/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.fitness.service

import android.Manifest
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Base64
import android.util.Log
import androidx.core.content.edit
import com.google.android.gms.common.internal.safeparcel.SafeParcelableSerializer
import com.google.android.gms.fitness.data.DataSource
import com.google.android.gms.fitness.data.DataType
import com.google.android.gms.fitness.data.Subscription

private const val TAG = "FitnessStepRecorder"
private const val PREFS_NAME = "fitness-step-recorder"
private const val SUBSCRIPTION_PREFS_NAME = "fitness-recording-subscriptions"
private const val LAST_RAW_STEPS = "last-raw-steps"
private const val LAST_EVENT_TIME = "last-event-time"
private const val NEXT_EVENT_ID = "next-event-id"
private const val EVENT_PREFIX = "event:"
private const val SUBSCRIPTION_PREFIX = "subscription:"
private const val LEGACY_ACTIVITY_RECOGNITION_PERMISSION = "com.google.android.gms.permission.ACTIVITY_RECOGNITION"

// Legacy minute samples are kept readable so an update does not discard recorded steps.
private const val SAMPLE_PREFIX = "sample:"
private const val SAMPLE_TIME_PREFIX = "sample-time:"
private const val MINUTE_MILLIS = 60_000L

internal data class StepSample(val startTimeMillis: Long, val endTimeMillis: Long, val steps: Int)

internal fun Context.enforceActivityRecognitionPermission(packageName: String) {
    val permission = if (android.os.Build.VERSION.SDK_INT >= 29) {
        Manifest.permission.ACTIVITY_RECOGNITION
    } else {
        LEGACY_ACTIVITY_RECOGNITION_PERMISSION
    }
    if (packageManager.checkPermission(permission, packageName) != PackageManager.PERMISSION_GRANTED) {
        throw SecurityException("$packageName does not hold $permission")
    }
}

private fun sampleMinute(key: String): Long? =
    key.takeIf { it.startsWith(SAMPLE_PREFIX) }?.removePrefix(SAMPLE_PREFIX)?.toLongOrNull()

private fun storedSample(preferences: SharedPreferences, key: String, value: Any?): StepSample? = when {
    key.startsWith(EVENT_PREFIX) -> (value as? String)?.split(',')?.takeIf { it.size == 3 }?.let {
        val start = it[0].toLongOrNull() ?: return@let null
        val end = it[1].toLongOrNull() ?: return@let null
        val steps = it[2].toIntOrNull() ?: return@let null
        StepSample(start, end, steps)
    }
    else -> sampleMinute(key)?.let { minute ->
        val steps = value as? Int ?: return@let null
        StepSample(minute, preferences.getLong("$SAMPLE_TIME_PREFIX$minute", minute + MINUTE_MILLIS), steps)
    }
}

internal object FitnessStepRecorder : SensorEventListener {
    private var preferences: SharedPreferences? = null
    private var sensorManager: SensorManager? = null
    private var registered = false

    private fun preferences(context: Context): SharedPreferences = preferences
        ?: context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).also { preferences = it }

    private fun subscriptionPreferences(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(SUBSCRIPTION_PREFS_NAME, Context.MODE_PRIVATE)

    private fun subscriptionKey(clientId: String): String = SUBSCRIPTION_PREFIX + Base64.encodeToString(
        clientId.toByteArray(Charsets.UTF_8), Base64.URL_SAFE or Base64.NO_WRAP
    )

    private fun decodeSubscription(value: String?): Subscription? {
        value ?: return null
        return runCatching {
            SafeParcelableSerializer.deserializeFromBytes(Base64.decode(value, Base64.DEFAULT), Subscription.CREATOR)
        }.getOrNull()
    }

    @Synchronized
    private fun start(context: Context): Boolean {
        if (registered) return true
        val appContext = context.applicationContext
        if (!appContext.ensureActivityRecognitionPermission()) return false
        preferences(appContext)
        val manager = appContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager ?: return false
        val sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) ?: return false
        return try {
            manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_NORMAL).also { success ->
                registered = success
                sensorManager = manager.takeIf { success }
            }
        } catch (e: SecurityException) {
            Log.w(TAG, "Activity recognition permission is missing", e)
            false
        }
    }

    @Synchronized
    private fun stop() {
        if (registered) sensorManager?.unregisterListener(this)
        registered = false
        sensorManager = null
    }

    @Synchronized
    fun resume(context: Context) {
        if (subscriptionPreferences(context).all.keys.any { it.startsWith(SUBSCRIPTION_PREFIX) }) start(context)
    }

    @Synchronized
    fun subscribe(context: Context, clientId: String, subscription: Subscription): Boolean {
        if (!start(context)) return false
        val encoded = Base64.encodeToString(SafeParcelableSerializer.serializeToBytes(subscription), Base64.NO_WRAP)
        subscriptionPreferences(context).edit { putString(subscriptionKey(clientId), encoded) }
        return true
    }

    @Synchronized
    fun unsubscribe(context: Context, clientId: String, dataType: DataType?, dataSource: DataSource?) {
        if (dataType == null && dataSource == null) return
        val preferences = subscriptionPreferences(context)
        val key = subscriptionKey(clientId)
        decodeSubscription(preferences.getString(key, null))?.let { subscription ->
            val subscribedType = subscription.dataType ?: subscription.dataSource?.dataType
            val matchesType = dataType == null || dataType.name == subscribedType?.name
            val matchesSource = dataSource == null || dataSource.streamIdentifier == subscription.dataSource?.streamIdentifier
            if (matchesType && matchesSource) preferences.edit { remove(key) }
        }
        if (preferences.all.keys.none { it.startsWith(SUBSCRIPTION_PREFIX) }) stop()
    }

    @Synchronized
    fun subscriptions(context: Context, clientId: String, dataType: DataType?): List<Subscription> =
        listOfNotNull(decodeSubscription(subscriptionPreferences(context).getString(subscriptionKey(clientId), null)))
            .filter { dataType == null || dataType.name == (it.dataType ?: it.dataSource?.dataType)?.name }

    @Synchronized
    fun samples(context: Context, startTimeMillis: Long, endTimeMillis: Long): List<StepSample> {
        if (endTimeMillis <= startTimeMillis) return emptyList()
        val preferences = preferences(context)
        return preferences.all.mapNotNull { (key, value) ->
            storedSample(preferences, key, value)?.takeIf {
                it.endTimeMillis > startTimeMillis && it.endTimeMillis <= endTimeMillis
            }?.let { it.copy(startTimeMillis = maxOf(it.startTimeMillis, startTimeMillis)) }
        }.sortedBy { it.endTimeMillis }
    }

    @Synchronized
    fun deleteSamples(context: Context, startTimeMillis: Long, endTimeMillis: Long) {
        if (endTimeMillis <= startTimeMillis) return
        val preferences = preferences(context)
        val keys = preferences.all.mapNotNull { (key, value) ->
            key.takeIf {
                storedSample(preferences, key, value)?.let { sample ->
                    sample.endTimeMillis > startTimeMillis && sample.endTimeMillis <= endTimeMillis
                } == true
            }
        }
        preferences.edit {
            keys.forEach { key ->
                remove(key)
                sampleMinute(key)?.let { remove("$SAMPLE_TIME_PREFIX$it") }
            }
        }
    }

    @Synchronized
    override fun onSensorChanged(event: SensorEvent) {
        if (event.sensor.type != Sensor.TYPE_STEP_COUNTER) return
        val value = event.values.firstOrNull()?.takeIf { it.isFinite() } ?: return
        val rawSteps = value.toInt()
        if (rawSteps < 0) return
        val preferences = preferences ?: return
        preferences.edit {
            val previousRawSteps = preferences.getInt(LAST_RAW_STEPS, -1)
            val now = System.currentTimeMillis()
            val previousEventTime = preferences.getLong(LAST_EVENT_TIME, now)
            putInt(LAST_RAW_STEPS, rawSteps)
            putLong(LAST_EVENT_TIME, now)
            val steps = when {
                previousRawSteps < 0 -> 0
                rawSteps >= previousRawSteps -> rawSteps - previousRawSteps
                else -> rawSteps
            }
            if (steps > 0) {
                val eventId = preferences.getLong(NEXT_EVENT_ID, 0)
                val start = previousEventTime.takeIf { it <= now } ?: now
                putLong(NEXT_EVENT_ID, eventId + 1)
                putString("$EVENT_PREFIX$eventId", "$start,$now,$steps")
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}
