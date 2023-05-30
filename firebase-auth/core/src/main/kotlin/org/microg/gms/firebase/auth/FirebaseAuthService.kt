/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.firebase.auth

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Handler
import android.os.Parcel
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.firebase.auth.ActionCodeSettings
import com.google.firebase.auth.EmailAuthCredential
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.api.internal.*
import org.json.JSONArray
import org.json.JSONObject
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils

private const val TAG = "GmsFirebaseAuth"

fun JSONObject.getStringOrNull(key: String) = if (has(key)) getString(key) else null
fun JSONObject.getJSONArrayOrNull(key: String) = if (has(key)) getJSONArray(key) else null
fun JSONArray?.orEmpty() = this ?: JSONArray()
fun JSONObject.getJSONArrayLength(key: String) = getJSONArrayOrNull(key).orEmpty().length()

private val ActionCodeSettings.requestTypeAsString: String
    get() = when (requestType) {
        1 -> "PASSWORD_RESET"
        2 -> "OLD_EMAIL_AGREE"
        3 -> "NEW_EMAIL_ACCEPT"
        4 -> "VERIFY_EMAIL"
        5 -> "RECOVER_EMAIL"
        6 -> "EMAIL_SIGNIN"
        7 -> "VERIFY_AND_CHANGE_EMAIL"
        8 -> "REVERT_SECOND_FACTOR_ADDITION"
        else -> "OOB_REQ_TYPE_UNSPECIFIED"
    }

private val UserProfileChangeRequest.deleteAttributeList: List<String>
    get() {
        val list = arrayListOf<String>()
        if (shouldRemoveDisplayName) list.add("DISPLAY_NAME")
        if (shouldRemovePhotoUri) list.add("PHOTO_URL")
        return list
    }

private fun Intent.getSmsMessages(): Array<SmsMessage> {
    return if (Build.VERSION.SDK_INT >= 19) {
        Telephony.Sms.Intents.getMessagesFromIntent(this)
    } else {
        (getSerializableExtra("pdus") as? Array<ByteArray>)?.map { SmsMessage.createFromPdu(it) }.orEmpty().toTypedArray()
    }
}

class FirebaseAuthService : BaseService(TAG, GmsService.FIREBASE_AUTH) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService?) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        val apiKey = request.extras?.getString(Constants.EXTRA_API_KEY)
        val libraryVersion = request.extras?.getString(Constants.EXTRA_LIBRARY_VERSION)
        if (apiKey == null) {
            callback.onPostInitComplete(CommonStatusCodes.DEVELOPER_ERROR, null, null)
        } else {
            callback.onPostInitComplete(0, FirebaseAuthServiceImpl(this, lifecycle, request.packageName, libraryVersion, apiKey).asBinder(), null)
        }
    }
}

class FirebaseAuthServiceImpl(private val context: Context, override val lifecycle: Lifecycle, private val packageName: String, private val libraryVersion: String?, private val apiKey: String) : IFirebaseAuthService.Stub(), LifecycleOwner {
    private val client = IdentityToolkitClient(context, apiKey)
    private var authorizedDomain: String? = null

    private suspend fun getAuthorizedDomain(): String {
        authorizedDomain?.let { return it }
        val authorizedDomain = try {
            client.getProjectConfig().getJSONArray("authorizedDomains").getString(0)
        } catch (e: Exception) {
            Log.w(TAG, e)
            "localhost"
        }
        this.authorizedDomain = authorizedDomain
        return authorizedDomain
    }

    private suspend fun refreshTokenResponse(cachedState: String): GetTokenResponse {
        var tokenResponse = GetTokenResponse.parseJson(cachedState)
        if (System.currentTimeMillis() + 300000L < tokenResponse.issuedAt + tokenResponse.expiresIn * 1000) {
            return tokenResponse
        }
        return client.getTokenByRefreshToken(tokenResponse.refreshToken).toGetTokenResponse()
    }

    private fun JSONObject.toGetTokenResponse() = GetTokenResponse().apply {
        refreshToken = getStringOrNull("refresh_token")
        accessToken = getStringOrNull("access_token")
        expiresIn = getStringOrNull("expires_in")?.toLong()
        tokenType = getStringOrNull("token_type")
    }

    private fun JSONObject.toGetAccountInfoUser(): GetAccountInfoUser = GetAccountInfoUser().apply {
        localId = getStringOrNull("localId")
        email = getStringOrNull("email")
        isEmailVerified = optBoolean("emailVerified")
        displayName = getStringOrNull("displayName")
        photoUrl = getStringOrNull("photoUrl")
        for (i in 0 until getJSONArrayLength("providerUserInfo")) {
            getJSONArray("providerUserInfo").getJSONObject(i).run {
                providerInfoList.providerUserInfos.add(ProviderUserInfo().apply {
                    federatedId = getStringOrNull("federatedId")
                    displayName = getStringOrNull("displayName")
                    photoUrl = getStringOrNull("photoUrl")
                    providerId = getStringOrNull("providerId")
                    phoneNumber = getStringOrNull("phoneNumber")
                    email = getStringOrNull("email")
                    rawUserInfo = this@run.toString()
                })
            }
        }
        password = getStringOrNull("rawPassword")
        phoneNumber = getStringOrNull("phoneNumber")
        creationTimestamp = getStringOrNull("createdAt")?.toLong() ?: 0L
        lastSignInTimestamp = getStringOrNull("lastLoginAt")?.toLong() ?: 0L
    }

    private fun JSONObject.toCreateAuthUriResponse(): CreateAuthUriResponse = CreateAuthUriResponse().apply {
        authUri = getStringOrNull("authUri")
        isRegistered = optBoolean("registered")
        providerId = getStringOrNull("providerId")
        isForExistingProvider = optBoolean("forExistingProvider")
        for (i in 0 until getJSONArrayLength("allProviders")) {
            stringList.values.add(getJSONArray("allProviders").getString(i))
        }
        for (i in 0 until getJSONArrayLength("signinMethods")) {
            signInMethods.add(getJSONArray("signinMethods").getString(i))
        }
    }

    override fun applyActionCode(request: ApplyActionCodeAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: applyActionCode")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun applyActionCodeCompat(code: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: applyActionCodeCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun changeEmail(request: ChangeEmailAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: changeEmail")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun changeEmailCompat(cachedState: String?, email: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: changeEmailCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun changePassword(request: ChangePasswordAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: changePassword")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun changePasswordCompat(cachedState: String?, password: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: changePasswordCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun checkActionCode(request: CheckActionCodeAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: checkActionCode")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun checkActionCodeCompat(code: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: checkActionCodeCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun confirmPasswordReset(request: ConfirmPasswordResetAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: confirmPasswordReset")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun confirmPasswordResetCompat(code: String?, newPassword: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: confirmPasswordResetCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun createUserWithEmailAndPassword(request: CreateUserWithEmailAndPasswordAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "createUserWithEmailAndPassword")
            try {
                val tokenResult = client.signupNewUser(email = request.email, password = request.password, tenantId = request.tenantId)
                val idToken = tokenResult.getString("idToken")
                val refreshToken = tokenResult.getString("refreshToken")
                val getTokenResponse = client.getTokenByRefreshToken(refreshToken).toGetTokenResponse()
                val accountInfoResult = client.getAccountInfo(idToken = idToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser().apply { this.isNewUser = true }
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun createUserWithEmailAndPasswordCompat(email: String?, password: String?, callbacks: IFirebaseAuthCallbacks) {
        createUserWithEmailAndPassword(CreateUserWithEmailAndPasswordAidlRequest().apply { this.email = email; this.password = password }, callbacks)
    }

    override fun delete(request: DeleteAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: delete")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun deleteCompat(cachedState: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: deleteCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun finalizeMfaEnrollment(request: FinalizeMfaEnrollmentAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: finalizeMfaEnrollment")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun finalizeMfaSignIn(request: FinalizeMfaSignInAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: finalizeMfaSignIn")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun getAccessToken(request: GetAccessTokenAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "getAccessToken")
            try {
                callbacks.onGetTokenResponse(client.getTokenByRefreshToken(request.refreshToken).toGetTokenResponse())
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun getAccessTokenCompat(refreshToken: String?, callbacks: IFirebaseAuthCallbacks) {
        getAccessToken(GetAccessTokenAidlRequest().apply { this.refreshToken = refreshToken }, callbacks)
    }

    override fun getProvidersForEmail(request: GetProvidersForEmailAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "getProvidersForEmail")
            try {
                callbacks.onCreateAuthUriResponse(client.createAuthUri(identifier = request.email, tenantId = request.tenantId).toCreateAuthUriResponse())
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun getProvidersForEmailCompat(email: String?, callbacks: IFirebaseAuthCallbacks) {
        getProvidersForEmail(GetProvidersForEmailAidlRequest().apply { this.email = email }, callbacks)
    }

    override fun linkEmailAuthCredential(request: LinkEmailAuthCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "linkEmailAuthCredential")
            try {
                val getTokenResponse = refreshTokenResponse(request.cachedState)
                val accountInfoResult = client.getAccountInfo(idToken = getTokenResponse.accessToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser()
                val setAccountInfo = client.setAccountInfo(idToken = getTokenResponse.accessToken, localId = accountInfoResult.localId, email = request.email, password = request.password).toGetAccountInfoUser()
                accountInfoResult.email = setAccountInfo.email
                accountInfoResult.isEmailVerified = setAccountInfo.isEmailVerified
                accountInfoResult.providerInfoList = setAccountInfo.providerInfoList
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun linkEmailAuthCredentialCompat(email: String?, password: String?, cachedState: String?, callbacks: IFirebaseAuthCallbacks) {
        linkEmailAuthCredential(LinkEmailAuthCredentialAidlRequest().apply { this.email = email; this.password = password; this.cachedState = cachedState }, callbacks)
    }

    override fun linkFederatedCredential(request: LinkFederatedCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: linkFederatedCredential")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun linkFederatedCredentialCompat(cachedState: String?, verifyAssertionRequest: VerifyAssertionRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: linkFederatedCredentialCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun linkPhoneAuthCredential(request: LinkPhoneAuthCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: linkPhoneAuthCredential")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun linkPhoneAuthCredentialCompat(cachedState: String?, credential: PhoneAuthCredential?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: linkPhoneAuthCredentialCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun reload(request: ReloadAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "reload")
                val getTokenResponse = refreshTokenResponse(request.cachedState)
                val accountInfoResult = client.getAccountInfo(idToken = getTokenResponse.accessToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser()
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun reloadCompat(cachedState: String?, callbacks: IFirebaseAuthCallbacks) {
        reload(ReloadAidlRequest().apply { this.cachedState = cachedState }, callbacks)
    }

    override fun sendEmailVerification(request: SendEmailVerificationWithSettingsAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "sendEmailVerification")
                client.getOobConfirmationCode(
                        requestType = "VERIFY_EMAIL",
                        idToken = request.token,
                        iOSBundleId = request.settings?.iOSBundle,
                        iOSAppStoreId = request.settings?.iOSAppStoreId,
                        continueUrl = request.settings?.url,
                        androidInstallApp = request.settings?.androidInstallApp,
                        androidMinimumVersion = request.settings?.androidMinimumVersion,
                        androidPackageName = request.settings?.androidPackageName,
                        canHandleCodeInApp = request.settings?.handleCodeInApp
                )
                callbacks.onEmailVerificationResponse()
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun sendEmailVerificationCompat(token: String?, actionCodeSettings: ActionCodeSettings?, callbacks: IFirebaseAuthCallbacks) {
        sendEmailVerification(SendEmailVerificationWithSettingsAidlRequest().apply { this.token = token; this.settings = actionCodeSettings }, callbacks)
    }

    override fun sendVerificationCode(request: SendVerificationCodeAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "sendVerificationCode")
                val reCaptchaToken = when {
                    request.request.recaptchaToken != null -> request.request.recaptchaToken
                    ReCaptchaOverlay.isSupported(context) -> ReCaptchaOverlay.awaitToken(context, apiKey, getAuthorizedDomain())
                    ReCaptchaActivity.isSupported(context) -> ReCaptchaActivity.awaitToken(context, apiKey, getAuthorizedDomain())
                    else -> throw RuntimeException("No recaptcha token available")
                }
                var sessionInfo: String? = null
                var registered = true
                val receiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        var smsCode: String? = null
                        for (message in intent.getSmsMessages()) {
                            smsCode = Regex("\\b([0-9]{6})\\b").find(message.messageBody)?.groups?.get(1)?.value
                                    ?: continue
                            Log.d(TAG, "Received SMS verification code: $smsCode")
                            break
                        }
                        if (smsCode == null) return
                        registered = false
                        context.unregisterReceiver(this)
                        try {
                            callbacks.onVerificationCompletedResponse(PhoneAuthCredential().apply {
                                this.phoneNumber = request.request.phoneNumber
                                this.sessionInfo = sessionInfo
                                this.smsCode = smsCode
                            })
                            Log.d(TAG, "callback: onVerificationCompletedResponse")
                        } catch (e: Exception) {
                            Log.w(TAG, e)
                        }
                    }
                }
                context.registerReceiver(receiver, IntentFilter("android.provider.Telephony.SMS_RECEIVED"))
                var timeout = request.request.timeoutInSeconds * 1000L
                if (timeout <= 0L) timeout = 120000L
                Handler().postDelayed({
                    if (registered) {
                        Log.d(TAG, "Waited ${timeout}ms for verification code SMS, timeout.")
                        context.unregisterReceiver(receiver)
                        callbacks.onVerificationAutoTimeOut(sessionInfo)
                        Log.d(TAG, "callback: onVerificationAutoTimeOut")
                    }
                }, timeout)
                sessionInfo = client.sendVerificationCode(phoneNumber = request.request.phoneNumber, reCaptchaToken = reCaptchaToken).getString("sessionInfo")
                callbacks.onSendVerificationCodeResponse(sessionInfo)
                Log.d(TAG, "callback: onSendVerificationCodeResponse")
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun sendVerificationCodeCompat(request: SendVerificationCodeRequest, callbacks: IFirebaseAuthCallbacks) {
        sendVerificationCode(SendVerificationCodeAidlRequest().apply { this.request = request }, callbacks)
    }

    override fun sendGetOobConfirmationCodeEmail(request: SendGetOobConfirmationCodeEmailAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            try {
                Log.d(TAG, "sendGetOobConfirmationCodeEmail")
                client.getOobConfirmationCode(
                        requestType = request.settings?.requestTypeAsString ?: "OOB_REQ_TYPE_UNSPECIFIED",
                        email = request.email,
                        iOSBundleId = request.settings?.iOSBundle,
                        iOSAppStoreId = request.settings?.iOSAppStoreId,
                        continueUrl = request.settings?.url,
                        androidInstallApp = request.settings?.androidInstallApp,
                        androidMinimumVersion = request.settings?.androidMinimumVersion,
                        androidPackageName = request.settings?.androidPackageName,
                        canHandleCodeInApp = request.settings?.handleCodeInApp
                )
                Log.d(TAG, "callback: onResetPasswordResponse")
                callbacks.onResetPasswordResponse(null)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun sendGetOobConfirmationCodeEmailCompat(email: String?, actionCodeSettings: ActionCodeSettings?, callbacks: IFirebaseAuthCallbacks) {
        sendGetOobConfirmationCodeEmail(SendGetOobConfirmationCodeEmailAidlRequest().apply { this.email = email; this.settings = actionCodeSettings }, callbacks)
    }

    override fun setFirebaseUiVersion(request: SetFirebaseUiVersionAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: setFirebaseUiVersion")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun setFirebaseUIVersionCompat(firebaseUiVersion: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: setFirebaseUIVersionCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun signInAnonymously(request: SignInAnonymouslyAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "signInAnonymously")
            try {
                val tokenResult = client.signupNewUser(tenantId = request.tenantId)
                val idToken = tokenResult.getString("idToken")
                val refreshToken = tokenResult.getString("refreshToken")
                val getTokenResponse = client.getTokenByRefreshToken(refreshToken).toGetTokenResponse()
                val accountInfoResult = client.getAccountInfo(idToken = idToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser().apply { this.isNewUser = true }
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun signInAnonymouslyCompat(callbacks: IFirebaseAuthCallbacks) {
        signInAnonymously(SignInAnonymouslyAidlRequest(), callbacks)
    }

    override fun signInWithCredential(request: SignInWithCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: signInWithCredential")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun signInWithCredentialCompat(verifyAssertionRequest: VerifyAssertionRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: signInWithCredentialCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun signInWithCustomToken(request: SignInWithCustomTokenAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "signInWithCustomToken")
            try {
                val tokenResult = client.verifyCustomToken(token = request.token)
                val idToken = tokenResult.getString("idToken")
                val refreshToken = tokenResult.getString("refreshToken")
                val isNewUser = tokenResult.optBoolean("isNewUser")
                val getTokenResponse = client.getTokenByRefreshToken(refreshToken).toGetTokenResponse()
                val accountInfoResult = client.getAccountInfo(idToken = idToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser().apply { this.isNewUser = isNewUser }
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun signInWithCustomTokenCompat(token: String, callbacks: IFirebaseAuthCallbacks) {
        signInWithCustomToken(SignInWithCustomTokenAidlRequest().apply { this.token = token }, callbacks)
    }

    override fun signInWithEmailAndPassword(request: SignInWithEmailAndPasswordAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "signInWithEmailAndPassword")
            try {
                val tokenResult = client.verifyPassword(email = request.email, password = request.password, tenantId = request.tenantId)
                val idToken = tokenResult.getString("idToken")
                val refreshToken = tokenResult.getString("refreshToken")
                val getTokenResponse = client.getTokenByRefreshToken(refreshToken).toGetTokenResponse()
                val accountInfoResult = client.getAccountInfo(idToken = idToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser()
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun signInWithEmailAndPasswordCompat(email: String?, password: String?, callbacks: IFirebaseAuthCallbacks) {
        signInWithEmailAndPassword(SignInWithEmailAndPasswordAidlRequest().apply { this.email = email; this.password = password }, callbacks)
    }

    override fun signInWithEmailLink(request: SignInWithEmailLinkAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: signInWithEmailLink")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun signInWithEmailLinkCompat(credential: EmailAuthCredential?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: signInWithEmailLinkCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun signInWithPhoneNumber(request: SignInWithPhoneNumberAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "signInWithPhoneNumber")
            try {
                val tokenResult = client.verifyPhoneNumber(
                        phoneNumber = request.credential.phoneNumber,
                        temporaryProof = request.credential.temporaryProof,
                        sessionInfo = request.credential.sessionInfo,
                        code = request.credential.smsCode
                )
                val idToken = tokenResult.getString("idToken")
                val refreshToken = tokenResult.getString("refreshToken")
                val isNewUser = tokenResult.optBoolean("isNewUser")
                val getTokenResponse = client.getTokenByRefreshToken(refreshToken).toGetTokenResponse()
                val accountInfoResult = client.getAccountInfo(idToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser().apply { this.isNewUser = isNewUser }
                Log.d(TAG, "callback: onGetTokenResponseAndUser")
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun signInWithPhoneNumberCompat(credential: PhoneAuthCredential?, callbacks: IFirebaseAuthCallbacks) {
        signInWithPhoneNumber(SignInWithPhoneNumberAidlRequest().apply { this.credential = credential }, callbacks)
    }

    override fun startMfaEnrollmentWithPhoneNumber(request: StartMfaPhoneNumberEnrollmentAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: startMfaEnrollmentWithPhoneNumber")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun startMfaSignInWithPhoneNumber(request: StartMfaPhoneNumberSignInAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: startMfaSignInWithPhoneNumber")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun unenrollMfa(request: UnenrollMfaAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: unenrollMfa")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun unlinkEmailCredential(request: UnlinkEmailCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: unlinkEmailCredential")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun unlinkEmailCredentialCompat(cachedState: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: unlinkEmailCredentialCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun unlinkFederatedCredential(request: UnlinkFederatedCredentialAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: unlinkFederatedCredential")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun unlinkFederatedCredentialCompat(provider: String?, cachedState: String?, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: unlinkFederatedCredentialCompat")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }

    override fun updateProfile(request: UpdateProfileAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        lifecycleScope.launchWhenStarted {
            Log.d(TAG, "updateProfile")
            try {
                val getTokenResponse = refreshTokenResponse(request.cachedState)
                val accountInfoResult = client.getAccountInfo(idToken = getTokenResponse.accessToken).getJSONArray("users").getJSONObject(0).toGetAccountInfoUser()
                val setAccountInfo = client.setAccountInfo(idToken = getTokenResponse.accessToken, localId = accountInfoResult.localId, displayName = request.request.displayName, photoUrl = request.request.photoUrl, deleteAttribute = request.request.deleteAttributeList).toGetAccountInfoUser()
                accountInfoResult.photoUrl = setAccountInfo.photoUrl
                accountInfoResult.displayName = setAccountInfo.displayName
                callbacks.onGetTokenResponseAndUser(getTokenResponse, accountInfoResult)
            } catch (e: Exception) {
                Log.w(TAG, "callback: onFailure", e)
                callbacks.onFailure(Status(CommonStatusCodes.INTERNAL_ERROR, e.message))
            }
        }
    }

    override fun updateProfileCompat(cachedState: String?, userProfileChangeRequest: UserProfileChangeRequest, callbacks: IFirebaseAuthCallbacks) {
        updateProfile(UpdateProfileAidlRequest().apply { this.cachedState = cachedState; this.request = userProfileChangeRequest}, callbacks)
    }

    override fun verifyBeforeUpdateEmail(request: VerifyBeforeUpdateEmailAidlRequest, callbacks: IFirebaseAuthCallbacks) {
        Log.d(TAG, "Not yet implemented: verifyBeforeUpdateEmail")
        callbacks.onFailure(Status(CommonStatusCodes.CANCELED, "Not supported"))
    }


    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
        if (super.onTransact(code, data, reply, flags)) return true
        Log.d(TAG, "onTransact: $code, $data, $flags")
        return false
    }
}
