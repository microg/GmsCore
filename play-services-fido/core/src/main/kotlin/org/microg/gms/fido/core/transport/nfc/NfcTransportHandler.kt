/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.fido.core.transport.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.util.Log
import androidx.core.app.OnNewIntentProvider
import androidx.core.app.PendingIntentCompat
import androidx.core.util.Consumer
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAssertionResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorAttestationResponse
import com.google.android.gms.fido.fido2.api.common.AuthenticatorResponse
import com.google.android.gms.fido.fido2.api.common.RequestOptions
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import org.microg.gms.fido.core.RequestOptionsType
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.TransportHandler
import org.microg.gms.fido.core.transport.TransportHandlerCallback
import org.microg.gms.fido.core.type

class NfcTransportHandler(private val activity: Activity, callback: TransportHandlerCallback? = null) :
    TransportHandler(Transport.NFC, callback) {
    override val isSupported: Boolean
        get() = NfcAdapter.getDefaultAdapter(activity)?.isEnabled == true && activity is OnNewIntentProvider

    private var deferred = CompletableDeferred<Tag>()

    private suspend fun waitForNewNfcTag(adapter: NfcAdapter): Tag {
        val intent = Intent(activity, activity.javaClass).apply { addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP) }
        val pendingIntent: PendingIntent = PendingIntentCompat.getActivity(activity, 0, intent, 0, true)!!
        adapter.enableForegroundDispatch(
            activity,
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
        return CtapNfcConnection(activity, tag).open {
            register(it, activity, options, callerPackage)
        }
    }

    suspend fun sign(
        options: RequestOptions,
        callerPackage: String,
        tag: Tag
    ): AuthenticatorAssertionResponse {
        return CtapNfcConnection(activity, tag).open {
            sign(it, activity, options, callerPackage)
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
        val adapter = NfcAdapter.getDefaultAdapter(activity)
        val newIntentListener = Consumer<Intent> {
            if (it?.action != NfcAdapter.ACTION_TECH_DISCOVERED) return@Consumer
            val tag = it.getParcelableExtra<Tag>(NfcAdapter.EXTRA_TAG) ?: return@Consumer
            deferred.complete(tag)
        }
        try {
            (activity as OnNewIntentProvider).addOnNewIntentListener(newIntentListener)
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
        } finally {
            (activity as OnNewIntentProvider).removeOnNewIntentListener(newIntentListener)
        }
    }

    companion object {
        const val TAG = "FidoNfcHandler"
    }
}
