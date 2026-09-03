/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.chimera.component

import android.content.Context

class ChimeraComponentProxy {
    companion object {

        @JvmStatic
        fun bindComponentProxy(provider: ChimeraModuleContextProvider, obj: Any, callback: ChimeraProxyCallback, context: Context) {
            val moduleContext = provider.createModuleContext(obj, callback.javaClass, context)
            callback.setProxyCallbacks(obj, moduleContext)
            provider.setProxyWrapper(callback, moduleContext)
        }
    }
}
