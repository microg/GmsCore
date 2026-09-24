/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.common.app

import android.content.Context
import android.content.ContextWrapper
import androidx.annotation.Keep

@Keep
abstract class BaseApplicationContext: ContextWrapper {
    private var baseContext: Context?
    private var globalGmsState: BaseApplicationContext?
    private var inSafeBoot = false

    constructor(base: Context?): this(base, null)

    constructor(context: Context?, baseApplicationContext: BaseApplicationContext?) : super(context) {
        inSafeBoot = false
        baseContext = context
        globalGmsState = baseApplicationContext
    }

    public override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        this.baseContext = base
    }

    override fun createAttributionContext(attributionTag: String?): Context {
        return ApplicationContextWrapper(this, super.createAttributionContext(attributionTag))
    }

    override fun createDeviceProtectedStorageContext(): Context {
        return ApplicationContextWrapper(this, super.createDeviceProtectedStorageContext())
    }

    override fun getBaseContext(): Context? {
        return baseContext
    }

    protected fun getGlobalState(): BaseApplicationContext? {
        return this.globalGmsState
    }

    fun getInSafeBoot(): Boolean {
        return this.inSafeBoot
    }
    fun setInSafeBoot() {
        this.inSafeBoot = true
    }

    fun watchForLeaks(object0: Any?) {
    }

    class ApplicationContextWrapper(private val originalContext: Context, base: Context) :
        ContextWrapper(base) {
        override fun getApplicationContext(): Context {
            return originalContext
        }

        override fun createAttributionContext(attributionTag: String?): Context {
            return ApplicationContextWrapper(originalContext, super.createAttributionContext(attributionTag))
        }

        override fun createDeviceProtectedStorageContext(): Context {
            return ApplicationContextWrapper(originalContext, super.createDeviceProtectedStorageContext())
        }
    }
}
