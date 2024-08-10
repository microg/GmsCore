/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay

import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.pay.CheckReadinessForEmoneyRequest
import com.google.android.gms.pay.GetMdocCredentialRequest
import com.google.android.gms.pay.GetPayApiAvailabilityStatusRequest
import com.google.android.gms.pay.GetPendingIntentForWalletOnWearRequest
import com.google.android.gms.pay.NotifyCardTapEventRequest
import com.google.android.gms.pay.NotifyEmoneyCardStatusUpdateRequest
import com.google.android.gms.pay.PayApiAvailabilityStatus
import com.google.android.gms.pay.PushEmoneyCardRequest
import com.google.android.gms.pay.SavePassesRequest
import com.google.android.gms.pay.SyncBundleRequest
import com.google.android.gms.pay.internal.IPayServiceCallbacks
import com.google.android.gms.pay.internal.IThirdPartyPayService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService


private const val TAG = "ThirdPartyPayService"

class ThirdPartyPayService : BaseService(TAG, GmsService.PAY) {

    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, ThirdPartyPayServiceImpl().asBinder(), ConnectionInfo().apply {
            features = arrayOf(
                Feature("pay_get_pay_api_availability_status", 3),
                Feature("pay_save_passes", 5),
                Feature("pay_save_passes_jwt", 3),
                Feature("pay_sync_bundle", 2),
                Feature("pay_get_pending_intent_for_wallet_on_wear", 2),
                Feature("pay_get_mdoc_credential_pending_intent", 1),
                Feature("pay_notify_card_tap_event", 1),
                Feature("pay_check_readiness_for_emoney", 1),
                Feature("pay_push_emoney_card", 1),
                Feature("pay_notify_emoney_card_status_update", 1)
            )
        })
    }
}

class ThirdPartyPayServiceImpl : IThirdPartyPayService.Stub() {
    override fun getPayApiAvailabilityStatus(request: GetPayApiAvailabilityStatusRequest?, callback: IPayServiceCallbacks) {
        Log.d(TAG, "onPayApiAvailabilityStatus: Reporting NOT_ELIGIBLE")
        callback.onPayApiAvailabilityStatus(Status.SUCCESS, PayApiAvailabilityStatus.NOT_ELIGIBLE)
    }

    override fun savePasses(request: SavePassesRequest?, callback: IPayServiceCallbacks) {
        Log.d(TAG, "savePasses: return SERVICE_MISSING")
        callback.onPendingIntent(Status(CommonStatusCodes.SERVICE_MISSING))
    }

    override fun syncBundle(request: SyncBundleRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "syncBundle Not yet implemented")
    }

    override fun getPendingForWalletOnWear(request: GetPendingIntentForWalletOnWearRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "getPendingForWalletOnWear Not yet implemented")
    }

    override fun getMdocCredential(request: GetMdocCredentialRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "getMdocCredential Not yet implemented")
    }

    override fun notifyCardTapEvent(request: NotifyCardTapEventRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "notifyCardTapEvent Not yet implemented")
    }

    override fun checkReadinessForEmoney(request: CheckReadinessForEmoneyRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "checkReadinessForEmoney Not yet implemented")
    }

    override fun pushEmoneyCard(request: PushEmoneyCardRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "pushEmoneyCard Not yet implemented")
    }

    override fun notifyEmoneyCardStatusUpdate(request: NotifyEmoneyCardStatusUpdateRequest?, callback: IPayServiceCallbacks?) {
        Log.d(TAG, "notifyEmoneyCardStatusUpdate Not yet implemented")
    }

}