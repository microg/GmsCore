/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.tapandpay

import android.app.KeyguardManager
import android.content.Context
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcel
import android.util.Base64
import android.util.Log
import android.util.SparseArray
import androidx.core.content.getSystemService
import com.google.android.gms.common.Feature
import com.google.android.gms.common.api.ApiMetadata
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.tapandpay.TapAndPayStatusCodes.TAP_AND_PAY_NO_ACTIVE_WALLET
import com.google.android.gms.tapandpay.firstparty.AccountInfo
import com.google.android.gms.tapandpay.firstparty.GetActiveAccountResponse
import com.google.android.gms.tapandpay.firstparty.GetAllCardsResponse
import com.google.android.gms.tapandpay.firstparty.GetSecurityParamsResponse
import com.google.android.gms.tapandpay.firstparty.IsDeviceRecentlyUnlockedRequest
import com.google.android.gms.tapandpay.firstparty.IsDeviceRecentlyUnlockedResponse
import com.google.android.gms.tapandpay.firstparty.LogUserCurrentScreenRequest
import com.google.android.gms.tapandpay.firstparty.RefreshSeCardsResponse
import com.google.android.gms.tapandpay.firstparty.RegisterServiceListenerRequest
import com.google.android.gms.tapandpay.internal.ITapAndPayService
import com.google.android.gms.tapandpay.internal.ITapAndPayServiceCallbacks
import com.google.android.gms.tapandpay.internal.firstparty.GetActiveAccountRequest
import com.google.android.gms.tapandpay.internal.firstparty.GetAllCardsRequest
import com.google.android.gms.tapandpay.internal.firstparty.IsDeviceUnlockedForPaymentRequest
import com.google.android.gms.tapandpay.internal.firstparty.RefreshSeCardsRequest
import com.google.android.gms.tapandpay.internal.firstparty.SetActiveAccountRequest
import com.google.android.gms.tapandpay.internal.firstparty.SetSelectedTokenRequest
import com.google.android.gms.tapandpay.issuer.ListTokensRequest
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest
import org.microg.gms.BaseService
import org.microg.gms.common.GmsService
import org.microg.gms.common.GooglePackagePermission
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.ExtendedPackageInfo
import org.microg.gms.utils.toBase64
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GmsTapAndPay"

class TapAndPayService : BaseService(TAG, GmsService.WALLET_TAP_AND_PAY) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackage(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")
        callback.onPostInitCompleteWithConnectionInfo(CommonStatusCodes.SUCCESS, TapAndPayImpl(this, packageName), ConnectionInfo().apply {
            features = arrayOf(
                Feature("tapandpay", 1),
                Feature("tapandpay_account_linking", 1),
                Feature("tapandpay_add_service_listener", 1),
                Feature("tapandpay_backup_and_restore_tokenize", 1),
                Feature("tapandpay_block_payment_cards", 1),
                Feature("tapandpay_check_contactless_eligibility", 1),
                Feature("tapandpay_check_notification_governance", 1),
                Feature("tapandpay_dismiss_quick_access_wallet", 1),
                Feature("tapandpay_enable_secure_keyguard", 1),
                Feature("tapandpay_felica_tos", 1),
                Feature("tapandpay_get_active_wallet_infos", 1L),
                Feature("tapandpay_get_all_cards_for_account", 1),
                Feature("tapandpay_get_contactless_setup_configuration", 1),
                Feature("tapandpay_get_data_for_backup", 1),
                Feature("tapandpay_get_environment", 1L),
                Feature("tapandpay_get_last_attestation_result", 1),
                Feature("tapandpay_get_quick_access_tile_status", 1),
                Feature("tapandpay_get_stable_hardware_id", 1L),
                Feature("tapandpay_get_token_details", 1L),
                Feature("tapandpay_get_token_status", 1L),
                Feature("tapandpay_global_actions", 1),
                Feature("tapandpay_has_eligible_tokenization_target", 1L),
                Feature("tapandpay_issuer_api", 2),
                Feature("tapandpay_issuer_tokenize", 1),
                Feature("tapandpay_override_payment_network", 3L),
                Feature("tapandpay_get_parental_consent_intent", 1L),
                Feature("tapandpay_set_supervised_child_account_type_and_reset_onboarding_info", 1L),
                Feature("tapandpay_get_is_supervised_child_wallet_user", 1L),
                Feature("tapandpay_perform_secure_element_management_operation", 1L),
                Feature("tapandpay_perform_tokenization_operation", 1L),
                Feature("tapandpay_polling_frame_handler", 1L),
                Feature("tapandpay_push_tokenize_session", 6),
                Feature("tapandpay_push_tokenize", 1L),
                Feature("tapandpay_quick_access_wallet", 1),
                Feature("tapandpay_report_unlock", 1L),
                Feature("tapandpay_request_delete_token", 1L),
                Feature("tapandpay_request_select_token", 1L),
                Feature("tapandpay_secureelement", 1),
                Feature("tapandpay_send_wear_request_to_phone", 1),
                Feature("tapandpay_settings", 2L),
                Feature("tapandpay_screen_logging", 1L),
                Feature("tapandpay_show_wear_card_management_view", 1),
                Feature("tapandpay_sync_device_info", 1),
                Feature("tapandpay_token_listing", 3),
                Feature("tapandpay_token_listing_with_request", 1),
                Feature("tapandpay_tokenize_account", 1),
                Feature("tapandpay_tokenize_cache", 1),
                Feature("tapandpay_tokenize_pan", 1),
                Feature("tapandpay_transmission_event", 1),
                Feature("tapandpay_wallet_feedback_psd", 1),
                Feature("tapandpay_wallet_set_tap_doodle_enabled", 1L),
                Feature("tapandpay_wallet_ui_shown_status", 1L),
                Feature("tapandpay_set_receives_ineligible_card_notification", 1L)
            )
        })
    }
}

class TapAndPayImpl(private val context: Context, private val packageName: String) : ITapAndPayService.Stub() {

    private val isFirstParty
        get() = ExtendedPackageInfo(context, packageName).hasGooglePackagePermission(GooglePackagePermission.WALLET)

    private var accountId: String? = null
    private var accountName: String? = null

    override fun setSelectedToken(request: SetSelectedTokenRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onSetSelectedTokenResponse(Status.INTERNAL_ERROR)
            return
        }
        Log.d(TAG, "Not yet implemented: setSelectedToken($request)")
        callbacks?.onSetSelectedTokenResponse(Status.SUCCESS)
    }

    override fun getAllCards(request: GetAllCardsRequest?, callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "getAllCards()")
        callbacks.onGetAllCardsResponse(Status.SUCCESS, GetAllCardsResponse(emptyArray(), null, null, null, SparseArray(), ByteArray(0)))
    }

    override fun setActiveAccount(request: SetActiveAccountRequest?, callbacks: ITapAndPayServiceCallbacks?, metadata: ApiMetadata) {
        if (!isFirstParty) {
            callbacks?.onSetActiveAccountResponse(Status.INTERNAL_ERROR)
            return
        }
        Log.d(TAG, "setActiveAccount(${request?.accountName})")
        this.accountId = request?.accountName // TODO: Get actual account id
        this.accountName = request?.accountName
        callbacks?.onSetActiveAccountResponse(Status.SUCCESS)
    }

    override fun getActiveAccount(request: GetActiveAccountRequest?, callbacks: ITapAndPayServiceCallbacks?, metadata: ApiMetadata) {
        if (!isFirstParty) {
            callbacks?.onGetActiveAccountResponse(Status.INTERNAL_ERROR, null)
            return
        }
        val accountInfo = accountId?.let { AccountInfo(accountId, accountName, 1) }
        Log.d(TAG, "getActiveAccount() = $accountInfo")
        callbacks?.onGetActiveAccountResponse(Status.SUCCESS, GetActiveAccountResponse(accountInfo))
    }

    override fun registerDataChangedListener(callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "registerDataChangedListener()")
        callbacks.onStatus(Status.SUCCESS)
    }

    override fun isDeviceUnlockedForPayment(request: IsDeviceUnlockedForPaymentRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onHandleStatusPendingIntent(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "isDeviceUnlockedForPayment($request) = true")
        callbacks?.onIsDeviceUnlockedForPaymentResponse(Status.SUCCESS, true)
    }

    override fun getActiveWalletId(callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "getActiveWalletId: ")
        callbacks.onGetActiveWalletIdResponse(Status(TAP_AND_PAY_NO_ACTIVE_WALLET), "")
    }

    override fun getTokenStatus(tokenProvider: Int, issuerTokenId: String, callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "getTokenStatus($tokenProvider, $issuerTokenId)")
        callbacks.onGetTokenStatusResponse(Status(TAP_AND_PAY_NO_ACTIVE_WALLET), null)
    }

    override fun pushTokenize(request: PushTokenizeRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: pushTokenize($request)")
        callbacks?.onHandleStatusPendingIntent(Status(TAP_AND_PAY_NO_ACTIVE_WALLET), null)
    }

    override fun createWallet(callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: createWallet")
        callbacks?.onHandleStatusPendingIntent(Status(CommonStatusCodes.RESOLUTION_REQUIRED, null, ), Bundle.EMPTY)
    }

    override fun getStableHardwareId(callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "getStableHardwareId()")
        callbacks.onGetStableHardwareIdResponse(Status.SUCCESS, "")
    }

    override fun getSecurityParams(callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onGetSecurityParamsResponse(Status.INTERNAL_ERROR, GetSecurityParamsResponse(false, false, false, false))
            return
        }
        val isDeviceSecure = runCatching { SDK_INT >= 23 && (context.getSystemService<KeyguardManager>()?.isDeviceSecure ?: false) }.getOrDefault(false)
        Log.d(TAG, "getSecurityParams() = $isDeviceSecure")
        callbacks?.onGetSecurityParamsResponse(Status.SUCCESS, GetSecurityParamsResponse(isDeviceSecure, false, false, false))
    }

    override fun refreshSeCards(request: RefreshSeCardsRequest?, callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "refreshSeCards()")
        callbacks.onRefreshSeCardsResponse(Status.SUCCESS, RefreshSeCardsResponse())
    }

    override fun listTokensDefault(callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        listTokens(ListTokensRequest(), callbacks, metadata)
    }

    override fun tokenization(data: ByteArray?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onByteArray(Status.INTERNAL_ERROR, byteArrayOf())
            return
        }
        Log.d(TAG, "Not yet implemented: tokenization(${data?.toBase64(Base64.NO_WRAP)})")
        callbacks?.onByteArray(Status(CommonStatusCodes.DEVELOPER_ERROR, "Unimplemented"), byteArrayOf())
    }

    override fun registerServiceListener(request: RegisterServiceListenerRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: registerServiceListener($request)")
        callbacks?.onStatus(Status.SUCCESS)
    }

    override fun unregisterServiceListener(request: RegisterServiceListenerRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        Log.d(TAG, "Not yet implemented: unregisterServiceListener($request)")
        callbacks?.onStatus(Status.SUCCESS)
    }

    override fun listTokens(request: ListTokensRequest?, callbacks: ITapAndPayServiceCallbacks, metadata: ApiMetadata) {
        Log.d(TAG, "listTokens($request)")
        callbacks.onListTokensResponse(Status.SUCCESS, emptyArray())
    }

    override fun getIsSupervisedChildWalletUser(callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onGetIsSupervisedChildWalletUserResponse(Status.INTERNAL_ERROR, false)
            return
        }
        Log.d(TAG, "getIsSupervisedChildWalletUser() = false")
        callbacks?.onGetIsSupervisedChildWalletUserResponse(Status.SUCCESS, false)
    }

    override fun logUserCurrentScreen(request: LogUserCurrentScreenRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onStatus(Status.INTERNAL_ERROR)
            return
        }
        Log.d(TAG, "logUserCurrentScreen($request)")
        callbacks?.onStatus(Status.SUCCESS)
    }

    override fun isDeviceRecentlyUnlocked(request: IsDeviceRecentlyUnlockedRequest?, callbacks: ITapAndPayServiceCallbacks?, apiMetadata: ApiMetadata?) {
        if (!isFirstParty) {
            callbacks?.onIsDeviceRecentlyUnlockedResponse(Status.INTERNAL_ERROR, null)
            return
        }
        Log.d(TAG, "isDeviceRecentlyUnlocked($request) = true")
        callbacks?.onIsDeviceRecentlyUnlockedResponse(Status.SUCCESS, IsDeviceRecentlyUnlockedResponse(true))
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}
