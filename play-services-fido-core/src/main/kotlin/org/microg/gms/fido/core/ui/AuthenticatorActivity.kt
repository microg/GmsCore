/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui

import android.annotation.TargetApi
import android.app.KeyguardManager
import android.bluetooth.BluetoothManager
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.getSystemService
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.fido.fido2.api.common.*
import com.google.android.gms.fido.fido2.api.common.ErrorCode.*
import org.microg.gms.common.GmsService
import org.microg.gms.fido.api.FidoConstants.*
import org.microg.gms.fido.core.*
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_APP_NAME
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_FACET_ID
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_IS_FIRST
import org.microg.gms.fido.core.ui.AuthenticatorActivityFragmentData.Companion.KEY_SUPPORTED_TRANSPORTS
import org.microg.gms.fido.core.ui.Transport.*

const val TAG = "FidoUi"

class AuthenticatorActivity : AppCompatActivity() {
    private val service: GmsService
        get() = GmsService.byServiceId(intent.getIntExtra(KEY_SERVICE, GmsService.UNKNOWN.SERVICE_ID))
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

    private lateinit var navHostFragment: NavHostFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {

            val callingPackage = callingActivity?.packageName
            if (callingPackage == null) {
                return finish()
            } else if (!intent.extras?.keySet().orEmpty().containsAll(REQUIRED_EXTRAS)) {
                return finish(UNKNOWN_ERR, "Extra missing from request")
            } else if (Build.VERSION.SDK_INT < 24) {
                return finish(NOT_SUPPORTED_ERR, "FIDO2 API is not supported on devices below N")
            }
            val options = options ?: return finish(DATA_ERR, "The request options are not valid")

            Log.d(TAG, "onCreate caller=$callingPackage options=$options")

            options.checkIsValid(this, callingPackage)
            val facetId = getFacetId(this, options, callingPackage)
            val appName = getApplicationName(this, options, callingPackage)

            Log.d(TAG, "facetId=$facetId, appName=$appName")

            if (options.type == RequestOptionsType.SIGN) {
                val store = InternalCredentialStore(this)
                for (descriptor in options.signOptions.allowList) {
                    try {
                        val (type, data) = CredentialId.decodeTypeAndData(descriptor.id)
                        if (type == 1.toByte() && store.containsKey(options.rpId, data)) {
                            startScreenLockHandling()
                            window.setBackgroundDrawable(ColorDrawable(0))
                            window.statusBarColor = Color.TRANSPARENT
                            setTheme(R.style.Theme_Fido_Translucent)
                            return
                        }
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            }

            setTheme(R.style.Theme_AppCompat_DayNight_NoActionBar)
            setContentView(R.layout.fido_authenticator_activity)
            val arguments = Bundle().apply {
                putString(KEY_APP_NAME, appName)
                putString(KEY_FACET_ID, facetId)
                putBoolean(KEY_IS_FIRST, true)
                putStringArrayList(KEY_SUPPORTED_TRANSPORTS,
                    ArrayList(listOfNotNull(
                        BLUETOOTH.takeIf { getSystemService<BluetoothManager>()?.adapter != null },
                        NFC.takeIf { NfcAdapter.getDefaultAdapter(this@AuthenticatorActivity) != null },
                        USB.takeIf { packageManager.hasSystemFeature("android.hardware.usb.host") },
                        SCREEN_LOCK.takeIf { Build.VERSION.SDK_INT >= 23 && getSystemService<KeyguardManager>()?.isDeviceSecure == true }
                    ).filter { it in IMPLEMENTED_TRANSPORTS }.map { it.toString() })
                )
            }
            navHostFragment = NavHostFragment.create(R.navigation.nav_fido_authenticator, arguments)
            // TODO: Go directly to appropriate fragment for known key
            // TODO: If not first usage, skip welcome and go directly to transport selection
            //navHostFragment.findNavController().navigate(next, arguments)
            supportFragmentManager.commit {
                replace(R.id.fragment_container, navHostFragment)
            }
        } catch (e: RequestHandlingException) {
            finish(e.errorCode, e.message ?: e.errorCode.name)
        } catch (e: Exception) {
            finish(UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
        }
    }

    fun finish(errorCode: ErrorCode, errorMessage: String) {
        Log.d(TAG, "Finish with error: $errorMessage ($errorCode)")
        finish(AuthenticatorErrorResponse(errorCode, errorMessage))
    }

    fun finish(response: AuthenticatorResponse) {
        Log.d(TAG, "Finish with response: $response")
        finish(PublicKeyCredential.Builder().setResponse(response).build())
    }

    fun finish(publicKeyCredential: PublicKeyCredential) {
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

    @TargetApi(23)
    fun startScreenLockHandling() {
        lifecycleScope.launchWhenStarted {
            val options = options ?: return@launchWhenStarted
            val callingPackage = callingPackage ?: return@launchWhenStarted
            try {
                val result = when (options.type) {
                    RequestOptionsType.REGISTER -> registerInternal(this@AuthenticatorActivity, options, callingPackage)
                    RequestOptionsType.SIGN -> signInternal(this@AuthenticatorActivity, options, callingPackage)
                }
                finish(result)
            } catch (e: RequestHandlingException) {
                Log.w(TAG, e)
                finish(e.errorCode, e.message ?: e.errorCode.name)
            } catch (e: Exception) {
                Log.w(TAG, e)
                finish(ErrorCode.UNKNOWN_ERR, e.message ?: e.javaClass.simpleName)
            }
        }
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

        val IMPLEMENTED_TRANSPORTS = setOf(USB, SCREEN_LOCK)
    }
}


