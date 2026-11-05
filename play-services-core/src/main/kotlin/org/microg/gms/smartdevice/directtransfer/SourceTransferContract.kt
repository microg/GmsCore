/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.smartdevice.directtransfer

import android.accounts.Account
import android.app.PendingIntent
import android.os.BadParcelableException
import android.os.IBinder
import android.os.Parcel
import android.os.Parcelable
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.safeparcel.SafeParcelWriter

/** Immutable restrictions from the request. Presentation and skip-authentication flags confer no authority. */
internal class SourceTransferRestrictions(val accountNames: Set<String>?, val authenticatingEmail: String?) {
    fun allows(account: Account): Boolean = account.type == "com.google" &&
        (accountNames == null || account.name in accountNames) &&
        (authenticatingEmail == null || account.name == authenticatingEmail)

    override fun toString() = "SourceTransferRestrictions(redacted)"
}

/** Bounded reader for the private DirectTransferConfigurations SafeParcel contract. */
internal object SourceTransferParcel {
    private const val MAX_ACCOUNTS = 16

    fun restrictions(parcel: Parcel): SourceTransferRestrictions {
        var names: Set<String>? = null
        var email: String? = null
        readObject(parcel, parcel.dataSize()) { id, end ->
            when (id) {
                10 -> if (parcel.dataPosition() < end) {
                    readObject(parcel, end) { nested, fieldEnd ->
                        if (nested == 3) email = readAccountString(parcel, fieldEnd)
                    }
                }
                11 -> if (parcel.dataPosition() < end) {
                    requireBytes(parcel, 4, end)
                    val size = parcel.readInt()
                    if (size !in -1..MAX_ACCOUNTS) fail()
                    // -1 is an absent list (no restriction); an explicit empty list allows no account.
                    if (size >= 0) {
                        val found = LinkedHashSet<String>()
                        repeat(size) {
                            requireBytes(parcel, 4, end)
                            if (parcel.readInt() == 0) fail()
                            var name: String? = null
                            var type: String? = null
                            var unsupported = false
                            readObject(parcel, end) { nested, fieldEnd ->
                                when (nested) {
                                    2 -> name = readAccountString(parcel, fieldEnd)
                                    3 -> type = readAccountString(parcel, fieldEnd)
                                    4, 6 -> {
                                        requireBytes(parcel, 4, fieldEnd)
                                        unsupported = unsupported || parcel.readInt() != 0
                                    }
                                }
                            }
                            if (name == null || type != "com.google" || unsupported) fail()
                            found.add(requireNotNull(name))
                        }
                        names = found.toSet()
                    }
                }
            }
        }
        return SourceTransferRestrictions(names, email)
    }

    private fun readObject(parcel: Parcel, limit: Int, field: (Int, Int) -> Unit) {
        val (magic, end) = header(parcel, limit)
        if (magic != 20293) fail()
        val seen = HashSet<Int>()
        while (parcel.dataPosition() < end) {
            val (id, fieldEnd) = header(parcel, end)
            if (!seen.add(id)) fail()
            field(id, fieldEnd)
            if (parcel.dataPosition() > fieldEnd) fail()
            parcel.setDataPosition(fieldEnd)
        }
        if (parcel.dataPosition() != end) fail()
    }

    private fun header(parcel: Parcel, limit: Int): Pair<Int, Int> {
        requireBytes(parcel, 4, limit)
        val header = parcel.readInt()
        val size = if (header ushr 16 == 65535) {
            requireBytes(parcel, 4, limit)
            parcel.readInt()
        } else header ushr 16
        if (size < 0 || size > limit - parcel.dataPosition()) fail()
        return (header and 65535) to (parcel.dataPosition() + size)
    }

    private fun readAccountString(parcel: Parcel, end: Int): String? {
        if (parcel.dataPosition() == end) return null
        requireBytes(parcel, 4, end)
        val start = parcel.dataPosition()
        val characters = parcel.readInt()
        parcel.setDataPosition(start)
        if (characters !in -1..320) fail()
        val value = parcel.readString()
        if (parcel.dataPosition() > end || value?.any { it < ' ' } == true) fail()
        return value
    }

    private fun requireBytes(parcel: Parcel, size: Int, limit: Int) {
        if (limit > parcel.dataSize() || parcel.dataPosition() > limit - size) fail()
    }

    private fun fail(): Nothing = throw BadParcelableException("Invalid direct-transfer configuration")
}

/** Remote-confirmed result; deliberately has no account-bearing toString. */
internal class SourceTransferAccountResult(val account: Account, val result: Int, val lockScreenAuthenticationType: Int)

internal class SourceTransferListener(private val binder: IBinder) {
    fun asBinder() = binder
    fun onConsentRequired(intent: PendingIntent) = transact(2) { it.writeInt(1); intent.writeToParcel(it, 0) }
    fun onError(code: Int) = transact(3) {
        it.writeInt(code)
        // The client requires a non-null description. Never expose exception or account data.
        it.writeString("Direct transfer did not complete.")
    }
    fun onComplete(results: List<SourceTransferAccountResult>) = transact(1) { parcel ->
        parcel.writeInt(results.size)
        results.forEach { result ->
            parcel.writeInt(1)
            val start = SafeParcelWriter.writeObjectHeader(parcel)
            SafeParcelWriter.write(parcel, 2, AccountParcel(result.account), 0, false)
            SafeParcelWriter.write(parcel, 3, result.result)
            SafeParcelWriter.write(parcel, 4, result.lockScreenAuthenticationType)
            SafeParcelWriter.finishObjectHeader(parcel, start)
        }
    }

    // Listener calls are synchronous in the official ABI. Invoke only on the bounded callback executor.
    private fun transact(code: Int, write: (Parcel) -> Unit) {
        val data = Parcel.obtain()
        val reply = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.google.android.gms.smartdevice.d2d.internal.IDirectTransferListener")
            write(data)
            check(binder.transact(code, data, reply, 0)) { "Direct-transfer listener unavailable" }
            reply.readException()
        } finally { reply.recycle(); data.recycle() }
    }

    private class AccountParcel(private val account: Account) : Parcelable {
        override fun describeContents() = 0
        override fun writeToParcel(parcel: Parcel, flags: Int) {
            val start = SafeParcelWriter.writeObjectHeader(parcel)
            SafeParcelWriter.write(parcel, 2, account.name, false)
            SafeParcelWriter.write(parcel, 3, account.type, false)
            SafeParcelWriter.finishObjectHeader(parcel, start)
        }
    }
}

internal class SourceTransferCallback(private val binder: IBinder) {
    fun asBinder() = binder
    fun onStatus(code: Int) {
        val data = Parcel.obtain()
        try {
            data.writeInterfaceToken("com.google.android.gms.smartdevice.directtransfer.internal.IDirectTransferCallback")
            data.writeInt(1)
            Status(code).writeToParcel(data, 0)
            check(binder.transact(1, data, null, IBinder.FLAG_ONEWAY)) { "Direct-transfer callback unavailable" }
        } finally { data.recycle() }
    }
}
