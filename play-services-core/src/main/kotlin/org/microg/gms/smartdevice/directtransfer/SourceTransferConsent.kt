/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.app.Activity
import android.app.KeyguardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import com.google.android.gms.R
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

/** Same-process capabilities. No result, account or authorization is accepted from Intent extras. */
internal object SourceTransferConsent {
    private val requests = ConcurrentHashMap<String, Request>()

    internal class Request(
        val accounts: List<Account>,
        private val checkCurrent: () -> Unit,
        private val finish: (Account, Long) -> Unit,
        private val reject: () -> Unit,
        /** Name the watch announced, already stripped of control and bidi characters. */
        val targetName: String? = null
    ) {
        val token: String = UUID.randomUUID().toString()
        private val claimed = AtomicBoolean()
        private val credentialConsumed = AtomicBoolean()
        private val terminal = AtomicBoolean()
        private var selected: Account? = null
        private var credentialChallenge: String? = null
        private var terminalObserver: (() -> Unit)? = null
        private var verificationObserver: ((SourceTransferWebRequest) -> Unit)? = null
        private var verification: SourceTransferWebRequest? = null

        @Synchronized
        fun observeVerification(observer: ((SourceTransferWebRequest) -> Unit)?) {
            verificationObserver = observer
        }

        fun showVerification(request: SourceTransferWebRequest) {
            val observer = synchronized(this) {
                check(!terminal.get() && credentialConsumed.get() && verification == null)
                checkCurrent()
                request.checkCurrent()
                check(request.authorization.account == selected)
                requireNotNull(verificationObserver).also { verification = request }
            }
            observer(request)
        }

        fun claim(): Boolean = claimed.compareAndSet(false, true).also { if (it) check() }
        fun check() { check(!terminal.get() && !credentialConsumed.get()); checkCurrent() }
        fun isTerminal() = terminal.get()

        @Synchronized
        fun register() {
            check(!terminal.get() && !credentialConsumed.get())
            check(requests.putIfAbsent(token, this) == null)
        }

        /** The observer is invoked outside the monitor, including a terminal-before-attach race. */
        fun observeTerminal(observer: () -> Unit) {
            val alreadyTerminal = synchronized(this) {
                check(terminalObserver == null)
                if (terminal.get()) true else { terminalObserver = observer; false }
            }
            if (alreadyTerminal) notifyTerminal(observer)
        }

        @Synchronized
        fun removeTerminalObserver(observer: () -> Unit) {
            if (terminalObserver === observer) terminalObserver = null
        }

        @Synchronized
        fun beginCredentialConfirmation(index: Int): String {
            check()
            check(credentialChallenge == null && index in accounts.indices)
            selected = accounts[index]
            return UUID.randomUUID().toString().also { credentialChallenge = it }
        }

        fun credentialConfirmed(challenge: String) {
            check()
            val account = synchronized(this) {
                check(!terminal.get() && credentialChallenge == challenge)
                val account = requireNotNull(selected)
                check(credentialConsumed.compareAndSet(false, true))
                requests.remove(token, this)
                account
            }
            // Consuming the PIN does not finish the transfer. Cancellation must remain possible
            // while enrollment and the encrypted exchange are still running.
            try { finish(account, SystemClock.elapsedRealtime()) } catch (error: Exception) { cancel(); throw error }
        }

        fun cancel() = end(cancelled = true)
        fun invalidate() = end(cancelled = false)

        private fun end(cancelled: Boolean) {
            val observer = synchronized(this) {
                if (!terminal.compareAndSet(false, true)) return
                requests.remove(token, this)
                verification?.cancel()
                verification = null
                verificationObserver = null
                terminalObserver.also { terminalObserver = null }
            }
            try { if (cancelled) reject() }
            finally { if (observer != null) notifyTerminal(observer) }
        }

        private fun notifyTerminal(observer: () -> Unit) {
            // A stale window or failed UI dispatch must not prevent session cleanup.
            try { observer() } catch (_: Exception) { }
        }

        override fun toString() = "SourceTransferConsent.Request(redacted)"
    }

    fun add(request: Request) = request.register()

    fun claim(token: String?): Request? {
        if (token == null || token.length > 64) return null
        val request = requests[token] ?: return null
        return try { request.takeIf { it.claim() } } catch (_: Exception) { request.cancel(); null }
    }
}

/** Runs in :persistent with SourceDirectTransferService; deliberately not exported. */
class SourceTransferConsentActivity : Activity() {
    private var request: SourceTransferConsent.Request? = null
    private var credentialChallenge: String? = null
    private var awaitingCredential = false
    private var webFlow: SourceTransferWebFlow? = null
    private val onTerminal: () -> Unit = { runOnUiThread { webFlow?.close(); webFlow = null; if (!isFinishing) finish() } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        // Process death or activity recreation cannot resurrect a previously confirmed capability.
        if (savedInstanceState != null) { finish(); return }
        val current = SourceTransferConsent.claim(intent?.getStringExtra(EXTRA_TOKEN))
        if (current == null) { finish(); return }
        request = current
        current.observeVerification { verification -> runOnUiThread {
            try {
                check(!current.isTerminal() && !isFinishing)
                val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
                layout.addView(TextView(this).apply { setText(R.string.direct_transfer_verification_body) })
                webFlow = SourceTransferWebFlow(this, verification, layout) {
                    webFlow?.close()
                    webFlow = null
                    showProgress(current)
                }
                layout.addView(Button(this).apply {
                    setText(android.R.string.cancel)
                    setOnClickListener { current.cancel(); finish() }
                })
                setContentView(layout)
                webFlow?.start()
            } catch (error: Exception) {
                verification.fail((error as? SourceTransferProtocolException)?.reason ?: SourceTransferDiagnosticReason.WEB_LOAD)
                webFlow?.close()
                webFlow = null
                // Leave the existing progress window until the session reports its failure.
            }
        } }
        current.observeTerminal(onTerminal)
        if (current.isTerminal() || isFinishing) { finish(); return }
        val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
        if (Build.VERSION.SDK_INT < 23 || !manager.isDeviceSecure) { current.cancel(); finish(); return }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            filterTouchesWhenObscured = true
        }
        fun label(id: Int) = TextView(this).also {
            it.setText(id)
            it.setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            layout.addView(it)
        }
        label(R.string.direct_transfer_consent_title).textSize = 22f
        label(R.string.direct_transfer_consent_body)
        current.targetName?.let { name ->
            layout.addView(TextView(this).also {
                it.text = getString(R.string.direct_transfer_consent_target, name)
                it.setPadding(0, 0, 0, (16 * resources.displayMetrics.density).toInt())
            })
        }
        val accounts = Spinner(this).apply {
            filterTouchesWhenObscured = true
            adapter = ArrayAdapter(this@SourceTransferConsentActivity, android.R.layout.simple_spinner_dropdown_item,
                current.accounts.map { it.name })
        }
        layout.addView(accounts, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        val confirm = Button(this).apply {
            setText(R.string.direct_transfer_consent_confirm)
            filterTouchesWhenObscured = true
            setOnClickListener {
                try {
                    current.check()
                    val challenge = current.beginCredentialConfirmation(accounts.selectedItemPosition)
                    val intent = manager.createConfirmDeviceCredentialIntent(
                        getString(R.string.direct_transfer_consent_title),
                        getString(R.string.direct_transfer_credential_body))
                    check(intent != null) { "Device confirmation unavailable" }
                    credentialChallenge = challenge
                    awaitingCredential = true
                    isEnabled = false
                    accounts.isEnabled = false
                    startActivityForResult(intent, CREDENTIAL_REQUEST)
                } catch (_: Exception) { current.cancel(); finish() }
            }
        }
        layout.addView(confirm)
        layout.addView(Button(this).apply {
            setText(android.R.string.cancel)
            filterTouchesWhenObscured = true
            setOnClickListener { current.cancel(); finish() }
        })
        setContentView(layout)
    }

    override fun dispatchTouchEvent(event: MotionEvent): Boolean {
        if (event.flags and MotionEvent.FLAG_WINDOW_IS_OBSCURED != 0 ||
            (Build.VERSION.SDK_INT >= 29 && event.flags and MotionEvent.FLAG_WINDOW_IS_PARTIALLY_OBSCURED != 0)) return false
        return super.dispatchTouchEvent(event)
    }

    @Deprecated("Platform credential confirmation callback")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != CREDENTIAL_REQUEST || !awaitingCredential) return
        awaitingCredential = false
        val current = request ?: return finish()
        try {
            check(resultCode == RESULT_OK)
            val manager = getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            check(Build.VERSION.SDK_INT >= 23 && manager.isDeviceSecure && !manager.isDeviceLocked)
            current.credentialConfirmed(requireNotNull(credentialChallenge))
            if (!current.isTerminal() && !isFinishing) showProgress(current)
        } catch (_: Exception) { current.cancel(); finish() }
    }

    private fun showProgress(current: SourceTransferConsent.Request) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (24 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
            filterTouchesWhenObscured = true
        }
        layout.addView(TextView(this).apply {
            setText(R.string.direct_transfer_progress_title)
            textSize = 22f
        })
        layout.addView(ProgressBar(this).apply { isIndeterminate = true })
        layout.addView(TextView(this).apply { setText(R.string.direct_transfer_progress_body) })
        layout.addView(Button(this).apply {
            setText(android.R.string.cancel)
            filterTouchesWhenObscured = true
            setOnClickListener { current.cancel(); finish() }
        })
        // The companion treats a return to its account screen before a transfer result as cancel.
        // Keep this window until the session signals its real terminal state, never a fixed delay.
        if (!current.isTerminal() && !isFinishing) setContentView(layout)
    }

    override fun onDestroy() {
        // Cancellation is fail-closed on rotation/process recreation rather than restoring a PIN result.
        request?.removeTerminalObserver(onTerminal)
        request?.observeVerification(null)
        webFlow?.close()
        webFlow = null
        request?.cancel()
        request = null
        super.onDestroy()
    }

    companion object {
        internal const val EXTRA_TOKEN = "org.microg.gms.smartdevice.directtransfer.CONSENT"
        private const val CREDENTIAL_REQUEST = 701
    }
}
