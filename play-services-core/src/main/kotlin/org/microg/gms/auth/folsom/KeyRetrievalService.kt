/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.folsom

import android.app.PendingIntent
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Log
import com.google.android.gms.auth.folsom.RecoveryRequest
import com.google.android.gms.auth.folsom.RecoveryResult
import com.google.android.gms.auth.folsom.SharedKey
import com.google.android.gms.auth.folsom.internal.IBooleanCallback
import com.google.android.gms.auth.folsom.internal.IByteArrayCallback
import com.google.android.gms.auth.folsom.internal.IByteArrayListCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalConsentCallback
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalService
import com.google.android.gms.auth.folsom.internal.IKeyRetrievalSyncStatusCallback
import com.google.android.gms.auth.folsom.internal.IRecoveryResultCallback
import com.google.android.gms.auth.folsom.internal.ISecurityDomainMembersCallback
import com.google.android.gms.auth.folsom.internal.ISharedKeyCallback
import com.google.android.gms.auth.folsom.internal.IStringListCallback
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.auth.folsom.ui.GenericActivity
import org.microg.gms.common.Constants.GMS_PACKAGE_NAME
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "KeyRetrievalService"

private val FEATURES = arrayOf(
    Feature("key_retrieval", 2L),
    Feature("list_recovered_security_domains", 1L),
    Feature("start_recovery", 3L),
    Feature("recoverability_fix", 2L),
    Feature("lskf_consent", 1L),
    Feature("reset_security_domain", 2L),
    Feature("generate_open_vault_request", 1L),
    Feature("silently_add_gaia_password_member", 1L),
)

class KeyRetrievalService : BaseService(TAG, GmsService.FOLSOM) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        Log.d(TAG, "handleServiceRequest: packageName: ${request.packageName}")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, KeyRetrievalServiceImpl(this), ConnectionInfo().apply { features = FEATURES })
    }
}

class KeyRetrievalServiceImpl(val context: Context) : IKeyRetrievalService.Stub() {

    override fun setConsent(
        callback: IKeyRetrievalConsentCallback?, accountName: String?, force: Boolean, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented setConsent accountName:$accountName force:$force metadata:$metadata")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun getConsent(
        callback: IKeyRetrievalConsentCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getConsent accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun getSyncStatus(
        callback: IKeyRetrievalSyncStatusCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getSyncStatus accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun markLocalKeysAsStale(
        callback: IKeyRetrievalCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented markLocalKeysAsStale accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getKeyMaterial(
        callback: ISharedKeyCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getKeyMaterial accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, emptyArray<SharedKey>())
    }

    override fun setKeyMaterial(
        callback: IKeyRetrievalCallback?, accountName: String?, keys: Array<out SharedKey?>?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented setKeyMaterial accountName:$accountName keys:$keys metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun getRecoveredSecurityDomains(
        callback: IStringListCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getRecoveredSecurityDomains accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, emptyArray<String>())
    }

    override fun startRecoveryOperation(
        callback: IRecoveryResultCallback?, metadata: ApiMetadata?, request: RecoveryRequest?
    ) {
        Log.d(TAG, "Not implemented startRecoveryOperation request:$request metadata:$metadata")
        callback?.onResult(Status.SUCCESS, RecoveryResult())
    }

    override fun listVaultsOperation(
        callback: IByteArrayListCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented listVaultsOperation accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, emptyList<ByteArray>())
    }

    override fun getProductDetails(
        callback: IByteArrayCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented getProductDetails accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, byteArrayOf())
    }

    override fun joinSecurityDomain(
        callback: IStatusCallback?, accountName: String?, bytes: ByteArray?, type: Int, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented joinSecurityDomain accountName:$accountName type:$type metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun startUxFlow(
        callback: IKeyRetrievalCallback?, accountName: String?, type: Int, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented startUxFlow accountName:$accountName type:$type metadata:$metadata")
        val intent = Intent().apply { setClassName(GMS_PACKAGE_NAME, GenericActivity::class.java.name) }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, FLAG_UPDATE_CURRENT or FLAG_IMMUTABLE)
        val states = Status(CommonStatusCodes.SUCCESS, "UX flow PendingIntent retrieved.", pendingIntent)
        callback?.onResult(states)
    }

    override fun promptForLskfConsent(
        callback: IKeyRetrievalCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented promptForLskfConsent accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun resetSecurityDomain(
        callback: IStatusCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented resetSecurityDomain accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun listSecurityDomainMembers(
        callback: ISecurityDomainMembersCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented listSecurityDomainMembers accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, emptyList<Int>())
    }

    override fun generateOpenVaultRequestOperation(
        callback: IByteArrayCallback?, request: RecoveryRequest?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented generateOpenVaultRequestOperation request:$request metadata:$metadata")
        callback?.onResult(Status.SUCCESS, byteArrayOf())
    }

    override fun canSilentlyAddGaiaPassword(
        callback: IBooleanCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented canSilentlyAddGaiaPassword accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS, true)
    }

    override fun addGaiaPasswordMember(
        callback: IStatusCallback?, accountName: String?, metadata: ApiMetadata?
    ) {
        Log.d(TAG, "Not implemented addGaiaPasswordMember accountName:$accountName metadata:$metadata")
        callback?.onResult(Status.SUCCESS)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        return warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
    }
}
