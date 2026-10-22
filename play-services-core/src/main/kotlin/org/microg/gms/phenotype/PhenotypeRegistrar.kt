/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.CertData
import com.google.android.gms.phenotype.Configurations
import com.google.android.gms.phenotype.Flag
import com.google.android.gms.phenotype.RegistrationInfo
import java.io.IOException
import kotlinx.coroutines.delay
import okio.ByteString.Companion.toByteString
import org.microg.gms.common.isGooglePackage

private const val RETRY_REGISTER_SYNC = "Sync__retry_register_sync"
private const val RETRY_REGISTER_SYNC_PACKAGES = "Sync__retry_register_sync_static_config_packages"
private const val RETRY_REGISTER_SYNC_MAX_RETRIES = "Sync__retry_register_sync_max_retries"
private const val RETRY_REGISTER_SYNC_DELAY_MILLIS = "Sync__retry_register_sync_delay_millis"
private const val RETRY_REGISTER_SYNC_BACKOFF_FACTOR = "Sync__retry_register_sync_delay_backoff_factor"

private data class RegisterSyncRetryPolicy(
    val retries: Long,
    val delayMillis: Long,
    val backoffFactor: Long
)

internal object PhenotypeRegistrar {
    private const val PACKAGE_FLAGS = PackageManager.GET_META_DATA or PackageManager.GET_SIGNATURES

    fun register(context: Context, registration: DeclarativeRegistration, user: String? = null): FetchReason {
        val resource = if (registration.registrationSource == RegistrationSource.RESOURCE) registration
        else PhenotypeResourceReader(context).readRegistration(
            registration.androidPackage.orEmpty(),
            registration.configPackage.orEmpty()
        )
        return register(context, registration, user, resource)
    }

    private fun register(
        context: Context, registration: DeclarativeRegistration, user: String?,
        resource: DeclarativeRegistration?
    ): FetchReason {
        val database = PhenotypeDatabase.getInstance(context)
        val baseline = resource?.baselineCl ?: readBaselineCl(
            PhenotypeResourceReader(context), registration.androidPackage.orEmpty()
        )
        return database.registerPackage(registration, user, resource, baseline)
    }

    private fun readBaselineCl(reader: PhenotypeResourceReader, packageName: String): Long? = try {
        reader.readBaselineCl(packageName)
    } catch (_: IOException) {
        null
    } catch (_: Resources.NotFoundException) {
        null
    }

    fun registerForPackage(context: Context, packageName: String, triggerSync: Boolean) {
        val info = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(packageName, PACKAGE_FLAGS)
        } catch (_: PackageManager.NameNotFoundException) {
            return
        }
        val reason = registerForPackage(context, info)
        if (triggerSync && reason != FetchReason.UNSPECIFIED) PhenotypeSyncDebouncer.trigger(context, reason)
    }

    private fun registerForPackage(context: Context, info: PackageInfo): FetchReason {
        val packageName = info.packageName
        @Suppress("DEPRECATION")
        if (info.signatures.orEmpty().none { isGooglePackage(packageName, CertData(it.toByteArray())) }) {
            return FetchReason.UNSPECIFIED
        }
        val reader = PhenotypeResourceReader(context)
        val registrations = reader.readRegistrations(info)
        val database = PhenotypeDatabase.getInstance(context)
        var reason = FetchReason.UNSPECIFIED
        for (registration in registrations) {
            val configPackage = registration.configPackage!!
            val entryReason = register(context, registration, null, registration)
            if (!configPackage.startsWith(PHENOTYPE_INTERNAL_PACKAGE_PREFIX) && entryReason.value > reason.value) {
                reason = entryReason
            }
        }
        val currentPackages = registrations.mapTo(hashSetOf()) {
            database.resolvePackageName(requireNotNull(it.configPackage), packageName)
        }
        unregisterStaleResources(database, packageName, currentPackages)
        return reason
    }

    fun registerAll(context: Context, triggerSync: Boolean = true) {
        val packageManager = context.packageManager

        @Suppress("DEPRECATION")
        val packages = packageManager.getInstalledPackages(PACKAGE_FLAGS)
            .associateByTo(linkedMapOf()) { it.packageName }
        for (packageName in PhenotypeResourceReader(context).registrationPackageNames()) {
            if (packageName in packages) continue
            try {
                @Suppress("DEPRECATION")
                packages[packageName] = packageManager.getPackageInfo(packageName, PACKAGE_FLAGS)
            } catch (_: PackageManager.NameNotFoundException) {
            }
        }
        var reason = FetchReason.UNSPECIFIED
        for (info in packages.values) {
            try {
                val entryReason = registerForPackage(context, info)
                if (entryReason.value > reason.value) reason = entryReason
            } catch (e: Exception) {
                Log.w(PHENOTYPE_LOG_TAG, "Failed to read registration resources for ${info.packageName} (${e.javaClass.simpleName})")
            }
        }
        val database = PhenotypeDatabase.getInstance(context)
        for (owner in database.getResourceRegistrationOwners() - packages.keys) {
            unregisterStaleResources(database, owner, emptySet())
        }
        if (triggerSync && reason != FetchReason.UNSPECIFIED) PhenotypeSyncDebouncer.trigger(context, reason)
    }

    fun registerBatch(context: Context, infos: List<RegistrationInfo?>): Status {
        var failed = false
        var reason = FetchReason.UNSPECIFIED
        return try {
            val reader = PhenotypeResourceReader(context)
            val owners = infos.mapNotNull { it?.clientPackageName }.distinct()
            val resources = if (infos.any { it != null && it.registrationSource != RegistrationSource.RESOURCE.value }) {
                buildMap {
                    for (owner in owners) {
                        val info = try {
                            context.packageManager.getPackageInfo(owner, PackageManager.GET_META_DATA)
                        } catch (_: PackageManager.NameNotFoundException) {
                            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Registration APK not found")
                        }
                        for (entry in reader.readRegistrations(info)) {
                            check(put(entry.configPackage.orEmpty() to entry.androidPackage.orEmpty(), entry) == null) {
                                "Duplicate declarative registration"
                            }
                        }
                    }
                }
            } else emptyMap()
            val baselines = owners.associateWith { readBaselineCl(reader, it) }
            val database = PhenotypeDatabase.getInstance(context)
            val db = database.writableDatabase
            db.beginTransaction()
            try {
                for (info in infos) {
                    if (info == null) {
                        failed = true
                        continue
                    }
                    val owner = info.clientPackageName.orEmpty()
                    val source = RegistrationSource.fromValue(info.registrationSource)?.value
                        ?: RegistrationSource.CLIENT.value
                    val resource = if (source == RegistrationSource.RESOURCE.value) null
                    else resources[info.packageName.orEmpty() to owner]
                    try {
                        val registration = clientRegistration(
                            info.packageName.orEmpty(), info.version, owner,
                            info.field3, info.field4, info.weak, info.field6, info.clientCertHash, source
                        )
                        val declaration = if (source == RegistrationSource.RESOURCE.value) registration else resource
                        val entryReason = database.registerPackage(
                            registration, null, declaration,
                            declaration?.baselineCl ?: baselines[owner]
                        )
                        val resolved = database.resolvePackageName(info.packageName.orEmpty(), owner)
                        if (!resolved.startsWith(PHENOTYPE_INTERNAL_PACKAGE_PREFIX) && entryReason.value > reason.value) {
                            reason = entryReason
                        }
                    } catch (e: PhenotypeException) {
                        failed = true
                        Log.w(PHENOTYPE_LOG_TAG, "Batch registration entry rejected: status=${e.statusCode}")
                    }
                }
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
            if (reason != FetchReason.UNSPECIFIED) PhenotypeSyncDebouncer.trigger(context, reason)
            if (failed) Status(PhenotypeStatusCode.PARTIAL_REGISTRATION) else Status.SUCCESS
        } catch (e: PhenotypeException) {
            Status(e.statusCode, e.message)
        } catch (e: Exception) {
            Log.w(PHENOTYPE_LOG_TAG, "Batch registration failed", e)
            Status.INTERNAL_ERROR
        }
    }

    fun unregisterForPackage(context: Context, packageName: String) {
        PhenotypeDatabase.getInstance(context).unregisterByAndroidPackage(packageName)
    }

    private fun unregisterStaleResources(
        database: PhenotypeDatabase, androidPackage: String,
        currentPackages: Set<String>
    ) {
        for (packageName in database.getResourceConfigPackageNamesByAndroidPackage(androidPackage) - currentPackages) {
            check(database.unregisterPackage(packageName, androidPackage)) {
                "Failed to unregister stale configuration package $packageName"
            }
        }
    }
}

internal fun clientRegistration(
    packageName: String,
    version: Int,
    owner: String,
    logSources: Array<out String>?,
    params: ByteArray?,
    weak: Boolean? = null,
    weakIds: IntArray? = null,
    heterodyneInfo: ByteArray? = null,
    source: Int = 1
): DeclarativeRegistration {
    if (logSources.orEmpty().any { it.isEmpty() }) {
        throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid log source")
    }
    return DeclarativeRegistration(
        configPackage = packageName,
        version = version,
        androidPackage = owner,
        logSources = logSources.orEmpty().toList(),
        params = params?.takeIf { it.isNotEmpty() }?.toByteString(),
        weak = weak,
        weakExperimentIds = weakIds?.toList().orEmpty(),
        heterodyneInfo = heterodyneInfo?.takeIf { it.isNotEmpty() }?.toByteString(),
        registrationSource = RegistrationSource.fromValue(source)
    )
}

internal fun Context.registerPhenotypeConfiguration(
    callingPackage: String?,
    packageName: String?,
    version: Int,
    logSources: Array<out String>?,
    params: ByteArray?,
    weak: Boolean,
    weakIds: IntArray?
): Status {
    val registration = clientRegistration(
        packageName.orEmpty(), version, callingPackage.orEmpty(), logSources, params, weak, weakIds
    )
    val reason = PhenotypeRegistrar.register(this, registration)
    if (!registration.configPackage.orEmpty().startsWith(PHENOTYPE_INTERNAL_PACKAGE_PREFIX) &&
        reason != FetchReason.UNSPECIFIED
    ) PhenotypeSyncDebouncer.trigger(this, reason)
    return Status.SUCCESS
}

private suspend fun Context.registerPhenotypeAndSync(
    registration: DeclarativeRegistration,
    account: String,
    packageName: String
) {
    val reason = PhenotypeRegistrar.register(this, registration, account)
    if (packageName.startsWith(PHENOTYPE_INTERNAL_PACKAGE_PREFIX) || reason == FetchReason.UNSPECIFIED) return
    val result = HeterodyneSyncer(this).syncPackages(listOf(packageName), account, reason)
    if (result.resultCode != HeterodyneSyncer.RESULT_SUCCESS &&
        result.resultCode != HeterodyneSyncer.RESULT_NO_PACKAGES
    ) {
        throw PhenotypeException(PhenotypeStatusCode.SYNC_FAILED, result.error ?: "Register sync failed")
    }
}

private fun PhenotypeDatabase.registerSyncRetryPolicy(packageName: String): RegisterSyncRetryPolicy? {
    val enabled = getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, RETRY_REGISTER_SYNC)
        ?.takeIf { it.dataType == Flag.DATA_TYPE_BOOL }?.getBool() == true
    if (!enabled) return null
    val packages = getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, RETRY_REGISTER_SYNC_PACKAGES)
        ?.takeIf { it.dataType == Flag.DATA_TYPE_BYTES } ?: return null
    val allowed = try {
        ExperimentLogSources.ADAPTER.decode(packages.getBytes()).names
    } catch (e: java.io.IOException) {
        Log.w(PHENOTYPE_LOG_TAG, "Invalid registerSync retry package list", e)
        return null
    }
    if (packageName.substringBefore('#') !in allowed) return null
    val retries = getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, RETRY_REGISTER_SYNC_MAX_RETRIES)
        ?.takeIf { it.dataType == Flag.DATA_TYPE_LONG }?.getLong() ?: return null
    val delayMillis = getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, RETRY_REGISTER_SYNC_DELAY_MILLIS)
        ?.takeIf { it.dataType == Flag.DATA_TYPE_LONG }?.getLong() ?: return null
    val backoffFactor = getCommittedFlag(PHENOTYPE_CONTENT_AUTHORITY, RETRY_REGISTER_SYNC_BACKOFF_FACTOR)
        ?.takeIf { it.dataType == Flag.DATA_TYPE_LONG }?.getLong() ?: return null
    if (retries <= 0L || delayMillis < 0L || backoffFactor < 1L) {
        Log.w(PHENOTYPE_LOG_TAG, "Invalid registerSync retry policy")
        return null
    }
    return RegisterSyncRetryPolicy(retries, delayMillis, backoffFactor)
}

internal suspend fun Context.registerPhenotypeSync(
    database: PhenotypeDatabase,
    registration: DeclarativeRegistration,
    account: String,
    packageName: String,
    baseSnapshotToken: String?
): Configurations {
    registerPhenotypeAndSync(registration, account, packageName)
    val configuration = database.getConfigurationSnapshot(packageName, account, baseSnapshotToken)
    if (configuration.snapshotToken.isNotEmpty()) return configuration
    val policy = database.registerSyncRetryPolicy(packageName) ?: return configuration
    var remaining = policy.retries
    var delayMillis = policy.delayMillis
    var retryConfiguration = configuration
    while (retryConfiguration.snapshotToken.isEmpty() && remaining > 0L) {
        delay(delayMillis)
        registerPhenotypeAndSync(registration, account, packageName)
        retryConfiguration = database.getConfigurationSnapshot(packageName, account, baseSnapshotToken)
        remaining--
        delayMillis = if (delayMillis > Long.MAX_VALUE / policy.backoffFactor) {
            Long.MAX_VALUE
        } else {
            delayMillis * policy.backoffFactor
        }
    }
    if (retryConfiguration.snapshotToken.isEmpty()) {
        throw PhenotypeException(
            PhenotypeStatusCode.REGISTER_SYNC_RETRIES_EXHAUSTED,
            "Failed to registerSync for $packageName after ${policy.retries} retries"
        )
    }
    return retryConfiguration
}
