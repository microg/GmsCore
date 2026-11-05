/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.accounts.AccountManager
import android.app.KeyguardManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import android.os.UserManager
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeout
import okio.ByteString
import org.microg.gms.cryptauth.CryptAuthBootstrapAssertion
import org.microg.gms.cryptauth.CryptAuthBootstrapAuthorization
import java.io.Closeable
import java.util.concurrent.Executor
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/** One phone-to-watch transfer. The companion's two FDs and verified caller cannot be replaced. */
internal class SourceTransferSession(
    private val context: Context,
    private val caller: SourceTransferCaller,
    private val restrictions: SourceTransferRestrictions,
    private val inputDescriptor: ParcelFileDescriptor,
    private val outputDescriptor: ParcelFileDescriptor,
    private val listener: SourceTransferListener,
    private val callback: SourceTransferCallback,
    private val callbacks: Executor,
    private val scheduler: ScheduledExecutorService,
    private val sourceAndroidDeviceId: () -> Long,
    private val bootstrapInfo: suspend (CryptAuthBootstrapAuthorization) -> SourceTransferBootstrapInfo,
    private val assertion: suspend (CryptAuthBootstrapAuthorization, ByteString, ByteString) -> CryptAuthBootstrapAssertion,
    private val onClosed: (SourceTransferSession) -> Unit
) : Closeable {
    private val createdAt = SystemClock.elapsedRealtime()
    private val expiresAt = createdAt + SESSION_TIMEOUT
    private val closed = AtomicBoolean()
    private val terminal = AtomicBoolean()
    private val diagnosticRecorded = AtomicBoolean()
    @Volatile private var diagnosticPhase = SourceTransferDiagnosticPhase.START
    private val remoteSessionId = AtomicReference<Long?>()
    private val peer = AtomicReference<ByteString?>()
    private val authorization = AtomicReference<Authorization?>()
    private val consent = AtomicReference<SourceTransferConsent.Request?>()
    private val consentResult = CompletableDeferred<CryptAuthBootstrapAuthorization>()
    private val io = SourceTransferIo(inputDescriptor, outputDescriptor)
    private val timer = AtomicReference<ScheduledFuture<*>?>()
    private val death = IBinder.DeathRecipient { fail(ERROR_CANCELLED) }
    private val transport = SourceTransferTransport(idleTimeoutMillis = SESSION_TIMEOUT, verifyPeer = { verification ->
        checkCurrent()
        // Only the pinned companion may provide these endpoint FDs. Bind the actual UKEY2
        // transcript to this exact session; it is never an authorization to release an account.
        check(verification.size == 32 && peer.compareAndSet(null, verification))
        true
    })

    fun start(executor: Executor) {
        try {
            checkCurrent()
            callback.asBinder().linkToDeath(death, 0)
            checkLinked(callback.asBinder())
            listener.asBinder().linkToDeath(death, 0)
            checkLinked(listener.asBinder())
            val expiry = scheduler.scheduleAtFixedRate({
                try {
                    checkCurrent()
                    io.enforceDeadline(SystemClock.elapsedRealtime())
                    if (transport.state != SourceTransferTransport.State.NEW) transport.checkTimeout(SystemClock.elapsedRealtime())
                }
                catch (error: Exception) { noteFailure(error); fail(ERROR_TIMEOUT) }
            }, 1, 1, TimeUnit.SECONDS)
            check(timer.compareAndSet(null, expiry))
            if (closed.get()) { timer.getAndSet(null)?.cancel(false); checkCurrent() }
            executor.execute { try { io.run { execute() } } catch (error: Exception) { noteFailure(error); fail(ERROR_CANCELLED) } }
            // SUCCESS means the start operation was accepted, not that an account was copied.
            dispatch { callback.onStatus(0) }
        } catch (error: Exception) {
            noteFailure(error)
            dispatch { callback.onStatus(8) }
            fail(ERROR_CANCELLED)
        }
    }

    fun matchesSession(id: Long) = !closed.get() && (id == -1L || remoteSessionId.get() == id)
    fun cancel() = fail(ERROR_CANCELLED)

    private fun checkLinked(binder: IBinder) {
        if (closed.get()) {
            try { binder.unlinkToDeath(death, 0) } catch (_: Exception) { }
            checkCurrent()
        }
    }

    fun checkCurrent() {
        val now = SystemClock.elapsedRealtime()
        diagnosed(SourceTransferDiagnosticReason.SESSION_NOT_CURRENT) {
            check(!closed.get() && now >= createdAt && now < expiresAt) { "Direct-transfer session expired" }
        }
        diagnosed(SourceTransferDiagnosticReason.CALLER_CHANGED) { caller.enforceInstalled(context) }
        diagnosed(SourceTransferDiagnosticReason.TRANSPORT_CLOSED) {
            check(inputDescriptor.fileDescriptor.valid() && outputDescriptor.fileDescriptor.valid()) { "Transfer transport closed" }
        }
        diagnosed(SourceTransferDiagnosticReason.CALLER_DISCONNECTED) {
            check(callback.asBinder().isBinderAlive && listener.asBinder().isBinderAlive) { "Transfer caller disconnected" }
        }
    }

    private suspend fun execute() {
        var protocol: SourceTransferProtocol? = null
        var completionQueued = false
        try {
            checkCurrent()
            diagnosticPhase = SourceTransferDiagnosticPhase.HANDSHAKE
            writeFrame(transport.begin(SystemClock.elapsedRealtime()))
            while (transport.state != SourceTransferTransport.State.ENCRYPTED) {
                val frame = io.read(HANDSHAKE_MAXIMUM)
                try { transport.receiveHandshake(frame, SystemClock.elapsedRealtime())?.let(::writeFrame) }
                finally { frame.fill(0) }
            }
            diagnosticPhase = SourceTransferDiagnosticPhase.OPTIONS
            val verification = requireNotNull(peer.get())
            protocol = SourceTransferProtocol(verification, object : SourceTransferProtocol.Hooks {
                override fun checkCurrent() = this@SourceTransferSession.checkCurrent()
                override fun sourceAndroidDeviceId() = sourceAndroidDeviceId.invoke()
                override suspend fun requestConsent(sessionId: Long, peerVerification: ByteString, targetDisplayName: String?): List<CryptAuthBootstrapAuthorization> {
                    diagnosticPhase = SourceTransferDiagnosticPhase.CONSENT
                    return authorize(sessionId, peerVerification, targetDisplayName)
                }
                override suspend fun getBootstrapInfo(auth: CryptAuthBootstrapAuthorization): SourceTransferBootstrapInfo {
                    diagnosticPhase = SourceTransferDiagnosticPhase.ENROLLMENT
                    return boundedOperation { bootstrapInfo(auth) }
                }
                override suspend fun createAssertion(auth: CryptAuthBootstrapAuthorization, expectedPublicKey: ByteString,
                                                     challenge: ByteString): CryptAuthBootstrapAssertion {
                    diagnosticPhase = SourceTransferDiagnosticPhase.ASSERTION
                    return boundedOperation { assertion(auth, expectedPublicKey, challenge) }
                }
                override suspend fun verifyCredentials(auth: CryptAuthBootstrapAuthorization, fallbackUrl: String): String {
                    diagnosticPhase = SourceTransferDiagnosticPhase.VERIFICATION
                    auth.enforceCurrent()
                    val request = SourceTransferWebRequest(auth, fallbackUrl)
                    return try {
                        requireNotNull(consent.get()).showVerification(request)
                        boundedOperation { request.result.await() }.also { auth.enforceCurrent() }
                    } finally { request.cancel() }
                }
                override suspend fun sendPayload(bytes: ByteArray) {
                    diagnosticPhase = if (protocol?.state == SourceTransferProtocol.State.FINISH)
                        SourceTransferDiagnosticPhase.FINAL_ACK else SourceTransferDiagnosticPhase.WRITE
                    checkCurrent()
                    try { writeFrame(transport.encode(bytes, SystemClock.elapsedRealtime())) }
                    finally { bytes.fill(0) }
                }
            })
            val messageProtocol = requireNotNull(protocol)
            while (!closed.get()) {
                diagnosticPhase = when (messageProtocol.state) {
                    SourceTransferProtocol.State.OPTIONS -> SourceTransferDiagnosticPhase.OPTIONS
                    SourceTransferProtocol.State.CONSENT -> SourceTransferDiagnosticPhase.CONSENT
                    SourceTransferProtocol.State.CHALLENGE -> SourceTransferDiagnosticPhase.CHALLENGE
                    SourceTransferProtocol.State.RESULTS -> SourceTransferDiagnosticPhase.RESULTS
                    SourceTransferProtocol.State.VERIFICATION -> SourceTransferDiagnosticPhase.VERIFICATION
                    SourceTransferProtocol.State.FINISH -> SourceTransferDiagnosticPhase.FINAL_ACK
                    else -> SourceTransferDiagnosticPhase.COMPLETION
                }
                val frame = io.read()
                val payload = try { transport.decode(frame, SystemClock.elapsedRealtime()) } finally { frame.fill(0) }
                val results = try { messageProtocol.receive(payload) } finally { payload.fill(0) }
                if (results != null) {
                    checkCurrent()
                    val approved = requireNotNull(authorization.get())
                    approved.enforceCurrent()
                    check(results.size == 1 && results.single().account == approved.account) { "Unexpected transfer result account" }
                    diagnosticPhase = SourceTransferDiagnosticPhase.COMPLETION
                    completionQueued = true
                    dispatch {
                        try {
                            checkCurrent()
                            approved.enforceCurrent()
                            // Cancellation and completion race at this single terminal transition.
                            if (terminal.compareAndSet(false, true)) listener.onComplete(results)
                        } catch (error: Exception) { noteFailure(error); fail(ERROR_TRANSFER) }
                        finally { close() }
                    }
                    return
                }
            }
        } catch (error: Exception) {
            noteFailure(error)
            fail(ERROR_TRANSFER)
        } finally {
            protocol?.close()
            if (!completionQueued) close()
        }
    }

    private suspend fun <T> boundedOperation(block: suspend () -> T): T {
        checkCurrent()
        return withTimeout(minOf(OPERATION_TIMEOUT, expiresAt - SystemClock.elapsedRealtime())) { block() }
    }

    private suspend fun authorize(sessionId: Long, verification: ByteString, targetName: String?): List<CryptAuthBootstrapAuthorization> {
        checkCurrent()
        diagnosed(SourceTransferDiagnosticReason.CONSENT_OPTIONS) {
            check(sessionId != 0L && sessionId != -1L && remoteSessionId.compareAndSet(null, sessionId)) { "Repeated transfer options" }
        }
        diagnosed(SourceTransferDiagnosticReason.CONSENT_PEER) {
            check(peer.get() == verification && transport.verificationString == verification) { "Transfer peer changed" }
        }
        diagnosed(SourceTransferDiagnosticReason.CONSENT_CREDENTIAL) {
            val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            check(Build.VERSION.SDK_INT >= 23 && manager.isDeviceSecure) { "Device credential required" }
        }
        diagnosed(SourceTransferDiagnosticReason.CONSENT_PROFILE) {
            val users = context.getSystemService(Context.USER_SERVICE) as UserManager
            if (Build.VERSION.SDK_INT >= 24) check(users.isUserUnlocked && !users.isManagedProfile) { "Unsupported user profile" }
        }
        val localAccounts = diagnosed(SourceTransferDiagnosticReason.CONSENT_ACCOUNT_QUERY) {
            AccountManager.get(context).getAccountsByType("com.google")
        }
        diagnosed(SourceTransferDiagnosticReason.CONSENT_NO_LOCAL_ACCOUNT) {
            check(localAccounts.isNotEmpty()) { "No eligible local account" }
        }
        val accounts = localAccounts.filter(restrictions::allows).take(MAX_ACCOUNTS).map { Account(it.name, it.type) }
        diagnosed(SourceTransferDiagnosticReason.CONSENT_NO_ELIGIBLE_ACCOUNT) {
            check(accounts.isNotEmpty()) { "No eligible local account" }
        }
        val request = SourceTransferConsent.Request(accounts, ::checkCurrent, { account, confirmedAt ->
            checkCurrent()
            diagnosed(SourceTransferDiagnosticReason.CONSENT_SELECTION) {
                check(accounts.contains(account) && restrictions.allows(account)) { "Account selection changed" }
            }
            val result = Authorization(Account(account.name, account.type), sessionId, verification, confirmedAt)
            result.enforceCurrent()
            diagnosed(SourceTransferDiagnosticReason.CONSENT_ALREADY_AUTHORIZED) {
                check(authorization.compareAndSet(null, result)) { "Account already authorized" }
            }
            consentResult.complete(result)
        }, { consentResult.cancel(); fail(ERROR_CANCELLED) }, targetName)
        diagnosed(SourceTransferDiagnosticReason.CONSENT_REGISTRATION) { check(consent.compareAndSet(null, request)) }
        var pending: PendingIntent? = null
        return try {
            diagnosed(SourceTransferDiagnosticReason.CONSENT_REGISTRATION) { SourceTransferConsent.add(request) }
            checkCurrent()
            val intent = Intent(context, SourceTransferConsentActivity::class.java)
                .setData(Uri.parse("microg://direct-transfer-consent/${request.token}"))
                .putExtra(SourceTransferConsentActivity.EXTRA_TOKEN, request.token)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val consentIntent = diagnosed(SourceTransferDiagnosticReason.CONSENT_INTENT) {
                PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE)
            }
            pending = consentIntent
            dispatch {
                checkCurrent()
                diagnosed(SourceTransferDiagnosticReason.CONSENT_CALLBACK) { listener.onConsentRequired(consentIntent) }
            }
            val result = diagnosed(SourceTransferDiagnosticReason.CONSENT_RESULT) {
                withTimeout(minOf(CONSENT_TIMEOUT, expiresAt - SystemClock.elapsedRealtime())) { consentResult.await() }
            }
            result.enforceCurrent()
            listOf(result)
        // The PIN capability is consumed, but its UI remains the cancellable progress window.
        // Only close() ends the request and lets the companion resume after a terminal result.
        } finally { pending?.cancel() }
    }

    private inner class Authorization(
        override val account: Account,
        private val sessionId: Long,
        private val verification: ByteString,
        private val confirmedAt: Long
    ) : CryptAuthBootstrapAuthorization {
        override fun enforceCurrent() {
            checkCurrent()
            val now = SystemClock.elapsedRealtime()
            check(now >= confirmedAt && now - confirmedAt < CREDENTIAL_VALIDITY) { "Device confirmation expired" }
            check(remoteSessionId.get() == sessionId && peer.get() == verification && transport.verificationString == verification) {
                "Transfer binding changed"
            }
            val manager = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            check(Build.VERSION.SDK_INT >= 23 && manager.isDeviceSecure && !manager.isDeviceLocked) { "Device locked" }
            check(AccountManager.get(context).getAccountsByType("com.google").contains(account) && restrictions.allows(account)) {
                "Local account changed"
            }
        }

        override fun toString() = "SourceTransferAuthorization(redacted)"
    }

    private fun writeFrame(bytes: ByteArray) {
        try { checkCurrent(); io.write(bytes) }
        finally { bytes.fill(0) }
    }

    private fun dispatch(block: () -> Unit) {
        try {
            callbacks.execute { try { block() } catch (error: Exception) { noteFailure(error); close() } }
        } catch (error: RuntimeException) { noteFailure(error); close() }
    }

    private inline fun <T> diagnosed(reason: SourceTransferDiagnosticReason, block: () -> T): T = try {
        block()
    } catch (error: Exception) {
        noteFailure(error, reason)
        throw error
    }

    /** Diagnostics contain no exception message, stack, account, session ID or peer material. */
    private fun noteFailure(error: Exception, reason: SourceTransferDiagnosticReason? = null) {
        if (diagnosticRecorded.compareAndSet(false, true)) {
            Log.w("SourceTransfer", sourceTransferFailureSummary(diagnosticPhase, error.javaClass,
                reason ?: (error as? SourceTransferProtocolException)?.reason))
        }
    }

    private fun fail(code: Int) {
        if (terminal.compareAndSet(false, true)) dispatch { listener.onError(code) }
        close()
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        consent.getAndSet(null)?.invalidate()
        consentResult.cancel()
        authorization.set(null)
        timer.getAndSet(null)?.cancel(false)
        // Cancel suspended hooks and signal blocked descriptors before closing the crypto context.
        io.close()
        try { transport.close() } catch (_: Exception) { }
        try { callback.asBinder().unlinkToDeath(death, 0) } catch (_: Exception) { }
        try { listener.asBinder().unlinkToDeath(death, 0) } catch (_: Exception) { }
        onClosed(this)
    }

    companion object {
        private const val SESSION_TIMEOUT = 300_000L
        private const val CONSENT_TIMEOUT = 120_000L
        private const val CREDENTIAL_VALIDITY = 120_000L
        private const val OPERATION_TIMEOUT = 120_000L
        private const val HANDSHAKE_MAXIMUM = 65_536
        private const val MAX_ACCOUNTS = 16
        const val ERROR_CANCELLED = 16
        const val ERROR_TIMEOUT = 15
        const val ERROR_TRANSFER = 8
    }
}
