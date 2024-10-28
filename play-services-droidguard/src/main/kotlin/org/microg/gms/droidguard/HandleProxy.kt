/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard

import android.content.Context
import android.os.Parcelable

class HandleProxy(private val handle: Any, val vmKey: String) {
    constructor(clazz: Class<*>, context: Context, vmKey: String, data: Parcelable) : this(kotlin.runCatching {
        clazz.getDeclaredConstructor(Context::class.java, Parcelable::class.java).newInstance(context, data)
    }.getOrElse {
        throw it
    }, vmKey
    )

    fun init(): Boolean {
        try {
            return handle.javaClass.getDeclaredMethod("init").invoke(handle) as Boolean
        } catch (e: Exception) {
            throw e
        }
    }

    fun close() {
        try {
            handle.javaClass.getDeclaredMethod("close").invoke(handle)
        } catch (e: Exception) {
            throw e
        }
    }
}