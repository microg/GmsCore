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
import android.os.*
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.ExposureConfiguration
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.*
import com.google.android.gms.nearby.exposurenotification.ExposureSummary
import com.google.android.gms.nearby.exposurenotification.TemporaryExposureKey
import com.google.android.gms.nearby.exposurenotification.internal.*
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.Constants
import org.microg.gms.common.PackageUtils
import org.microg.gms.nearby.exposurenotification.Constants.*
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyExport
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyProto
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.math.roundToInt
import kotlin.random.Random

class ExposureNotificationServiceImpl(private val context: Context, private val lifecycle: Lifecycle, private val packageName: String) : INearbyExposureNotificationService.Stub(), LifecycleOwner {

    override fun getLifecycle(): Lifecycle = lifecycle

    private fun pendingConfirm(permission: String): PendingIntent {
        val intent = Intent(ACTION_CONFIRM)
        intent.`package` = context.packageName
        intent.putExtra(KEY_CONFIRM_PACKAGE, packageName)
        intent.putExtra(KEY_CONFIRM_ACTION, permission)
        intent.putExtra(KEY_CONFIRM_RECEIVER, object : ResultReceiver(null) {
            override fun onReceiveResult(resultCode: Int, resultData: Bundle?) {
                if (resultCode == Activity.RESULT_OK) {
                    tempGrantedPermissions.add(packageName to permission)
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

    private suspend fun confirmPermission(permission: String): Status {
        if (packageName == context.packageName) return Status.SUCCESS
        return ExposureDatabase.with(context) { database ->
            if (tempGrantedPermissions.contains(packageName to permission)) {
                database.grantPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission)
                tempGrantedPermissions.remove(packageName to permission)
                Status.SUCCESS
            } else if (!database.hasPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission)) {
                Status(RESOLUTION_REQUIRED, "Permission EN#$permission required.", pendingConfirm(permission))
            } else {
                Status.SUCCESS
            }
        }
    }

    override fun getVersion(params: GetVersionParams) {
        params.callback.onResult(Status.SUCCESS, Constants.MAX_REFERENCE_VERSION.toLong())
    }

    override fun getCalibrationConfidence(params: GetCalibrationConfidenceParams) {
        params.callback.onResult(Status.SUCCESS, currentDeviceInfo.confidence)
    }

    override fun start(params: StartParams) {
        if (ExposurePreferences(context).enabled) return
        lifecycleScope.launchWhenStarted {
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
    }

    override fun stop(params: StopParams) {
        lifecycleScope.launchWhenStarted {
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
    }

    override fun isEnabled(params: IsEnabledParams) {
        try {
            params.callback.onResult(Status.SUCCESS, ExposurePreferences(context).enabled)
        } catch (e: Exception) {
            Log.w(TAG, "Callback failed", e)
        }
    }

    override fun getTemporaryExposureKeyHistory(params: GetTemporaryExposureKeyHistoryParams) {
        lifecycleScope.launchWhenStarted {
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
    }

    private fun TemporaryExposureKeyProto.toKey(): TemporaryExposureKey = TemporaryExposureKey.TemporaryExposureKeyBuilder()
            .setKeyData(key_data?.toByteArray() ?: throw IllegalArgumentException("key data missing"))
            .setRollingStartIntervalNumber(rolling_start_interval_number
                    ?: throw IllegalArgumentException("rolling start interval number missing"))
            .setRollingPeriod(rolling_period ?: throw IllegalArgumentException("rolling period missing"))
            .setTransmissionRiskLevel(transmission_risk_level ?: 0)
            .build()

    private fun InputStream.copyToFile(outputFile: File) {
        outputFile.outputStream().use { output ->
            copyTo(output)
            output.flush()
        }
    }

    private fun MessageDigest.digest(file: File): ByteArray = file.inputStream().use { input ->
        val buf = ByteArray(4096)
        var bytes = input.read(buf)
        while (bytes != -1) {
            update(buf, 0, bytes)
            bytes = input.read(buf)
        }
        digest()
    }

    private suspend fun buildExposureSummary(token: String): ExposureSummary = ExposureDatabase.with(context) { database ->
        val pair = database.loadConfiguration(packageName, token)
        val (configuration, exposures) = if (pair != null) {
            pair.second to database.findAllMeasuredExposures(pair.first).merge()
        } else {
            ExposureConfiguration.ExposureConfigurationBuilder().build() to emptyList()
        }

        ExposureSummary.ExposureSummaryBuilder()
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
    }

    override fun provideDiagnosisKeys(params: ProvideDiagnosisKeysParams) {
        Log.w(TAG, "provideDiagnosisKeys() with $packageName/${params.token}")
        lifecycleScope.launchWhenStarted {
            val tid = ExposureDatabase.with(context) { database ->
                if (params.configuration != null) {
                    database.storeConfiguration(packageName, params.token, params.configuration)
                } else {
                    database.getTokenId(packageName, params.token)
                }
            }
            if (tid == null) {
                Log.w(TAG, "Unknown token without configuration: $packageName/${params.token}")
                try {
                    params.callback.onResult(Status.INTERNAL_ERROR)
                } catch (e: Exception) {
                    Log.w(TAG, "Callback failed", e)
                }
                return@launchWhenStarted
            }
            ExposureDatabase.with(context) { database ->
                val start = System.currentTimeMillis()

                // keys
                params.keys?.let { database.batchStoreSingleDiagnosisKey(tid, it) }

                var keys = params.keys?.size ?: 0

                // Key files
                val todoKeyFiles = arrayListOf<Pair<File, ByteArray>>()
                for (file in params.keyFiles.orEmpty()) {
                    try {
                        val cacheFile = File(context.cacheDir, "en-keyfile-${System.currentTimeMillis()}-${Random.nextInt()}.zip")
                        ParcelFileDescriptor.AutoCloseInputStream(file).use { it.copyToFile(cacheFile) }
                        val hash = MessageDigest.getInstance("SHA-256").digest(cacheFile)
                        val storedKeys = database.storeDiagnosisFileUsed(tid, hash)
                        if (storedKeys != null) {
                            keys += storedKeys.toInt()
                            cacheFile.delete()
                        } else {
                            todoKeyFiles.add(cacheFile to hash)
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed parsing file", e)
                    }
                }

                if (todoKeyFiles.size > 0) {
                    val time = (System.currentTimeMillis() - start).toDouble() / 1000.0
                    Log.d(TAG, "$packageName/${params.token} processed $keys keys (${todoKeyFiles.size} files pending) in ${time}s -> ${(keys.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")
                }

                Handler(Looper.getMainLooper()).post {
                    try {
                        params.callback.onResult(Status.SUCCESS)
                    } catch (e: Exception) {
                        Log.w(TAG, "Callback failed", e)
                    }
                }

                var newKeys = if (params.keys != null) database.finishSingleMatching(tid) else 0
                for ((cacheFile, hash) in todoKeyFiles) {
                    ZipFile(cacheFile).use { zip ->
                        for (entry in zip.entries()) {
                            if (entry.name == "export.bin") {
                                val stream = zip.getInputStream(entry)
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
                                    val export = TemporaryExposureKeyExport.ADAPTER.decode(stream)
                                    database.finishFileMatching(tid, hash, export.end_timestamp?.let { it * 1000 }
                                            ?: System.currentTimeMillis(), export.keys.map { it.toKey() }, export.revised_keys.map { it.toKey() })
                                    keys += export.keys.size + export.revised_keys.size
                                    newKeys += export.keys.size
                                } else {
                                    Log.d(TAG, "export.bin had invalid prefix")
                                }
                            }
                        }
                    }
                    cacheFile.delete()
                }

                val time = (System.currentTimeMillis() - start).toDouble() / 1000.0
                Log.d(TAG, "$packageName/${params.token} processed $keys keys ($newKeys new) in ${time}s -> ${(keys.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")

                database.noteAppAction(packageName, "provideDiagnosisKeys", JSONObject().apply {
                    put("request_token", params.token)
                    put("request_keys_size", params.keys?.size)
                    put("request_keyFiles_size", params.keyFiles?.size)
                    put("request_keys_count", keys)
                }.toString())

                val exposureSummary = buildExposureSummary(params.token)

                try {
                    val intent = if (exposureSummary.matchedKeyCount > 0) {
                        Intent(ACTION_EXPOSURE_STATE_UPDATED).putExtra(EXTRA_EXPOSURE_SUMMARY, exposureSummary)
                    } else {
                        Intent(ACTION_EXPOSURE_NOT_FOUND)
                    }
                    intent.putExtra(EXTRA_TOKEN, params.token)
                    intent.`package` = packageName
                    Log.d(TAG, "Sending $intent")
                    context.sendOrderedBroadcast(intent, null)
                } catch (e: Exception) {
                    Log.w(TAG, "Callback failed", e)
                }
            }
        }
    }

    override fun getExposureSummary(params: GetExposureSummaryParams) {
        lifecycleScope.launchWhenStarted {
            val response = buildExposureSummary(params.token)

            ExposureDatabase.with(context) { database ->
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
            }
            try {
                params.callback.onResult(Status.SUCCESS, response)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun getExposureInformation(params: GetExposureInformationParams) {
        lifecycleScope.launchWhenStarted {
            ExposureDatabase.with(context) { database ->
                val pair = database.loadConfiguration(packageName, params.token)
                val response = if (pair != null) {
                    database.findAllMeasuredExposures(pair.first).merge().map {
                        it.toExposureInformation(pair.second)
                    }
                } else {
                    emptyList()
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
    }

    override fun getExposureWindows(params: GetExposureWindowsParams) {
        Log.w(TAG, "Not yet implemented: getExposureWindows")
        params.callback.onResult(Status.INTERNAL_ERROR, emptyList())
    }

    override fun getDailySummaries(params: GetDailySummariesParams) {
        Log.w(TAG, "Not yet implemented: getDailySummaries")
        params.callback.onResult(Status.INTERNAL_ERROR, emptyList())
    }

    override fun setDiagnosisKeysDataMapping(params: SetDiagnosisKeysDataMappingParams) {
        Log.w(TAG, "Not yet implemented: setDiagnosisKeysDataMapping")
        params.callback.onResult(Status.INTERNAL_ERROR)
    }

    override fun getDiagnosisKeysDataMapping(params: GetDiagnosisKeysDataMappingParams) {
        Log.w(TAG, "Not yet implemented: getDiagnosisKeysDataMapping")
        params.callback.onResult(Status.INTERNAL_ERROR, null)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact [unknown]: $code, $data, $flags")
        return false
    }

    companion object {
        private val tempGrantedPermissions: MutableSet<Pair<String, String>> = hashSetOf()
    }
}
