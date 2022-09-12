/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.chimera

import android.content.Context
import com.google.android.chimera.Service

class StaticServiceLoader<T : Service>(private val serviceClass: Class<T>) : ServiceLoader {
    override fun loadService(context: Context): Service {
        return serviceClass.getDeclaredConstructor().newInstance()
    }
}
