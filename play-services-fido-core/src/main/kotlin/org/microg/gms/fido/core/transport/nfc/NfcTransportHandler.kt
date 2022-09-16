/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.nfc

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.os.Bundle
import android.util.Base64
import android.util.Log
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.fido.core.RequestOptionsType
import org.microg.gms.fido.core.protocol.msgs.decodeResponseApdu
import org.microg.gms.fido.core.protocol.msgs.encodeCommandApdu
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.transport.usb.UsbTransportHandler
import org.microg.gms.fido.core.transport.usb.ctaphid.CtapHidConnection
import org.microg.gms.fido.core.type
import org.microg.gms.fido.core.ui.AuthenticatorActivity
import org.microg.gms.utils.toBase64

class NfcTransportHandler(private val context: Context, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.NFC, callback) {
    override val isSupported: Boolean
        get() = NfcAdapter.getDefaultAdapter(context)?.isEnabled == true

    private var deferred = CompletableDeferred<Tag>()

    private suspend fun waitForNewNfcTag(adapter: NfcAdapter): Tag {
        require(context is AuthenticatorActivity)
        val intent = Intent(context, context.javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_MUTABLE)
        adapter.enableForegroundDispatch(
            context,
            pendingIntent,
            arrayOf(IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)),
            arrayOf(arrayOf(IsoDep::class.java.name))
        )
        invokeStatusChanged(TransportHandlerCallback.STATUS_WAITING_FOR_DEVICE)
        val tag = deferred.await()
        deferred = CompletableDeferred()
        return tag
    }

    suspend fun register(
        options: RequestOptions,
        callerPackage: String,
        tag: Tag
    ): AuthenticatorAttestationResponse {
        return CtapNfcConnection(context, tag).open {
            register(it, context, options, callerPackage)
        }
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String,
        tag: Tag
    ): AuthenticatorAssertionResponse {
        return CtapNfcConnection(context, tag).open {
            sign(it, context, options, callerPackage)
        }
    }


    suspend fun handle(
        options: RequestOptions,
        callerPackage: String,
        tag: Tag
    ): AuthenticatorResponse {
        return when (options.type) {
            RequestOptionsType.REGISTER -> register(options, callerPackage, tag)
            RequestOptionsType.SIGN -> sign(options, callerPackage, tag)
        }
    }


    override suspend fun start(options: RequestOptions, callerPackage: String): AuthenticatorResponse {
        val adapter = NfcAdapter.getDefaultAdapter(context)
        while (true) {
            val tag = waitForNewNfcTag(adapter)
            try {
                return handle(options, callerPackage, tag)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, e)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        if (intent?.action != NfcAdapter.ACTION_TECH_DISCOVERED) return
        val tag = intent.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return
        deferred.complete(tag)
    }

    companion object {
        const val TAG = "FidoNfcHandler"
    }
}
