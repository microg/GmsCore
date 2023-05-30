/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.recaptcha

import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Parcel
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.recaptcha.RecaptchaAction
import com.google.android.gms.recaptcha.RecaptchaHandle
import com.google.android.gms.recaptcha.internal.*
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.droidguard.core.DroidGuardPreferences
import org.microg.gms.safetynet.SafetyNetPreferences
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "RecaptchaService"

class RecaptchaService : BaseService(TAG, GmsService.RECAPTCHA) {
    private fun getRecaptchaImpl(packageName: String) = when {
        SafetyNetPreferences.isEnabled(this) && SDK_INT >= 19 -> RecaptchaWebImpl(this, packageName, lifecycle)
        DroidGuardPreferences.isEnabled(this) -> RecaptchaGuardImpl(this, packageName)
        else -> RecaptchaImpl.Unsupported
    }

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)!!
        val impl = getRecaptchaImpl(packageName)
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RecaptchaServiceImpl(this, packageName, lifecycle, impl),
            ConnectionInfo().apply {
                features = arrayOf(
                    Feature("verify_with_recaptcha_v2_internal", 1),
                    Feature("init", 3),
                    Feature("execute", 5),
                    Feature("close", 2)
                )
            }
        )
    }
}

class RecaptchaServiceImpl(
    private val context: Context,
    private val packageName: String,
    override val lifecycle: Lifecycle,
    private val impl: RecaptchaImpl
) : IRecaptchaService.Stub(), LifecycleOwner {

    override fun verifyWithRecaptcha(callback: IExecuteCallback, siteKey: String, packageName: String) {
        Log.d(TAG, "Not yet implemented: verifyWithRecaptcha($siteKey, $packageName)")
    }

    override fun init(callback: IInitCallback, siteKey: String) {
        init2(callback, InitParams().also {
            it.siteKey = siteKey
            it.version = LEGACY_VERSION
        })
    }

    override fun execute(callback: IExecuteCallback, handle: RecaptchaHandle, action: RecaptchaAction) {
        execute2(callback, ExecuteParams().also {
            it.handle = handle
            it.action = action
            it.version = LEGACY_VERSION
        })
    }

    override fun close(callback: ICloseCallback, handle: RecaptchaHandle) {
        lifecycleScope.launch {
            Log.d(TAG, "close($handle)")
            try {
                callback.onClosed(Status.SUCCESS, impl.close(handle))
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun init2(callback: IInitCallback, params: InitParams) {
        lifecycleScope.launch {
            Log.d(TAG, "init($params)")
            try {
                val handle = impl.init(params)
                if (params.version == LEGACY_VERSION) {
                    callback.onHandle(Status.SUCCESS, handle)
                } else {
                    callback.onResults(Status.SUCCESS, InitResults().also { it.handle = handle })
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                try {
                    if (params.version == LEGACY_VERSION) {
                        callback.onHandle(Status.INTERNAL_ERROR, null)
                    } else {
                        callback.onResults(Status.INTERNAL_ERROR, InitResults())
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }
    }

    override fun execute2(callback: IExecuteCallback, params: ExecuteParams) {
        Log.d(TAG, "execute($params)")
        lifecycleScope.launch {
            try {
                val data = impl.execute(params)
                if (params.version == LEGACY_VERSION) {
                    callback.onData(Status.SUCCESS, data)
                } else {
                    callback.onResults(Status.SUCCESS, ExecuteResults().also { it.data = data })
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                try {
                    if (params.version == LEGACY_VERSION) {
                        callback.onData(Status.INTERNAL_ERROR, null)
                    } else {
                        callback.onResults(Status.INTERNAL_ERROR, ExecuteResults())
                    }
                } catch (e: Exception) {
                    // Ignored
                }
            }
        }

    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) {
            super.onTransact(code, data, reply, flags)
        }

    companion object {
        const val LEGACY_VERSION = "16.0.0"
    }
}
