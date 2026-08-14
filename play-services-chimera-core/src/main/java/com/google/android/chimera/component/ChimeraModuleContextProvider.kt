/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.content.Context

interface ChimeraModuleContextProvider {
    fun createContextWrapper(proxy: Any?, baseContext: Context): Context
    fun createModuleContext(module: Any?, moduleClass: Class<*>?, baseContext: Context): Context
    fun setProxyWrapper(proxy: Any?, context: Context)
    fun setProxyWrapper(moduleName: String?, proxy: Any?, context: Context)
}
