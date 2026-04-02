/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui.hybrid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.Fido.FIDO2_KEY_CREDENTIAL_EXTRA
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredential
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import kotlinx.coroutines.suspendCancellableCoroutine
import org.microg.gms.fido.core.hybrid.controller.HybridAuthenticatorController
import org.microg.gms.fido.core.hybrid.model.QrCodeData
import org.microg.gms.fido.core.protocol.AttestationObject
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionResponse
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetInfoRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetInfoResponse
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialResponse
import org.microg.gms.fido.core.ui.AuthenticatorActivity
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume

private const val TAG = "HybridAuthenticate"
private const val REQUEST_BLUETOOTH_PERMISSIONS = 1001

@RequiresApi(23)
class HybridAuthenticateActivity : AppCompatActivity() {
    private var bottomSheet: HybridReceiverBottomSheetFragment? = null
    private var qrCodeData: QrCodeData? = null

    private var hybridAuthenticatorController: HybridAuthenticatorController? = null

    private lateinit var waitingLauncher: ActivityResultLauncher<Intent>
    private var waitingLauncherContinuation: Continuation<ActivityResult>? = null

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val fidoUrl = intent.dataString
        if (fidoUrl == null || !fidoUrl.startsWith(QrCodeData.PREFIX_FIDO, ignoreCase = true)) {
            Log.w(TAG, "Invalid FIDO URL: $fidoUrl")
            finishWithError("Invalid FIDO URL: $fidoUrl")
            return
        }
        Log.d(TAG, "onCreate: $fidoUrl")
        qrCodeData = QrCodeData.parse(fidoUrl)
        if (qrCodeData == null) {
            Log.w(TAG, "Failed to parse QR code data")
            finishWithError("Failed to parse QR code data")
            return
        }
        waitingLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            waitingLauncherContinuation?.resume(it)
        }

        val isRegistration = qrCodeData?.flowIdentifier == "mc"

        var bottomSheetFragment = supportFragmentManager.findFragmentByTag(HybridReceiverBottomSheetFragment.TAG)
        if (bottomSheetFragment == null) {
            bottomSheetFragment = HybridReceiverBottomSheetFragment.newInstance(isRegistration)
            bottomSheetFragment.show(supportFragmentManager, HybridReceiverBottomSheetFragment.TAG)
        }

        (bottomSheetFragment as HybridReceiverBottomSheetFragment).also { bottomSheet = it }.setOnContinueListener {
            Log.d(TAG, "User confirmed - starting connection flow")
            // Update UI to CONNECTING state
            bottomSheetFragment.showConnecting()

            if (!hasBluetoothPermissions()) {
                Log.d(TAG, "Bluetooth permissions not granted, requesting...")
                requestBluetoothPermissions()
                return@setOnContinueListener
            }

            startHybridConnectionFlow()
        }

        bottomSheet?.setOnCancelListener {
            Log.d(TAG, "User canceled")
            finishWithError("User canceled")
        }
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onDestroy() {
        super.onDestroy()
        hybridAuthenticatorController?.release()
        hybridAuthenticatorController = null
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    private fun startHybridConnectionFlow() {
        lifecycleScope.launchWhenStarted {
            hybridAuthenticatorController = hybridAuthenticatorController ?: HybridAuthenticatorController(this@HybridAuthenticateActivity)
            try {
                hybridAuthenticatorController?.startAuth(qrCodeData!!, handleAuthenticator = {
                    when (it) {
                        is AuthenticatorMakeCredentialRequest -> handleMakeCredential(it)
                        is AuthenticatorGetAssertionRequest -> handleGetAssertion(it)
                        is AuthenticatorGetInfoRequest -> handleGetInfo(it)
                        else -> null
                    }
                }, completed = {
                    if (it) finishWithSuccess() else finishWithError("auth error")
                })
            } catch (e: Exception) {
                Log.w(TAG, "startHybridConnectionFlow: ", e)
                bottomSheet?.showError()
            } finally {
                hybridAuthenticatorController?.release()
            }
        }
    }

    private suspend fun handleMakeCredential(request: AuthenticatorMakeCredentialRequest): AuthenticatorMakeCredentialResponse {
        val publicKeyCredentialCreationOptions = PublicKeyCredentialCreationOptions.Builder()
            .setRp(request.rp)
            .setUser(request.user)
            .setParameters(request.pubKeyCredParams)
            .setChallenge(request.clientDataHash)
            .setAttestationConveyancePreference(AttestationConveyancePreference.NONE)
            .setAuthenticatorSelection(
                AuthenticatorSelectionCriteria.Builder()
                    .setRequireResidentKey(request.options?.residentKey ?: false)
                    .setRequireUserVerification(request.options?.userVerification?.takeIf { it }?.let { UserVerificationRequirement.REQUIRED })
                    .build()
            )
            .setExcludeList(request.excludeList)
            .build()

        val browserOptions = BrowserPublicKeyCredentialCreationOptions.Builder()
            .setPublicKeyCredentialCreationOptions(publicKeyCredentialCreationOptions)
            .setOrigin("https://${request.rp.id}".toUri())
            .setClientDataHash(request.clientDataHash).build()

        val intent = Intent(this, AuthenticatorActivity::class.java)
            .putExtra(AuthenticatorActivity.KEY_SOURCE, AuthenticatorActivity.SOURCE_HYBRID)
            .putExtra(AuthenticatorActivity.KEY_TYPE, AuthenticatorActivity.TYPE_REGISTER)
            .putExtra(AuthenticatorActivity.KEY_OPTIONS, browserOptions.serializeToBytes())

        val result = suspendCancellableCoroutine { continuation ->
            waitingLauncherContinuation = continuation
            waitingLauncher.launch(intent)
        }

        val resultBytes = result.data?.getByteArrayExtra(FIDO2_KEY_CREDENTIAL_EXTRA) ?: throw RuntimeException("No result")
        val publicKeyCredential = PublicKeyCredential.deserializeFromBytes(resultBytes)
        val response = publicKeyCredential.response as? AuthenticatorAttestationResponse? ?: throw RuntimeException("Invalid result")
        val attestationObject = AttestationObject.decode(response.attestationObject)

        return AuthenticatorMakeCredentialResponse(
            authData = attestationObject.authData,
            fmt = attestationObject.fmt,
            attStmt = attestationObject.attStmt
        )
    }

    private suspend fun handleGetAssertion(request: AuthenticatorGetAssertionRequest): AuthenticatorGetAssertionResponse {
        val publicKeyCredentialRequestOptions = PublicKeyCredentialRequestOptions.Builder()
            .setRpId(request.rpId)
            .setChallenge(request.clientDataHash)
            .setAllowList(request.allowList)
            .setRequireUserVerification(request.options?.userVerification?.takeIf { it }?.let { UserVerificationRequirement.REQUIRED })
            .build()

        val browserOptions = BrowserPublicKeyCredentialRequestOptions.Builder()
            .setPublicKeyCredentialRequestOptions(publicKeyCredentialRequestOptions)
            .setOrigin("https://${request.rpId}".toUri())
            .setClientDataHash(request.clientDataHash).build()

        val intent = Intent(this, AuthenticatorActivity::class.java)
            .putExtra(AuthenticatorActivity.KEY_SOURCE, AuthenticatorActivity.SOURCE_HYBRID)
            .putExtra(AuthenticatorActivity.KEY_TYPE, AuthenticatorActivity.TYPE_SIGN)
            .putExtra(AuthenticatorActivity.KEY_OPTIONS, browserOptions.serializeToBytes())

        val result = suspendCancellableCoroutine { continuation ->
            waitingLauncherContinuation = continuation
            waitingLauncher.launch(intent)
        }

        val resultBytes = result.data?.getByteArrayExtra(FIDO2_KEY_CREDENTIAL_EXTRA) ?: throw RuntimeException("No result")
        val publicKeyCredential = PublicKeyCredential.deserializeFromBytes(resultBytes)
        val response = publicKeyCredential.response as? AuthenticatorAssertionResponse? ?: throw RuntimeException("Invalid result")
        val userEntity = result.data?.getStringExtra(AuthenticatorActivity.KEY_USER_JSON)?.let { PublicKeyCredentialUserEntity.parseJson(it) }?:
            response.userHandle?.let { PublicKeyCredentialUserEntity(it,  "", null, "") }

        return AuthenticatorGetAssertionResponse(
            credential = PublicKeyCredentialDescriptor("public-key", response.keyHandle, null),
            authData = response.authenticatorData,
            signature = response.signature,
            user = userEntity,
            numberOfCredentials = 1
        )
    }

    private fun handleGetInfo(request: AuthenticatorGetInfoRequest): AuthenticatorGetInfoResponse {
        return AuthenticatorGetInfoResponse(
            versions = arrayListOf("FIDO_2_0", "FIDO_2_1"),
            aaguid = ByteArray(16),
            options = AuthenticatorGetInfoResponse.Companion.Options(
                residentKey = true,
                userPresence = true,
                userVerification = true,
                platformDevice = true
            )
        )
    }

    private fun hasBluetoothPermissions(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.BLUETOOTH_SCAN
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestBluetoothPermissions() {
        ActivityCompat.requestPermissions(
            this, arrayOf(
                Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT
            ), REQUEST_BLUETOOTH_PERMISSIONS
        )
    }

    @RequiresPermission(Manifest.permission.BLUETOOTH_ADVERTISE)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_BLUETOOTH_PERMISSIONS) {
            val allGranted = grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }

            if (allGranted) {
                Log.d(TAG, "Bluetooth permissions granted")
                // Resume flow with pending QR code data
                startHybridConnectionFlow()
            } else {
                Log.e(TAG, "Bluetooth permissions denied")
                finishWithError("Bluetooth permissions required for cross-device authentication")
            }
        }
    }

    private fun finishWithSuccess() {
        Log.d(TAG, "Finishing with success")
        bottomSheet?.dismiss()
        bottomSheet = null
        setResult(RESULT_OK)
        finish()
    }

    private fun finishWithError(message: String) {
        Log.d(TAG, "Finishing with error: $message")
        bottomSheet?.dismiss()
        bottomSheet = null
        setResult(RESULT_CANCELED, Intent().apply {
            putExtra("error", message)
        })
        finish()
    }
}
