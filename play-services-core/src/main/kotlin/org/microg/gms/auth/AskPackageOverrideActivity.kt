/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth

import android.accounts.Account
import android.accounts.AccountAuthenticatorResponse
import android.accounts.AccountManager
import android.content.DialogInterface
import android.content.DialogInterface.OnCancelListener
import android.content.DialogInterface.OnClickListener
import android.content.Intent
import android.os.Bundle
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.BundleCompat
import androidx.core.text.HtmlCompat
import com.google.android.gms.R
import com.google.android.gms.common.internal.CertData
import org.microg.gms.auth.loginservice.AccountAuthenticator
import org.microg.gms.ui.buildAlertDialog
import org.microg.gms.utils.digest
import org.microg.gms.utils.getApplicationLabel
import org.microg.gms.utils.getCertificates
import org.microg.gms.utils.toHexString
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

private const val TAG = "AuthPackageOverride"

class AskPackageOverrideActivity : AppCompatActivity(), OnCancelListener, OnClickListener {
    private lateinit var requestingPackage: String
    private lateinit var requestingPackageCertificate: CertData

    private lateinit var accountName: String
    private lateinit var accountType: String

    private lateinit var overridePackage: String
    private lateinit var overrideCertificate: CertData

    private var response: AccountAuthenticatorResponse? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestingPackage = intent.extras?.getString(AccountManager.KEY_ANDROID_PACKAGE_NAME) ?: return finishResult(RESULT_CANCELED)
        requestingPackageCertificate = packageManager.getCertificates(requestingPackage).firstOrNull() ?: return finishResult(RESULT_CANCELED)
        accountName = intent.extras?.getString(AccountManager.KEY_ACCOUNT_NAME) ?: return finishResult(RESULT_CANCELED)
        accountType = intent.extras?.getString(AccountManager.KEY_ACCOUNT_TYPE) ?: return finishResult(RESULT_CANCELED)
        overridePackage = intent.extras?.getString(AccountAuthenticator.KEY_OVERRIDE_PACKAGE) ?: return finishResult(RESULT_CANCELED)
        overrideCertificate =
            intent.extras?.getByteArray(AccountAuthenticator.KEY_OVERRIDE_CERTIFICATE)?.let { CertData(it) } ?: return finishResult(RESULT_CANCELED)
        response =
            intent.extras?.let { BundleCompat.getParcelable(it, AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, AccountAuthenticatorResponse::class.java) }
        showDialog()
    }

    private fun finishResult(resultCode: Int, resultData: Intent? = null) {
        setResult(resultCode, resultData)
        if (resultCode == RESULT_OK && resultData != null && resultData.extras != null) {
            response?.onResult(resultData.extras)
        } else if (resultCode == RESULT_CANCELED){
            response?.onError(AccountManager.ERROR_CODE_CANCELED, "Cancelled")
        }
        finish()
    }

    fun showDialog() {
        val overridePackageLabel = when {
            overridePackage == "com.android.vending" -> "Play Store"
            overridePackage == "com.google.android.youtube" -> "YouTube"
            else -> runCatching { packageManager.getApplicationLabel(overridePackage) }.getOrNull() ?: overridePackage
        }
        val overridePackageVendor = runCatching {
            val principal = (CertificateFactory.getInstance("X.509")
                .generateCertificate(overrideCertificate.bytes.inputStream()) as X509Certificate).subjectX500Principal.name
            val sections = principal.split(",")
            var proc = sections.indexOfFirst { it.startsWith("O=") }
            var organization = sections[proc].substring(2)
            while (organization.last() == '\\') organization = organization.substring(0, organization.length - 1) + "," + sections[++proc]
            while ((organization.count { it == '"' } % 2) != 0) organization += "," + sections[++proc]
            if (organization.startsWith('"')) organization = organization.substring(1, organization.length - 1).replace("\"\"", "\"")
            organization.trim().trimEnd('.')
        }.getOrNull() ?: return finishResult(RESULT_CANCELED)
        val requestingPackageLabel = runCatching { packageManager.getApplicationLabel(requestingPackage) }.getOrNull() ?: requestingPackage
        val alertDialog = buildAlertDialog()
            .setOnCancelListener(this)
            .setPositiveButton(R.string.allow, this)
            .setNegativeButton(R.string.deny, this)
            .setIcon(R.drawable.ic_manage_accounts)
            .setTitle(HtmlCompat.fromHtml(getString(R.string.auth_package_override_request_title, Html.escapeHtml(requestingPackageLabel), Html.escapeHtml(accountName)), HtmlCompat.FROM_HTML_MODE_COMPACT))
            .setMessage(HtmlCompat.fromHtml(getString(R.string.auth_package_override_request_message, Html.escapeHtml(requestingPackageLabel), Html.escapeHtml(overridePackageLabel), Html.escapeHtml(overridePackageVendor)), HtmlCompat.FROM_HTML_MODE_COMPACT))
            .create()
        alertDialog.setCanceledOnTouchOutside(false)
        alertDialog.show()
        response?.onRequestContinued()
    }

    override fun onCancel(dialog: DialogInterface?) {
        finishResult(RESULT_CANCELED)
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == DialogInterface.BUTTON_POSITIVE) {
            val requestingDigestString = requestingPackageCertificate.digest("SHA-256").toHexString("")
            val overrideCertificateDigestString = overrideCertificate.digest("SHA-256").toHexString("")
            val overrideUserDataKey = "override.$requestingPackage:$requestingDigestString:$overridePackage:$overrideCertificateDigestString"
            AccountManager.get(this).setUserData(Account(accountName, accountType), overrideUserDataKey, "1")
            finishResult(RESULT_OK, Intent().apply { putExtra("retry", true) })
        } else {
            finishResult(RESULT_CANCELED)
        }
    }
}