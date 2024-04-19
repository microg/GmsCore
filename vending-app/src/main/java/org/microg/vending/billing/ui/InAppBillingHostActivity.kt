/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.vending.billing.ui

import android.accounts.Account
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Rect
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.Window
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.BillingClient
import org.microg.vending.billing.ui.logic.NotificationEventId
import org.microg.vending.billing.ui.logic.InAppBillingViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.vending.billing.ADD_PAYMENT_METHOD_URL
import org.microg.vending.billing.TAG

private const val ADD_PAYMENT_REQUEST_CODE = 30002

@RequiresApi(21)
class InAppBillingHostActivity : ComponentActivity() {
    private val inAppBillingViewModel by viewModels<InAppBillingViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "InAppBillingHostActivity.onCreate")
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        lifecycleScope.launchWhenCreated {
            initEventHandler()
        }
        loadView(savedInstanceState != null)
    }

    private fun loadView(isRebuild: Boolean) {
        if (!isRebuild) {
            inAppBillingViewModel.startParams = intent.extras
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    inAppBillingViewModel.loadData(this@InAppBillingHostActivity)
                } catch (e: Exception) {
                    if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "loadSheetUIViewData", e)
                    withContext(Dispatchers.Main) {
                        finishWithResult(
                            bundleOf(
                                "RESPONSE_CODE" to BillingClient.BillingResponseCode.DEVELOPER_ERROR,
                                "DEBUG_MESSAGE" to "init ui failed"
                            )
                        )
                    }
                }
            }
        }
        initWindow()
        setContent { BillingUiPage(viewModel = inAppBillingViewModel) }
    }

    private suspend fun initEventHandler() {
        inAppBillingViewModel.event.collect {
            when (it.id) {
                NotificationEventId.FINISH -> finishWithResult(it.params)
                NotificationEventId.OPEN_PAYMENT_METHOD_ACTIVITY -> {
                    val account = it.params.getParcelable<Account>("account")
                    val src = it.params.getString("src")
                    openPaymentMethodActivity(src, account)
                }
            }
        }
    }

    private fun initWindow() {
        val lp = window.attributes
        lp.width = getWindowWidth(this)
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT
        lp.gravity = Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL
        window.attributes = lp
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
    }

    private fun openPaymentMethodActivity(src: String?, account: Account?) {
        val intent = Intent(this, PlayWebViewActivity::class.java)
        intent.putExtra(KEY_WEB_VIEW_ACTION, WebViewAction.ADD_PAYMENT_METHOD.toString())
        intent.putExtra(KEY_WEB_VIEW_OPEN_URL, ADD_PAYMENT_METHOD_URL)
        account?.let {
            intent.putExtra(KEY_WEB_VIEW_ACCOUNT, account)
        }
        startActivityForResult(intent, ADD_PAYMENT_REQUEST_CODE)
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        return when (event?.action) {
            MotionEvent.ACTION_UP -> {
                val r = Rect(0, 0, 0, 0)
                this.window.decorView.getHitRect(r)
                val intersects: Boolean = r.contains(event.x.toInt(), event.y.toInt())
                if (!intersects) {
                    inAppBillingViewModel.close()
                    return true
                }
                super.onTouchEvent(event)
            }

            else -> super.onTouchEvent(event)
        }
    }

    private fun finishWithResult(result: Bundle) {
        if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "InAppBillingHostActivity.finishWithResult $result")
        val resultIntent = Intent()
        resultIntent.putExtras(result)
        setResult(RESULT_OK, resultIntent)
        finish()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ADD_PAYMENT_REQUEST_CODE -> {
                if (Log.isLoggable(TAG, Log.DEBUG)) Log.d(TAG, "add payment method resultCode: $resultCode, data: $data")
                loadView(false)
            }

            else -> {
                super.onActivityResult(requestCode, resultCode, data)
                finishWithResult(
                    bundleOf(
                        "RESPONSE_CODE" to BillingClient.BillingResponseCode.USER_CANCELED
                    )
                )
            }
        }
    }
}