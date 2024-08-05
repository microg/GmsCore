/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.credential

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Parcel
import android.util.Base64
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.IStatusCallback
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.credential.manager.EXTRA_KEY_ACCOUNT_NAME
import com.google.android.gms.credential.manager.PASSWORD_MANAGER_CLASS_NAME
import com.google.android.gms.credential.manager.common.IPendingIntentCallback
import com.google.android.gms.credential.manager.common.ISettingsCallback
import com.google.android.gms.credential.manager.firstparty.internal.ICredentialManagerService
import com.google.android.gms.credential.manager.invocationparams.CredentialManagerInvocationParams
import org.microg.gms.BaseService
import org.microg.gms.common.Constants
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.toBase64
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "CredentialManager"

class CredentialManagerService : BaseService(TAG, GmsService.CREDENTIAL_MANAGER) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        if (!PackageUtils.callerHasGooglePackagePermission(this, GooglePackagePermission.CREDENTIALS)) {
            Log.d(TAG, "No access to ${request.packageName}, lacks permission.")
            callback.onPostInitComplete(ConnectionResult.API_DISABLED_FOR_CONNECTION, null, null)
            return
        }
        callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.SUCCESS, CredentialManagerServiceImpl(this, lifecycle), ConnectionInfo().apply {
            features = arrayOf(
                Feature("credential_manager_first_party_api", 1),
                Feature("password_checkup_first_party_api", 1),
                Feature("user_service_security", 1),
            )
        })
    }

}

private class CredentialManagerServiceImpl(private val context: Context, override val lifecycle: Lifecycle) : ICredentialManagerService.Stub(), LifecycleOwner {

    override fun getCredentialManagerIntent(callback: IPendingIntentCallback?, params: CredentialManagerInvocationParams) {
        Log.d(TAG, "Method getCredentialManagerIntent $params called")
        lifecycleScope.launchWhenStarted {
            runCatching {
                val intent = Intent().apply {
                    setClassName(Constants.GMS_PACKAGE_NAME, PASSWORD_MANAGER_CLASS_NAME)
                    putExtra(EXTRA_KEY_ACCOUNT_NAME, params.account.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                val pendingIntent = PendingIntentCompat.getActivity(context, 0, intent, 0, false)
                callback?.onPendingIntent(Status.SUCCESS, pendingIntent)
            }.onFailure {
                Log.d(TAG, "getCredentialManagerIntent error", it)
                callback?.onPendingIntent(Status.INTERNAL_ERROR, null)
            }
        }
    }

    override fun getSetting(callback: ISettingsCallback?, key: String?) {
        Log.d(TAG, "Not yet implemented: getSetting $key")
        lifecycleScope.launchWhenStarted {
            try {
                callback?.onSetting(Status.INTERNAL_ERROR, null)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun setSetting(callback: IStatusCallback?, key: String?, value: ByteArray?) {
        Log.d(TAG, "Not yet implemented: setSetting $key ${value?.toBase64(Base64.NO_WRAP)}")
        lifecycleScope.launchWhenStarted {
            try {
                callback?.onResult(Status.INTERNAL_ERROR)
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}