/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.pay

import android.content.Context
import android.os.Parcel
import android.util.Base64
import android.util.Log
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.pay.DataChangeListenerRequest
import com.google.android.gms.pay.GetBulletinsRequest
import com.google.android.gms.pay.GetClosedLoopCardsFromServerRequest
import com.google.android.gms.pay.GetClosedLoopCardsRequest
import com.google.android.gms.pay.GetDigitalCarKeysRequest
import com.google.android.gms.pay.GetOnboardingInfoRequest
import com.google.android.gms.pay.GetPayCapabilitiesRequest
import com.google.android.gms.pay.GetPaymentMethodsRequest
import com.google.android.gms.pay.GetSortOrderRequest
import com.google.android.gms.pay.GetValuablesFromServerRequest
import com.google.android.gms.pay.GetValuablesRequest
import com.google.android.gms.pay.internal.IPayService
import com.google.android.gms.pay.internal.IPayServiceCallbacks
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.ExtendedPackageInfo
import org.microg.gms.utils.toBase64
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "PayService"

class PayService : BaseService(TAG, GmsService.PAY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, PayServiceImpl(this, packageName), ConnectionInfo().apply {
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

class PayServiceImpl(private val context: Context, private val packageName: String) : IPayService.Stub() {

    private val isFirstParty
        get() = ExtendedPackageInfo(context, packageName).hasGooglePackagePermission(GooglePackagePermission.WALLET)

    override fun getValuables(request: GetValuablesRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getValuables($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getValuablesFromServer(request: GetValuablesFromServerRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getValuablesFromServer($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getClosedLoopCards(request: GetClosedLoopCardsRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getClosedLoopCards($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getClosedLoopCardsFromServer(request: GetClosedLoopCardsFromServerRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getClosedLoopCardsFromServer($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun registerDataChangedListener(request: DataChangeListenerRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) return
        Log.d(TAG, "Not yet implemented: registerDataChangedListener($request)")
    }

    override fun getSortOrder(request: GetSortOrderRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onGetSortOrderResponse(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getSortOrder($request)")
        callbacks?.onGetSortOrderResponse(Status.INTERNAL_ERROR, null)
    }

    override fun getPaymentMethods(request: GetPaymentMethodsRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getPaymentMethods($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getOnboardingInfo(request: GetOnboardingInfoRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getOnboardingInfo($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getPayCapabilities(request: GetPayCapabilitiesRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: getPayCapabilities($request)")
        callbacks?.onStatus(Status.INTERNAL_ERROR)
    }

    override fun getDigitalCarKeys(request: GetDigitalCarKeysRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getDigitalCarKeys($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun getWalletBulletins(request: GetBulletinsRequest?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: getWalletBulletins($request)")
        callbacks?.onProtoSafeParcelable(Status.INTERNAL_ERROR, null)
    }

    override fun performIdCard(request: ByteArray?, callbacks: IPayServiceCallbacks?, metadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onByteArray(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "Not yet implemented: performIdCard(${request?.toBase64(Base64.NO_WRAP)})")
        callbacks?.onByteArray(Status.INTERNAL_ERROR, null)
    }


    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}