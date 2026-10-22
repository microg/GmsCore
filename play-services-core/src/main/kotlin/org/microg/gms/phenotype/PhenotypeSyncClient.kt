/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.accounts.Account
import android.accounts.AccountManager
import android.accounts.AuthenticatorException
import android.accounts.OperationCanceledException
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import org.microg.gms.profile.Build
import androidx.core.content.edit

internal class PhenotypeSyncClient(private val context: Context) {

    companion object {
        private const val TAG = "PhenotypeSyncClient"

        private const val PRIMARY_URL = "https://www.googleapis.com/experimentsandconfigs/v1/getExperimentsAndConfigs"
        private const val AUTH_SCOPE = "oauth2:https://www.googleapis.com/auth/experimentsandconfigs"
        private const val PSEUDONYMOUS_ID_PREFS = "PseudonymousIdIntentService"
        private const val PSEUDONYMOUS_ID_KEY = "pseudonymousId"

        @JvmStatic
        fun accounts(context: Context): List<Account> {
            val types = if (context.packageManager.hasSystemFeature("cn.google")) setOf("cn.google")
            else setOf("com.google", "com.google.work")
            return try {
                AccountManager.get(context).accounts.filter { it.type in types }.distinctBy { it.name }
            } catch (_: SecurityException) {
                Log.w(TAG, "Device accounts are unavailable")
                emptyList()
            }
        }

        private const val CONNECT_TIMEOUT = 30000
        private const val READ_TIMEOUT = 60000

        private const val USER_AGENT_FORMAT = "GoogleAuth/1.4 (%s %s); gzip"
    }

    data class SyncResult(
        val response: SyncResponse?,
        val retryDelaySeconds: Int,
        val httpStatusCode: Int,
        val request: SyncRequest? = null,
        val registrationRevisions: Map<String, Long> = emptyMap(),
        val users: List<String> = emptyList(),
        val unavailableAccounts: Int = 0
    ) {
        val isSuccess: Boolean get() = httpStatusCode in 200..299 && response != null
    }

    @SuppressLint("MissingPermission")
    fun sync(
        packages: List<PackageInfo>,
        reason: FetchReason,
        user: String? = null
    ): SyncResult {
        val manager = AccountManager.get(context)
        val available = if (user == "") emptyList() else accounts(context)
        val requested = if (user == null) listOf("") + available.map { it.name } else listOf(user)
        val credentials = linkedMapOf<Account, String>()
        var unavailable = 0
        for (name in requested) {
            if (name.isEmpty()) continue
            val account = available.find { it.name == name }
            val token = if (account == null) null else try {
                manager.blockingGetAuthToken(account, AUTH_SCOPE, false)
            } catch (_: IOException) {
                Log.w(TAG, "Account credential temporarily unavailable")
                null
            } catch (_: AuthenticatorException) {
                Log.w(TAG, "Account credential unavailable")
                null
            } catch (_: OperationCanceledException) {
                Log.w(TAG, "Account credential request canceled")
                null
            }
            if (token.isNullOrEmpty()) unavailable++ else credentials[requireNotNull(account)] = token
        }
        val users = requested.filter { name -> name.isEmpty() || credentials.keys.any { it.name == name } }
        if (users.isEmpty()) return SyncResult(null, 0, 401, unavailableAccounts = unavailable)
        val snapshot = buildSyncRequest(packages, reason, users)
        val request = snapshot.request
        val result = syncInternal(request, credentials.values.joinToString("+"))
        val invalid = if (result.httpStatusCode == 401) credentials.keys.toList()
        else result.response?.failedAccounts.orEmpty().mapNotNull { credentials.keys.elementAtOrNull(it) }
        for (account in invalid) {
            try {
                manager.invalidateAuthToken(account.type, credentials.getValue(account))
            } catch (_: SecurityException) {
                Log.w(TAG, "Rejected account credential could not be invalidated")
            }
        }
        return result.copy(
            request = request, registrationRevisions = snapshot.registrationRevisions,
            users = users, unavailableAccounts = unavailable
        )
    }

    private fun syncInternal(
        request: SyncRequest,
        authToken: String
    ): SyncResult {
        val fetchReason = request.fetchReason?.value ?: FetchReason.UNSPECIFIED.value
        val apiVersion = request.apiVersion ?: 0
        val url = "$PRIMARY_URL?r=$fetchReason&c=$apiVersion"

        var connection: HttpURLConnection? = null
        try {
            connection = (URL(url).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doInput = true
                doOutput = true
                connectTimeout = CONNECT_TIMEOUT
                readTimeout = READ_TIMEOUT

                setRequestProperty("Content-Type", "application/x-protobuf")
                setRequestProperty("Content-Encoding", "gzip")
                setRequestProperty("Accept-Encoding", "gzip")
                setRequestProperty("User-Agent", USER_AGENT_FORMAT.format(Build.DEVICE, Build.ID))

                if (authToken.isNotEmpty()) {
                    setRequestProperty("Authorization", "Bearer $authToken")
                }
            }

            GZIPOutputStream(connection.outputStream).use { it.write(request.encode()) }

            val responseCode = connection.responseCode
            Log.d(TAG, "Response code: $responseCode")
            val retryAfter = connection.getHeaderField("Retry-After")?.toIntOrNull() ?: 0

            if (responseCode !in 200..299) {
                return SyncResult(null, retryAfter, responseCode)
            }

            val input = connection.inputStream.let {
                if (connection.contentEncoding?.equals("gzip", ignoreCase = true) == true) GZIPInputStream(it) else it
            }
            val response = input.use { SyncResponse.ADAPTER.decode(it) }
            updatePseudonymousId(request.clientInfo?.deviceState, response.deviceState)

            return SyncResult(
                response = response,
                retryDelaySeconds = retryAfter,
                httpStatusCode = responseCode
            )

        } finally {
            connection?.disconnect()
        }
    }

    private fun buildSyncRequest(
        packages: List<PackageInfo>, fetchReason: FetchReason,
        users: List<String>
    ): PhenotypeDatabase.SyncRequestSnapshot {
        val abiList = if (android.os.Build.VERSION.SDK_INT >= 21) {
            android.os.Build.SUPPORTED_ABIS.joinToString(",")
        } else {
            listOfNotNull(android.os.Build.CPU_ABI, android.os.Build.CPU_ABI2)
                .filter { it.isNotEmpty() }
                .joinToString(",")
        }
        val deviceConfig = PhenotypeDeviceConfig.Builder()
            .sdkVersion(android.os.Build.VERSION.SDK_INT.toLong())
            .buildType(getBuildType())
            .manufacturer(Build.MANUFACTURER)
            .model(Build.MODEL)
            .product(Build.PRODUCT)
            .brand(Build.BRAND)
            .device(Build.DEVICE)
            .hardware(Build.HARDWARE)
            .board(Build.BOARD)
            .radio(Build.RADIO)
            .bootloader(Build.BOOTLOADER)
            .fingerprint(Build.FINGERPRINT)
            .buildId(Build.ID)
            .buildDisplay(Build.DISPLAY)
            .sdkVersionStr(android.os.Build.VERSION.SDK_INT.toString())
            .abiList(abiList)
            .gmsVersionCode(getGmsVersionCode())
            .playEnabled(true)
            .build()

        val syncInfo = SyncInfo.Builder()
            .syncType(4)
            .deviceConfig(deviceConfig)
            .build()

        val snapshot = PhenotypeDatabase.getInstance(context).createSyncRequest(
            packages.associate { it.configPackageName to it.version }, users, fetchReason
        )
        val request = snapshot.request
        val clientInfo = requireNotNull(request.clientInfo).newBuilder()
            .syncInfo(syncInfo)
            .deviceState(readPseudonymousId())
            .build()

        return snapshot.copy(
            request = request.newBuilder()
                .clientInfo(clientInfo)
                .apiVersion(1)
                .build()
        )
    }

    private fun readPseudonymousId(): String? = context.getSharedPreferences(PSEUDONYMOUS_ID_PREFS, Context.MODE_PRIVATE)
        .getString(PSEUDONYMOUS_ID_KEY, null)?.takeIf { it.isNotEmpty() }

    private fun updatePseudonymousId(expected: String?, value: String?) {
        if (value.isNullOrEmpty()) return
        val checked = if (value.length >= 2 && value.first() == '"' && value.last() == '"')
            value.substring(1, value.lastIndex) else value
        if (checked.any { it.code !in 0x21..0x7e || it == '"' || it == ',' || it == ';' || it == '\\' }) {
            Log.w(TAG, "Ignoring invalid pseudonymous ID from server")
            return
        }
        val preferences = context.getSharedPreferences(PSEUDONYMOUS_ID_PREFS, Context.MODE_PRIVATE)
        synchronized(preferences) {
            if (preferences.getString(PSEUDONYMOUS_ID_KEY, null) == expected) {
                preferences.edit(commit = true) { putString(PSEUDONYMOUS_ID_KEY, value) }
            }
        }
    }

    data class PackageInfo(
        val configPackageName: String,
        val version: Long
    )

    private fun getBuildType(): Int = when (android.os.Build.TYPE) {
        "user" -> 1
        "userdebug" -> 2
        "eng" -> 3
        else -> 1
    }

    private fun getGmsVersionCode(): Int {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionCode
        } catch (_: Exception) {
            0
        }
    }
}
