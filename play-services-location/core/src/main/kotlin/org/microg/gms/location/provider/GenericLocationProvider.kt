/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.location.Location
import android.os.IBinder
import java.io.PrintWriter

interface GenericLocationProvider {
    fun getBinder(): IBinder
    fun enable()
    fun disable()
    fun dump(writer: PrintWriter)
    fun reportLocationToSystem(location: Location)
}