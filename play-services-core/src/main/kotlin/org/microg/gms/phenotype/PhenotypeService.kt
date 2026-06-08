/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.phenotype

import android.os.Parcel
import android.util.Log
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.phenotype.*
import com.google.android.gms.phenotype.internal.IGetStorageInfoCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeCallbacks
import com.google.android.gms.phenotype.internal.IPhenotypeService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.internal.ConnectionInfo
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PhenotypeService"

private val FEATURES = arrayOf(
    Feature("commit_to_configuration_v2_api", 1),
    Feature("get_serving_version_api", 1),
    Feature("get_experiment_tokens_api", 1),
    Feature("register_flag_update_listener_api", 2),
    Feature("sync_after_api", 1),
    Feature("sync_after_for_application_api", 1),
    Feature("set_app_wide_properties_api", 1),
    Feature("set_runtime_properties_api", 1),
    Feature("get_storage_info_api", 1),
)

class PhenotypeService : BaseService(TAG, GmsService.PHENOTYPE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request?.packageName)
        callback.onPostInitCompleteWithConnectionInfo(0, PhenotypeServiceImpl(packageName, this).asBinder(), ConnectionInfo().apply {
            features = FEATURES
        })
    }
}

private val RCS_PROVISIONING_FLAGS = arrayOf(
    Flag("RcsProvisioning__min_gmscore_version_for_upi_without_acs_fallback_met", true, 0),
    Flag("RcsProvisioning__allow_manual_phone_number_input", true, 0),
    Flag("RcsFlags__acs_url", "", 0),
    // Carrier-generic Jibe URL template (%s = MCC)
    Flag("RcsFlags__mcc_url_format", "rcs-acs-mcc%s.jibe.google.com", 0),
    Flag("RcsFlags__allow_overrides", true, 0),
    Flag("RcsProvisioning__enable_upi", true, 0),
    Flag("RcsProvisioning__enable_upi_mvp", true, 0),
    Flag("RcsProvisioning__enable_client_attestation_check", false, 0),
    Flag("RcsProvisioning__enable_client_attestation_check_v2", false, 0),
)

private val CONFIGURATION_OPTIONS = mapOf(
    "com.google.android.apps.search.assistant.mobile.user#com.google.android.googlequicksearchbox" to arrayOf(
        // Enable Gemini voice input for all devices
        Flag("45477527", true, 0),
        // Enable Gemini AI chat auto-reply
        Flag("45628155", false, 0),
        Flag("45627469", true, 0),
        Flag("45627893", byteArrayOf(0x0A, 0x01, 0x2A), 0),
        Flag("45622089", true, 0),
        // Enable Gemini optional models
        Flag("45681308", true, 0),
        Flag("45688209", true, 0),
        Flag("45682144", true, 0),
        // Enable Gemini sharing and video features
        Flag("45638955", true, 0),
        Flag("45621205", true, 0),
        Flag("45616812", true, 0)
    ),
    "com.google.android.inputmethod.latin#com.google.android.inputmethod.latin" to arrayOf(
        // Enable Gboard supports voice input in other languages
        Flag("enable_voice_in_chinese", true, 0),
        Flag("enable_voice_in_japanese", true, 0),
        Flag("enable_voice_in_korean", true, 0),
        Flag("enable_voice_in_handwriting", true, 0),
    ),
    "com.google.android.libraries.communications.conference.device#com.google.android.apps.tachyon" to arrayOf(
        // Enable Google Meet calling using mobile phone number
        Flag("45428442", true, 0),
        Flag("45620216", true, 0),
        // Start a Video Call
        Flag("45692241", true, 0),
        Flag("45680977", true, 0),
        Flag("45620220", true, 0),
        // Show Audio Call Button
        Flag("45613814", true, 0),
        // Show Group Call Button
        Flag("45620498", true, 0),
    ),
    "com.google.apps_mobile.common.services.gmail.android#com.google.android.gm" to arrayOf(
        Flag("45661535", encodeSupportedLanguageList(), 0),
        Flag("45700179", encodeSupportedLanguageList(), 0)
    ),
    "gmail_android.user#com.google.android.gm" to arrayOf(
        Flag("45624002", true, 0),
        Flag("45668769", true, 0),
        Flag("45633067", true, 0),
    ),
    "com.google.android.apps.photos" to arrayOf(
        Flag("45617431", true, 0),
    ),
    "com.google.android.apps.messaging" to RCS_PROVISIONING_FLAGS,
    "com.google.android.apps.messaging#com.google.android.apps.messaging" to RCS_PROVISIONING_FLAGS,
    "com.google.android.ims.library" to RCS_PROVISIONING_FLAGS,
    "com.google.android.ims.library#com.google.android.apps.messaging" to RCS_PROVISIONING_FLAGS,
    "com.google.android.ims.library#com.google.android.ims" to RCS_PROVISIONING_FLAGS,
)

private const val IMS_PB_NAME = "com.google.android.ims.library#com.google.android.ims.pb"

class PhenotypeServiceImpl(val packageName: String?, private val context: android.content.Context) : IPhenotypeService.Stub() {

    companion object {
        @Volatile private var provisioningTriggerActive = false
    }

    private fun scheduleProvisioningTrigger() {
        if (provisioningTriggerActive) return
        provisioningTriggerActive = true
        val handler = android.os.Handler(android.os.Looper.getMainLooper())
        val intervalMs = 15_000L
        val maxAttempts = 20
        var attempt = 0
        val runnable = object : Runnable {
            override fun run() {
                attempt++
                try {
                    val pm = context.packageManager
                    pm.getPackageInfo("com.google.android.apps.messaging", 0)
                } catch (e: Exception) {
                    Log.d(TAG, "Messages not installed, stopping provisioning trigger")
                    provisioningTriggerActive = false
                    return
                }
                try {
                    val intent = android.content.Intent("com.google.android.ims.library.phenotype.UPDATE")
                    intent.setPackage("com.google.android.apps.messaging")
                    context.sendBroadcast(intent)
                    if (attempt <= 3) Log.i(TAG, "Sent phenotype UPDATE broadcast ($attempt)")
                } catch (e: Exception) {
                    Log.w(TAG, "Provisioning trigger failed: ${e.message}")
                }
                if (attempt < maxAttempts) handler.postDelayed(this, intervalMs)
                else provisioningTriggerActive = false
            }
        }
        handler.postDelayed(runnable, 5_000L)
    }

    private fun ensureImsPbMarker() {
        val callerPkg = packageName ?: return
        if (!callerPkg.contains("messaging")) return
        try {
            val callerCtx = context.createPackageContext(callerPkg, android.content.Context.CONTEXT_IGNORE_SECURITY)
            val dir = java.io.File(callerCtx.filesDir, "phenotype/shared")
            val marker = java.io.File(dir, IMS_PB_NAME)
            if (marker.exists()) {
                Log.d(TAG, "IMS .pb marker already exists for $callerPkg")
                return
            }
            dir.mkdirs()
            val template = dir.listFiles()?.firstOrNull { it.length() in 20..30 }
            if (template != null) {
                template.copyTo(marker)
            } else {
                marker.writeBytes(byteArrayOf(0x0a, 0x07, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x12, 0x00, 0x1a, 0x07, 0x75, 0x6e, 0x6b, 0x6e, 0x6f, 0x77, 0x6e, 0x20, 0xea.toByte(), 0xa7.toByte(), 0xe0.toByte(), 0xcb.toByte(), 0x06))
            }
            Log.i(TAG, "Created IMS .pb marker for $callerPkg")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to create IMS .pb marker: ${e.message}")
        }
    }

    override fun register(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?) {
        Log.d(TAG, "register($packageName, version=$version, p3=${p3?.contentToString()}, callingUid=${android.os.Binder.getCallingUid()})")
        if (packageName?.contains("ims.library") == true) ensureImsPbMarker()
        callbacks.onRegistered(if (version != 0) Status.SUCCESS else Status.CANCELED)
    }

    override fun weakRegister(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: IntArray?, p5: ByteArray?) {
        Log.d(TAG, "weakRegister($packageName, version=$version, p3=${p3?.contentToString()}, callingUid=${android.os.Binder.getCallingUid()})")
        callbacks.onWeakRegistered(Status.SUCCESS)
    }

    override fun unregister(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "unregister($packageName)")
        callbacks.onUnregistered(Status.SUCCESS)
    }

    override fun getConfigurationSnapshot(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        getConfigurationSnapshotWithToken(callbacks, packageName, user, null)
    }

    override fun commitToConfiguration(callbacks: IPhenotypeCallbacks, snapshotToken: String?) {
        Log.d(TAG, "commitToConfiguration($snapshotToken)")
        callbacks.onCommitedToConfiguration(Status.SUCCESS)
    }

    override fun getExperimentTokens(callbacks: IPhenotypeCallbacks, packageName: String?, logSourceName: String?) {
        getExperimentTokensForLogging(callbacks, packageName, logSourceName, null, this.packageName)
    }

    override fun getDogfoodsToken(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getDogfoodsToken()")
        callbacks.onDogfoodsToken(Status.SUCCESS, DogfoodsToken())
    }

    override fun setDogfoodsToken(callbacks: IPhenotypeCallbacks, p1: ByteArray?) {
        Log.d(TAG, "setDogfoodsToken($p1)")
        callbacks.onDogfoodsTokenSet(Status.SUCCESS)
    }

    override fun getFlag(callbacks: IPhenotypeCallbacks, packageName: String?, name: String?, type: Int) {
        Log.d(TAG, "getFlag($packageName, $name, $type)")
        callbacks.onFlag(Status.SUCCESS, null)
    }

    override fun getCommitedConfiguration(callbacks: IPhenotypeCallbacks, packageName: String?) {
        Log.d(TAG, "getCommitedConfiguration($packageName)")
        if (packageName in CONFIGURATION_OPTIONS.keys) {
            val flags = CONFIGURATION_OPTIONS[packageName]
            callbacks.onCommittedConfiguration(Status.SUCCESS, configurationsResult(arrayOf(Configuration().apply {
                id = 0
                this.flags = flags
                removeNames = emptyArray()
            })))
        } else {
            callbacks.onCommittedConfiguration(Status.SUCCESS, configurationsResult())
        }
    }

    override fun getConfigurationSnapshotWithToken(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?) {
        Log.d(TAG, "getConfigurationSnapshotWithToken($packageName, $user, $p3)")
        if (packageName in CONFIGURATION_OPTIONS.keys) {
            val flags = CONFIGURATION_OPTIONS[packageName]
            Log.d(TAG, "Serving ${flags?.size ?: 0} phenotype flags for $packageName")
            if (flags === RCS_PROVISIONING_FLAGS) scheduleProvisioningTrigger()
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult(arrayOf(Configuration().apply {
                id = 0
                this.flags = flags
                removeNames = emptyArray()
            })))
        } else {
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult())
        }
    }

    override fun syncAfterOperation(callbacks: IPhenotypeCallbacks, packageName: String?, version: Long) {
        Log.d(TAG, "syncAfterOperation($packageName, $version)")
        callbacks.onSyncFinished(Status.SUCCESS, version)
    }

    override fun registerSync(callbacks: IPhenotypeCallbacks, packageName: String?, version: Int, p3: Array<out String>?, p4: ByteArray?, p5: String?, p6: String?) {
        Log.d(TAG, "registerSync($packageName, $version, $p3, $p5, $p6)")
        val key = packageName ?: ""
        val flags = CONFIGURATION_OPTIONS[key]
        if (flags != null) {
            Log.d(TAG, "registerSync: serving ${flags.size} flags for $key")
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult(arrayOf(Configuration().apply {
                id = 0
                this.flags = flags
                removeNames = emptyArray()
            })))
        } else {
            callbacks.onConfiguration(Status.SUCCESS, configurationsResult())
        }
    }

    override fun setFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?, flagType: Int, flagDataType: Int, flagValue: String?) {
        Log.d(TAG, "setFlagOverrides($packageName, $user, $flagName, $flagDataType, $flagType, $flagDataType, $flagValue)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun deleteFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "deleteFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun listFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, flagName: String?) {
        Log.d(TAG, "listFlagOverrides($packageName, $user, $flagName)")
        callbacks.onFlagOverrides(Status.SUCCESS, FlagOverrides())
    }

    override fun clearFlagOverrides(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?) {
        Log.d(TAG, "clearFlagOverrides($packageName, $user)")
        callbacks.onFlagOverridesSet(Status.SUCCESS)
    }

    override fun bulkRegister(callbacks: IPhenotypeCallbacks, infos: Array<out RegistrationInfo>?) {
        Log.d(TAG, "bulkRegister($infos)")
        callbacks.onRegistered(Status.SUCCESS)
    }

    override fun setAppSpecificProperties(callbacks: IPhenotypeCallbacks, packageName: String?, p2: ByteArray?) {
        Log.d(TAG, "setAppSpecificProperties($packageName, $p2)")
        callbacks.onAppSpecificPropertiesSet(Status.SUCCESS)
    }

    override fun getServingVersion(callbacks: IPhenotypeCallbacks) {
        Log.d(TAG, "getServingVersion()")
        callbacks.onServingVersion(Status.SUCCESS, 1)
    }

    override fun getExperimentTokensForLogging(callbacks: IPhenotypeCallbacks, packageName: String?, user: String?, p3: String?, clientPackageName: String?) {
        Log.d(TAG, "getExperimentTokens($packageName, $user, $p3, $clientPackageName)")
        callbacks.onExperimentTokens(Status.SUCCESS, ExperimentTokens().apply {
            field2 = ""
        })
    }

    override fun syncAllAfterOperation(callbacks: IPhenotypeCallbacks?, p1: Long) {
        Log.d(TAG, "Not yet implemented: syncAllAfterOperation")
    }

    override fun setRuntimeProperties(callbacks: IStatusCallback?, p1: String?, p2: ByteArray?) {
        Log.d(TAG, "Not yet implemented: setRuntimeProperties")
    }

    override fun commitToConfigurationV2(callbacks: IPhenotypeCallbacks, data: ByteArray?) {
        Log.d(TAG, "commitToConfigurationV2(${data?.size ?: 0} bytes)")
        ensureImsPbMarker()
        callbacks.onCommitedToConfiguration(Status(29501))
    }

    override fun getStorageInfo(callbacks: IGetStorageInfoCallbacks?) {
        Log.d(TAG, "getStorageInfo(callingPackage=$packageName)")
        // Error 29514 causes clients to create a timestamp-only fallback StorageInfo.
        callbacks?.onStorageInfo(Status(29514), null)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
