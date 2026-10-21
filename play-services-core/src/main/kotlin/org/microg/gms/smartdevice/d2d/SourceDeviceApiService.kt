/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.smartdevice.d2d

import android.accounts.Account
import android.os.Handler
import android.os.HandlerThread
import android.os.Process
import android.util.Log
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import cryptauthv2.ClientMetadata
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.microg.gms.BaseService
import org.microg.gms.cryptauth.RegisterForKeyPairOperation
import org.microg.gms.common.GmsService
import org.microg.gms.common.GmsService.SMARTDEVICE_D2D_SOURCE_DEVICE
import org.microg.gms.common.PackageUtils
import org.microg.gms.fido.sourcedevice.FidoSourceKeyStore
import org.microg.gms.fido.sourcedevice.SourceFidoDirectTransferOperation
import org.microg.gms.utils.ExtendedPackageInfo

private const val TAG = "SourceDeviceApiService"
private const val FORCE_REGISTRATION_CLIENT_NAME = "ForceRegistration"
private val CLASSIC_DIRECT_TRANSFER_KEY_NAMES = listOf("PublicKey", "authzen")

class SourceDeviceApiService : BaseService(TAG, SMARTDEVICE_D2D_SOURCE_DEVICE) {
    private var serviceImpl: SourceDeviceServiceImpl? = null
    private lateinit var sourceHandler: Handler

    override fun onCreate() {
        super.onCreate()

        val sourceHandlerThread = HandlerThread(
            "SourceDeviceBackground",
            Process.THREAD_PRIORITY_BACKGROUND,
        ).apply { start() }
        sourceHandler = Handler(sourceHandlerThread.looper)
    }

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val callerPackage = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        val hasDefaultFeatures = !request.defaultFeatures.isNullOrEmpty()
        val isGoogleOrPlatformPackage = ExtendedPackageInfo(this, callerPackage).isGoogleOrPlatformPackage
        if (hasDefaultFeatures || !isGoogleOrPlatformPackage) {
            Log.w(TAG, "Rejected unsupported source-device service request")
            callback.onPostInitComplete(CommonStatusCodes.INTERNAL_ERROR, null, null)
            return
        }
        val activeService = serviceImpl ?: SourceDeviceServiceImpl(
            context = this,
            sourceHandler = sourceHandler,
            registerDirectTransferKeys = ::registerDirectTransferKeys,
            startSourceFidoTransfer = { options, input, output, statusCallback ->
                SourceFidoDirectTransferOperation(this, options, input, output).start(statusCallback)
            },
        ).also { serviceImpl = it }
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, activeService, null)
    }

    private fun registerDirectTransferKeys(account: Account, fidoFlow: Boolean, callback: (Boolean) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val success = try {
                RegisterForKeyPairOperation(
                    context = applicationContext,
                    account = account,
                    invocationReason = ClientMetadata.InvocationReason.FAST_PERIODIC,
                    keyNames = if (fidoFlow) {
                        listOf(FidoSourceKeyStore.SOFTWARE_KEY_NAME)
                    } else {
                        CLASSIC_DIRECT_TRANSFER_KEY_NAMES
                    },
                    clientName = FORCE_REGISTRATION_CLIENT_NAME,
                ).execute()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "DirectTransfer CryptAuth registration failed", e)
                false
            }
            callback(success)
        }
    }

    override fun onDestroy() {
        serviceImpl?.destroy()
        sourceHandler.looper.quitSafely()
        super.onDestroy()
    }
}
