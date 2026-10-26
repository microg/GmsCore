/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.recaptcha

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.profile.ProfileManager
import org.microg.gms.recaptcha.modac.RecaptchaQrVerifier
import org.microg.gms.recaptcha.modac.VerificationOutcome

private const val TAG = "RecaptchaDeepLink"
private const val QR_TOKEN_SEPARATOR = "/qr/"
private const val MAX_QR_TOKEN_LENGTH = 4096
private val ALLOWED_HOSTS = setOf("recaptcha.net", "recaptcha.google.com")

class RecaptchaDeepLinkActivity : AppCompatActivity() {

    private var status by mutableStateOf(VerificationStatus.CONFIRM)
    private var qrToken: String? = null
    private var verificationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ProfileManager.ensureInitialized(this)
        RecaptchaQrVerifier.init(this, signalUpdateScope = lifecycleScope)
        if (!acceptDeepLink(intent)) {
            finish()
            return
        }
        setContent {
            RecaptchaDeepLinkScreen(
                status = status,
                onPrimaryButtonClick = { onPrimaryButtonClick(status) },
                onSecondaryButtonClick = ::finish,
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        verificationJob?.cancel()
        verificationJob = null
        qrToken = null
        if (!acceptDeepLink(intent)) finish()
    }

    private fun acceptDeepLink(intent: Intent?): Boolean {
        val data = intent?.data
        val token = data?.let(::extractQrToken)
        if (intent?.action != Intent.ACTION_VIEW || data == null || data.scheme != "https" ||
            ALLOWED_HOSTS.none { it.equals(data.host, ignoreCase = true) } || token == null
        ) {
            Log.d(TAG, "Rejecting invalid reCAPTCHA QR deep link")
            return false
        }
        qrToken = token
        status = VerificationStatus.CONFIRM
        return true
    }

    private fun onPrimaryButtonClick(status: VerificationStatus) {
        when (status) {
            VerificationStatus.CONFIRM -> startVerification()
            VerificationStatus.LOADING,
            VerificationStatus.FAILED,
            VerificationStatus.VERIFIED -> finish()
        }
    }

    private fun startVerification() {
        val token = qrToken
        if (token == null) {
            Log.d(TAG, "Verification requested without an accepted token")
            finish()
            return
        }
        qrToken = null
        status = VerificationStatus.LOADING
        verificationJob?.cancel()
        verificationJob = lifecycleScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                RecaptchaQrVerifier.verifyToken(token)
            }
            status = when (outcome) {
                VerificationOutcome.Verified -> VerificationStatus.VERIFIED
                is VerificationOutcome.Failed -> VerificationStatus.FAILED
            }
        }
    }

    private fun extractQrToken(uri: Uri): String? {
        if (!isAllowedPath(uri.path)) return null
        val token = uri.pathSegments.drop(1).joinToString("/")
        return token.takeIf { it.isNotEmpty() && it.length <= MAX_QR_TOKEN_LENGTH }
    }

    private fun isAllowedPath(path: String?): Boolean = path?.startsWith(QR_TOKEN_SEPARATOR) == true
}
