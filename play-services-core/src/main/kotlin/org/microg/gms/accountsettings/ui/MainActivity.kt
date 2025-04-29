/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.accountsettings.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.Gravity
import android.view.View
import android.webkit.WebView
import android.widget.ProgressBar
import android.widget.RelativeLayout
import android.widget.RelativeLayout.LayoutParams.MATCH_PARENT
import android.widget.RelativeLayout.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.gms.R
import org.microg.gms.accountsettings.ui.bridge.OcAdvertisingIdBridge
import org.microg.gms.accountsettings.ui.bridge.OcAndroidIdBridge
import org.microg.gms.accountsettings.ui.bridge.OcAppBarBridge
import org.microg.gms.accountsettings.ui.bridge.OcAppPermissionsBridge
import org.microg.gms.accountsettings.ui.bridge.OcClientInfoBridge
import org.microg.gms.accountsettings.ui.bridge.OcConsistencyBridge
import org.microg.gms.accountsettings.ui.bridge.OcContactsBridge
import org.microg.gms.accountsettings.ui.bridge.OcFido2Bridge
import org.microg.gms.accountsettings.ui.bridge.OcFidoU2fBridge
import org.microg.gms.accountsettings.ui.bridge.OcFilePickerBridge
import org.microg.gms.accountsettings.ui.bridge.OcFolsomBridge
import org.microg.gms.accountsettings.ui.bridge.OcPermissionsBridge
import org.microg.gms.accountsettings.ui.bridge.OcPlayProtectBridge
import org.microg.gms.accountsettings.ui.bridge.OcTelephonyBridge
import org.microg.gms.accountsettings.ui.bridge.OcTrustAgentBridge
import org.microg.gms.accountsettings.ui.bridge.OcUdcBridge
import org.microg.gms.accountsettings.ui.bridge.OcUiBridge
import org.microg.gms.auth.AuthConstants
import org.microg.gms.common.Constants
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

private const val TAG = "AccountSettings"

// TODO: There likely is some API to figure those out...
private val SCREEN_ID_TO_URL = hashMapOf(
    1 to "https://myaccount.google.com",
    200 to "https://myaccount.google.com/privacycheckup",
    203 to "https://myaccount.google.com/email",
    204 to "https://myaccount.google.com/phone",
    205 to "https://myaccount.google.com/birthday",
    206 to "https://myaccount.google.com/gender",
    210 to "https://myaccount.google.com/locationsharing",
    214 to "https://myaccount.google.com/dashboard",
    215 to "https://takeout.google.com",
    216 to "https://myaccount.google.com/inactive",
    218 to "https://myaccount.google.com/profile-picture?interop=o",
    219 to "https://myactivity.google.com/myactivity",
    220 to "https://www.google.com/maps/timeline",
    224 to "https://myactivity.google.com/activitycontrols?settings=search",
    227 to "https://myactivity.google.com/activitycontrols?settings=location",
    231 to "https://myactivity.google.com/activitycontrols?settings=youtube",
    235 to "https://myactivity.google.com/activitycontrols/youtube",
    238 to "https://www.google.com/setting/search/privateresults/",
    241 to "https://myaccount.google.com/communication-preferences",
    242 to "https://myadcenter.google.com/controls",
    300 to "https://myaccount.google.com/language",
    301 to "https://drive.google.com/settings/storage",
    302 to "https://myaccount.google.com/deleteservices",
    303 to "https://myaccount.google.com/deleteaccount",
    307 to "https://payments.google.com/payments/home",
    308 to "https://myaccount.google.com/subscriptions",
    309 to "https://myaccount.google.com/purchases",
    310 to "https://myaccount.google.com/reservations",
    312 to "https://myaccount.google.com/accessibility",
    313 to "https://myaccount.google.com/inputtools",
    400 to "https://myaccount.google.com/security-checkup/6",
    401 to "https://myaccount.google.com/signinoptions/password",
    403 to "https://myaccount.google.com/signinoptions/two-step-verification",
    406 to "https://myaccount.google.com/signinoptions/rescuephone",
    407 to "https://myaccount.google.com/recovery/email",
    409 to "https://myaccount.google.com/notifications",
    410 to "https://myaccount.google.com/device-activity",
    417 to "https://myaccount.google.com/find-your-phone",
    425 to "https://myaccount.google.com/account-enhanced-safe-browsing",
    426 to "https://myaccount.google.com/two-step-verification/authenticator",
    427 to "https://myaccount.google.com/two-step-verification/backup-codes",
    429 to "https://myaccount.google.com/two-step-verification/security-keys",
    430 to "https://myaccount.google.com/two-step-verification/prompt",
    431 to "https://myaccount.google.com/connections",
    432 to "https://myaccount.google.com/two-step-verification/phone-numbers",
    433 to "https://myaccount.google.com/signinoptions/passkeys",
    437 to "https://myaccount.google.com/signinoptions/passwordoptional",
    500 to "https://policies.google.com/privacy",
    503 to "https://policies.google.com/terms",
    519 to "https://myaccount.google.com/yourdata/maps",
    520 to "https://myaccount.google.com/yourdata/search",
    530 to "https://fit.google.com/privacy/settings",
    547 to "https://myactivity.google.com/product/search",
    562 to "https://myaccount.google.com/yourdata/youtube",
    580 to "https://families.google.com/kidonboarding",
    10003 to "https://myaccount.google.com/personal-info",
    10004 to "https://myaccount.google.com/data-and-privacy",
    10005 to "https://myaccount.google.com/people-and-sharing",
    10006 to "https://myaccount.google.com/security",
    10007 to "https://myaccount.google.com/payments-and-subscriptions",
    10015 to "https://support.google.com/accounts",
    10050 to "https://myaccount.google.com/profile",
    10052 to "https://myaccount.google.com/family/details",
    10090 to "https://myaccount.google.com/profile/name",
    10704 to "https://www.google.com/account/about",
    10706 to "https://myaccount.google.com/profile/profiles-summary",
    10728 to "https://myaccount.google.com/data-and-privacy/how-data-improves-experience",
    10729 to "https://myaccount.google.com/data-and-privacy/data-visibility",
    10759 to "https://myaccount.google.com/address/home",
    10760 to "https://myaccount.google.com/address/work",
    14500 to "https://profilewidgets.google.com/alternate-profile/edit?interop=o&opts=sb",
)

private val ALLOWED_WEB_PREFIXES = setOf(
    "https://accounts.google.com/",
    "https://myaccount.google.com/",
    "https://one.google.com/",
    "https://myactivity.google.com/",
    "https://timeline.google.com/",
    "https://takeout.google.com/",
    "https://www.google.com/maps/",
    "https://www.google.com/setting/",
    "https://drive.google.com/settings/",
    "https://drive.google.com/accounts/",
    "https://drive.google.com/u/1/settings/",
    "https://payments.google.com/",
    "https://policies.google.com/",
    "https://fit.google.com/privacy/settings",
    "https://maps.google.com/maps/timeline",
    "https://myadcenter.google.com/controls",
    "https://families.google.com/kidonboarding",
    "https://profilewidgets.google.com/alternate-profile/edit",
)

private val ACTION_TO_SCREEN_ID = hashMapOf(
    ACTION_SECURITY_SETTINGS to 10006,
    ACTION_PRIVACY_SETTINGS to 10004,
    ACTION_LOCATION_SHARING to 210,
)

class MainActivity : AppCompatActivity() {
    private lateinit var webView: WebView
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    private fun getSelectedAccountName(): String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        val extras = intent?.extras?.also { it.keySet() }
        Log.d(TAG, "Invoked with ${intent.action} and extras $extras")
        super.onCreate(savedInstanceState)

        val screenId = ACTION_TO_SCREEN_ID[intent.action] ?: intent?.getIntExtra(EXTRA_SCREEN_ID, -1)?.takeIf { it > 0 } ?: 1
        val product = intent?.getStringExtra(EXTRA_SCREEN_MY_ACTIVITY_PRODUCT)
        val kidOnboardingParams = intent?.getStringExtra(EXTRA_SCREEN_KID_ONBOARDING_PARAMS)

        val screenOptions = intent.extras?.keySet().orEmpty()
            .filter { it.startsWith(EXTRA_SCREEN_OPTIONS_PREFIX) }
            .map { it.substring(EXTRA_SCREEN_OPTIONS_PREFIX.length) to intent.getStringExtra(it) }
            .toMap()

        val callingPackage = intent?.getStringExtra(EXTRA_CALLING_PACKAGE_NAME) ?: callingActivity?.packageName ?: Constants.GMS_PACKAGE_NAME

        val ignoreAccount = intent?.getBooleanExtra(EXTRA_IGNORE_ACCOUNT, false) ?: false
        val accountName = if (ignoreAccount) null else {
            val accounts = AccountManager.get(this).getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)
            val accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME) ?: intent.getParcelableExtra<Account>("account")?.name ?: getSelectedAccountName()
            accounts.find { it.name.equals(accountName) }?.name
        }

        if (accountName == null) {
            Log.w(TAG, "No account, going without!")
        }

        if (screenId in SCREEN_ID_TO_URL) {
            val screenUrl = SCREEN_ID_TO_URL[screenId]?.run {
                if (screenId == 547 && !product.isNullOrEmpty()) {
                    replace("search", product)
                } else if (screenId == 580 && !kidOnboardingParams.isNullOrEmpty()){
                    "$this?params=$kidOnboardingParams"
                } else { this }
            }
            val layout = RelativeLayout(this)
            val titleView = TextView(this).apply {
                text = ContextCompat.getString(context, R.string.pref_accounts_title)
                textSize = 20f
                setTextColor(Color.BLACK)
                maxLines = 1
                ellipsize = TextUtils.TruncateAt.END
                setTypeface(Typeface.create("sans-serif", Typeface.NORMAL))
                layoutParams = Toolbar.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.START)
            }
            val toolbar = Toolbar(this).apply {
                id = View.generateViewId()
                setBackgroundColor(Color.WHITE)
                if (SDK_INT >= 21) {
                    backgroundTintList = null
                }
                layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply {
                    addRule(RelativeLayout.ALIGN_PARENT_TOP)
                }
                navigationIcon = ContextCompat.getDrawable(context, R.drawable.ic_arrow_close)
                setNavigationOnClickListener { finish() }
                addView(titleView)
            }
            val progressBar = ProgressBar(this).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT).apply {
                    addRule(RelativeLayout.CENTER_IN_PARENT)
                }
                isIndeterminate = true
            }
            webView = WebView(this).apply {
                id = View.generateViewId()
                layoutParams = RelativeLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT).apply {
                    addRule(RelativeLayout.BELOW, toolbar.id)
                }
                visibility = View.INVISIBLE
                loadJsBridge(accountName, toolbar)
            }
            layout.addView(toolbar)
            layout.addView(progressBar)
            layout.addView(webView)
            setContentView(layout)
            WebViewHelper(this, webView, ALLOWED_WEB_PREFIXES).openWebView(screenUrl, accountName, callingPackage)
            setResult(RESULT_OK)
        } else {
            Log.w(TAG, "Unknown screen id, can't open corresponding web page")
            setResult(RESULT_CANCELED)
            finish()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (!executor.isShutdown) {
            executor.shutdown()
        }
    }

    override fun onBackPressed() {
        if (this::webView.isInitialized && webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    private fun WebView.loadJsBridge(accountName: String?, toolbar: Toolbar) {
        addJavascriptInterface(OcUiBridge(this@MainActivity, accountName, this, executor), OcUiBridge.NAME)
        addJavascriptInterface(OcConsistencyBridge(), OcConsistencyBridge.NAME)
        addJavascriptInterface(OcAppBarBridge(toolbar, this), OcAppBarBridge.NAME)
        addJavascriptInterface(OcPlayProtectBridge(this), OcPlayProtectBridge.NAME)
        addJavascriptInterface(OcTrustAgentBridge(this), OcTrustAgentBridge.NAME)
        addJavascriptInterface(OcPermissionsBridge(this), OcPermissionsBridge.NAME)
        addJavascriptInterface(OcFido2Bridge(this), OcFido2Bridge.NAME)
        addJavascriptInterface(OcClientInfoBridge(), OcClientInfoBridge.NAME)
        addJavascriptInterface(OcTelephonyBridge(), OcTelephonyBridge.NAME)
        addJavascriptInterface(OcUdcBridge(this), OcUdcBridge.NAME)
        addJavascriptInterface(OcAdvertisingIdBridge(this@MainActivity), OcAdvertisingIdBridge.NAME)
        addJavascriptInterface(OcAndroidIdBridge(this@MainActivity), OcAndroidIdBridge.NAME)
        addJavascriptInterface(OcAppPermissionsBridge(this), OcAppPermissionsBridge.NAME)
        addJavascriptInterface(OcFolsomBridge(), OcFolsomBridge.NAME)
        addJavascriptInterface(OcFidoU2fBridge(this), OcFidoU2fBridge.NAME)
        addJavascriptInterface(OcContactsBridge(this), OcContactsBridge.NAME)
        addJavascriptInterface(OcFilePickerBridge(this@MainActivity, this, executor), OcFilePickerBridge.NAME)
    }
}