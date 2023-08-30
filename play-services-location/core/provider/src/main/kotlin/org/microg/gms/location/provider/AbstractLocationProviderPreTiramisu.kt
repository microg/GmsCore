/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.location.provider

import android.content.Context
import android.location.LocationProvider
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import androidx.annotation.RequiresApi
import com.android.location.provider.LocationProviderBase
import com.android.location.provider.ProviderPropertiesUnbundled
import java.io.FileDescriptor
import java.io.PrintWriter

abstract class AbstractLocationProviderPreTiramisu : LocationProviderBase, GenericLocationProvider {
    @Deprecated("Use only with SDK < 31")
    constructor(properties: ProviderPropertiesUnbundled) : super(TAG, properties)

    @RequiresApi(31)
    constructor(context: Context, properties: ProviderPropertiesUnbundled) : super(context, TAG, properties)

    private var statusUpdateTime = SystemClock.elapsedRealtime()

    override fun onDump(fd: FileDescriptor, pw: PrintWriter, args: Array<out String>) {
        dump(pw)
    }

    override fun dump(writer: PrintWriter) {
        // Nothing by default
    }

    override fun onFlush(callback: OnFlushCompleteCallback?) {
        Log.d(TAG, "onFlush")
        callback!!.onFlushComplete()
    }

    override fun onSendExtraCommand(command: String?, extras: Bundle?): Boolean {
        Log.d(TAG, "onSendExtraCommand $command $extras")
        return false
    }

    @Deprecated("Overriding this is required pre-Q, but not used since Q")
    override fun onEnable() {
        Log.d(TAG, "onEnable")
        statusUpdateTime = SystemClock.elapsedRealtime()
    }

    @Deprecated("Overriding this is required pre-Q, but not used since Q")
    override fun onDisable() {
        Log.d(TAG, "onDisable")
        statusUpdateTime = SystemClock.elapsedRealtime()
    }

    @Deprecated("Overriding this is required pre-Q, but not used since Q")
    override fun onGetStatus(extras: Bundle?): Int {
        Log.d(TAG, "onGetStatus $extras")
        return LocationProvider.AVAILABLE
    }


    @Deprecated("Overriding this is required pre-Q, but not used since Q")
    override fun onGetStatusUpdateTime(): Long {
        Log.d(TAG, "onGetStatusUpdateTime")
        return statusUpdateTime
    }
}