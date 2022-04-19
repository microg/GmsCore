/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.nearby.exposurenotification

import android.app.Activity
import android.app.PendingIntent
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.location.LocationManager
import android.os.*
import android.util.Base64
import android.util.Log
import androidx.core.location.LocationManagerCompat
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
import org.microg.gms.nearby.exposurenotification.proto.TEKSignatureList
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyExport
import org.microg.gms.nearby.exposurenotification.proto.TemporaryExposureKeyProto
import org.microg.gms.utils.warnOnTransactionIssues
import java.io.File
import java.io.InputStream
import java.security.KeyFactory
import java.security.MessageDigest
import java.security.Signature
import java.security.spec.X509EncodedKeySpec
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.random.Random

class ExposureNotificationServiceImpl(private val context: Context, private val lifecycle: Lifecycle, private val packageName: String) : INearbyExposureNotificationService.Stub(), LifecycleOwner {

    // Table of back-end public keys, used to verify the signature of the diagnosed TEKs.
    // The table is indexed by package names.
    private val backendPubKeyForPackage = mapOf<String, String>(
            "ch.admin.bag.dp3t.dev" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsFcEnOPY4AOAKkpv9HSdW2BrhUCWwL15Hpqu5zHaWy1Wno2KR8G6dYJ8QO0uZu1M6j8z6NGXFVZcpw7tYeXAqQ==",
            "ch.admin.bag.dp3t.test" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsFcEnOPY4AOAKkpv9HSdW2BrhUCWwL15Hpqu5zHaWy1Wno2KR8G6dYJ8QO0uZu1M6j8z6NGXFVZcpw7tYeXAqQ==",
            "ch.admin.bag.dp3t.abnahme" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEsFcEnOPY4AOAKkpv9HSdW2BrhUCWwL15Hpqu5zHaWy1Wno2KR8G6dYJ8QO0uZu1M6j8z6NGXFVZcpw7tYeXAqQ==",
            "ch.admin.bag.dp3t" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEK2k9nZ8guo7JP2ELPQXnUkqDyjjJmYmpt9Zy0HPsiGXCdI3SFmLr204KNzkuITppNV5P7+bXRxiiY04NMrEITg==",
            // CWA, see https://github.com/corona-warn-app/cwa-documentation/issues/740#issuecomment-963223074
            "de.rki.coronawarnapp" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc7DEstcUIRcyk35OYDJ95/hTg3UVhsaDXKT0zK7NhHPXoyzipEnOp3GyNXDVpaPi3cAfQmxeuFMZAIX2+6A5Xg==",
            // CCTG uses CWA infrastucture
            "de.corona.tracing" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc7DEstcUIRcyk35OYDJ95/hTg3UVhsaDXKT0zK7NhHPXoyzipEnOp3GyNXDVpaPi3cAfQmxeuFMZAIX2+6A5Xg==",
            // CCTG-Test builds don't have access any staging infrastructure, so again CWA key
            "de.corona.tracing.test" to
                    "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAEc7DEstcUIRcyk35OYDJ95/hTg3UVhsaDXKT0zK7NhHPXoyzipEnOp3GyNXDVpaPi3cAfQmxeuFMZAIX2+6A5Xg==",
        )

    // Back-end public key for this package
    private val backendPublicKey = backendPubKeyForPackage[packageName]?.let {
        try {
            KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(Base64.decode(it, Base64.DEFAULT)))
        } catch (e: Exception) {
            Log.w(TAG, "Failed to retrieve back-end public key for ${packageName}: " + e.message)
            null
        }
    }

    // Table of supported signature algorithms for the diagnosed TEKs.
    // The table is indexed by ASN.1 OIDs as specified in https://tools.ietf.org/html/rfc5758#section-3.2
    private val sigAlgoForOid = mapOf<String, Function0<Signature>>(
            "1.2.840.10045.4.3.2" to { Signature.getInstance("SHA256withECDSA") },
            "1.2.840.10045.4.3.4" to { Signature.getInstance("SHA512withECDSA") },
    )

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
        val pi = PendingIntent.getActivity(context, permission.hashCode(), intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
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
                .setDaysSinceLastExposure(exposures.map { it.daysSinceExposure }.minOrNull()?.toInt() ?: 0)
                .setMatchedKeyCount(exposures.map { it.key }.distinct().size)
                .setMaximumRiskScore(exposures.map { it.getRiskScore(configuration) }.maxOrNull()?.toInt() ?: 0)
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
                        if (backendPublicKey != null && !verifyKeyFile(cacheFile)) {
                            Log.w(TAG, "Skipping non-verified key file")
                            return@withContext
                        }
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

    private fun verifyKeyFile(file: File): Boolean {
        try {
            ZipFile(file).use { zip ->
                var dataEntry: ZipEntry? = null
                var sigEntry: ZipEntry? = null

                for (entry in zip.entries()) {
                    when (entry.name) {
                        "export.bin" ->
                            if (dataEntry != null) {
                                throw Exception("Zip archive contains more than one 'export.bin' entry")
                            } else {
                                dataEntry = entry
                            }
                        "export.sig" ->
                            if (sigEntry != null) {
                                throw Exception("Zip archive contains more than one 'export.sig' entry")
                            } else {
                                sigEntry = entry
                            }
                        else -> throw Exception("Unexpected entry in zip archive: ${entry.name}")
                    }
                }
                when {
                    dataEntry == null -> throw Exception("Zip archive does not contain 'export.bin'")
                    sigEntry == null -> throw Exception("Zip archive does not contain 'export.sin'")
                }

                val sigStream = zip.getInputStream(sigEntry)
                val sigList = TEKSignatureList.ADAPTER.decode(sigStream)

                for (sig in sigList.signatures) {
                    Log.d(TAG, "Verifying signature ${sig.batch_num}/${sig.batch_size}")
                    val sigInfo = sig.signature_info ?: throw Exception("Signature information is missing")
                    Log.d(TAG, "Signature info: algo=${sigInfo.signature_algorithm} key={id=${sigInfo.verification_key_id}, version=${sigInfo.verification_key_version}}")

                    val signature = sig.signature?.toByteArray() ?: throw Exception("Signature contents is missing")
                    val sigVerifier = (sigAlgoForOid.get(sigInfo.signature_algorithm) ?: throw Exception("Signature algorithm not supported: ${sigInfo.signature_algorithm}"))()
                    sigVerifier.initVerify(backendPublicKey)

                    val stream = zip.getInputStream(dataEntry)
                    val buf = ByteArray(1024)
                    var nbRead = 0
                    while (nbRead != -1) {
                        nbRead = stream.read(buf)
                        if (nbRead > 0) {
                            sigVerifier.update(buf, 0, nbRead)
                        }
                    }

                    if (!sigVerifier.verify(signature)) {
                        throw Exception("Signature does not verify")
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Key file verification failed: " + e.message)
            return false
        }

        Log.i(TAG, "Key file verification successful")
        return true
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
        lifecycleScope.launchSafely {
            val isAuthorized = ExposureDatabase.with(context) { database ->
                database.noteAppAction(packageName, "getStatus")
                database.isAppAuthorized(packageName)
            }
            val flags = hashSetOf<ExposureNotificationStatus>()
            val adapter = BluetoothAdapter.getDefaultAdapter()
            if (adapter == null || !adapter.isEnabled) {
                flags.add(ExposureNotificationStatus.BLUETOOTH_DISABLED)
                flags.add(ExposureNotificationStatus.BLUETOOTH_SUPPORT_UNKNOWN)
            } else if (Build.VERSION.SDK_INT < 21) {
                flags.add(ExposureNotificationStatus.EN_NOT_SUPPORT)
            } else if (adapter.bluetoothLeScanner == null){
                flags.add(ExposureNotificationStatus.HW_NOT_SUPPORT)
            }
            if (!LocationManagerCompat.isLocationEnabled(context.getSystemService(Context.LOCATION_SERVICE) as LocationManager)) {
                flags.add(ExposureNotificationStatus.LOCATION_DISABLED)
            }
            if (!isAuthorized) {
                flags.add(ExposureNotificationStatus.NO_CONSENT)
            }
            if (isAuthorized && ExposurePreferences(context).enabled) {
                flags.add(ExposureNotificationStatus.ACTIVATED)
            } else {
                flags.add(ExposureNotificationStatus.INACTIVATED)
            }
            try {
                params.callback.onResult(Status.SUCCESS, ExposureNotificationStatus.setToFlags(flags))
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
            params.callback.onResult(Status(FAILED_KEY_RELEASE_NOT_PREAUTHORIZED))
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags) { super.onTransact(code, data, reply, flags) }

    companion object {
        private val tempGrantedPermissions: MutableSet<Pair<String, String>> = hashSetOf()
    }
}
