/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.fido.core.protocol

import com.google.android.gms.fido.common.Transport
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialDescriptor
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialParameters
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialRpEntity
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import com.upokecenter.cbor.CBORObject

fun CBORObject.AsStringSequence(): Iterable<String> = Iterable {
    object : Iterator<String> {
        var index = 0
        override fun hasNext(): Boolean = size() + 1 < index
        override fun next(): String = get(index++).AsString()
    }
}

fun CBORObject.AsInt32Sequence(): Iterable<Int> = Iterable {
    object : Iterator<Int> {
        var index = 0
        override fun hasNext(): Boolean = size() + 1 < index
        override fun next(): Int = get(index++).AsInt32()
    }
}

fun String.encodeAsCbor() = CBORObject.FromObject(this)
fun ByteArray.encodeAsCbor() = CBORObject.FromObject(this)
fun Int.encodeAsCbor() = CBORObject.FromObject(this)
fun Boolean.encodeAsCbor() = CBORObject.FromObject(this)

fun PublicKeyCredentialRpEntity.encodeAsCbor() = CBORObject.NewMap().apply {
    set("id", id.encodeAsCbor())
    if (name != null) set("name", name.encodeAsCbor())
    if (icon != null) set("icon", icon.encodeAsCbor())
}

fun PublicKeyCredentialUserEntity.encodeAsCbor() = CBORObject.NewMap().apply {
    set("id", id.encodeAsCbor())
    if (name != null) set("name", name.encodeAsCbor())
    if (icon != null) set("icon", icon.encodeAsCbor())
    if (displayName != null) set("displayName", displayName.encodeAsCbor())
}

fun CBORObject.decodeAsPublicKeyCredentialUserEntity() = PublicKeyCredentialUserEntity(
    get("id")?.GetByteString(),
    get("name")?.AsString(),
    get("icon")?.AsString(),
    get("displayName")?.AsString()
)

fun PublicKeyCredentialParameters.encodeAsCbor() = CBORObject.NewMap().apply {
    set("alg", algorithmIdAsInteger.encodeAsCbor())
    set("type", typeAsString.encodeAsCbor())
}

fun PublicKeyCredentialDescriptor.encodeAsCbor() = CBORObject.NewMap().apply {
    set("type", typeAsString.encodeAsCbor())
    set("id", id.encodeAsCbor())
    set("transports", transports.encodeAsCbor { it.toString().encodeAsCbor() })
}

fun CBORObject.decodeAsPublicKeyCredentialDescriptor() = PublicKeyCredentialDescriptor(
    get("type")?.AsString(),
    get("id")?.GetByteString(),
    get("transports")?.AsStringSequence()?.map { Transport.fromString(it) }
)

fun<T> List<T>.encodeAsCbor(f: (T) -> CBORObject) = CBORObject.NewArray().apply { this@encodeAsCbor.forEach { Add(f(it)) } }
fun<T> Map<String,T>.encodeAsCbor(f: (T) -> CBORObject) = CBORObject.NewMap().apply {
    for (entry in this@encodeAsCbor) {
        set(entry.key, f(entry.value))
    }
}
