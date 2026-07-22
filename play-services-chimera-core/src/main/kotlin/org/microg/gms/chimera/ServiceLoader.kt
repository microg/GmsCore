/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.chimera

import android.content.Context
import com.google.android.chimera.Service

interface ServiceLoader {
    fun loadService(context: Context): Service

    companion object {
        inline fun <reified T : Service> static() = StaticServiceLoader(T::class.java)
    }
}

