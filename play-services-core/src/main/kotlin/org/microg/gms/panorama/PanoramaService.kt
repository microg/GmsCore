/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.panorama

import android.net.Uri
import android.os.Bundle
import android.util.Log
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.panorama.internal.IPanoramaCallbacks
import com.google.android.gms.panorama.internal.IPanoramaService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService

class PanoramaService : BaseService("GmsPanoramaService", GmsService.PANORAMA) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitComplete(CommonStatusCodes.SUCCESS, PanoramaServiceImpl().asBinder(), null)
    }
}

class PanoramaServiceImpl : IPanoramaService.Stub() {
    override fun loadPanoramaInfo(callback: IPanoramaCallbacks?, uri: Uri, bundle: Bundle, needGrantReadUriPermissions: Boolean) {
        Log.d("GmsPanoramaService", "ERROR:PanoramaService not implement! Print by GMS...$uri bundle:$bundle")
    }
}
