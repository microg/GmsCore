/*
 * SPDX-FileCopyrightText: 2019 e Foundation
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.appinivite

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Parcel
import android.os.RemoteException
import android.util.Log
import com.google.android.gms.appinvite.internal.IAppInviteCallbacks
import com.google.android.gms.appinvite.internal.IAppInviteService
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "AppInviteService"

class AppInviteService : BaseService(TAG, GmsService.APP_INVITE) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        PackageUtils.getAndCheckCallingPackage(this, request.packageName)
        Log.d(TAG, "callb: $callback ; req: $request ; serv: $service")
        callback.onPostInitComplete(0, AppInviteServiceImpl(this, request.packageName, request.extras), null)
    }
}


class AppInviteServiceImpl(context: Context?, packageName: String?, extras: Bundle?) : IAppInviteService.Stub() {
    override fun updateInvitationOnInstall(callback: IAppInviteCallbacks, invitationId: String) {
        callback.onStatus(Status.SUCCESS)
    }

    override fun convertInvitation(callback: IAppInviteCallbacks, invitationId: String) {
        callback.onStatus(Status.SUCCESS)
    }

    override fun getInvitation(callback: IAppInviteCallbacks) {
        callback.onStatusIntent(Status(Activity.RESULT_CANCELED), null)
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean = warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
