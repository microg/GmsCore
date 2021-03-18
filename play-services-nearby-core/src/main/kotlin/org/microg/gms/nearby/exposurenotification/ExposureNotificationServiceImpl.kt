/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.os.*
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.Status
import com.google.android.gms.nearby.exposurenotification.*
import com.google.android.gms.nearby.exposurenotification.ExposureNotificationStatusCodes.*
import com.google.android.gms.nearby.exposurenotification.internal.*
import kotlinx.coroutines.*
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.common.PackageUtils
import org.microg.gms.nearby.exposurenotification.Constants.*
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyExport
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyProto
import java.io.File
import java.io.InputStream
import java.security.MessageDigest
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class ExposureNotificationServiceImpl(private val context: Context, private val lifecycle: Lifecycle, private val packageName: String) : INearbyExposureNotificationService.Stub(), LifecycleOwner {

    private fun LifecycleCoroutineScope.launchSafely(block: suspend CoroutineScope.() -> Unit): Job = launchWhenStarted { try { block() } catch (e: Exception) { Log.w(TAG, "Error in coroutine", e) } }

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

    private fun hasConfirmActivity(): Boolean {
        val intent = Intent(ACTION_CONFIRM)
        intent.`package` = context.packageName
        return try {
            context.packageManager.resolveActivity(intent, 0) != null
        } catch (e: Exception) {
            false
        }
    }

    private suspend fun confirmPermission(permission: String, force: Boolean = false): Status {
        return ExposureDatabase.with(context) { database ->
            when {
                tempGrantedPermissions.contains(packageName to permission) -> {
                    database.grantPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission)
                    tempGrantedPermissions.remove(packageName to permission)
                    Status.SUCCESS
                }
                !force && database.hasPermission(packageName, PackageUtils.firstSignatureDigest(context, packageName)!!, permission) -> {
                    Status.SUCCESS
                }
                !hasConfirmActivity() -> {
                    Status.SUCCESS
                }
                else -> {
                    Status(RESOLUTION_REQUIRED, "Permission EN#$permission required.", pendingConfirm(permission))
                }
            }
        }
    }

    override fun getVersion(params: GetVersionParams) {
        params.callback.onResult(Status.SUCCESS, VERSION_FULL)
    }

    override fun getCalibrationConfidence(params: GetCalibrationConfidenceParams) {
        params.callback.onResult(Status.SUCCESS, currentDeviceInfo.confidence)
    }

    override fun start(params: StartParams) {
        lifecycleScope.launchSafely {
            val isAuthorized = ExposureDatabase.with(context) { it.isAppAuthorized(packageName) }
            val adapter = BluetoothAdapter.getDefaultAdapter()
            val status = if (isAuthorized && ExposurePreferences(context).enabled) {
                Status.SUCCESS
            } else if (adapter == null) {
                Status(FAILED_NOT_SUPPORTED, "No Bluetooth Adapter available.")
            } else {
                val status = confirmPermission(CONFIRM_ACTION_START, !adapter.isEnabled)
                if (status.isSuccess) {
                    val context = context
                    adapter.enableAsync(context)
                    ExposurePreferences(context).enabled = true
                    ExposureDatabase.with(context) { database ->
                        database.authorizeApp(packageName)
                        database.noteAppAction(packageName, "start")
                    }
                }
                status
            }
            try {
                params.callback.onResult(status)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun stop(params: StopParams) {
        lifecycleScope.launchSafely {
            val isAuthorized = ExposureDatabase.with(context) { database ->
                database.isAppAuthorized(packageName).also {
                    if (it) database.noteAppAction(packageName, "stop")
                }
            }
            if (isAuthorized) {
                ExposurePreferences(context).enabled = false
            }
            try {
                params.callback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun isEnabled(params: IsEnabledParams) {
        lifecycleScope.launchSafely {
            val isAuthorized = ExposureDatabase.with(context) { database ->
                database.isAppAuthorized(packageName)
            }
            try {
                params.callback.onResult(Status.SUCCESS, isAuthorized && ExposurePreferences(context).enabled)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun getTemporaryExposureKeyHistory(params: GetTemporaryExposureKeyHistoryParams) {
        lifecycleScope.launchSafely {
            val status = confirmPermission(CONFIRM_ACTION_KEYS)
            val response = when {
                status.isSuccess -> ExposureDatabase.with(context) { database ->
                    database.authorizeApp(packageName)
                    database.exportKeys()
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

    private fun ExposureConfiguration?.orDefault() = this
            ?: ExposureConfiguration.ExposureConfigurationBuilder().build()

    private suspend fun buildExposureSummary(token: String): ExposureSummary = ExposureDatabase.with(context) { database ->
        if (!database.isAppAuthorized(packageName)) {
            // Not providing summary if app not authorized
            Log.d(TAG, "$packageName not yet authorized")
            return@with ExposureSummary.ExposureSummaryBuilder().build()
        }
        val pair = database.loadConfiguration(packageName, token)
        val (configuration, exposures) = if (pair != null) {
            pair.second.orDefault() to database.findAllMeasuredExposures(pair.first).merge()
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
        val token = params.token ?: TOKEN_A
        Log.w(TAG, "provideDiagnosisKeys() with $packageName/$token")
        lifecycleScope.launchSafely {
            val tid = ExposureDatabase.with(context) { database ->
                val configuration = params.configuration
                if (configuration != null) {
                    database.storeConfiguration(packageName, token, configuration)
                } else {
                    database.getOrCreateTokenId(packageName, token)
                }
            }
            if (tid == null) {
                Log.w(TAG, "Unknown token without configuration: $packageName/$token")
                try {
                    params.callback.onResult(Status.INTERNAL_ERROR)
                } catch (e: Exception) {
                    Log.w(TAG, "Callback failed", e)
                }
                return@launchSafely
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
                params.keyFileSupplier?.let { keyFileSupplier ->
                    Log.d(TAG, "Using key file supplier")
                    try {
                        while (keyFileSupplier.isAvailable && keyFileSupplier.hasNext()) {
                            withContext(Dispatchers.IO) {
                                try {
                                    val cacheFile = File(context.cacheDir, "en-keyfile-${System.currentTimeMillis()}-${Random.nextLong()}.zip")
                                    ParcelFileDescriptor.AutoCloseInputStream(keyFileSupplier.next()).use { it.copyToFile(cacheFile) }
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
                        }
                    } catch (e: Exception) {
                        Log.w(TAG, "Disconnected from key file supplier", e)
                    }
                }

                if (todoKeyFiles.size > 0) {
                    val time = (System.currentTimeMillis() - start).coerceAtLeast(1).toDouble() / 1000.0
                    Log.d(TAG, "$packageName/$token processed $keys keys (${todoKeyFiles.size} files pending) in ${time}s -> ${(keys.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")
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
                    withContext(Dispatchers.IO) {
                        try {
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
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed parsing file", e)
                        }
                    }
                }

                val time = (System.currentTimeMillis() - start).coerceAtLeast(1).toDouble() / 1000.0
                Log.d(TAG, "$packageName/$token processed $keys keys ($newKeys new) in ${time}s -> ${(keys.toDouble() / time * 1000).roundToInt().toDouble() / 1000.0} keys/s")

                database.noteAppAction(packageName, "provideDiagnosisKeys", JSONObject().apply {
                    put("request_token", token)
                    put("request_keys_size", params.keys?.size)
                    put("request_keyFiles_size", params.keyFiles?.size)
                    put("request_keys_count", keys)
                }.toString())

                if (!database.isAppAuthorized(packageName)) {
                    // Not sending results via broadcast if app not authorized
                    Log.d(TAG, "$packageName not yet authorized")
                    return@with
                }

                val exposureSummary = buildExposureSummary(token)

                try {
                    val intent = if (exposureSummary.matchedKeyCount > 0) {
                        Intent(ACTION_EXPOSURE_STATE_UPDATED)
                    } else {
                        Intent(ACTION_EXPOSURE_NOT_FOUND)
                    }
                    if (token != TOKEN_A) {
                        intent.putExtra(EXTRA_EXPOSURE_SUMMARY, exposureSummary)
                    }
                    intent.putExtra(EXTRA_TOKEN, token)
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
        lifecycleScope.launchSafely {
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
        lifecycleScope.launchSafely {
            ExposureDatabase.with(context) { database ->
                val pair = database.loadConfiguration(packageName, params.token)
                val response = if (pair != null && database.isAppAuthorized(packageName)) {
                    database.findAllMeasuredExposures(pair.first).merge().map {
                        it.toExposureInformation(pair.second.orDefault())
                    }
                } else {
                    // Not providing information if app not authorized
                    Log.d(TAG, "$packageName not yet authorized")
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

    private fun ScanInstance.Builder.apply(subExposure: MergedSubExposure): ScanInstance.Builder {
        return this
                .setSecondsSinceLastScan(subExposure.duration.coerceAtMost(5 * 60).toInt())
                .setMinAttenuationDb(subExposure.attenuation) // FIXME: We use the average for both, because we don't store the minimum attenuation yet
                .setTypicalAttenuationDb(subExposure.attenuation)
    }

    private fun List<MergedSubExposure>.toScanInstances(): List<ScanInstance> {
        val res = arrayListOf<ScanInstance>()
        for (subExposure in this) {
            res.add(ScanInstance.Builder().apply(subExposure).build())
            if (subExposure.duration > 5 * 60 * 1000L) {
                res.add(ScanInstance.Builder().apply(subExposure).setSecondsSinceLastScan((subExposure.duration - 5 * 60).coerceAtMost(5 * 60).toInt()).build())
            }
        }
        return res
    }

    private fun DiagnosisKeysDataMapping?.orDefault() = this ?: DiagnosisKeysDataMapping()

    private suspend fun getExposureWindowsInternal(token: String = TOKEN_A): List<ExposureWindow> {
        val (exposures, mapping) = ExposureDatabase.with(context) { database ->
            val triple = database.loadConfiguration(packageName, token)
            if (triple != null && database.isAppAuthorized(packageName)) {
                database.findAllMeasuredExposures(triple.first).merge() to triple.third.orDefault()
            } else {
                // Not providing windows if app not authorized
                Log.d(TAG, "$packageName not yet authorized")
                emptyList<MergedExposure>() to DiagnosisKeysDataMapping()
            }
        }
        return exposures.map {
            val infectiousness =
                    if (it.key.daysSinceOnsetOfSymptoms == DAYS_SINCE_ONSET_OF_SYMPTOMS_UNKNOWN)
                        mapping.infectiousnessWhenDaysSinceOnsetMissing
                    else
                        mapping.daysSinceOnsetToInfectiousness[it.key.daysSinceOnsetOfSymptoms]
                                ?: Infectiousness.NONE
            val reportType =
                    if (it.key.reportType == ReportType.UNKNOWN)
                        mapping.reportTypeWhenMissing
                    else
                        it.key.reportType

            ExposureWindow.Builder()
                    .setCalibrationConfidence(it.confidence)
                    .setDateMillisSinceEpoch(it.key.rollingStartIntervalNumber.toLong() * ROLLING_WINDOW_LENGTH_MS)
                    .setInfectiousness(infectiousness)
                    .setReportType(reportType)
                    .setScanInstances(it.subs.toScanInstances())
                    .build()
        }
    }

    override fun getExposureWindows(params: GetExposureWindowsParams) {
        lifecycleScope.launchSafely {
            val response = getExposureWindowsInternal(params.token ?: TOKEN_A)

            ExposureDatabase.with(context) { database ->
                database.noteAppAction(packageName, "getExposureWindows", JSONObject().apply {
                    put("request_token", params.token)
                    put("response_size", response.size)
                }.toString())
            }

            try {
                params.callback.onResult(Status.SUCCESS, response)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    private fun DailySummariesConfig.bucketFor(attenuation: Int): Int {
        if (attenuation < attenuationBucketThresholdDb[0]) return 0
        if (attenuation < attenuationBucketThresholdDb[1]) return 1
        if (attenuation < attenuationBucketThresholdDb[2]) return 2
        return 3
    }

    private fun DailySummariesConfig.weightedDurationFor(attenuation: Int, seconds: Int): Double {
        return attenuationBucketWeights[bucketFor(attenuation)] * seconds
    }

    private fun Collection<DailySummary.ExposureSummaryData>.sum(): DailySummary.ExposureSummaryData {
        return DailySummary.ExposureSummaryData(map { it.maximumScore }.maxOrNull()
                ?: 0.0, sumByDouble { it.scoreSum }, sumByDouble { it.weightedDurationSum })
    }

    override fun getDailySummaries(params: GetDailySummariesParams) {
        lifecycleScope.launchSafely {
            val response = getExposureWindowsInternal().groupBy { it.dateMillisSinceEpoch }.map {
                val map = arrayListOf<DailySummary.ExposureSummaryData>()
                for (i in 0 until ReportType.VALUES) {
                    map[i] = DailySummary.ExposureSummaryData(0.0, 0.0, 0.0)
                }
                for (entry in it.value.groupBy { it.reportType }) {
                    for (window in entry.value) {
                        val weightedDuration = window.scanInstances.map { params.config.weightedDurationFor(it.typicalAttenuationDb, it.secondsSinceLastScan) }.sum()
                        val score = (params.config.reportTypeWeights[window.reportType] ?: 1.0) *
                                (params.config.infectiousnessWeights[window.infectiousness] ?: 1.0) *
                                weightedDuration
                        if (score >= params.config.minimumWindowScore) {
                            map[entry.key] = DailySummary.ExposureSummaryData(max(map[entry.key].maximumScore, score), map[entry.key].scoreSum + score, map[entry.key].weightedDurationSum + weightedDuration)
                        }
                    }
                }
                DailySummary((it.key / (1000L * 60 * 60 * 24)).toInt(), map, map.sum())
            }

            ExposureDatabase.with(context) { database ->
                database.noteAppAction(packageName, "getDailySummaries", JSONObject().apply {
                    put("response_size", response.size)
                }.toString())
            }

            try {
                params.callback.onResult(Status.SUCCESS, response)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun setDiagnosisKeysDataMapping(params: SetDiagnosisKeysDataMappingParams) {
        lifecycleScope.launchSafely {
            ExposureDatabase.with(context) { database ->
                database.storeConfiguration(packageName, TOKEN_A, params.mapping)
                database.noteAppAction(packageName, "setDiagnosisKeysDataMapping")
            }
            try {
                params.callback.onResult(Status.SUCCESS)
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun getDiagnosisKeysDataMapping(params: GetDiagnosisKeysDataMappingParams) {
        lifecycleScope.launchSafely {
            val mapping = ExposureDatabase.with(context) { database ->
                val triple = database.loadConfiguration(packageName, TOKEN_A)
                database.noteAppAction(packageName, "getDiagnosisKeysDataMapping")
                triple?.third
            }
            try {
                params.callback.onResult(Status.SUCCESS, mapping.orDefault())
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun getPackageConfiguration(params: GetPackageConfigurationParams) {
        Log.w(TAG, "Not yet implemented: getPackageConfiguration")
        lifecycleScope.launchSafely {
            ExposureDatabase.with(context) { database ->
                database.noteAppAction(packageName, "getPackageConfiguration")
            }
            try {
                params.callback.onResult(Status.SUCCESS, PackageConfiguration.PackageConfigurationBuilder().setValues(Bundle.EMPTY).build())
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun getStatus(params: GetStatusParams) {
        Log.w(TAG, "Not yet implemented: getStatus")
        lifecycleScope.launchSafely {
            ExposureDatabase.with(context) { database ->
                database.noteAppAction(packageName, "getStatus")
            }
            try {
                params.callback.onResult(Status.SUCCESS, ExposureNotificationStatus.setToFlags(setOf(ExposureNotificationStatus.UNKNOWN)))
            } catch (e: Exception) {
                Log.w(TAG, "Callback failed", e)
            }
        }
    }

    override fun requestPreAuthorizedTemporaryExposureKeyHistory(params: RequestPreAuthorizedTemporaryExposureKeyHistoryParams) {
        // TODO: Proper implementation
        lifecycleScope.launchSafely {
            params.callback.onResult(Status.CANCELED)
        }
    }

    override fun requestPreAuthorizedTemporaryExposureKeyRelease(params: RequestPreAuthorizedTemporaryExposureKeyReleaseParams) {
        // TODO: Proper implementation
        lifecycleScope.launchSafely {
            params.callback.onResult(Status.CANCELED)
        }
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
