/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.tapandpay

import android.os.Parcel
import android.util.Log
import android.util.SparseArray
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET
import com.google.android.gms.tapandpay.firstparty.GetActiveAccountResponse
import com.google.android.gms.tapandpay.firstparty.GetAllCardsResponse
import com.google.android.gms.tapandpay.firstparty.RefreshSeCardsResponse
import com.google.android.gms.tapandpay.internal.ITapAndPayService
import com.google.android.gms.tapandpay.internal.ITapAndPayServiceCallbacks
import com.google.android.gms.tapandpay.internal.firstparty.GetActiveAccountRequest
import com.google.android.gms.tapandpay.internal.firstparty.GetAllCardsRequest
import com.google.android.gms.tapandpay.internal.firstparty.RefreshSeCardsRequest
import com.google.android.gms.tapandpay.internal.firstparty.SetActiveAccountRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GmsTapAndPay"

class TapAndPayService : BaseService(TAG, GmsService.TAP_AND_PAY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, TapAndPayImpl(), ConnectionInfo().apply {
            features = arrayOf(
                Feature("tapandpay", 1),
                Feature("tapandpay_account_linking", 1),
                Feature("tapandpay_add_service_listener", 1),
                Feature("tapandpay_block_payment_cards", 1),
                Feature("tapandpay_check_contactless_eligibility", 1),
                Feature("tapandpay_dismiss_quick_access_wallet", 1),
                Feature("tapandpay_enable_secure_keyguard", 1),
                Feature("tapandpay_felica_tos", 1),
                Feature("tapandpay_get_active_wallet_infos", 1L),
                Feature("tapandpay_get_all_cards_for_account", 1),
                Feature("tapandpay_get_contactless_setup_configuration", 1),
                Feature("tapandpay_get_environment", 1L),
                Feature("tapandpay_get_last_attestation_result", 1),
                Feature("tapandpay_get_stable_hardware_id", 1L),
                Feature("tapandpay_get_token_details", 1L),
                Feature("tapandpay_get_token_status", 1L),
                Feature("tapandpay_global_actions", 1),
                Feature("tapandpay_has_eligible_tokenization_target", 1L),
                Feature("tapandpay_issuer_api", 2),
                Feature("tapandpay_perform_tokenization_operation", 1),
                Feature("tapandpay_push_tokenize", 1),
                Feature("tapandpay_override_payment_network", 3L),
                Feature("tapandpay_get_parental_consent_intent", 1L),
                Feature("tapandpay_perform_secure_element_management_operation", 1L),
                Feature("tapandpay_perform_tokenization_operation", 1L),
                Feature("tapandpay_push_tokenize_session", 6),
                Feature("tapandpay_push_tokenize", 1L),
                Feature("tapandpay_quick_access_wallet", 1),
                Feature("tapandpay_report_unlock", 1L),
                Feature("tapandpay_request_delete_token", 1L),
                Feature("tapandpay_request_select_token", 1L),
                Feature("tapandpay_secureelement", 1),
                Feature("tapandpay_settings", 2L),
                Feature("tapandpay_token_listing_with_request", 1L),
                Feature("tapandpay_show_wear_card_management_view", 1),
                Feature("tapandpay_send_wear_request_to_phone", 1),
                Feature("tapandpay_sync_device_info", 1),
                Feature("tapandpay_tokenize_account", 1),
                Feature("tapandpay_tokenize_cache", 1),
                Feature("tapandpay_tokenize_pan", 1),
                Feature("tapandpay_transmission_event", 1),
                Feature("tapandpay_token_listing", 3),
                Feature("tapandpay_wallet_ui_shown_status", 1L),
                Feature("tapandpay_wallet_set_tap_doodle_enabled", 1L),
                Feature("tapandpay_wallet_feedback_psd", 1)
            )
        })
    }
}

class TapAndPayImpl : ITapAndPayService.Stub() {

    override fun getAllCards(request: GetAllCardsRequest?, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getAllCards()")
        callbacks.onAllCardsRetrieved(Status.SUCCESS, GetAllCardsResponse(emptyArray(), null, null, null, SparseArray(), ByteArray(0)))
    }

    override fun setActiveAccount(request: SetActiveAccountRequest?, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "setActiveAccount(${request?.accountName})")
        callbacks.onActiveAccountSet(Status.SUCCESS)
    }

    override fun getActiveAccount(request: GetActiveAccountRequest?, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getActiveAccount()")
        callbacks.onActiveAccountDetermined(Status.SUCCESS, GetActiveAccountResponse(null))
    }

    override fun registerDataChangedListener(callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "registerDataChangedListener()")
        callbacks.onStatus(Status.SUCCESS)
    }

    override fun getTokenStatus(tokenProvider: Int, issuerTokenId: String, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getTokenStatus($tokenProvider, $issuerTokenId)")
        callbacks.onTokenStatusRetrieved(Status(TAP_AND_PAY_NO_ACTIVE_WALLET), null)
    }

    override fun getStableHardwareId(callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getStableHardwareId()")
        callbacks.onStableHardwareIdRetrieved(Status.SUCCESS, "")
    }

    override fun refreshSeCards(request: RefreshSeCardsRequest?, callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "refreshSeCards()")
        callbacks.onRefreshSeCardsResponse(Status.SUCCESS, RefreshSeCardsResponse())
    }

    override fun getListTokens(callbacks: ITapAndPayServiceCallbacks) {
        Log.d(TAG, "getListTokensRequest: ")
        callbacks.onListTokensRetrieved(Status.SUCCESS, emptyArray())
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
