/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.container

import android.content.Context
import androidx.annotation.Keep
import com.google.android.chimera.loader.ChimeraModuleApk
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

@Keep
abstract class ModuleApi {

    protected companion object {
        @JvmStatic
        @Throws(Exception::class)
        protected fun invokeStaticMethod(method: Method, args: Array<Any>) {
            try {
                method.invoke(null, *args)
            } catch (invocationTargetException: InvocationTargetException) {
                val cause = invocationTargetException.cause
                throw if (cause is Exception) cause else Exception(cause)
            } catch (throwable: VerifyError) {
                throw Exception(throwable)
            } catch (throwable: ExceptionInInitializerError) {
                throw Exception(throwable)
            }
        }
    }

    abstract fun onApkLoaded(context: Context)

    open fun onBeforeApkLoad(context: Context, moduleApk: ChimeraModuleApk) {
    }

    open fun onModuleLoaded(moduleName: String, providerClass: String?, context: Context) {
    }
}
