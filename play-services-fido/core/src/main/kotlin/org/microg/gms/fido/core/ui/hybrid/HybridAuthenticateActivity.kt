/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.ui.hybrid

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.fido.fido2.api.common.AttestationConveyancePreference
import com.google.android.gms.fido.fido2.api.common.AuthenticatorSelectionCriteria
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.BrowserPublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.ErrorCode
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialCreationOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRequestOptions
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialType
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.google.android.gms.fido.fido2.api.common.UserVerificationRequirement
import com.upokecenter.cbor.CBORObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.microg.gms.fido.core.Database
import org.microg.gms.fido.core.R
import org.microg.gms.fido.core.RequestHandlingException
import org.microg.gms.fido.core.hybrid.controller.HybridAuthenticatorController
import org.microg.gms.fido.core.hybrid.model.QrCodeData
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorGetAssertionResponse
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialRequest
import org.microg.gms.fido.core.protocol.msgs.AuthenticatorMakeCredentialResponse
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.screenlock.ScreenLockTransportHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "HybridAuthenticate"
private const val REQUEST_BLUETOOTH_PERMISSIONS = 1001

@RequiresApi(23)
class HybridAuthenticateActivity : AppCompatActivity() {
    private val transport = ScreenLockTransportHandler(this)
    private val database = Database(this)
    private var bottomSheet: HybridReceiverBottomSheetFragment? = null
    private var qrCodeData: QrCodeData? = null

    private var hybridAuthenticatorController: HybridAuthenticatorController? = null

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

        val isRegistration = qrCodeData?.flowIdentifier?.contains("reg", ignoreCase = true) == true
        val rpId = qrCodeData?.flowIdentifier ?: "website"

        var bottomSheetFragment = supportFragmentManager.findFragmentByTag(HybridReceiverBottomSheetFragment.TAG)
        if (bottomSheetFragment == null) {
            bottomSheetFragment = HybridReceiverBottomSheetFragment.newInstance(isRegistration, rpId)
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
    private fun startHybridConnectionFlow() {
        lifecycleScope.launchWhenStarted {
            hybridAuthenticatorController = hybridAuthenticatorController ?: HybridAuthenticatorController(this@HybridAuthenticateActivity)
            try {
                hybridAuthenticatorController?.startAuth(qrCodeData!!, handleAuthenticator = {
                    when (it) {
                        is AuthenticatorMakeCredentialRequest -> handleMakeCredential(it)
                        is AuthenticatorGetAssertionRequest -> handleGetAssertion(it)
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

    private suspend fun handleMakeCredential(request: AuthenticatorMakeCredentialRequest): ByteArray? {
        val publicKeyCredentialCreationOptions = PublicKeyCredentialCreationOptions.Builder().apply {
            setRp(request.rp)
            setUser(request.user)
            setParameters(request.pubKeyCredParams)
            setChallenge(request.clientDataHash)
            setAttestationConveyancePreference(AttestationConveyancePreference.NONE)
            setAuthenticatorSelection(
                AuthenticatorSelectionCriteria.Builder().apply {
                    setRequireResidentKey(request.options?.residentKey ?: false)
                    if (request.options?.userVerification == true) {
                        setRequireUserVerification(UserVerificationRequirement.REQUIRED)
                    }
                }.build()
            )
            if (request.excludeList.isNotEmpty()) {
                setExcludeList(request.excludeList)
            }
        }.build()
        val browserOptions = BrowserPublicKeyCredentialCreationOptions.Builder().setPublicKeyCredentialCreationOptions(publicKeyCredentialCreationOptions).setOrigin("https://${request.rp.id}".toUri())
            .setClientDataHash(request.clientDataHash).build()

        val response = withContext(Dispatchers.Main) {
            transport.register(browserOptions, packageName)
        }

        val credentialId = Base64.encodeToString(response.keyHandle, Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        val userEntity = publicKeyCredentialCreationOptions.user
        val userJson = userEntity.toJson()
        database.insertKnownRegistration(
            request.rp.id, credentialId, Transport.SCREEN_LOCK, userJson
        )
        Log.d(TAG, "✓ Credential saved to database: $credentialId")
        val attestationObj = CBORObject.DecodeFromBytes(response.attestationObject)
        return byteArrayOf(0x00) + AuthenticatorMakeCredentialResponse(
            authData = attestationObj["authData"].GetByteString(), fmt = attestationObj["fmt"]?.AsString() ?: "none", attStmt = attestationObj["attStmt"]
        ).encodeAsCbor().EncodeToBytes()
    }

    private suspend fun handleGetAssertion(request: AuthenticatorGetAssertionRequest): ByteArray? {
        val knownRegistrations = database.getKnownRegistrationInfo(request.rpId)
        val userGroups = knownRegistrations.groupBy { it.userJson }
        val selectedUserInfo = when {
            userGroups.isEmpty() -> {
                Log.d(TAG, "No credentials found for ${request.rpId}")
                null
            }

            userGroups.size == 1 -> {
                Log.d(TAG, "Single account found, using it directly")
                userGroups.keys.first()
            }

            else -> {
                Log.d(TAG, "Multiple accounts found (${userGroups.size}), showing selection UI")
                showAccountSelectionAndWait(userGroups.keys.toList())
            }
        }
        Log.d(TAG, "Selected user for authentication: ${selectedUserInfo ?: "none"}")

        val publicKeyCredentialRequestOptions = PublicKeyCredentialRequestOptions.Builder().apply {
            setRpId(request.rpId)
            setChallenge(request.clientDataHash)
            if (request.allowList.isNotEmpty()) {
                setAllowList(request.allowList.map { cred ->
                    PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, cred.id)
                })
            }
            request.options?.userVerification?.let { requireUserVerification ->
                if (requireUserVerification) {
                    setRequireUserVerification(UserVerificationRequirement.REQUIRED)
                }
            }
        }.build()

        val browserOptions = BrowserPublicKeyCredentialRequestOptions.Builder().setPublicKeyCredentialRequestOptions(publicKeyCredentialRequestOptions).setOrigin("https://${request.rpId}".toUri())
            .setClientDataHash(request.clientDataHash).build()

        val userEntity = selectedUserInfo?.let { PublicKeyCredentialUserEntity.parseJson(it) }

        val response = withContext(Dispatchers.Main) {
            transport.sign(browserOptions, packageName, selectedUserInfo)
        }

        return byteArrayOf(0x00) + AuthenticatorGetAssertionResponse(
            credential = PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, response.keyHandle),
            authData = response.authenticatorData,
            signature = response.signature,
            user = response.userHandle?.let { PublicKeyCredentialUserEntity(it, userEntity?.name ?: "", userEntity?.icon, userEntity?.displayName ?: "") },
            numberOfCredentials = 1
        ).encodeAsCbor().EncodeToBytes()
    }

    private suspend fun showAccountSelectionAndWait(userJsonList: List<String>): String {
        return suspendCancellableCoroutine { continuation ->
            runOnUiThread {
                val userInfoList = userJsonList.mapNotNull { json ->
                    try {
                        PublicKeyCredentialUserEntity.parseJson(json)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to parse user JSON: $json", e)
                        null
                    }
                }

                if (userInfoList.isEmpty()) {
                    Log.e(TAG, "No valid user accounts to display")
                    continuation.resumeWithException(
                        RequestHandlingException(ErrorCode.NOT_ALLOWED_ERR, "No valid accounts")
                    )
                    return@runOnUiThread
                }

                AlertDialog.Builder(this).setTitle(getString(R.string.fido_sign_in_selection_title))
                    .setItems(userInfoList.map { user -> "${user.displayName} (${user.name})" }.toTypedArray()) { _, which ->
                        continuation.resume(userJsonList[which])
                    }.setOnCancelListener {
                        continuation.resumeWithException(
                            RequestHandlingException(ErrorCode.NOT_ALLOWED_ERR, "User cancelled")
                        )
                    }.show()
            }
        }
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