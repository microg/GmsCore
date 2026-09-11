/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.chimera.util

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewStub
import android.view.LayoutInflater
import java.lang.reflect.Constructor
import java.util.concurrent.ConcurrentHashMap

object ChimeraViewCreator {

    private val constructorCache = ConcurrentHashMap<ClassLoader, ConcurrentHashMap<String, Constructor<out View>>>()

    private val CONSTRUCTOR_SIGNATURE = arrayOf(Context::class.java, AttributeSet::class.java)

    fun createView(
        moduleClassLoader: ClassLoader,
        context: Context,
        viewName: String,
        attrs: AttributeSet
    ): View? {
        if (!viewName.contains(".")) return null

        try {
            val clCache = constructorCache.getOrPut(moduleClassLoader) { ConcurrentHashMap() }

            val constructor = clCache.getOrPut(viewName) {
                val viewClass = moduleClassLoader.loadClass(viewName).asSubclass(View::class.java)
                viewClass.getConstructor(*CONSTRUCTOR_SIGNATURE).also {
                    it.isAccessible = true
                }
            }

            val view = constructor.newInstance(context, attrs)

            if (view is ViewStub) {
                view.layoutInflater = LayoutInflater.from(context)
            }

            return view
        } catch (_: Exception) {
            return null
        }
    }
}
