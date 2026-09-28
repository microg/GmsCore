/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.app

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context

@SuppressLint("StaticFieldLeak")
object AppContext {
    private lateinit var context: Context
    private var isInitialized = false

    fun init(application: Application) {
        context = application
        isInitialized = true
    }
    fun get(): Context = context
    fun isInitialized(): Boolean = isInitialized
}
