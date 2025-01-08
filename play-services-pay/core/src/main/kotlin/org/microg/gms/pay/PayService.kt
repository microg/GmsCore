/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay

import android.os.Parcel
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.pay.internal.IPayService
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PayService"

class PayService : BaseService(TAG, GmsService.PAY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, PayServiceImpl(), ConnectionInfo().apply {
            features = arrayOf(
                Feature("pay", 10),
                Feature("pay_attestation_signal", 1),
                Feature("pay_pay_capabilities", 1),
                Feature("pay_feature_check", 1),
                Feature("pay_get_card_centric_bundle", 1),
                Feature("pay_get_passes", 1),
                Feature("pay_get_pay_api_availability_status", 3),
                Feature("pay_get_se_prepaid_card", 1),
                Feature("pay_debit_se_prepaid_card", 1),
                Feature("pay_get_specific_bulletin", 1),
                Feature("pay_get_transit_cards", 1),
                Feature("pay_get_wallet_status", 1),
                Feature("pay_global_actions", 1),
                Feature("pay_gp3_support", 1),
                Feature("pay_homescreen_sorting", 3),
                Feature("pay_homescreen_bulletins", 2),
                Feature("pay_onboarding", 2),
                Feature("pay_mark_tos_accepted_for_partner", 1),
                Feature("pay_move_card_on_other_device", 1),
                Feature("pay_passes_field_update_notifications", 1),
                Feature("pay_passes_notifications", 2),
                Feature("pay_payment_method", 1),
                Feature("pay_payment_method_action_tokens", 2),
                Feature("pay_payment_method_server_action", 1),
                Feature("pay_provision_se_prepaid_card", 1),
                Feature("pay_request_module", 1),
                Feature("pay_reverse_purchase", 1),
                Feature("pay_save_passes", 5),
                Feature("pay_save_passes_jwt", 3),
                Feature("pay_save_purchased_card", 1),
                Feature("pay_sync_bundle", 2),
                Feature("pay_settings", 1),
                Feature("pay_topup_se_prepaid_card", 1),
                Feature("pay_list_commuter_pass_renewal_options_for_se_prepaid_card", 1),
                Feature("pay_transactions", 6),
                Feature("pay_update_bundle_with_client_settings", 1),
                Feature("pay_clock_skew_millis", 1),
                Feature("pay_se_postpaid_transactions", 1),
                Feature("pay_se_prepaid_transactions", 1),
                Feature("pay_get_clock_skew_millis", 1),
                Feature("pay_renew_commuter_pass_for_se_prepaid_card", 1),
                Feature("pay_remove_se_postpaid_token", 1),
                Feature("pay_change_se_postpaid_default_status", 1),
                Feature("pay_wear_payment_methods", 2),
                Feature("pay_wear_closed_loop_cards", 1),
                Feature("pay_perform_wear_operation", 1),
                Feature("pay_delete_se_prepaid_card", 1),
                Feature("pay_transit_issuer_tos", 1),
                Feature("pay_get_se_mfi_prepaid_cards", 1),
                Feature("pay_get_last_user_present_timestamp", 1),
                Feature("pay_mdoc", 7),
                Feature("pay_get_se_feature_readiness_status", 1),
                Feature("pay_recover_se_card", 1),
                Feature("pay_set_wallet_item_surfacing", 2),
                Feature("pay_set_se_transit_default", 1),
                Feature("pay_get_wallet_bulletins", 2),
                Feature("pay_mse_operation", 1),
                Feature("pay_clear_bulletin_interaction_for_dev", 1),
                Feature("pay_get_pending_intent_for_wallet_on_wear", 2),
                Feature("pay_get_predefined_rotating_barcode_values", 1),
                Feature("pay_get_mdl_refresh_timestamps", 1),
                Feature("pay_store_mdl_refresh_timestamp", 1),
                Feature("pay_perform_id_card_operation", 1),
                Feature("pay_block_closed_loop_cards", 1),
                Feature("pay_delete_data_for_tests", 1),
                Feature("pay_perform_closed_loop_operation", 1)
            )
        })
    }
}

class PayServiceImpl : IPayService.Stub() {
    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}