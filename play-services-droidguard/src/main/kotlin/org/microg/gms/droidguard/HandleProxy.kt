/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Bundle
import android.os.Parcelable

class HandleProxy(val handle: Any, val vmKey: String, val extra: ByteArray = ByteArray(0)) {
    constructor(clazz: Class<*>, context: Context, vmKey: String, data: Parcelable) : this(
        kotlin.runCatching {
            clazz.getDeclaredConstructor(Context::class.java, Parcelable::class.java).newInstance(context, data)
        }.getOrElse {
            throw BytesException(ByteArray(0), it)
        },
        vmKey
    )

    constructor(clazz: Class<*>, context: Context, flow: String?, byteCode: ByteArray, callback: Any, vmKey: String, extra: ByteArray, bundle: Bundle?) : this(
        kotlin.runCatching {
            clazz.getDeclaredConstructor(Context::class.java, String::class.java, ByteArray::class.java, Object::class.java, Bundle::class.java).newInstance(context, flow, byteCode, callback, bundle)
        }.getOrElse {
            throw BytesException(extra, it)
        }, vmKey, extra)

    fun run(data: Map<Any, Any>): ByteArray {
        try {
            return handle.javaClass.getDeclaredMethod("run", Map::class.java).invoke(handle, data) as ByteArray
        } catch (e: Exception) {
            throw BytesException(extra, e)
        }
    }

    fun init(): Boolean {
        try {
            return handle.javaClass.getDeclaredMethod("init").invoke(handle) as Boolean
        } catch (e: Exception) {
            throw BytesException(extra, e)
        }
    }

    fun close() {
        try {
            handle.javaClass.getDeclaredMethod("close").invoke(handle)
        } catch (e: Exception) {
            throw BytesException(extra, e)
        }
    }

}
