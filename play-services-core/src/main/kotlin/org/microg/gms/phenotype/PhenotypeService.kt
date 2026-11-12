/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.content.Context
import android.os.Parcel
import android.util.Base64
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.DogfoodsToken
import com.google.android.gms.phenotype.Flag
import com.google.android.gms.phenotype.FlagOverrides
import com.google.android.gms.phenotype.GenericDimension
import com.google.android.gms.phenotype.RegistrationInfo
import com.google.android.gms.phenotype.internal.IFlagUpdateListener
import com.google.android.gms.phenotype.internal.IGetStorageInfoCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

class PhenotypeService : BaseService(PHENOTYPE_LOG_TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        val connectionInfo = ConnectionInfo().apply { features = PHENOTYPE_FEATURES }
        callback.onPostInitCompleteWithConnectionInfo(0, PhenotypeServiceImpl(this, packageName, lifecycle).asBinder(), connectionInfo)
    }
}

internal class PhenotypeServiceImpl(
    private val context: Context,
    private val callingPackage: String?,
    override val lifecycle: Lifecycle
) : IPhenotypeService.Stub(), LifecycleOwner {

    private val database: PhenotypeDatabase by lazy {
        PhenotypeDatabase.getInstance(context)
    }

    private fun resolvePackageName(packageName: String, caller: String): String =
        database.resolvePackageName(packageName, caller)

    private fun verifyCallingPackage() = context.verifyPhenotypeCaller(callingPackage)

    private fun assertFirstParty() = context.assertPhenotypeFirstParty()

    private fun assertNamespaceAccess(packageName: String?) =
        context.assertPhenotypeNamespaceAccess(database, packageName, callingPackage)

    override fun register(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        version: Int,
        logSources: Array<out String>?,
        params: ByteArray?
    ) {
        assertNamespaceAccess(packageName)
        val sources = logSources?.clone()
        val parameters = params?.clone()
        runStatusOperation(callbacks::onRegistered) {
            context.registerPhenotypeConfiguration(
                callingPackage, packageName, version, sources, parameters, false, null
            )
        }
    }

    private fun runStatusOperation(callback: (Status) -> Unit, operation: suspend () -> Status) {
        runOperation({ status, result -> callback(result ?: status) }, operation = operation)
    }

    private fun runOptionalStatusOperation(callback: IStatusCallback?, operation: suspend () -> Status) {
        runOperation({ status, result -> callback?.onResult(result ?: status) }, operation = operation)
    }

    private fun <T> runOperation(callback: (Status, T?) -> Unit, operation: suspend () -> T) {
        verifyCallingPackage()
        runPhenotypeOperation(context, callback, operation)
    }

    override fun weakRegister(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        version: Int,
        logSources: Array<out String>?,
        logSourceIds: IntArray?,
        params: ByteArray?
    ) {
        assertFirstParty()
        val sources = logSources?.clone()
        val ids = logSourceIds?.clone()
        val parameters = params?.clone()
        runStatusOperation(callbacks::onWeakRegistered) {
            context.registerPhenotypeConfiguration(
                callingPackage, packageName, version, sources, parameters, true, ids
            )
        }
    }

    override fun unregister(callbacks: IPhenotypeCallbacks, packageName: String?) {
        assertNamespaceAccess(packageName)
        runStatusOperation(callbacks::onUnregistered) {
            if (packageName == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package name")
            check(database.unregisterPackage(resolvePackageName(packageName, callingPackage.orEmpty()), callingPackage)) {
                "Unregistration failed"
            }
            Status.SUCCESS
        }
    }

    override fun getConfigurationSnapshot(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?
    ) {
        getConfigurationSnapshotWithToken(callbacks, packageName, user, null)
    }

    override fun commitToConfiguration(callbacks: IPhenotypeCallbacks, snapshotToken: String?) {
        commit(callbacks, snapshotToken, null)
    }

    private fun commit(callbacks: IPhenotypeCallbacks, snapshotToken: String?, propertiesPackage: String?) {
        assertNamespaceAccess(phenotypeSnapshotTarget(snapshotToken))
        runStatusOperation(callbacks::onCommitedToConfiguration) {
            context.commitPhenotypeConfiguration(database, callingPackage, snapshotToken, propertiesPackage)
        }
    }

    override fun commitToConfigurationWithProperties(callbacks: IPhenotypeCallbacks, properties: ByteArray?) {
        val request = try {
            CommitConfigurationProperties.ADAPTER.decode(properties!!)
        } catch (e: Exception) {
            runStatusOperation(callbacks::onCommitedToConfiguration) {
                throw PhenotypeException(
                    PhenotypeStatusCode.INVALID_ARGUMENT,
                    "Failed to parse commit properties",
                    e
                )
            }
            return
        }
        commit(callbacks, request.snapshotToken.orEmpty(), request.configPackage.orEmpty())
    }

    override fun getExperimentTokens(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        logSourceName: String?
    ) {
        assertNamespaceAccess(packageName)
        experimentTokens(callbacks, packageName, logSourceName, callingPackage)
    }

    override fun getDogfoodsToken(callbacks: IPhenotypeCallbacks) {
        assertFirstParty()
        runOperation(callbacks::onDogfoodsToken) {
            DogfoodsToken(database.getDogfoodsToken())
        }
    }

    override fun setDogfoodsToken(callbacks: IPhenotypeCallbacks, token: ByteArray?) {
        assertFirstParty()
        val value = token?.clone()
        runStatusOperation(callbacks::onDogfoodsTokenSet) {
            database.setDogfoodsToken(value)
            val result = HeterodyneSyncer(context).syncAllPackages(fetchReason = FetchReason.MOBDOG)
            if (result.resultCode != HeterodyneSyncer.RESULT_SUCCESS) {
                throw PhenotypeException(PhenotypeStatusCode.SYNC_FAILED, "Sync failed")
            }
            Status.SUCCESS
        }
    }

    override fun getFlag(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        name: String?,
        type: Int
    ) {
        assertNamespaceAccess(packageName)
        runOperation(callbacks::onFlag) {
            if (packageName == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package name")
            if (name == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No flag name")
            val resolved = resolvePackageName(packageName, callingPackage.orEmpty())
            if (!database.packageExists(resolved)) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            database.getCommittedFlag(resolved, name, type)
        }
    }

    override fun getCommitedConfiguration(callbacks: IPhenotypeCallbacks, packageName: String?) {
        assertNamespaceAccess(packageName)
        runOperation(callbacks::onCommittedConfiguration) {
            if (packageName == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package name")
            val resolved = resolvePackageName(packageName, callingPackage.orEmpty())
            if (resolved == "all") database.getRegistrationSnapshot()
            else database.getCommittedConfiguration(resolved)
                ?: throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
        }
    }

    override fun getConfigurationSnapshotWithToken(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?,
        baseSnapshotToken: String?
    ) {
        assertNamespaceAccess(packageName)
        runOperation(callbacks::onConfiguration) {
            if (packageName == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package name")
            val resolved = resolvePackageName(packageName, callingPackage.orEmpty())
            database.getConfigurationSnapshot(resolved, context.validatePhenotypeUser(user), baseSnapshotToken)
        }
    }

    override fun getExperimentTokensForLogging(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        logSourceName: String?,
        token: String?,
        clientPackageName: String?
    ) {
        assertFirstParty()
        experimentTokens(callbacks, packageName, logSourceName, clientPackageName)
    }

    private fun experimentTokens(callbacks: IPhenotypeCallbacks, packageName: String?, logSourceName: String?, caller: String?) {
        runOperation(callbacks::onExperimentTokens) {
            database.getExperimentTokens(packageName, logSourceName, caller)
                ?: throw PhenotypeException(
                    PhenotypeStatusCode.NO_CONFIGURATION_PACKAGES,
                    "No configuration packages for this query"
                )
        }
    }

    override fun syncAfterOperation(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        version: Long
    ) {
        assertFirstParty()
        syncAfter(callbacks, packageName, null, version)
    }

    private fun syncAfter(callbacks: IPhenotypeCallbacks?, packageName: String?, androidPackageName: String?, version: Long) {
        runOperation({ status, _ -> callbacks?.onSyncFinished(status, 0L) }) {
            HeterodyneSyncer(context).syncAfter(packageName, androidPackageName, version)
        }
    }

    override fun registerSync(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        version: Int,
        logSources: Array<out String>?,
        params: ByteArray?,
        user: String?,
        baseSnapshotToken: String?
    ) {
        assertNamespaceAccess(packageName)
        val sources = logSources?.clone()
        val parameters = params?.clone()
        runOperation(callbacks::onConfiguration) {
            val account = context.validatePhenotypeUser(user)
            val registration = clientRegistration(packageName.orEmpty(), version, callingPackage.orEmpty(), sources, parameters)
            val resolved = resolvePackageName(packageName.orEmpty(), callingPackage.orEmpty())
            context.registerPhenotypeSync(database, registration, account, resolved, baseSnapshotToken)
        }
    }

    override fun setFlagOverrides(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?,
        flagName: String?,
        flagType: Int,
        flagDataType: Int,
        flagValue: String?
    ) {
        assertFirstParty()
        runStatusOperation(callbacks::onFlagOverridesSet) {
            try {
                if (packageName.isNullOrEmpty() || flagName.isNullOrEmpty()) {
                    throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package or flag name")
                }
                val value = requireNotNull(flagValue) { "No flag value" }
                val flag = when (flagDataType) {
                    Flag.DATA_TYPE_LONG -> Flag(flagName, value.toLong(), flagType)
                    Flag.DATA_TYPE_BOOL -> {
                        require(value.equals("true", true) || value.equals("false", true)) { "Invalid boolean value" }
                        Flag(flagName, value.equals("true", true), flagType)
                    }

                    Flag.DATA_TYPE_DOUBLE -> Flag(flagName, value.toDouble(), flagType)
                    Flag.DATA_TYPE_STRING -> Flag(flagName, value, flagType)
                    Flag.DATA_TYPE_BYTES -> Flag(
                        flagName,
                        Base64.decode(value, Base64.NO_PADDING or Base64.NO_WRAP), flagType
                    )

                    else -> throw IllegalArgumentException("Unrecognized flag type")
                }
                context.setPhenotypeFlagOverrides(
                    database, callingPackage, packageName, user, listOf(flag)
                )
            } catch (e: IllegalArgumentException) {
                throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid flag value")
            }
        }
    }

    override fun deleteFlagOverrides(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?,
        flagName: String?
    ) {
        assertFirstParty()
        runOperation(callbacks::onFlagOverrides) {
            val deleted = database.deleteFlagOverrides(packageName?.let { resolvePackageName(it, callingPackage.orEmpty()) }, user, flagName)
            database.publishSharedConfigurationsForKnownUsers(
                context, deleted.map { it.packageName },
                additionalUsers = deleted.map { it.user })
            PhenotypeListeners.notifyChanges(context, deleted.map { it.packageName }.toSet())
            FlagOverrides(deleted)
        }
    }

    override fun listFlagOverrides(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?,
        flagName: String?
    ) {
        assertFirstParty()
        runOperation({ status, result -> callbacks.onFlagOverrides(status, result ?: FlagOverrides()) }) {
            FlagOverrides(database.listFlagOverrides(packageName?.let {
                resolvePackageName(it, callingPackage.orEmpty())
            }, user, flagName))
        }
    }

    override fun setFlags(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        user: String?,
        flags: Array<out Flag>?
    ) {
        assertFirstParty()
        val values = flags?.map { it?.deepCopy() }
        runStatusOperation(callbacks::onFlagOverridesSet) {
            context.setPhenotypeFlagOverrides(database, callingPackage, packageName, user, values)
        }
    }

    override fun bulkRegister(callbacks: IPhenotypeCallbacks, infos: Array<out RegistrationInfo?>?) {
        assertFirstParty()
        val registrations = infos?.map { it?.deepCopy() }.orEmpty()
        runStatusOperation(callbacks::onRegistered) { PhenotypeRegistrar.registerBatch(context, registrations) }
    }

    override fun setAppSpecificProperties(
        callbacks: IPhenotypeCallbacks,
        packageName: String?,
        properties: ByteArray?
    ) {
        assertNamespaceAccess(packageName)
        val value = properties?.clone()
        runStatusOperation(callbacks::onAppSpecificPropertiesSet) {
            if (packageName == null || callingPackage == null) {
                throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Missing package")
            }
            val resolved = resolvePackageName(packageName, callingPackage)
            val changed = database.setAppSpecificProperties(resolved, callingPackage, value)
            if (changed < 0) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            if (changed > 0) PhenotypeSyncDebouncer.trigger(context, FetchReason.NEW_APP_PROPERTIES)
            Status.SUCCESS
        }
    }

    override fun getServingVersion(callbacks: IPhenotypeCallbacks) {
        assertFirstParty()
        runOperation({ status, version -> callbacks.onServingVersion(status, version ?: -1L) }) {
            database.getServingVersion()
        }
    }

    override fun syncAllAfterOperation(callbacks: IPhenotypeCallbacks?, version: Long) {
        assertFirstParty()
        syncAfter(callbacks, null, callingPackage, version)
    }

    override fun setRuntimeProperties(callbacks: IStatusCallback?, packageName: String?, properties: ByteArray?) {
        assertNamespaceAccess(packageName)
        val value = properties?.clone()
        runOptionalStatusOperation(callbacks) {
            if (packageName == null || callingPackage == null) {
                throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Missing package")
            }
            val resolved = resolvePackageName(packageName, callingPackage)
            val changed = database.setRuntimeProperties(resolved, callingPackage, value)
            if (changed < 0) {
                throw PhenotypeException(PhenotypeStatusCode.PACKAGE_NOT_REGISTERED, "Package not registered")
            }
            if (changed > 0) PhenotypeSyncDebouncer.trigger(context, FetchReason.NEW_APP_PROPERTIES)
            Status.SUCCESS
        }
    }

    override fun setAppWideProperties(callbacks: IStatusCallback?, properties: ByteArray?) {
        val value = properties?.clone()
        runOptionalStatusOperation(callbacks) {
            database.setAppWideProperties(requireNotNull(callingPackage), value)
            Status.SUCCESS
        }
    }

    override fun setExternalExperiments(callbacks: IStatusCallback?, packageName: String?, experimentTokens: MutableList<*>?) {
        assertNamespaceAccess(packageName)
        val values = experimentTokens?.map { if (it is ByteArray) it.clone() else it }
        runOptionalStatusOperation(callbacks) {
            try {
                if (values != null) {
                    val namespace = requireNotNull(packageName) { "No package name" }
                    val tokens = values.map { require(it is ByteArray) { "Invalid external experiment token" }; it }
                    database.setExternalExperiments(requireNotNull(callingPackage), namespace, tokens)
                }
                Status.SUCCESS
            } catch (e: IllegalArgumentException) {
                throw PhenotypeException(
                    PhenotypeStatusCode.INVALID_ARGUMENT,
                    e.message ?: "Invalid external experiment token",
                    e
                )
            }
        }
    }

    override fun getStorageInfo(callbacks: IGetStorageInfoCallbacks?) {
        runOperation({ status, payload -> callbacks?.onStorageInfo(status, payload ?: byteArrayOf()) }) {
            context.getPhenotypeStorageInfo(database, callingPackage)
        }
    }

    override fun registerFlagUpdateListener(processName: String?, listener: IFlagUpdateListener?) {
        runOperation({ _, _ -> }) {
            if (listener == null || callingPackage == null) {
                throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No listener")
            }
            PhenotypeListeners.register(callingPackage, processName, listener)
            PhenotypeListeners.notifyPendingChanges(context, callingPackage)
        }
    }

    override fun setFlagOverrideLegacy(
        callbacks: IPhenotypeCallbacks?,
        packageName: String?,
        user: String?,
        flagName: String?,
        flagValue: String?
    ) {
        verifyCallingPackage()
    }

    override fun setGenericDimensions(
        callbacks: IPhenotypeCallbacks?,
        packageName: String?,
        user: String?,
        dimensions: Array<out GenericDimension>?
    ) {
        verifyCallingPackage()
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, PHENOTYPE_LOG_TAG) { super.onTransact(code, data, reply, flags) }
}
