package com.google.android.gms.tapandpay.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.tapandpay.issuer.TokenStatus;

interface ITapAndPayServiceCallbacks {
    void onSetSelectedTokenResponse(in Status status) = 1;
    void onStatus3(in Status status, in Bundle data) = 2;
//    void onGetAllCardsResponse(in Status status, in GetAllCardsResponse response) = 3;
    void onDeleteTokenResponse(in Status status) = 4;
    void onSetActiveAccountResponse(in Status status) = 5;
//    void onGetActiveAccountResponse(in Status status, in GetActiveAccountResponse response) = 7;
    void onStatus9(in Status status) = 8;
    void onReturn10() = 9;
    void onIsDeviceUnlockedForPaymentResponse(in Status status, boolean isDeviceUnlockedForPayment) = 10;
    void onStatus12(in Status status) = 11;
    void onGetReceivesTransactionNotificationsResponse(in Status status, boolean receivesTransactionNotifications) = 12;
    void onSetReceivesTransactionNotificationsResponse(in Status status) = 13;
//    void onGetActiveCardsForAccountResponse(in Status status, in GetActiveCardsForAccountResponse response) = 14;
//    void onRetrieveInAppPaymentCredentialResponse(in Status status, in RetrieveInAppPaymentCredentialResponse response) = 16;
    void onGetAnalyticsContextResponse(in Status status, String analyticsContext) = 17;
    void onTokenStatus(in Status status, in TokenStatus tokenStatus) = 19;
    void onIsDeviceUnlockedForInAppPaymentResponse(in Status status, boolean isDeviceUnlockedForInAppPayment) = 20;
    void onReportInAppTransactionCompletedResponse(in Status status) = 21;
    void onGetStableHardwareIdResponse(in Status status, String stableHardwareId) = 22;
    void onGetEnvironmentResponse(in Status status, String env) = 23;
    void onEnablePayOnWearResponse(in Status status) = 24;
    void onIsPayPalAvailableResponse(in Status status, boolean IsPayPalAvailable) = 25;
//    void onGetSecurityParamsResponse(in Status status, in GetSecurityParamsResponse response) = 26;
//    void onGetNotificationSettingsResponse(in Status status, in GetNotificationSettingsResponse response) = 27;
    void onSetNotificationSettingsResponse(in Status status) = 28;
//    void onGetAvailableOtherPaymentMethodsResponse(in Status status, in GetAvailableOtherPaymentMethodsResponse response) = 29;
//    void onGetActiveTokensForAccountResponse(in Status status, in GetActiveTokensForAccountResponse response) = 30;
//    void onGetSeChipTransactionsResponse(in Status status, in GetSeChipTransactionsResponse response) = 34;
//    void onReserveResourceResponse(in Status status, in ReserveResourceResponse response) = 35;
    void onReleaseResourceResponse(in Status status) = 36;
    void onDisableSelectedTokenResponse(in Status status) = 37;
//    void onGetFelicaTosAcceptanceResponse(in Status status, in GetFelicaTosAcceptanceResponse response) = 38;
    void onSetFelicaTosAcceptanceResponse(in Status status) = 39;
//    void onRefreshSeCardsResponse(in Status status, in RefreshSeCardsResponse response) = 40;
//    void onGetGlobalActionCardsResponse(in Status status, in GetGlobalActionCardsResponse response) = 41;
    void onGetLinkingTokenResponse(in Status status, String linkingToken) = 42;
    void onBlockPaymentCardsResponse(in Status status) = 43;
    void onUnblockPaymentCardsResponse(in Status status) = 44;
//    void onGetLastAttestationResultResponse(in Status status, in GetLastAttestationResultResponse response) = 45;
//    void onQuickAccessWalletConfig(in Status status, in QuickAccessWalletConfig config) = 46;
}
