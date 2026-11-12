/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.MotionEvent
import android.widget.Button
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.R
import org.microg.gms.wearable.consent.WearableConsentStore

/** Records only an explicit, global terms choice. Optional service consents remain disabled. */
class TermsOfServiceActivity : AppCompatActivity() {
    private var decision: WearableTermsConsentDecision? = null
    private var store: WearableConsentStore? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)
        // A restored view or result must not turn into a new consent decision.
        if (savedInstanceState != null) { finish(); return }
        try {
            // Android supplies callingPackage for startActivityForResult. Never trust a package extra.
            val caller = WearableTermsConsentCaller.capture(this, requireNotNull(callingPackage))
            WearableTermsConsentRequest.validate(intent)
            val currentStore = WearableConsentStore(this)
            store = currentStore
            decision = WearableTermsConsentDecision({ caller.enforceCurrent(this) }, { guard -> currentStore.acceptTerms(guard) })
        } catch (_: Exception) { finish(); return }

        title = getString(R.string.wearable_terms_title)
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            filterTouchesWhenObscured = true
        }
        fun label(resource: Int) = TextView(this).also {
            it.setText(resource)
            it.setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            layout.addView(it)
        }
        label(R.string.wearable_terms_title).textSize = 22f
        label(R.string.wearable_terms_description)
        fun link(resource: Int, url: String) {
            layout.addView(Button(this).apply {
                setText(resource)
                filterTouchesWhenObscured = true
                setOnClickListener {
                    try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
                    catch (_: Exception) { Toast.makeText(this@TermsOfServiceActivity, R.string.wearable_terms_link_error, Toast.LENGTH_LONG).show() }
                }
            })
        }
        link(R.string.wearable_terms_google_terms, "https://policies.google.com/terms")
        link(R.string.wearable_terms_google_privacy, "https://policies.google.com/privacy")
        label(R.string.wearable_terms_optional_services)
        val confirm = Button(this).apply {
            setText(R.string.wearable_terms_accept)
            isEnabled = false
            filterTouchesWhenObscured = true
        }
        val selected = CheckBox(this).apply {
            setText(R.string.wearable_terms_choice)
            isChecked = false
            isSaveEnabled = false
            filterTouchesWhenObscured = true
            setOnCheckedChangeListener { _, checked -> confirm.isEnabled = checked }
        }
        layout.addView(selected)
        confirm.setOnClickListener {
            confirm.isEnabled = false
            selected.isEnabled = false
            if (decision?.accept(selected.isChecked) == true) setResult(RESULT_OK)
            else Toast.makeText(this, R.string.wearable_terms_save_error, Toast.LENGTH_LONG).show()
            finish()
        }
        layout.addView(confirm)
        layout.addView(Button(this).apply {
            setText(android.R.string.cancel)
            filterTouchesWhenObscured = true
            setOnClickListener { decision?.cancel(); finish() }
        })
        setContentView(ScrollView(this).apply { addView(layout) })
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0 ||
            (Build.VERSION.SDK_INT >= 29 && event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0)) return false
        return super.dispatchTouchEvent(event)
    }

    override fun onDestroy() {
        decision?.cancel()
        store?.close()
        super.onDestroy()
    }
}
