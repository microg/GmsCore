/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

internal class HeterodyneSyncer(private val context: Context) {

    companion object {
        const val RESULT_SUCCESS = 0
        const val RESULT_NETWORK_ERROR = 1
        const val RESULT_SERVER_ERROR = 2
        const val RESULT_AUTH_ERROR = 3
        const val RESULT_PARTIAL_SUCCESS = 4
        const val RESULT_NO_PACKAGES = 5
        private val syncMutex = Mutex()
    }

    private val database: PhenotypeDatabase by lazy {
        PhenotypeDatabase.getInstance(context)
    }

    private val syncClient: PhenotypeSyncClient by lazy {
        PhenotypeSyncClient(context)
    }

    data class SyncStatus(
        var packagesToSync: Int = 0,
        var packagesSynced: Int = 0,
        var packagesFailed: Int = 0,
        var flagsWritten: Int = 0,
        var tokensWritten: Int = 0
    )

    data class SyncResult(
        val resultCode: Int,
        val status: SyncStatus,
        val error: String? = null,
        val retryDelaySeconds: Int = 0
    )

    suspend fun syncAfter(packageName: String?, androidPackageName: String?, servingVersion: Long) = withContext(Dispatchers.IO) {
        syncMutex.withLock {
            val packages = database.getSyncAfterPackages(packageName, androidPackageName, servingVersion)
            if (packages.isEmpty()) return@withLock
            val result = syncPackagesLocked(packages, null, FetchReason.APPLICATION_PUSH)
            if (result.resultCode != RESULT_SUCCESS && result.resultCode != RESULT_NO_PACKAGES) {
                throw PhenotypeException(PhenotypeStatusCode.SYNC_FAILED, "Sync after operation failed")
            }
            currentCoroutineContext().ensureActive()
            database.recordSyncAfter(packageName, androidPackageName, servingVersion)
        }
    }

    suspend fun syncAllPackages(user: String? = null, fetchReason: FetchReason = FetchReason.PERIODIC): SyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock { syncAllPackagesLocked(user, fetchReason) }
    }

    private suspend fun syncAllPackagesLocked(user: String?, fetchReason: FetchReason): SyncResult {
        val status = SyncStatus()

        try {
            val packages = collectPackagesToSync()
            status.packagesToSync = packages.size

            if (packages.isEmpty()) {
                return SyncResult(RESULT_NO_PACKAGES, status)
            }

            val result = syncClient.sync(packages, fetchReason, user)

            if (!result.isSuccess) {
                Log.e(PHENOTYPE_LOG_TAG, "Configuration synchronization failed: HTTP ${result.httpStatusCode}")
                return when (result.httpStatusCode) {
                    401 -> SyncResult(RESULT_AUTH_ERROR, status, "Authentication required", result.retryDelaySeconds)
                    in 500..599 -> SyncResult(
                        RESULT_SERVER_ERROR, status,
                        "Server error: ${result.httpStatusCode}", result.retryDelaySeconds
                    )

                    else -> SyncResult(
                        RESULT_NETWORK_ERROR, status,
                        "HTTP ${result.httpStatusCode}", result.retryDelaySeconds
                    )
                }
            }

            val syncResult = processSuccessfulSync(result, packages.size, status)
            Log.d(
                PHENOTYPE_LOG_TAG, "Configuration synchronization finished: synced=${status.packagesSynced}, " +
                        "failed=${status.packagesFailed}, flags=${status.flagsWritten}, tokens=${status.tokensWritten}"
            )

            return syncResult

        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            Log.e(PHENOTYPE_LOG_TAG, "Network error during configuration synchronization (${e.javaClass.simpleName})")
            return SyncResult(RESULT_NETWORK_ERROR, status, e.message)
        } catch (e: Exception) {
            Log.e(PHENOTYPE_LOG_TAG, "Configuration synchronization failed (${e.javaClass.simpleName})")
            return SyncResult(RESULT_SERVER_ERROR, status, e.message)
        }
    }

    suspend fun syncPackages(packageNames: List<String>, user: String? = null, fetchReason: FetchReason = FetchReason.APPLICATION_PUSH): SyncResult = withContext(Dispatchers.IO) {
        syncMutex.withLock { syncPackagesLocked(packageNames, user, fetchReason) }
    }

    private suspend fun syncPackagesLocked(packageNames: List<String>, user: String?, fetchReason: FetchReason): SyncResult {
        val status = SyncStatus()

        try {
            val packages = packageNames.mapNotNull { packageName ->
                val version = database.getPackageVersion(packageName) ?: return@mapNotNull null
                PhenotypeSyncClient.PackageInfo(
                    configPackageName = packageName,
                    version = version
                )
            }

            status.packagesToSync = packages.size

            if (packages.isEmpty()) {
                return SyncResult(RESULT_NO_PACKAGES, status, "No valid packages found")
            }

            val result = syncClient.sync(packages, fetchReason, user)

            if (!result.isSuccess) {
                Log.e(PHENOTYPE_LOG_TAG, "Requested configuration synchronization failed: HTTP ${result.httpStatusCode}")
                return SyncResult(
                    if (result.httpStatusCode == 401) RESULT_AUTH_ERROR else RESULT_NETWORK_ERROR,
                    status, "HTTP ${result.httpStatusCode}", result.retryDelaySeconds
                )
            }

            val syncResult = processSuccessfulSync(result, packages.size, status)
            Log.d(
                PHENOTYPE_LOG_TAG, "Requested configuration synchronization finished: synced=${status.packagesSynced}, " +
                        "failed=${status.packagesFailed}"
            )
            return syncResult

        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(PHENOTYPE_LOG_TAG, "Requested configuration synchronization failed (${e.javaClass.simpleName})")
            return SyncResult(RESULT_SERVER_ERROR, status, e.message)
        }
    }

    private suspend fun processSuccessfulSync(
        result: PhenotypeSyncClient.SyncResult,
        packageCount: Int,
        status: SyncStatus
    ): SyncResult {
        currentCoroutineContext().ensureActive()
        val request = requireNotNull(result.request)
        val unavailablePackages = packageCount * result.unavailableAccounts
        status.packagesToSync = request.packages.size + unavailablePackages
        processSyncResponse(result.response!!, request, result.registrationRevisions, result.users, status)
        status.packagesFailed += unavailablePackages
        val resultCode = if (status.packagesFailed > 0) RESULT_PARTIAL_SUCCESS else RESULT_SUCCESS
        return SyncResult(resultCode, status, retryDelaySeconds = result.retryDelaySeconds)
    }

    private fun collectPackagesToSync(): List<PhenotypeSyncClient.PackageInfo> =
        database.readableDatabase.query(
            PhenotypeDatabase.TABLE_PACKAGES,
            arrayOf(PhenotypeDatabase.COL_PACKAGE_NAME, PhenotypeDatabase.COL_VERSION),
            "${PhenotypeDatabase.COL_PACKAGE_NAME} NOT GLOB '${PHENOTYPE_INTERNAL_PACKAGE_PREFIX}*'",
            null, null, null, null
        ).use { cursor ->
            buildList {
                while (cursor.moveToNext()) {
                    add(PhenotypeSyncClient.PackageInfo(cursor.getString(0), cursor.getLong(1)))
                }
            }
        }

    private fun processSyncResponse(
        response: SyncResponse,
        request: SyncRequest,
        registrationRevisions: Map<String, Long>,
        users: List<String>,
        status: SyncStatus
    ) {
        val db = database.writableDatabase
        val changedPackages = mutableSetOf<String>()
        val accounts = users.filter { it.isNotEmpty() }
        fun identity(version: PackageVersion): Triple<String?, Long?, Int> =
            Triple(version.packageName, version.version, version.signatureInfo?.accountIndex ?: 0)

        fun userFor(version: PackageVersion): String {
            val index = version.signatureInfo?.accountIndex ?: 0
            return if (index == -1 && "" in users) "" else {
                require(index in accounts.indices) { "Unrequested response account" }
                accounts[index]
            }
        }

        val requested = request.packages.map { requireNotNull(it.version) }.associateBy { identity(it) }
        require(registrationRevisions.keys == requested.values.map { requireNotNull(it.packageName) }.toSet()) {
            "Missing request registrations"
        }
        val returned = mutableSetOf<Triple<String?, Long?, Int>>()
        val failedAccounts = response.failedAccounts.toSet()
        require(failedAccounts.all { index -> requested.keys.any { it.third == index } }) { "Unrequested failed account" }

        try {
            db.beginTransaction()
            database.checkSyncRegistrations(registrationRevisions)
            database.updateRequestTag(db, users, response.bytesTag)

            for (configPackage in response.configPackages) {
                val version = requireNotNull(configPackage.version) { "Missing package version" }
                val key = identity(version)
                require(key in requested) { "Unrequested configuration package" }
                require(returned.add(key)) { "Duplicate configuration response" }
                if (processConfigPackage(db, response, configPackage, userFor(version), status)) {
                    changedPackages.add(requireNotNull(version.packageName))
                }
            }

            for ((key, version) in requested) {
                if (key.third in failedAccounts) {
                    status.packagesFailed++
                    continue
                }
                if (key !in returned) {
                    db.update(
                        PhenotypeDatabase.TABLE_EXPERIMENT_TOKENS, ContentValues().apply {
                            put(PhenotypeDatabase.COL_SERVING_VERSION, response.serverTimestamp ?: 0L)
                        }, "packageName = ? AND version = ? AND user = ? AND isCommitted = 0",
                        arrayOf(requireNotNull(version.packageName), version.version.toString(), userFor(version))
                    )
                }
                database.markPackageSyncedDirect(db, requireNotNull(version.packageName), requireNotNull(version.version))
                status.packagesSynced++
            }

            db.insertWithOnConflict(PhenotypeDatabase.TABLE_LAST_FETCH, null, ContentValues().apply {
                put("key", "last_sync")
                put("timestamp", response.serverTimestamp ?: 0L)
            }, SQLiteDatabase.CONFLICT_REPLACE)

            database.pruneHistory(db)
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            status.packagesSynced = 0
            status.packagesFailed = 0
            status.flagsWritten = 0
            status.tokensWritten = 0
            throw e
        } finally {
            db.endTransaction()
        }
        val pendingUpdates = changedPackages + database.getPendingUpdatePackages()
        try {
            database.publishSharedConfigurations(
                context,
                requested.values.filter { identity(it).third !in failedAccounts }
                    .map { version -> requireNotNull(version.packageName) to userFor(version) },
                response.serverTimestamp ?: 0L
            )
        } finally {
            PhenotypeListeners.notifyChanges(context, pendingUpdates)
        }
    }

    private fun processConfigPackage(
        db: SQLiteDatabase,
        response: SyncResponse,
        configPackage: ConfigPackage,
        user: String,
        status: SyncStatus
    ): Boolean {
        val packageVersion = requireNotNull(configPackage.version) { "Missing package version" }
        val packageName = requireNotNull(packageVersion.packageName) { "Missing package name" }
        val version = requireNotNull(packageVersion.version) { "Missing version" }
        require(version >= 0L) { "Invalid package version" }
        var partitionsChanged = false
        val partitionSelection = "packageName = ? AND version = ? AND user = ? AND partitionId = ?"

        for (partition in configPackage.flagPartitions.filter { it.partitionType == 2 }) {
            val ordinal = partition.tag?.ordinal ?: 0L
            val deletedFlags = database.deleteUncommittedFlagsByPartition(db, packageName, version, user, ordinal)
            val deletedTags = db.delete(
                PhenotypeDatabase.TABLE_APPLICATION_TAGS, partitionSelection,
                arrayOf(packageName, version.toString(), user, ordinal.toString())
            )
            if (deletedFlags > 0 || deletedTags > 0) partitionsChanged = true
        }
        for (partition in configPackage.flagPartitions.filter { it.partitionType != 2 }) {
            val ordinal = partition.tag?.ordinal ?: 0L
            val tag = partition.tag?.tag?.toByteArray()?.takeIf { it.isNotEmpty() } ?: continue
            val args = arrayOf(packageName, version.toString(), user, ordinal.toString())
            val oldTag = db.query(
                PhenotypeDatabase.TABLE_APPLICATION_TAGS, arrayOf("tag"),
                partitionSelection, args, null, null, null
            ).use {
                if (it.moveToFirst()) it.getBlob(0) else null
            }
            if (tag.contentEquals(oldTag)) continue
            val values = ContentValues().apply {
                put("packageName", packageName)
                put("version", version)
                put("user", user)
                put("partitionId", ordinal)
                put("tag", tag)
            }
            check(
                db.insertWithOnConflict(
                    PhenotypeDatabase.TABLE_APPLICATION_TAGS, null, values,
                    SQLiteDatabase.CONFLICT_REPLACE
                ) != -1L
            ) { "Partition tag write failed" }
            database.deleteUncommittedFlagsByPartition(db, packageName, version, user, ordinal)
            for (flag in partition.flags) {
                if (writeFlag(db, packageName, version, user, flag, ordinal)) status.flagsWritten++
            }
            partitionsChanged = true
        }

        var changed = partitionsChanged
        val old = database.getExperimentToken(packageName, version, user)
        val suppliedTokens = configPackage.experimentToken != null || configPackage.serverToken != null ||
                configPackage.tokensTag != null
        val direct = if (suppliedTokens) configPackage.experimentToken?.toByteArray() ?: byteArrayOf()
        else old?.experimentToken ?: byteArrayOf()
        val serverToken = if (suppliedTokens) configPackage.serverToken.orEmpty() else old?.serverToken.orEmpty()
        val tokensTag = if (suppliedTokens) configPackage.tokensTag?.toByteArray()?.takeIf { it.isNotEmpty() }
        else old?.tokensTag
        val tokensChanged = suppliedTokens && (old == null || !direct.contentEquals(old.experimentToken) ||
                serverToken != old.serverToken || !tokensTag.contentEquals(old.tokensTag))
        if (tokensChanged) {
            changed = true
            db.delete(
                PhenotypeDatabase.TABLE_CROSS_TOKENS,
                "fromUser = ? AND toPackageName = ? AND toVersion = ? AND isCommitted = 0",
                arrayOf(user, packageName, version.toString())
            )
        }
        for (cross in configPackage.crossLoggedTokens) {
            val sourcePackage = cross.sourceVersion?.packageName.orEmpty()
            val sourceVersion = cross.sourceVersion?.version ?: 0L
            val provenance = cross.type?.takeIf { it in 0..4 } ?: 0
            if (!tokensChanged && db.query(
                    PhenotypeDatabase.TABLE_CROSS_TOKENS, arrayOf("1"),
                    "fromPackageName = ? AND fromVersion = ? AND fromUser = ? AND toPackageName = ? " +
                            "AND toVersion = ? AND isCommitted = 0 AND provenance = ? AND hex(token) = upper(?)",
                    arrayOf(
                        sourcePackage, sourceVersion.toString(), user, packageName, version.toString(),
                        provenance.toString(), cross.token?.hex().orEmpty()
                    ), null, null, null, "1"
                ).use {
                    it.moveToFirst()
                }
            ) continue
            db.insertOrThrow(PhenotypeDatabase.TABLE_CROSS_TOKENS, null, ContentValues().apply {
                put("fromPackageName", sourcePackage)
                put("fromVersion", sourceVersion)
                put("fromUser", user)
                put("toPackageName", packageName)
                put("toVersion", version)
                put("isCommitted", 0)
                put("token", cross.token?.toByteArray() ?: byteArrayOf())
                put("provenance", provenance)
            })
            changed = true
        }
        val hash = if (partitionsChanged) partitionHash(configPackage).toString() else old?.configHash ?: "0"
        database.insertExperimentTokenDirect(
            db, packageName, version, user, direct, serverToken,
            configPackage.servingVersion ?: response.serverTimestamp ?: 0L, configHash = hash, tokensTag = tokensTag,
            configurationChanged = changed
        )
        status.tokensWritten++
        if (changed) database.incrementChangeCount(db, packageName)
        return changed
    }

    private fun partitionHash(configPackage: ConfigPackage): Int {
        var result = 0
        for (partition in configPackage.flagPartitions) {
            val ordinal = partition.tag?.ordinal ?: 0L
            var hash = (ordinal xor (ordinal ushr 32)).toInt()
            for (byte in partition.tag?.tag?.toByteArray() ?: byteArrayOf()) hash = hash * 31 + byte
            result = (result * 17) xor hash
        }
        return result
    }

    private fun writeFlag(
        db: SQLiteDatabase,
        packageName: String,
        version: Long,
        user: String,
        flagValue: FlagValue,
        partitionId: Long
    ): Boolean {
        val type = flagValue.valueType ?: return false
        if (type !in 1..5) return false
        database.insertFlagDirect(
            db = db,
            packageName = packageName,
            version = version,
            name = flagValue.name.orEmpty(),
            flagType = 0,
            intVal = if (type == 1) flagValue.intValue ?: 0L else null,
            boolVal = if (type == 2) flagValue.boolValue ?: false else null,
            floatVal = if (type == 3) flagValue.floatValue ?: 0.0 else null,
            stringVal = if (type == 4) flagValue.stringValue.orEmpty() else null,
            extensionVal = if (type == 5) flagValue.extensionValue?.data_?.toByteArray() ?: byteArrayOf() else null,
            partitionId = partitionId,
            user = user
        )
        return true
    }
}
