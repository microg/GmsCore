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
    private fun getRecaptchaImpl(packageName: String): ArrayList<RecaptchaImpl> {
        val list = ArrayList<RecaptchaImpl>()
        if (SafetyNetPreferences.isEnabled(this) && SDK_INT >= 19) {
            list.add(RecaptchaWebImpl(this, packageName, lifecycle))
        }
        if (DroidGuardPreferences.isAvailable(this)) {
            list.add(RecaptchaGuardImpl(this, packageName))
        }
        if (list.isEmpty()) {
            list.add(RecaptchaImpl.Unsupported)
        }
        return list
    }

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)!!
        val imps = getRecaptchaImpl(packageName)
        callback.onPostInitCompleteWithConnectionInfo(
            CommonStatusCodes.SUCCESS,
            RecaptchaServiceImpl(this, packageName, lifecycle, imps),
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
    private val imps: List<RecaptchaImpl>
) : IRecaptchaService.Stub(), LifecycleOwner {

    private var realRecaptchaImpl: RecaptchaImpl? = null

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
                if (realRecaptchaImpl == null) {
                    throw UnsupportedOperationException("Method <close> realRecaptchaImpl is null")
                }
                Log.d(TAG, "close realRecaptchaImpl:${realRecaptchaImpl}")
                callback.onClosed(Status.SUCCESS, realRecaptchaImpl!!.close(handle))
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun init2(callback: IInitCallback, params: InitParams) {
        lifecycleScope.launch {
            Log.d(TAG, "init($params)")
            try {
                Log.d(TAG, "imps size: ${imps.size}")
                for (recaptchaImpl in imps) {
                    Log.d(TAG, "recaptchaImpl:${recaptchaImpl}")
                    val recaptchaHandle = runCatching { recaptchaImpl.init(params) }.getOrNull() ?: continue
                    realRecaptchaImpl = recaptchaImpl
                    if (params.version == LEGACY_VERSION) {
                        callback.onHandle(Status.SUCCESS, recaptchaHandle)
                    } else {
                        callback.onResults(Status.SUCCESS, InitResults().also { it.handle = recaptchaHandle })
                    }
                    Log.d(TAG, "realRecaptchaImpl:${realRecaptchaImpl}")
                    return@launch
                }
                if (realRecaptchaImpl == null) {
                    throw UnsupportedOperationException("Method <init2> realRecaptchaImpl is null")
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
                if (realRecaptchaImpl == null) {
                    throw UnsupportedOperationException("Method <execute2> realRecaptchaImpl is null")
                }
                Log.d(TAG, "execute2 realRecaptchaImpl:${realRecaptchaImpl}")
                val data = realRecaptchaImpl!!.execute(params)
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
