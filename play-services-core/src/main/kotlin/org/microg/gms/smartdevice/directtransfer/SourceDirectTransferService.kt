/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.content.Intent
import android.os.BadParcelableException
import android.os.Binder
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.os.ParcelFileDescriptor
import androidx.lifecycle.LifecycleService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.AbstractGmsServiceBroker
import org.microg.gms.common.GmsService
import org.microg.gms.checkin.LastCheckinInfo
import org.microg.gms.cryptauth.createCryptAuthBootstrapAssertion
import org.microg.gms.cryptauth.getCryptAuthBootstrapInfo
import java.util.EnumSet
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadFactory
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/** Private API 210. The broker intentionally avoids logging account-bearing GetServiceRequest data. */
class SourceDirectTransferService : LifecycleService() {
    private val active = AtomicReference<SourceTransferSession?>()
    private val broker = object : AbstractGmsServiceBroker(EnumSet.of(GmsService.SMARTDEVICE_SOURCE_DIRECT_TRANSFER)) {
        override fun getService(callback: IGmsCallbacks, request: GetServiceRequest) {
            // The base broker logs unsupported requests, which may contain account data.
            check(request.serviceId == GmsService.SMARTDEVICE_SOURCE_DIRECT_TRANSFER.SERVICE_ID)
            handleServiceRequest(callback, request, GmsService.SMARTDEVICE_SOURCE_DIRECT_TRANSFER)
        }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code != IBinder.INTERFACE_TRANSACTION) {
                SourceTransferCaller.capture(this@SourceDirectTransferService, null)
                requireParcelBound(data)
            }
            return super.onTransact(code, data, reply, flags)
        }

        override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
            val caller = SourceTransferCaller.capture(this@SourceDirectTransferService, request.packageName)
            check(service == GmsService.SMARTDEVICE_SOURCE_DIRECT_TRANSFER)
            completeConnection(callback, TransferBinder(caller))
        }
    }

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return broker.asBinder()
    }

    override fun onDestroy() {
        active.getAndSet(null)?.cancel()
        super.onDestroy()
    }

    private inner class TransferBinder(private val caller: SourceTransferCaller) : Binder() {
        init { attachInterface(null, DESCRIPTOR) }

        override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean {
            if (code == IBinder.INTERFACE_TRANSACTION) {
                reply?.writeString(DESCRIPTOR)
                return true
            }
            if (code !in 1..3) return super.onTransact(code, data, reply, flags)
            caller.enforceBinder(this@SourceDirectTransferService)
            requireParcelBound(data)
            data.enforceInterface(DESCRIPTOR)
            val callback = SourceTransferCallback(requireNotNull(data.readStrongBinder()))
            when (code) {
                1 -> readStart(data, callback)
                2 -> {
                    readMetadataAndEnd(data)
                    active.get()?.cancel()
                    sendStatus(callback, 0)
                }
                3 -> {
                    val extras = if (data.readInt() != 0) Bundle.CREATOR.createFromParcel(data) else Bundle.EMPTY
                    readMetadataAndEnd(data)
                    val id = extras.getLong("sessionId", -1L)
                    val current = active.get()
                    sendStatus(callback, if (current == null) 10565 else if (current.matchesSession(id)) 0 else 10581)
                }
            }
            return true
        }

        private fun readStart(data: Parcel, callback: SourceTransferCallback) {
            val originals = ArrayList<ParcelFileDescriptor>(2)
            val duplicates = ArrayList<ParcelFileDescriptor>(2)
            var transferred = false
            try {
                check(data.readInt() != 0) { "Missing transfer configuration" }
                val restrictions = SourceTransferParcel.restrictions(data)
                check(data.readInt() == 2) { "Exactly two transfer descriptors required" }
                repeat(2) {
                    check(data.readInt() != 0) { "Missing transfer descriptor" }
                    originals.add(ParcelFileDescriptor.CREATOR.createFromParcel(data))
                }
                val listener = SourceTransferListener(requireNotNull(data.readStrongBinder()))
                readMetadataAndEnd(data)
                caller.enforceBinder(this@SourceDirectTransferService)
                check(active.get() == null) { "Transfer already active" }
                // Duplicates become session-owned; the received Binder parcel descriptors are closed below.
                originals.forEach { duplicates.add(ParcelFileDescriptor.dup(it.fileDescriptor)) }
                val session = SourceTransferSession(applicationContext, caller, restrictions, duplicates[0], duplicates[1],
                    listener, callback, CALLBACKS, SCHEDULER,
                    sourceAndroidDeviceId = { LastCheckinInfo.read(applicationContext).androidId },
                    bootstrapInfo = { authorization ->
                        val info = applicationContext.getCryptAuthBootstrapInfo(authorization)
                        SourceTransferBootstrapInfo(info.accountIdentifier, info.publicKey)
                    },
                    assertion = { authorization, publicKey, challenge ->
                        applicationContext.createCryptAuthBootstrapAssertion(authorization, publicKey, challenge)
                    },
                    onClosed = { active.compareAndSet(it, null) })
                if (!active.compareAndSet(null, session)) {
                    transferred = true
                    session.close()
                    sendStatus(callback, 8)
                    return
                }
                transferred = true
                session.start(IO)
            } catch (_: Exception) {
                sendStatus(callback, 10)
            } finally {
                originals.forEach { try { it.close() } catch (_: Exception) { } }
                if (!transferred) duplicates.forEach { try { it.close() } catch (_: Exception) { } }
            }
        }
    }

    private fun sendStatus(callback: SourceTransferCallback, code: Int) {
        try { CALLBACKS.execute { try { callback.onStatus(code) } catch (_: Exception) { } } }
        catch (_: RuntimeException) { }
    }

    companion object {
        internal fun completeConnection(callback: IGmsCallbacks, binder: IBinder) {
            // The source client checks this capability before invoking startTransfer.
            val info = ConnectionInfo().apply {
                features = arrayOf(Feature("source_direct_transfer_api", 1L))
            }
            callback.onPostInitCompleteWithConnectionInfo(0, binder, info)
        }

        private const val DESCRIPTOR = "com.google.android.gms.smartdevice.directtransfer.internal.ISourceDirectTransferService"
        private const val MAX_REQUEST_BYTES = 65_536
        private fun threads(name: String) = ThreadFactory { runnable -> Thread(runnable, name).apply { isDaemon = true } }
        // Process-wide bounds also apply if Android destroys and recreates the service while a peer blocks.
        private val IO = ThreadPoolExecutor(0, 1, 30, TimeUnit.SECONDS, SynchronousQueue(), threads("SourceTransferIO"))
        private val CALLBACKS = ThreadPoolExecutor(0, 2, 30, TimeUnit.SECONDS, ArrayBlockingQueue(8), threads("SourceTransferCallback"))
        private val SCHEDULER = ScheduledThreadPoolExecutor(1, threads("SourceTransferExpiry")).apply {
            removeOnCancelPolicy = true
        }

        private fun requireParcelBound(data: Parcel) {
            if (data.dataAvail() > MAX_REQUEST_BYTES) throw BadParcelableException("Transfer request too large")
        }

        private fun readMetadataAndEnd(data: Parcel) {
            // ApiMetadata is present in GMS 25; earlier clients can omit the trailing optional metadata.
            if (data.dataAvail() > 0 && data.readInt() != 0) ApiMetadata.CREATOR.createFromParcel(data)
            if (data.dataAvail() != 0) throw BadParcelableException("Unexpected transfer request data")
        }
    }
}
