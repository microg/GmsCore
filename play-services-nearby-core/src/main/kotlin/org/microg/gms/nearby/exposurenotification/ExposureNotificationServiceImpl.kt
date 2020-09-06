/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.app.Activity
import android.app.PendingIntent
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.*
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.*
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.internal.*
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.PackageUtils
import org.microg.gms.nearby.exposurenotification.Constants.*
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyExport
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyProto
import java.util.*
import java.util.zip.ZipInputStream
import kotlin.math.roundToInt

class ExposureNotificationServiceImpl(private val context: Context, private val packageName: String) : INearbyExposureNotificationService.Stub() {
    private fun pendingConfirm(permission: String): PendingIntent {
        val intent = Intent(ACTION_CONFIRM)
        intent.`package` = context.packageName
        intent.putExtra(KEY_CONFIRM_PACKAGE, packageName)
        intent.putExtra(KEY_CONFIRM_ACTION, permission)
        intent.putExtra(KEY_CONFIRM_RECEIVER, object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == Activity.RESULT_OK) {
                    ExposureDatabase.with(context) { database -> database.grantPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission) }
                }
            }
        })
        try {
            intent.component = ComponentName(context, context.packageManager.resolveActivity(intent, 0)?.activityInfo?.name!!)
        } catch (e: Exception) {
            Log.w(TAG, e)
        }
        Log.d(TAG, "Pending: $intent")
        val pi = PendingIntent.getActivity(context, permission.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT)
        Log.d(TAG, "Pending: $pi")
        return pi
    }

    private fun confirmPermission(permission: String): Status {
        if (packageName == context.packageName) return Status.SUCCESS
        return ExposureDatabase.with(context) { database ->
            if (!database.hasPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission)) {
                Status(RESOLUTION_REQUIRED, "Permission EN#$permission required.", pendingConfirm(permission))
            } else {
                Status.SUCCESS
            }
        }
    }

    override fun start(params: StartParams) {
        if (ExposurePreferences(context).enabled) return
        val status = confirmPermission(CONFIRM_ACTION_START)
        if (status.isSuccess) {
            ExposurePreferences(context).enabled = true
            ExposureDatabase.with(context) { database -> database.noteAppAction(packageName, "start") }
        }
        try {
            params.callback.onResult(status)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun stop(params: StopParams) {
        ExposurePreferences(context).enabled = false
        ExposureDatabase.with(context) { database ->
            database.noteAppAction(packageName, "stop")
        }
        try {
            params.callback.onResult(Status.SUCCESS)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun isEnabled(params: IsEnabledParams) {
        try {
            params.callback.onResult(Status.SUCCESS, ExposurePreferences(context).enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun getTemporaryExposureKeyHistory(params: GetTemporaryExposureKeyHistoryParams) {
        val status = confirmPermission(CONFIRM_ACTION_KEYS)
        val response = when {
            status.isSuccess -> ExposureDatabase.with(context) { database ->
                database.allKeys
            }
            else -> emptyList()
        }

        ExposureDatabase.with(context) { database ->
            database.noteAppAction(packageName, "getTemporaryExposureKeyHistory", JSONObject().apply {
                put("result", status.statusCode)
                put("response_keys_size", response.size)
            }.toString())
        }
        try {
            params.callback.onResult(status, response)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    private fun TemporaryExposureKeyProto.toKey(): TemporaryExposureKey = TemporaryExposureKey.TemporaryExposureKeyBuilder()
            .setKeyData(key_data?.toByteArray() ?: throw IllegalArgumentException("key data missing"))
            .setRollingStartIntervalNumber(rolling_start_interval_number
                    ?: throw IllegalArgumentException("rolling start interval number missing"))
            .setRollingPeriod(rolling_period ?: throw IllegalArgumentException("rolling period missing"))
            .setTransmissionRiskLevel(transmission_risk_level ?: 0)
            .build()

    private fun storeDiagnosisKeyExport(token: String, export: TemporaryExposureKeyExport): Int = ExposureDatabase.with(context) { database ->
        Log.d(TAG, "Importing keys from file ${export.start_timestamp?.let { Date(it * 1000) }} to ${export.end_timestamp?.let { Date(it * 1000) }}")
        database.batchStoreDiagnosisKey(packageName, token, export.keys.map { it.toKey() })
        database.batchUpdateDiagnosisKey(packageName, token, export.revised_keys.map { it.toKey() })
        export.keys.size + export.revised_keys.size
    }

    override fun provideDiagnosisKeys(params: ProvideDiagnosisKeysParams) {
        Thread(Runnable {
            ExposureDatabase.with(context) { database ->
                if (params.configuration != null) {
                    database.storeConfiguration(packageName, params.token, params.configuration)
                }

                val start = System.currentTimeMillis()

                // keys
                params.keys?.let { database.batchStoreDiagnosisKey(packageName, params.token, it) }

                // Key files
                var keys = params.keys?.size ?: 0
                for (file in params.keyFiles.orEmpty()) {
                    try {
                        ZipInputStream(ParcelFileDescriptor.AutoCloseInputStream(file)).use { stream ->
                            do {
                                val entry = stream.nextEntry ?: break
                                if (entry.name == "export.bin") {
                                    val prefix = ByteArray(16)
                                    var totalBytesRead = 0
                                    var bytesRead = 0
                                    while (bytesRead != -1 && totalBytesRead < prefix.size) {
                                        bytesRead = stream.read(prefix, totalBytesRead, prefix.size - totalBytesRead)
                                        if (bytesRead > 0) {
                                            totalBytesRead += bytesRead
                                        }
                                    }
                                    if (totalBytesRead == prefix.size && String(prefix).trim() == "EK Export v1") {
                                        val fileKeys = storeDiagnosisKeyExport(params.token, TemporaryExposureKeyExport.ADAPTER.decode(stream))
                                        keys += fileKeys
                                    } else {
                                        Log.d(TAG, "export.bin had invalid prefix")
                                    }
                                }
                                stream.closeEntry()
                            } while (true);
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing file", e)
                    }
                }
                val time = (System.currentTimeMillis() - start).toDouble() / 1000.0
                Log.d(TAG, "$packageName/${params.token} provided $keys keys in ${time}s -> ${(keys.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")

                database.noteAppAction(packageName, "provideDiagnosisKeys", JSONObject().apply {
                    put("request_token", params.token)
                    put("request_keys_size", params.keys?.size)
                    put("request_keyFiles_size", params.keyFiles?.size)
                    put("request_keys_count", keys)
                }.toString())

                database.finishMatching(packageName, params.token)

                Handler(Looper.getMainLooper()).post {
                    try {
                        params.callback.onResult(Status.SUCCESS)
                    } catch (e: Exception) {
                        Log.w(TAG, "Callback failed", e)
                    }
                }

                val match = database.findAllMeasuredExposures(packageName, params.token).isNotEmpty()

                try {
                    val intent = Intent(if (match) ACTION_EXPOSURE_STATE_UPDATED else ACTION_EXPOSURE_NOT_FOUND)
                    intent.putExtra(EXTRA_TOKEN, params.token)
                    intent.`package` = packageName
                    Log.d(TAG, "Sending $intent")
                    context.sendOrderedBroadcast(intent, null)
                } catch (e: Exception) {
                    Log.w(TAG, "Callback failed", e)
                }
            }
        }).start()
    }

    override fun getExposureSummary(params: GetExposureSummaryParams): Unit = ExposureDatabase.with(context) { database ->
        val configuration = database.loadConfiguration(packageName, params.token)
        if (configuration == null) {
            try {
                params.callback.onResult(Status.INTERNAL_ERROR, null)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
            return@with
        }
        val exposures = database.findAllMeasuredExposures(packageName, params.token).merge()
        val response = ExposureSummary.ExposureSummaryBuilder()
                .setDaysSinceLastExposure(exposures.map { it.daysSinceExposure }.min()?.toInt() ?: 0)
                .setMatchedKeyCount(exposures.map { it.key }.distinct().size)
                .setMaximumRiskScore(exposures.map { it.getRiskScore(configuration) }.max()?.toInt() ?: 0)
                .setAttenuationDurations(intArrayOf(
                        exposures.map { it.getAttenuationDurations(configuration)[0] }.sum(),
                        exposures.map { it.getAttenuationDurations(configuration)[1] }.sum(),
                        exposures.map { it.getAttenuationDurations(configuration)[2] }.sum()
                ))
                .setSummationRiskScore(exposures.map { it.getRiskScore(configuration) }.sum())
                .build()

        database.noteAppAction(packageName, "getExposureSummary", JSONObject().apply {
            put("request_token", params.token)
            put("response_days_since", response.daysSinceLastExposure)
            put("response_matched_keys", response.matchedKeyCount)
            put("response_max_risk", response.maximumRiskScore)
            put("response_attenuation_durations", JSONArray().apply {
                response.attenuationDurationsInMinutes.forEach { put(it) }
            })
            put("response_summation_risk", response.summationRiskScore)
        }.toString())
        try {
            params.callback.onResult(Status.SUCCESS, response)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun getExposureInformation(params: GetExposureInformationParams): Unit = ExposureDatabase.with(context) { database ->
        // TODO: Notify user?
        val configuration = database.loadConfiguration(packageName, params.token)
        if (configuration == null) {
            try {
                params.callback.onResult(Status.INTERNAL_ERROR, null)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
            return@with
        }
        val response = database.findAllMeasuredExposures(packageName, params.token).merge().map {
            it.toExposureInformation(configuration)
        }

        database.noteAppAction(packageName, "getExposureInformation", JSONObject().apply {
            put("request_token", params.token)
            put("response_size", response.size)
        }.toString())
        try {
            params.callback.onResult(Status.SUCCESS, response)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }
}
