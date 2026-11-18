/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads

import android.os.Bundle
import android.os.IBinder
import androidx.annotation.Keep
import com.google.android.gms.ads.internal.client.IAdPreloader
import com.google.android.gms.ads.internal.client.IAdPreloaderCreator
import com.google.android.gms.ads.internal.mediation.client.IAdapterCreator
import com.google.android.gms.dynamic.IObjectWrapper

@Keep
open class AdPreloaderRemoteCreatorImpl : IAdPreloaderCreator.Stub() {
    override fun createAdPreloader(
        context: IObjectWrapper?, adapterCreator: IAdapterCreator?, clientVersion: Int
    ): IAdPreloader = EmptyAdPreloader
}

@Keep
class ChimeraAdPreloaderCreatorImpl : AdPreloaderRemoteCreatorImpl()

// Keep SDK initialization on the remote path so it can finish without starting WebView.
private object EmptyAdPreloader : IAdPreloader.Stub(), IAdPreloader by IAdPreloader.Default() {
    override fun asBinder(): IBinder = this

    override fun getConfigurations(adFormat: Int): Bundle = Bundle()
}
