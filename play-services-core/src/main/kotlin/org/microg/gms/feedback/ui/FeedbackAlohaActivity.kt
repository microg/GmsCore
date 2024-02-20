/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.feedback.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Dialog
import android.content.Context
import android.content.pm.PackageInfo
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.CheckBox
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.gms.R
import com.google.android.gms.feedback.ErrorReport
import org.microg.gms.common.Constants
import org.microg.gms.feedback.Screenshot
import org.microg.gms.profile.Build
import java.util.Locale

private const val TAG = "FeedbackAlohaActivity"

const val KEY_ERROR_REPORT = "error_report"

const val KEY_SCREENSHOT_FILEPATH = "screenshot_filepath"

private const val FEEDBACK_SUBMIT_URL = "https://feedback-pa.googleapis.com/v1/feedback/android:submit";

class FeedbackAlohaActivity : AppCompatActivity() {

    private var report: ErrorReport? = null
    private var token: String? = null
    private var screenShotBitmap: Bitmap? = null

    private lateinit var accounts: Array<Account>
    private lateinit var sendIv: ImageView
    private lateinit var screenshotCb: CheckBox
    private lateinit var senderSpinner: Spinner
    private lateinit var feedbackMsgEt: EditText
    private lateinit var screenshotIv: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.feedback_aloha_activity)
        initAccount()
        initView()
        initData()
    }

    private fun initAccount() {
        accounts = AccountManager.get(this).getAccountsByType(Constants.ACCOUNT_TYPE)
        if (accounts.isEmpty()) {
            val account = Account(getString(R.string.google_user), Constants.ACCOUNT_TYPE)
            accounts = arrayOf(account)
        }
    }

    private fun initData() {
        val errorBefore = intent.getParcelableExtra<ErrorReport>(KEY_ERROR_REPORT)
        screenShotBitmap = intent.getStringExtra(KEY_SCREENSHOT_FILEPATH)?.let { BitmapFactory.decodeFile(it) }
        Log.d(TAG, "screenShotBitmap is null? ${screenShotBitmap == null}")

        screenShotBitmap?.let { screenshotIv.setImageBitmap(it) }
        report = errorBefore?.apply {
            val telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
            networkOperatorName = telephonyManager.networkOperator
            languageTag = Locale.getDefault().language
            if (networkOperatorName.length > 3) {
                mobileCountryCode = networkOperatorName.substring(0, 3).toInt()
                mobileNetworkCode = networkOperatorName.substring(3).toInt();
            }
            packageName = applicationErrorReport?.packageName
            val packageInfo: PackageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.let {
                versionCode = packageInfo.versionCode
                versionName = packageInfo.versionName
            }
            mobileDevice = Build.DEVICE
            mobileDisplay = Build.DISPLAY
            mobileType = Build.TYPE
            mobileModel = Build.MODEL
            mobileProduct = Build.PRODUCT
            mobileFingerprint = Build.FINGERPRINT
            mobileSdkInt = Build.VERSION.SDK_INT
            mobileRelease = Build.VERSION.RELEASE
            mobileIncremental = Build.VERSION.INCREMENTAL
            mobileCodeName = Build.VERSION.CODENAME
            mobileBoard = Build.BOARD
            mobileBrand = Build.BRAND
            phoneType = telephonyManager.phoneType
        }
    }

    private fun initView() {
        feedbackMsgEt = findViewById(R.id.feedback_msg_et)
        sendIv = findViewById(R.id.send_iv)
        screenshotIv = findViewById(R.id.screenshot_iv)
        screenshotCb = findViewById(R.id.screenshot_cb)
        senderSpinner = findViewById(R.id.sender_spinner)

        val senderList: MutableList<String> = ArrayList()
        for (account in accounts) {
            val stringBuilder = StringBuilder()
            stringBuilder.append(getString(R.string.feedback_sender))
            stringBuilder.append(account.name)
            senderList.add(stringBuilder.toString())
        }
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, senderList)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        senderSpinner.adapter = adapter

        sendIv.setOnClickListener { _ ->
            if (report == null) {
                return@setOnClickListener
            }
            if (feedbackMsgEt.text.toString().trim { it <= ' ' }.isEmpty()) {
                Toast.makeText(this, R.string.feedback_msg_empty_hint, Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (screenshotCb.isChecked && screenShotBitmap != null) {
                val sc: Screenshot = Screenshot.encodeBitmapToScreenshot(screenShotBitmap)
                Screenshot.setScreenshotToErrorReport(report, sc)
            } else {
                report?.bitmap = null
            }
            setInputData(report, feedbackMsgEt.text.toString().trim { it <= ' ' })

            val account = accounts[senderSpinner.selectedItemPosition]
            Log.d(TAG, "select account: $account")
            goFeedbackAsync(applicationContext)
        }

        findViewById<View>(R.id.close_iv).setOnClickListener { _ -> finish() }
        findViewById<View>(R.id.screenshot_card_view).setOnClickListener { _ ->
            screenShotBitmap?.run { showScreenshot(this) }
        }
    }

    private fun showScreenshot(bitmap: Bitmap) {
        val dialog = Dialog(this)
        val view = LayoutInflater.from(this).inflate(R.layout.feedback_screenshot_dialog, null)
        val screenshotIv = view.findViewById<ImageView>(R.id.screenshot_iv)
        screenshotIv.setImageBitmap(bitmap)
        dialog.setContentView(view)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.show()
    }

    private fun setInputData(errorReport: ErrorReport?, feedbackMsg: String) {
        val bundleText = Bundle()
        bundleText.putLong("max_char_count_delta", feedbackMsg.length.toLong())
        bundleText.putLong("input_start_time", System.currentTimeMillis())
        bundleText.putLong("input_end_time", System.currentTimeMillis() + 1000)
        errorReport?.feedbackMsg = feedbackMsg
        errorReport?.bundleText = bundleText
    }

    private fun goFeedbackAsync(mContext: Context?) {
        val request: StringRequest = object : StringRequest(
            Method.POST, FEEDBACK_SUBMIT_URL,
            Response.Listener {
                Log.d(TAG, "submit success")
            }, Response.ErrorListener { error ->
                Log.d(TAG, "submit error：$error")
            }) {

            override fun getHeaders(): Map<String, String> {
                return mutableMapOf<String, String>().apply {
                    "user-agent" to "AndroidGoogleFeedback/1.1 (coral SQ3A.220705.003.A1)"
                    "authorization" to token
                    "x-android-package" to Constants.GMS_PACKAGE_NAME
                    "content-type" to "application/x-protobuf"
                    "x-goog-api-key" to "AIzaSyAP-gfH3qvi6vgHZbSYwQ_XHqV_mXHhzIk"
                    "content-encoding" to "gzip"
                    "x-android-cert" to Constants.GMS_PACKAGE_SIGNATURE_SHA1
                }
            }

            override fun getBody(): ByteArray? {
                // build body by report
                return null
            }
        }
        Volley.newRequestQueue(this).add(request)
        Toast.makeText(mContext, R.string.thanks_your_feedback, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        screenShotBitmap?.recycle()
        screenShotBitmap = null
        super.onDestroy()
    }

}
