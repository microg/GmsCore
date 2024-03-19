/*
 * SPDX-FileCopyrightText: 2021 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.droidguard.core

import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.droidguard.DroidGuardChimeraService
import org.microg.gms.AbstractGmsServiceBroker
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import java.util.*

class DroidGuardServiceBroker(val service: DroidGuardChimeraService) : AbstractGmsServiceBroker(EnumSet.of(GmsService.DROIDGUARD)) {

    override fun getService(callback: IGmsCallbacks?, request: GetServiceRequest?) {
        handleServiceRequest(callback, request, null)
    }

    override fun handleServiceRequest(callback: IGmsCallbacks?, request: GetServiceRequest?, service: GmsService?) {
        val packageName = PackageUtils.getAndCheckCallingPackageOrImpersonation(this.service, request!!.packageName)
        callback!!.onPostInitComplete(0, DroidGuardServiceImpl(this.service, packageName!!), null)
    }
}
