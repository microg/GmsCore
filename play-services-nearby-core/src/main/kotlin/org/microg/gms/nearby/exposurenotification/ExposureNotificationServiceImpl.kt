/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.Intent.*
import android.os.*
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.*
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.internal.*
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.nearby.exposurenotification.Constants.ACTION_EXPOSURE_NOT_FOUND
import org.microg.gms.nearby.exposurenotification.Constants.ACTION_EXPOSURE_STATE_UPDATED
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyExport
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyProto
import java.util.*
import java.util.zip.ZipInputStream

class ExposureNotificationServiceImpl(private val context: Context, private val packageName: String, private val database: ExposureDatabase) : INearbyExposureNotificationService.Stub() {
    private fun confirm(action: String, callback: (resultCode: Int, resultData: Bundle?) -> Unit) {
        val intent = Intent(ACTION_CONFIRM)
        intent.`package` = context.packageName
        intent.putExtra(KEY_CONFIRM_PACKAGE, packageName)
        intent.putExtra(KEY_CONFIRM_ACTION, action)
        intent.putExtra(KEY_CONFIRM_RECEIVER, object : ResultReceiver(Handler(Looper.getMainLooper())) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                Log.d(TAG, "Result from action $action: ${getStatusCodeString(resultCode)}")
                callback(resultCode, resultData)
            }
        })
        intent.addFlags(FLAG_ACTIVITY_NEW_TASK)
        intent.addFlags(FLAG_ACTIVITY_MULTIPLE_TASK)
        intent.addFlags(FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        try {
            intent.component = ComponentName(context, context.packageManager.resolveActivity(intent, 0)?.activityInfo?.name!!)
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.w(TAG, e)
            callback(CommonStatusCodes.INTERNAL_ERROR, null)
        }
    }

    override fun start(params: StartParams) {
        if (ExposurePreferences(context).scannerEnabled) {
            params.callback.onResult(Status(FAILED_ALREADY_STARTED))
            return
        }
        confirm(CONFIRM_ACTION_START) { resultCode, resultData ->
            if (resultCode == SUCCESS) {
                ExposurePreferences(context).scannerEnabled = true
            }
            database.noteAppAction(packageName, "start", JSONObject().apply {
                put("result", resultCode)
            }.toString())
            try {
                params.callback.onResult(Status(if (resultCode == SUCCESS) SUCCESS else FAILED_REJECTED_OPT_IN, resultData?.getString("message")))
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun stop(params: StopParams) {
        confirm(CONFIRM_ACTION_STOP) { resultCode, _ ->
            if (resultCode == SUCCESS) {
                ExposurePreferences(context).scannerEnabled = false
            }
            database.noteAppAction(packageName, "stop", JSONObject().apply {
                put("result", resultCode)
            }.toString())
            try {
                params.callback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun isEnabled(params: IsEnabledParams) {
        try {
            params.callback.onResult(Status.SUCCESS, ExposurePreferences(context).scannerEnabled)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun getTemporaryExposureKeyHistory(params: GetTemporaryExposureKeyHistoryParams) {
        confirm(CONFIRM_ACTION_START) { resultCode, resultData ->
            val (status, response) = if (resultCode == SUCCESS) {
                SUCCESS to database.allKeys
            } else {
                FAILED_REJECTED_OPT_IN to emptyList()
            }

            database.noteAppAction(packageName, "getTemporaryExposureKeyHistory", JSONObject().apply {
                put("result", resultCode)
                put("response_keys_size", response.size)
            }.toString())
            try {
                params.callback.onResult(Status(status, resultData?.getString("message")), response)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    private fun TemporaryExposureKeyProto.toKey(): TemporaryExposureKey = TemporaryExposureKey.TemporaryExposureKeyBuilder()
            .setKeyData(key_data?.toByteArray() ?: throw IllegalArgumentException("key data missing"))
            .setRollingStartIntervalNumber(rolling_start_interval_number
                    ?: throw IllegalArgumentException("rolling start interval number missing"))
            .setRollingPeriod(rolling_period ?: throw IllegalArgumentException("rolling period missing"))
            .setTransmissionRiskLevel(transmission_risk_level ?: 0)
            .build()

    private fun storeDiagnosisKeyExport(token: String, export: TemporaryExposureKeyExport): Int {
        Log.d(TAG, "Importing keys from file ${export.start_timestamp?.let { Date(it * 1000) }} to ${export.end_timestamp?.let { Date(it * 1000) }}")
        for (key in export.keys) {
            database.storeDiagnosisKey(packageName, token, key.toKey())
        }
        for (key in export.revised_keys) {
            database.updateDiagnosisKey(packageName, token, key.toKey())
        }
        return export.keys.size + export.revised_keys.size
    }

    override fun provideDiagnosisKeys(params: ProvideDiagnosisKeysParams) {
        database.ref()
        Thread(Runnable {
            try {
                if (params.configuration != null) {
                    database.storeConfiguration(packageName, params.token, params.configuration)
                }

                // keys
                for (key in params.keys.orEmpty()) {
                    database.storeDiagnosisKey(packageName, params.token, key)
                }

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
                                        keys + fileKeys
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
                Log.d(TAG, "$packageName/${params.token} provided $keys keys")

                database.noteAppAction(packageName, "provideDiagnosisKeys", JSONObject().apply {
                    put("request_token", params.token)
                    put("request_keys_size", params.keys?.size)
                    put("request_keyFiles_size", params.keyFiles?.size)
                    put("request_keys_count", keys)
                }.toString())

                Handler(Looper.getMainLooper()).post {
                    try {
                        params.callback.onResult(Status.SUCCESS)
                    } catch (e: Exception) {
                        Log.w(TAG, "Callback failed", e)
                    }
                }

                database.finishMatching(packageName, params.token)
                val match = database.findAllMeasuredExposures(packageName, params.token).isNotEmpty()

                try {
                    val intent = Intent(if (match) ACTION_EXPOSURE_STATE_UPDATED else ACTION_EXPOSURE_NOT_FOUND)
                    intent.`package` = packageName
                    Log.d(TAG, "Sending $intent")
                    context.sendOrderedBroadcast(intent, PERMISSION_EXPOSURE_CALLBACK)
                } catch (e: Exception) {
                    Log.w(TAG, "Callback failed", e)
                }
            } finally {
                database.unref()
            }
        }).start()
    }

    override fun getExposureSummary(params: GetExposureSummaryParams) {
        val configuration = database.loadConfiguration(packageName, params.token)
        if (configuration == null) {
            try {
                params.callback.onResult(Status.INTERNAL_ERROR, null)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
            return
        }
        val exposures = database.findAllMeasuredExposures(packageName, params.token)
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

    override fun getExposureInformation(params: GetExposureInformationParams) {
        // TODO: Notify user?
        val configuration = database.loadConfiguration(packageName, params.token)
        if (configuration == null) {
            try {
                params.callback.onResult(Status.INTERNAL_ERROR, null)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
            return
        }
        val response = database.findAllMeasuredExposures(packageName, params.token).map {
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
}
