/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ErrorCode.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import org.microg.gms.common.GmsService
import org.microg.gms.fido.api.FidoConstants.*
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.Transport.*
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.bluetooth.BluetoothTransportHandler
import org.microg.gms.fido.core.transport.nfc.NfcTransportHandler
import org.microg.gms.fido.core.transport.screenlock.ScreenLockTransportHandler
import org.microg.gms.fido.core.transport.usb.UsbTransportHandler
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.utils.getFirstSignatureDigest
import org.microg.gms.utils.toBase64

const val TAG = "FidoUi"

class AuthenticatorActivity : AppCompatActivity(), TransportHandlerCallback {
    val options: RequestOptions?
        get() = when (intent.getStringExtra(KEY_SOURCE) to intent.getStringExtra(KEY_TYPE)) {
            SOURCE_BROWSER to TYPE_REGISTER ->
                BrowserPublicKeyCredentialCreationOptions.deserializeFromBytes(intent.getByteArrayExtra(KEY_OPTIONS))
            SOURCE_BROWSER to TYPE_SIGN ->
                BrowserPublicKeyCredentialRequestOptions.deserializeFromBytes(intent.getByteArrayExtra(KEY_OPTIONS))
            SOURCE_APP to TYPE_REGISTER ->
                PublicKeyCredentialCreationOptions.deserializeFromBytes(intent.getByteArrayExtra(KEY_OPTIONS))
            SOURCE_APP to TYPE_SIGN ->
                PublicKeyCredentialRequestOptions.deserializeFromBytes(intent.getByteArrayExtra(KEY_OPTIONS))
            else -> null
        }

    private val service: GmsService
        get() = GmsService.byServiceId(intent.getIntExtra(KEY_SERVICE, GmsService.UNKNOWN.SERVICE_ID))
    private val database by lazy { Database(this) }
    private val transportHandlers by lazy {
        setOfNotNull(
            BluetoothTransportHandler(this, this),
            NfcTransportHandler(this, this),
            if (Build.VERSION.SDK_INT >= 21) UsbTransportHandler(this, this) else null,
            if (Build.VERSION.SDK_INT >= 23) ScreenLockTransportHandler(this, this) else null
        )
    }

    lateinit var callerPackage: String
    lateinit var callerSignature: String
    private lateinit var navHostFragment: NavHostFragment

    private inline fun <reified T : TransportHandler> getTransportHandler(): T? =
        transportHandlers.filterIsInstance<T>().firstOrNull { it.isSupported }

    fun getTransportHandler(transport: Transport): TransportHandler? =
        transportHandlers.firstOrNull { it.transport == transport && it.isSupported }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {

            val callerPackage = callingActivity?.packageName ?: return finish()
            if (!intent.extras?.keySet().orEmpty().containsAll(REQUIRED_EXTRAS)) {
                return finishWithError(UNKNOWN_ERR, "Extra missing from request")
            }
            if (Build.VERSION.SDK_INT < 24) {
                return finishWithError(NOT_SUPPORTED_ERR, "FIDO2 API is not supported on devices below N")
            }
            val options = options ?: return finishWithError(DATA_ERR, "The request options are not valid")
            this.callerPackage = callerPackage
            this.callerSignature = packageManager.getFirstSignatureDigest(callerPackage, "SHA-256")?.toBase64()
                ?: return finishWithError(UNKNOWN_ERR, "Could not determine signature of app")

            Log.d(TAG, "onCreate caller=$callerPackage options=$options")

            val requiresPrivilege =
                options is BrowserRequestOptions && !database.isPrivileged(callerPackage, callerSignature)

            // Check if we can directly open screen lock handling
            if (!requiresPrivilege) {
                val instantTransport = transportHandlers.firstOrNull { it.isSupported && it.shouldBeUsedInstantly(options) }
                if (instantTransport != null && instantTransport.transport in INSTANT_SUPPORTED_TRANSPORTS) {
                    window.setBackgroundDrawable(ColorDrawable(0))
                    window.statusBarColor = Color.TRANSPARENT
                    setTheme(R.style.Theme_Fido_Translucent)
                }
            }

            setTheme(R.style.Theme_AppCompat_DayNight_NoActionBar)
            setContentView(R.layout.fido_authenticator_activity)

            lifecycleScope.launchWhenCreated {
                handleRequest(options)
            }
        } catch (e: RequestHandlingException) {
            finishWithError(e.errorCode, e.message ?: e.errorCode.name)
        } catch (e: Exception) {
            Log.w(TAG, e)
            finishWithError(UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
        }
    }

    @RequiresApi(24)
    suspend fun handleRequest(options: RequestOptions) {
        try {
            val facetId = getFacetId(this, options, callerPackage)
            options.checkIsValid(this, facetId, callerPackage)
            val appName = getApplicationName(this, options, callerPackage)
            val callerName = packageManager.getApplicationLabel(callerPackage).toString()

            val requiresPrivilege =
                options is BrowserRequestOptions && !database.isPrivileged(callerPackage, callerSignature)

            Log.d(TAG, "facetId=$facetId, appName=$appName")

            // Check if we can directly open screen lock handling
            if (!requiresPrivilege) {
                val instantTransport = transportHandlers.firstOrNull { it.isSupported && it.shouldBeUsedInstantly(options) }
                if (instantTransport != null && instantTransport.transport in INSTANT_SUPPORTED_TRANSPORTS) {
                    startTransportHandling(instantTransport.transport)
                    return
                }
            }

            val arguments = AuthenticatorActivityFragmentData().apply {
                this.appName = appName
                this.isFirst = true
                this.privilegedCallerName = callerName.takeIf { options is BrowserRequestOptions }
                this.requiresPrivilege = requiresPrivilege
                this.supportedTransports = transportHandlers.filter { it.isSupported }.map { it.transport }.toSet()
            }.arguments
            val next = if (!requiresPrivilege) {
                val knownRegistrationTransports = mutableSetOf<Transport>()
                val allowedTransports = mutableSetOf<Transport>()
                if (options.type == RequestOptionsType.SIGN) {
                    for (descriptor in options.signOptions.allowList) {
                        val knownTransport = database.getKnownRegistrationTransport(options.rpId, descriptor.id.toBase64(Base64.URL_SAFE, Base64.NO_WRAP, Base64.NO_PADDING))
                        if (knownTransport != null && knownTransport in IMPLEMENTED_TRANSPORTS)
                            knownRegistrationTransports.add(knownTransport)
                        if (descriptor.transports.isNullOrEmpty()) {
                            allowedTransports.addAll(Transport.values())
                        } else {
                            for (transport in descriptor.transports) {
                                val allowedTransport = when (transport) {
                                    com.google.android.gms.fido.common.Transport.BLUETOOTH_CLASSIC -> BLUETOOTH
                                    com.google.android.gms.fido.common.Transport.BLUETOOTH_LOW_ENERGY -> BLUETOOTH
                                    com.google.android.gms.fido.common.Transport.NFC -> NFC
                                    com.google.android.gms.fido.common.Transport.USB -> USB
                                    com.google.android.gms.fido.common.Transport.INTERNAL -> SCREEN_LOCK
                                    else -> null
                                }
                                if (allowedTransport != null && allowedTransport in IMPLEMENTED_TRANSPORTS)
                                    allowedTransports.add(allowedTransport)
                            }
                        }
                    }
                }
                val preselectedTransport = knownRegistrationTransports.singleOrNull() ?: allowedTransports.singleOrNull()
                if (database.wasUsed()) {
                    when (preselectedTransport) {
                        USB -> R.id.usbFragment
                        NFC -> R.id.nfcFragment
                        else -> R.id.transportSelectionFragment
                    }
                } else {
                    null
                }
            } else {
                null
            }
            navHostFragment = NavHostFragment()
            supportFragmentManager.commit {
                replace(R.id.fragment_container, navHostFragment)
                runOnCommit {
                    val navGraph = navHostFragment.navController.navInflater.inflate(R.navigation.nav_fido_authenticator)
                    if (next != null) {
                        navGraph.setStartDestination(next)
                    }
                    navHostFragment.navController.setGraph(navGraph, arguments)
                }
            }
        } catch (e: RequestHandlingException) {
            finishWithError(e.errorCode, e.message ?: e.errorCode.name)
        } catch (e: Exception) {
            Log.w(TAG, e)
            finishWithError(UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
        }
    }

    fun finishWithError(errorCode: ErrorCode, errorMessage: String) {
        Log.d(TAG, "Finish with error: $errorMessage ($errorCode)")
        finishWithCredential(
            PublicKeyCredential.Builder().setResponse(AuthenticatorErrorResponse(errorCode, errorMessage)).build()
        )
    }

    fun finishWithSuccessResponse(response: AuthenticatorResponse, transport: Transport) {
        Log.d(TAG, "Finish with success response: $response")
        if (options is BrowserRequestOptions) database.insertPrivileged(callerPackage, callerSignature)
        val rpId = options?.rpId
        val rawId = when(response) {
            is AuthenticatorAttestationResponse -> response.keyHandle
            is AuthenticatorAssertionResponse -> response.keyHandle
            else -> null
        }
        val id = rawId?.toBase64(Base64.URL_SAFE, Base64.NO_WRAP, Base64.NO_PADDING)
        if (rpId != null && id != null) database.insertKnownRegistration(rpId, id, transport)
        finishWithCredential(PublicKeyCredential.Builder().setResponse(response).setRawId(rawId).setId(id).build())
    }

    private fun finishWithCredential(publicKeyCredential: PublicKeyCredential) {
        val intent = Intent()
        intent.putExtra(FIDO2_KEY_CREDENTIAL_EXTRA, publicKeyCredential.serializeToBytes())
        val response: AuthenticatorResponse = publicKeyCredential.response
        if (response is AuthenticatorErrorResponse) {
            intent.putExtra(FIDO2_KEY_ERROR_EXTRA, response.serializeToBytes())
        } else {
            intent.putExtra(FIDO2_KEY_RESPONSE_EXTRA, response.serializeToBytes())
        }
        setResult(-1, intent)
        finish()
    }

    fun shouldStartTransportInstantly(transport: Transport): Boolean {
        return getTransportHandler(transport)?.shouldBeUsedInstantly(options ?: return false) == true
    }

    fun isScreenLockSigner(): Boolean {
        return shouldStartTransportInstantly(SCREEN_LOCK)
    }

    fun startTransportHandling(transport: Transport): Job = lifecycleScope.launchWhenResumed {
        val options = options ?: return@launchWhenResumed
        try {
            finishWithSuccessResponse(getTransportHandler(transport)!!.start(options, callerPackage), transport)
        } catch (e: CancellationException) {
            Log.w(TAG, e)
            // Ignoring cancellation here
        } catch (e: RequestHandlingException) {
            Log.w(TAG, e)
            finishWithError(e.errorCode, e.message ?: e.errorCode.name)
        } catch (e: Exception) {
            Log.w(TAG, e)
            finishWithError(UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
        }
    }

    override fun onStatusChanged(transport: Transport, status: String, extras: Bundle?) {
        Log.d(TAG, "$transport status set to $status ($extras)")
        try {
            for (callback in navHostFragment.childFragmentManager.fragments.filterIsInstance<TransportHandlerCallback>()) {
                try {
                    callback.onStatusChanged(transport, status, extras)
                } catch (e: Exception) {
                    // Ignoring
                }
            }
        } catch (e: Exception) {
            // Ignoring
        }
    }

    override fun onBackPressed() {
        try {
            if (navHostFragment.navController.popBackStack()) return
        } catch (e: Exception) {
            // Ignore
        }
        super.onBackPressed()
    }

    override fun onSupportNavigateUp(): Boolean {
        try {
            if (navHostFragment.navController.navigateUp()) return true
        } catch (e: Exception) {
            // Ignore
        }
        return super.onSupportNavigateUp()
    }

    companion object {
        const val KEY_SERVICE = "service"
        const val KEY_SOURCE = "source"
        const val KEY_TYPE = "type"
        const val KEY_OPTIONS = "options"
        val REQUIRED_EXTRAS = setOf(KEY_SERVICE, KEY_SOURCE, KEY_TYPE, KEY_OPTIONS)

        const val SOURCE_BROWSER = "browser"
        const val SOURCE_APP = "app"

        const val TYPE_REGISTER = "register"
        const val TYPE_SIGN = "sign"

        val IMPLEMENTED_TRANSPORTS = setOf(USB, SCREEN_LOCK, NFC)
        val INSTANT_SUPPORTED_TRANSPORTS = setOf(SCREEN_LOCK)
    }
}


