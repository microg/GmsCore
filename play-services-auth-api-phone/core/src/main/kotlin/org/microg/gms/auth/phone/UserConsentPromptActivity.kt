/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.auth.phone

import android.annotation.TargetApi
import android.content.Intent
import android.os.*
import android.text.Html
import android.view.Gravity
import android.view.ViewGroup.LayoutParams
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.phone.SmsRetriever
import org.microg.gms.ui.buildAlertDialog
import org.microg.gms.utils.getApplicationLabel

private const val TAG = "UserConsentPrompt"

class UserConsentPromptActivity : AppCompatActivity() {
    private val messenger: Messenger?
        get() = intent.getParcelableExtra(EXTRA_MESSENGER)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val callingPackage = callingActivity?.packageName ?: return finish()
        val messenger = messenger ?: return finish()
        messenger.send(Message.obtain().apply {
            what = MSG_REQUEST_MESSAGE_BODY
            replyTo = Messenger(object : Handler(Looper.getMainLooper()) {
                override fun handleMessage(msg: Message) {
                    if (msg.what == MSG_REQUEST_MESSAGE_BODY) {
                        val message = msg.data.getString("message") ?: return
                        showConsentDialog(callingPackage, message)
                    }
                }
            })
        })
    }

    @TargetApi(16)
    private fun showConsentDialog(callingPackage: String, message: String) {
        val view = layoutInflater.inflate(R.layout.dialog_sms_user_consent, null)
        val dialog = buildAlertDialog()
            .setCancelable(false)
            .setView(view)
            .create()
        val appName = packageManager.getApplicationLabel(callingPackage)

        view.findViewById<TextView>(android.R.id.title).text = Html.fromHtml(getString(R.string.sms_user_consent_title, Html.escapeHtml(appName)))
        view.findViewById<TextView>(android.R.id.text1).text = message
        view.findViewById<Button>(android.R.id.button2).setOnClickListener {
            dialog.cancel()
        }
        dialog.setOnCancelListener {
            setResult(RESULT_CANCELED)
            finish()
        }
        view.findViewById<Button>(android.R.id.button1).setOnClickListener {
            dialog.dismiss()
            setResult(RESULT_OK, Intent().apply {
                putExtra(SmsRetriever.EXTRA_SMS_MESSAGE, message)
            })
            messenger?.send(Message.obtain().apply {
                what = MSG_CONSUME_MESSAGE
            })
            finish()
        }
        if (!dialog.isShowing) {
            dialog.window?.setGravity(Gravity.BOTTOM)
            dialog.window?.setLayout(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)
            dialog.show()
        }
    }
}