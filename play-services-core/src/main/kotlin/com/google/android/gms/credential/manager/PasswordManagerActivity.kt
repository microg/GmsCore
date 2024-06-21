package com.google.android.gms.credential.manager

import android.accounts.AccountManager
import android.os.Bundle
import android.util.Log
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.appcompat.app.AppCompatActivity
import org.microg.gms.accountsettings.ui.WebViewHelper
import org.microg.gms.auth.AuthConstants

const val PASSWORD_MANAGER_CLASS_NAME = "com.google.android.gms.credential.manager.PasswordManagerActivity"

const val EXTRA_KEY_ACCOUNT_NAME = "pwm.DataFieldNames.accountName"
const val EXTRA_KEY_UTM_SOURCE = "utm_source"
const val EXTRA_KEY_UTM_MEDIUM = "utm_medium"
const val EXTRA_KEY_UTM_CAMPAIGN = "utm_campaign"
const val EXTRA_KEY_UTM_CONTENT = "utm_content"

private const val TAG = "PasswordManagerActivity"

private const val PSW_MANAGER_PATH = "https://passwords.google.com/"

class PasswordManagerActivity : AppCompatActivity() {

    private lateinit var webView: WebView

    private val accountName: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_ACCOUNT_NAME) }.getOrNull()
    private val utmSource: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_UTM_SOURCE) }.getOrNull()
    private val utmMedium: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_UTM_MEDIUM) }.getOrNull()
    private val utmCampaign: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_UTM_CAMPAIGN) }.getOrNull()
    private val utmContent: String?
        get() = runCatching { intent?.getStringExtra(EXTRA_KEY_UTM_CONTENT) }.getOrNull()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Log.d(TAG, "onCreate: start")

        val accounts = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
        val realAccountName = accounts.find { it.name.equals(accountName) }?.name

        Log.d(TAG, "realAccountName: $realAccountName")

        val layout = RelativeLayout(this)
        layout.addView(ProgressBar(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.WRAP_CONTENT, RelativeLayout.LayoutParams.WRAP_CONTENT).apply {
                addRule(RelativeLayout.CENTER_HORIZONTAL)
                addRule(RelativeLayout.CENTER_VERTICAL)
            }
            isIndeterminate = true
        })
        webView = WebView(this).apply {
            layoutParams = RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT, RelativeLayout.LayoutParams.MATCH_PARENT)
            visibility = View.INVISIBLE
        }
        layout.addView(webView)
        setContentView(layout)
        WebViewHelper(this, webView).openWebView(constructPswPath(), realAccountName)
    }

    private fun constructPswPath(): String {
        val psmPath = StringBuilder().apply {
            if (!utmSource.isNullOrEmpty()) {
                if (isEmpty()) append("?") else append("&")
                append("$EXTRA_KEY_UTM_SOURCE=$utmSource")
            }
            if (!utmMedium.isNullOrEmpty()) {
                if (isEmpty()) append("?") else append("&")
                append("$EXTRA_KEY_UTM_MEDIUM=$utmMedium")
            }
            if (!utmCampaign.isNullOrEmpty()) {
                if (isEmpty()) append("?") else append("&")
                append("$EXTRA_KEY_UTM_CAMPAIGN=$utmCampaign")
            }
            if (!utmContent.isNullOrEmpty()) {
                if (isEmpty()) append("?") else append("&")
                append("$EXTRA_KEY_UTM_CONTENT=$utmContent")
            }
            insert(0, PSW_MANAGER_PATH)
        }.toString()
        Log.d(TAG, "constructPswPath: $psmPath")
        return psmPath
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

}