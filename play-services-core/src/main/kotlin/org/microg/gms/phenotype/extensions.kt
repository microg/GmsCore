/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import android.os.Binder
import android.os.Parcel
import android.os.Process
import android.os.RemoteException
import android.os.TransactionTooLargeException
import android.os.UserManager
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.phenotype.Configurations
import com.google.android.gms.phenotype.Flag
import com.google.android.gms.phenotype.RegistrationInfo
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils

internal const val PHENOTYPE_LOG_TAG = "PhenotypeService"
internal const val PHENOTYPE_CONTENT_AUTHORITY = "com.google.android.gms.phenotype"
internal const val PHENOTYPE_INTERNAL_PACKAGE_PREFIX = "__internal."
internal const val PHENOTYPE_WILDCARD_USER = "*"
internal const val PHENOTYPE_CURRENT_TOKEN_PREFIX = "CURRENT:"

internal val PHENOTYPE_FEATURES = arrayOf(
    Feature("commit_to_configuration_v2_api", 1, true),
    Feature("get_serving_version_api", 1, true),
    Feature("get_experiment_tokens_api", 1, true),
    Feature("register_flag_update_listener_api", 2, true),
    Feature("sync_after_api", 1, true),
    Feature("sync_after_for_application_api", 1, true),
    Feature("set_app_wide_properties_api", 1, true),
    Feature("set_runtime_properties_api", 1, true),
    Feature("get_storage_info_api", 1, true)
)

internal object PhenotypeStatusCode {
    const val INVALID_ARGUMENT = 29500
    const val STALE_SNAPSHOT_TOKEN = 29501
    const val PACKAGE_NOT_REGISTERED = 29503
    const val SYNC_FAILED = 29504
    const val NO_CONFIGURATION_PACKAGES = 29505
    const val REGISTRATION_CONFLICT = 29506
    const val PARTIAL_REGISTRATION = 29507
    const val DIRECT_BOOT_UNSUPPORTED = 29508
    const val SNAPSHOT_TOO_LARGE = 29513
    const val STORAGE_INFO_UNSUPPORTED = 29514
    const val STORAGE_INFO_FAILURE = 29516
    const val COMMITTED_SNAPSHOT_BASE_CHANGED = 29539
    const val NEWER_SNAPSHOT_AVAILABLE = 29542
    const val SNAPSHOT_STATE_UNAVAILABLE = 29543
    const val REGISTER_SYNC_RETRIES_EXHAUSTED = 29546
}

internal class PhenotypeException(
    val statusCode: Int,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

internal fun Context.verifyPhenotypeCaller(callingPackage: String?) {
    if (Binder.getCallingUid() != Process.myUid()) {
        PackageUtils.getAndCheckCallingPackage(this, callingPackage)
    }
}

internal fun Context.assertPhenotypeFirstParty() {
    if (Binder.getCallingUid() != Process.myUid()) {
        PackageUtils.assertGooglePackagePermission(this, GooglePackagePermission.APP_CERT)
    }
}

internal fun Context.assertPhenotypeNamespaceAccess(
    database: PhenotypeDatabase,
    packageName: String?,
    callingPackage: String?
) {
    if (Binder.getCallingUid() == Process.myUid()) return
    if (PackageUtils.callerHasGooglePackagePermission(this, GooglePackagePermission.APP_CERT)) return
    val namespace = packageName?.substringBefore('#')
    val owner = packageName?.substringAfter('#', "")
    if (namespace != null && database.allowsThirdPartyNamespace(namespace) && owner == callingPackage) return
    throw SecurityException("Caller is not authorized for this configuration namespace")
}

internal fun Context.validatePhenotypeUser(user: String?, allowWildcard: Boolean = false): String {
    if (user == null) throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No user")
    if (user.isEmpty() || allowWildcard && user == PHENOTYPE_WILDCARD_USER) return user
    val accountTypes = if (packageManager.hasSystemFeature("cn.google")) setOf("cn.google")
        else setOf("com.google", "com.google.work")
    if (AccountManager.get(this).accounts.none { it.name == user && it.type in accountTypes }) {
        throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "User not on device")
    }
    return user
}

private fun Context.validatePhenotypeOverrideFlags(user: String, flags: List<Flag>) {
    if (flags.any { it.flagType == -1000 }) {
        if (user != PHENOTYPE_WILDCARD_USER) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Flag commit requires user *")
        }
        if (android.os.Build.VERSION.SDK_INT >= 24 && !getSystemService(UserManager::class.java).isUserUnlocked) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Cannot override flag during direct boot")
        }
    }
}

internal fun Flag.deepCopy(): Flag {
    val parcel = Parcel.obtain()
    return try {
        writeToParcel(parcel, 0)
        parcel.setDataPosition(0)
        Flag.CREATOR.createFromParcel(parcel)
    } finally {
        parcel.recycle()
    }
}

internal fun RegistrationInfo.deepCopy(): RegistrationInfo = RegistrationInfo().also { copy ->
    copy.packageName = packageName
    copy.version = version
    copy.field3 = field3?.clone()
    copy.field4 = field4?.clone()
    copy.weak = weak
    copy.field6 = field6?.clone()
    copy.clientPackageName = clientPackageName
    copy.clientCertHash = clientCertHash?.clone()
    copy.registrationSource = registrationSource
    copy.field10 = field10
}

internal fun phenotypeSnapshotTarget(snapshotToken: String?): String =
    if (snapshotToken?.startsWith(PHENOTYPE_CURRENT_TOKEN_PREFIX) == true) {
        snapshotToken.split(':', limit = 3).takeIf { it.size == 3 }?.get(2).orEmpty()
    } else {
        SnapshotTokenInfo.parse(snapshotToken, true)?.target.orEmpty()
    }

internal fun Context.commitPhenotypeConfiguration(
    database: PhenotypeDatabase,
    callingPackage: String?,
    snapshotToken: String?,
    propertiesPackage: String?
): Status {
    if (propertiesPackage != null && snapshotToken == "") {
        database.publishCommittedConfiguration(this, propertiesPackage)
        return Status.SUCCESS
    }
    if (snapshotToken.isNullOrEmpty()) {
        throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "Invalid snapshot token")
    }
    val user = if (snapshotToken.startsWith(PHENOTYPE_CURRENT_TOKEN_PREFIX)) {
        snapshotToken.split(':', limit = 3).takeIf { it.size == 3 }?.get(1)
    } else {
        SnapshotTokenInfo.parse(snapshotToken, true)?.user
    }
    validatePhenotypeUser(user)
    val resolvedToken = if (snapshotToken.startsWith(PHENOTYPE_CURRENT_TOKEN_PREFIX)) {
        val parts = snapshotToken.split(':', limit = 3)
        "$PHENOTYPE_CURRENT_TOKEN_PREFIX${parts[1]}:${database.resolvePackageName(parts[2], callingPackage.orEmpty())}"
    } else snapshotToken
    val packageName = database.commitConfiguration(resolvedToken)
    database.publishCommittedConfiguration(this, packageName)
    return Status.SUCCESS
}

internal fun Context.setPhenotypeFlagOverrides(
    database: PhenotypeDatabase,
    callingPackage: String?,
    packageName: String?,
    user: String?,
    flags: List<Flag?>?
): Status {
    try {
        if (packageName.isNullOrEmpty() || flags == null) {
            throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No package or flags")
        }
        val resolved = database.resolvePackageName(packageName, callingPackage.orEmpty())
        val account = validatePhenotypeUser(user, allowWildcard = true)
        val values = flags.map { requireNotNull(it) { "Null flag" } }
        validatePhenotypeOverrideFlags(account, values)
        val committed = database.setFlagOverrides(resolved, account, values)
        database.publishSharedConfigurationsForKnownUsers(this, listOf(resolved), additionalUsers = listOf(account))
        if (committed) notifyPhenotypeChange(resolved)
        PhenotypeListeners.notifyChanges(this, setOf(resolved))
    } catch (e: IllegalArgumentException) {
        throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, e.message ?: "Invalid flag value", e)
    }
    return Status.SUCCESS
}

internal fun Context.getPhenotypeStorageInfo(
    database: PhenotypeDatabase,
    callingPackage: String?
): ByteArray {
    if (callingPackage.isNullOrEmpty()) {
        throw PhenotypeException(PhenotypeStatusCode.INVALID_ARGUMENT, "No calling package")
    }
    if (callingPackage == "com.android.vending") {
        throw PhenotypeException(
            PhenotypeStatusCode.STORAGE_INFO_UNSUPPORTED,
            "Storage info not created for Play Store."
        )
    }
    try {
        val storageInfo = database.getOrCreateStorageInfo(this, callingPackage)
        return storageInfo.toByteArray(this, PhenotypeResourceReader(this).computeBaselineCl(callingPackage))
    } catch (e: Exception) {
        throw PhenotypeException(PhenotypeStatusCode.STORAGE_INFO_FAILURE, "Failed to create storage info", e)
    }
}

internal fun <T> LifecycleOwner.runPhenotypeOperation(
    context: Context,
    callback: (Status, T?) -> Unit,
    operation: suspend () -> T
) {
    lifecycleScope.launch(Dispatchers.IO) {
        var result: T? = null
        val status = try {
            if (android.os.Build.VERSION.SDK_INT >= 24 &&
                !context.getSystemService(UserManager::class.java).isUserUnlocked) {
                throw PhenotypeException(PhenotypeStatusCode.DIRECT_BOOT_UNSUPPORTED,
                    "API access is not supported in direct boot mode")
            }
            currentCoroutineContext().ensureActive()
            result = operation()
            Status.SUCCESS
        } catch (e: CancellationException) {
            throw e
        } catch (e: PhenotypeException) {
            Status(e.statusCode, e.message)
        } catch (e: Exception) {
            Log.w(PHENOTYPE_LOG_TAG, "Configuration operation failed (${e.javaClass.simpleName})")
            Status.INTERNAL_ERROR
        }
        currentCoroutineContext().ensureActive()
        try {
            try {
                callback(status, result)
            } catch (e: TransactionTooLargeException) {
                if (result !is Configurations) throw e
                callback(Status(PhenotypeStatusCode.SNAPSHOT_TOO_LARGE,
                    "Configuration snapshot too large"), null)
            }
        } catch (_: RemoteException) {
            Log.w(PHENOTYPE_LOG_TAG, "Configuration callback is unavailable")
        }
    }
}

internal fun Context.notifyPhenotypeChange(packageName: String?) {
    try {
        val uri = if (packageName == null) {
            Uri.parse("content://$PHENOTYPE_CONTENT_AUTHORITY")
        } else {
            Uri.parse("content://$PHENOTYPE_CONTENT_AUTHORITY/${Uri.encode(packageName)}")
        }
        contentResolver.notifyChange(uri, null)
    } catch (e: Exception) {
        Log.w(PHENOTYPE_LOG_TAG, "Failed to notify configuration change", e)
    }
}
