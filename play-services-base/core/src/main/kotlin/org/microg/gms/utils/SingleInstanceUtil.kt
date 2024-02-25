/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.utils

private val singleInstanceLock = Any()
private val singleInstanceMap: MutableMap<Class<*>, Any> = hashMapOf()

fun <T : Any> singleInstanceOf(tClass: Class<T>, tCreator: () -> T): T {
    val tVolatileItem = singleInstanceMap[tClass]
    @Suppress("UNCHECKED_CAST")
    if (tVolatileItem != null && tClass.isAssignableFrom(tVolatileItem.javaClass)) return tVolatileItem as T
    val tLock = synchronized(singleInstanceLock) {
        val tItem = singleInstanceMap[tClass]
        if (tItem != null) {
            @Suppress("UNCHECKED_CAST")
            if (tClass.isAssignableFrom(tItem.javaClass)) return tItem as T
            tItem
        } else {
            val tLock = Any()
            singleInstanceMap[tClass] = tLock
            tLock
        }
    }
    synchronized(tLock) {
        val tItem = synchronized(singleInstanceMap) { singleInstanceMap[tClass] }
        if (tItem == null) throw IllegalStateException()
        @Suppress("UNCHECKED_CAST")
        if (tClass.isAssignableFrom(tItem.javaClass)) return tItem as T
        if (tItem != tLock) throw IllegalStateException()

        val tNewItem = tCreator()
        synchronized(singleInstanceMap) {
            singleInstanceMap[tClass] = tNewItem
        }
        return tNewItem
    }
}

inline fun <reified T : Any> singleInstanceOf(noinline tCreator: () -> T): T = singleInstanceOf(T::class.java, tCreator)