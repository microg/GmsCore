/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.wearable.consent

import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.R
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.api.internal.ConnectionCallbacks
import com.google.android.gms.common.api.internal.OnConnectionFailedListener
import com.google.android.gms.wearable.internal.AcceptTermsRequest
import com.google.android.material.button.MaterialButton
import org.microg.gms.wearable.BaseWearableCallbacks
import org.microg.gms.wearable.WearableClientImpl
import org.microg.gms.wearable.WearableTerms
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger



class TermsOfServiceActivity : AppCompatActivity() {

    private lateinit var terms: List<WearableTerms.TermDef>
    private val checked = HashSet<Int>()

    private var termsContext = WearableTerms.CONTEXT_UNSUPERVISED
    private var watchNodeId: String? = null
    private var accountName: String? = null
    private var perWatchConsents = false
    private var showBackup = false
    private var isLeDevice = false

    private lateinit var scroll: NestedScrollView
    private lateinit var buttonBar: View
    private var acceptButton: MaterialButton? = null
    private var declineButton: MaterialButton? = null

    private var atBottom = false
    private var submitting = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setResult(RESULT_CANCELED)

        termsContext = intent.getIntExtra(EXTRA_TERMS_CONTEXT, WearableTerms.CONTEXT_UNSUPERVISED)
        watchNodeId = intent.getStringExtra(EXTRA_WATCH_PEER_ID)
        accountName = intent.getStringExtra(EXTRA_ACCOUNT_NAME)
        perWatchConsents = intent.getBooleanExtra(EXTRA_USE_CONSENT_PER_WATCH, false)
        showBackup = intent.getBooleanExtra(EXTRA_SHOW_BACKUP_CONSENT, false)
        isLeDevice = intent.getBooleanExtra(EXTRA_IS_LE_DEVICE, false)

        if (termsContext != WearableTerms.CONTEXT_UNSUPERVISED) {
            Log.w(TAG, "Unsupported terms context $termsContext")
            finish()
            return
        }

        terms = WearableTerms.forContext(termsContext)
            ?.filter { it.termType != WearableTerms.CLOUDSYNC && (it.termType != WearableTerms.BACKUP || showBackup) }
            ?: run { finish(); return }

        savedInstanceState?.getIntArray(STATE_CHECKED)?.let { checked.addAll(it.toList()) }

        setContentView(R.layout.activity_wearable_tos)

        acceptButton = findViewById(R.id.terms_of_service_accept_button)
        declineButton = findViewById(R.id.terms_of_service_decline_button)
        scroll = findViewById(R.id.terms_of_service_scroll_container)
        buttonBar = acceptButton?.parent as View

        bindHeader()

        findViewById<RecyclerView>(R.id.terms_of_service_list).apply {
            layoutManager = LinearLayoutManager(this@TermsOfServiceActivity)
            isNestedScrollingEnabled = false
            adapter = TermsAdapter(terms.filter { it.termType != WearableTerms.TOS })
        }

        buttonBar.setBackgroundColor(themeColor(android.R.attr.colorBackground))
        scroll.clipToPadding = true
        scroll.viewTreeObserver.addOnGlobalLayoutListener {
            if (scroll.paddingBottom != buttonBar.height) {
                scroll.setPadding(0, 0, 0, buttonBar.height)
            }
            updateAcceptButton()
        }
        scroll.setOnScrollChangeListener(NestedScrollView.OnScrollChangeListener { _, _, _, _, _ -> updateAcceptButton() })
        acceptButton?.setOnClickListener {
            if (!atBottom) scroll.fullScroll(View.FOCUS_DOWN) else submit()
        }
        declineButton?.setOnClickListener {
            setResult(RESULT_CANCELED)
            finish()
        }

    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray(STATE_CHECKED, checked.toIntArray())
    }

    private fun bindHeader() {
        val pad = dp(24)
        findViewById<ImageView>(R.id.header_image).visibility = View.GONE
        findViewById<View>(R.id.header).setPadding(pad, pad, pad, 0)
        val tos = terms.firstOrNull { it.termType == WearableTerms.TOS }
        findViewById<TextView>(R.id.terms_of_service_title).apply {
            tos?.getTitle(this@TermsOfServiceActivity)?.let { text = it }
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 24f)
        }
        findViewById<TextView>(R.id.terms_of_service_subtitle).apply {
            setPadding(pad, dp(8), pad, dp(8))
            text = tos?.getDescription(this@TermsOfServiceActivity)
            visibility = if (text.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
    }

    private fun updateAcceptButton() {
        atBottom = !scroll.canScrollVertically(1)
        acceptButton?.setText(if (atBottom) R.string.wearable_tos_accept else R.string.wearable_tos_more)
    }

    private fun buildRequest(): AcceptTermsRequest {
        val accepted = ArrayList<Int>()
        for (term in terms) {
            if (!term.explicit || term.termType in checked) accepted.add(term.termType)
        }
        if (isLeDevice) accepted.add(WearableTerms.CLOUDSYNC)
        val skipped = if (showBackup && WearableTerms.BACKUP !in checked) listOf(WearableTerms.BACKUP) else null
        return AcceptTermsRequest(
            termsContext, accepted, null, null,
            watchNodeId, accountName, skipped, perWatchConsents
        )
    }

    private fun submit() {
        if (submitting) return
        submitting = true
        acceptButton?.isEnabled = false
        declineButton?.isEnabled = false
        val request = buildRequest()
        Thread {
            val status = try {
                acceptTermsBlocking(request)
            } catch (e: Exception) {
                Log.w(TAG, "acceptTerms failed", e)
                CommonStatusCodes.ERROR
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                if (status == CommonStatusCodes.SUCCESS) {
                    setResult(RESULT_OK)
                    finish()
                } else {
                    submitting = false
                    acceptButton?.isEnabled = true
                    declineButton?.isEnabled = true
                }
            }
        }.start()
    }

    private fun acceptTermsBlocking(request: AcceptTermsRequest): Int {
        val connected = CountDownLatch(1)
        val client = WearableClientImpl(
            this, null,
            object : ConnectionCallbacks {
                override fun onConnected(connectionHint: Bundle?) = connected.countDown()
                override fun onConnectionSuspended(cause: Int) = Unit
            },
            OnConnectionFailedListener {
                Log.w(TAG, "Connection failed: $it")
                connected.countDown()
            }
        )
        try {
            client.connect()
            if (!connected.await(10, TimeUnit.SECONDS) || !client.isConnected) {
                Log.w(TAG, "Could not connect to the wearable service")
                return CommonStatusCodes.ERROR
            }
            val code = AtomicInteger(CommonStatusCodes.ERROR)
            val latch = CountDownLatch(1)
            client.serviceInterface.acceptTerms(object : BaseWearableCallbacks() {
                override fun onStatus(status: Status) {
                    code.set(status.statusCode)
                    latch.countDown()
                }
            }, request)

            if (!latch.await(30, TimeUnit.SECONDS))
                return CommonStatusCodes.ERROR

            return code.get()
        } finally {
            client.disconnect()
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()

    private fun themeColor(attr: Int): Int {
        val value = TypedValue()
        theme.resolveAttribute(attr, value, true)
        return value.data
    }

    private inner class TermsAdapter(private val items: List<WearableTerms.TermDef>) :
        RecyclerView.Adapter<TermsAdapter.Holder>() {

        inner class Holder(
            row: View,
            val title: TextView,
            val description: TextView,
            val toggle: SwitchCompat
        ) : RecyclerView.ViewHolder(row)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
            val ctx = parent.context
            val title = TextView(ctx).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
                setTypeface(typeface, Typeface.BOLD)
            }
            val description = TextView(ctx).apply { setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f) }
            val texts = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
                addView(title)
                addView(description)
            }
            val toggle = SwitchCompat(ctx).apply {
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginStart = dp(16) }
            }
            val row = LinearLayout(ctx).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(24), dp(12), dp(24), dp(12))
                layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                addView(texts)
                addView(toggle)
            }
            return Holder(row, title, description, toggle)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val term = items[position]
            val title = term.getTitle(this@TermsOfServiceActivity)
            holder.title.text = title
            holder.title.visibility = if (title.isNullOrEmpty()) View.GONE else View.VISIBLE
            holder.description.text = term.getDescription(this@TermsOfServiceActivity)
            holder.toggle.setOnCheckedChangeListener(null)
            holder.toggle.visibility = if (term.explicit) View.VISIBLE else View.GONE
            holder.toggle.isChecked = term.termType in checked
            holder.toggle.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) checked.add(term.termType) else checked.remove(term.termType)
            }
        }
    }


    companion object {
        private const val TAG = "TermsOfService"
        private const val STATE_CHECKED = "checked_terms"
        private const val EXTRA_TERMS_CONTEXT = "terms_context"
        private const val EXTRA_WATCH_PEER_ID = "watch_peer_id"
        private const val EXTRA_USE_CONSENT_PER_WATCH = "use_consent_per_watch"
        private const val EXTRA_ACCOUNT_NAME = "account_name"
        private const val EXTRA_SHOW_BACKUP_CONSENT = "show_backup_consent"
        private const val EXTRA_IS_LE_DEVICE = "is_le_device"
    }
}