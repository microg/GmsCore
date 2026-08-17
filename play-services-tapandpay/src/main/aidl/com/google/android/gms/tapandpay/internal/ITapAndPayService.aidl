package com.google.android.gms.tapandpay.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.tapandpay.firstparty.IsDeviceRecentlyUnlockedRequest;
import com.google.android.gms.tapandpay.firstparty.LogUserCurrentScreenRequest;
import com.google.android.gms.tapandpay.firstparty.RegisterServiceListenerRequest;
import com.google.android.gms.tapandpay.internal.ITapAndPayServiceCallbacks;
import com.google.android.gms.tapandpay.internal.firstparty.GetActiveAccountRequest;
import com.google.android.gms.tapandpay.internal.firstparty.GetAllCardsRequest;
import com.google.android.gms.tapandpay.internal.firstparty.RefreshSeCardsRequest;
import com.google.android.gms.tapandpay.internal.firstparty.IsDeviceUnlockedForPaymentRequest;
import com.google.android.gms.tapandpay.internal.firstparty.SetActiveAccountRequest;
import com.google.android.gms.tapandpay.internal.firstparty.SetSelectedTokenRequest;
import com.google.android.gms.tapandpay.issuer.ListTokensRequest;
import com.google.android.gms.tapandpay.issuer.PushTokenizeRequest;

interface ITapAndPayService {
    void setSelectedToken(in SetSelectedTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 0;
    void getAllCards(in GetAllCardsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 1;
//    void deleteToken(in DeleteTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 2;
//    void firstPartyTokenizePan(in FirstPartyTokenizePanRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 3;
    void setActiveAccount(in SetActiveAccountRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 4;


//    void showSecurityPrompt(in ShowSecurityPromptRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 7;
    void getActiveAccount(in GetActiveAccountRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 8;
    void registerDataChangedListener(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 9;
    void isDeviceUnlockedForPayment(in IsDeviceUnlockedForPaymentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 10;
//    void promptDeviceUnlockForPayment(in PromptDeviceUnlockForPaymentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 11;
//    void sendTapEvent(in SendTapEventRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 12;
//    void getReceivesTransactionNotification(in GetReceivesTransactionNotificationsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 13;
//    void setReceivesTransactionNotification(in SetReceivesTransactionNotificationsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 14;
//    void retrieveInAppPaymentCredential(in RetrieveInAppPaymentCredentialRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 15;

//    void getActiveCardsForAccount(in GetActiveCardsForAccountRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 17;

//    void getAnalyticsContext(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 19;
    void getActiveWalletId(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 20;
    void getTokenStatus(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 21;
//    void issuerTokenize(int tokenProvider, String issuerTokenId, String s2, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 22;
//    void requestSelectToken(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 23;
//    void requestDeleteToken(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 24;
//    void isDeviceUnlockedForInAppPayment(in IsDeviceUnlockedForInAppPaymentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 25;
//    void reportInAppTransactionCompleted(in ReportInAppTransactionCompletedRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 26;
    void pushTokenize(in PushTokenizeRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 27;
    void createWallet(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 28;
    void getStableHardwareId(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 29;
//    void getEnvironment(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 30;
//    void enablePayOnWear(in EnablePayOnWearRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 31;
//    void isPayPalAvailable(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 32;
    void getSecurityParams(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 34;

//    void getNotificationSettings(in GetNotificationSettingsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 36;
//    void setNotificationSettings(in SetNotificationSettingsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 37;
//    void addOtherPaymentOption(in AddOtherPaymentOptionRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 38;
//    void getAvailableOtherPaymentMethods(in GetAvailableOtherPaymentMethodsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 39;
//    void keyguardDismissed(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 40;
//    void reportInAppManualUnlock(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 41;
//    Status enableNfc(in ApiMetadata apiMetadata) = 42;





//    void getSeChipTransactions(in GetSeChipTransactionsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 48;
//    void deleteDataForTests(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 49;


//    void disableSelectedToken(in DisableSelectedTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 52;
//    void setFelicaTosAcceptance(in SetFelicaTosAcceptanceRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 53;
//    void getFelicaTosAcceptance(in GetFelicaTosAcceptanceRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 54;
//    void fun55(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 55;
    void refreshSeCards(in RefreshSeCardsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 56;
//    void tokenizeAccount(in TokenizeAccountRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 57;
//    void getGlobalActionCards(in GetGlobalActionCardsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 58;
//    void selectGlobalActionCard(in SelectGlobalActionCardRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 59;
//    void fun60(string data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 60;
//    void fun61(long data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 61;
//    void fun62(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 62;

//    void syncDeviceInfo(in SyncDeviceInfoRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 64;
//    void sendTransmissionEvent(in SendTransmissionEventRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 65;
//    void createPushProvisionSession(in CreatePushProvisionSessionRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 66;
//    void serverPushProvision(in ServerPushProvisionRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 67;
//    void getLastAttestationResult(in GetLastAttestationResultRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 68;
//    void dismissQuickAccessWallet(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 69;
//    void getQuickAccessWalletConfig(in GetQuickAccessWalletConfigRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 70;
//    void setQuickAccessWalletCards(in SetQuickAccessWalletCardsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 71;
//    void getContactlessSetupStatus(in GetContactlessSetupStatusRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 72;
    void listTokensDefault(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 73;
//    void isTokenized(in IsTokenizedRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 74;
//    void checkContactlessEligibility(in CheckContactlessEligibilityRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 75;
//    void showWearCardManagementView(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 76;
    void tokenization(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 77;
//    void viewToken(in ViewTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 78;
//    void sendWearRequestToPairedDevice(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 79;
//    void psdLogs(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 80;
//    void getTokenDetails(in GetTokenDetailsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 81;
//    void setTapDoodleEnabled(in SetTapDoodleEnabledRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 82;
//    void reportWalletUiShowedTime(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 83;
//    void checkWalletUiRecentlyShowed(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 84;
//    void reportUnlock(in reportUnlock request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 85;
//    void enableSecureKeyguard(in EnableSecureKeyguardRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 86;
//    void setTapAndPaySettings(in SetTapAndPaySettingsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 87;
//    void getTapAndPaySettings(in GetTapAndPaySettingsRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 88;
//    void setOverridePaymentNetwork(in SetOverridePaymentNetworkRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 89;
//    void getOverridePaymentNetwork(in GetOverridePaymentNetworkRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 90;
//    void hasEligibleTokenizationTarget(in HasEligibleTokenizationTargetRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 91;
//    void tokenize(in TokenizeRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 92;
    void registerServiceListener(in RegisterServiceListenerRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 93;
    void unregisterServiceListener(in RegisterServiceListenerRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 94;
//    void getActiveWalletInfos(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 95;
//    void managedSecureElement(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 96;
//    void getCobadgedShowPaymentNetworkToggle(in GetCobadgedShowPaymentNetworkToggleRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 97;
//    void requestDeleteToken(in RequestDeleteTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 98;
//    void requestSelectToken(in RequestSelectTokenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 99;
//    void getTokenStatus(in GetTokenStatusRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 100;
    void listTokens(in ListTokensRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 101;
//    void getStableHardwareId(in GetStableHardwareIdRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 102;
//    void getEnvironment(in GetEnvironmentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 103;
//    void getParentalConsentIntent(in GetParentalConsentIntentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 104;
    void getIsSupervisedChildWalletUser(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 105;
//    void getQuickAccessTileStatus(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 106;
//    void backupAndRestoreTokenize(in BackupAndRestoreTokenizeRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 107;
//    void setReceivesIneligibleCardNotification(in SetReceivesIneligibleCardNotificationRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 108;
//    void getDataForBackup(in GetDataForBackupRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 109;
//    void checkNotificationGovernance(in byte[] data, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 110;
//    void sendPollingFrameHandlerEvent(in SendPollingFrameHandlerEventRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 111;
//    void getPollingFrameHandlerEvent(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 112;
//    void setSupervisedChildAccountTypeAndResetOnboardingInfo(in SetSupervisedChildAccountTypeAndResetOnboardingInfoRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 113;
    void logUserCurrentScreen(in LogUserCurrentScreenRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 114;
    void isDeviceRecentlyUnlocked(in IsDeviceRecentlyUnlockedRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 115;
//    void sendWearRequestToPairedDeviceWithString(String data1, in byte[] data2, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 116;
//    void unifiedTokenization(in UnifiedTokenizationRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 117;
//    void checkStorageKeyLocally(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 118;
//    void setDataStoreEnvironment(in SetDataStoreEnvironmentRequest request, ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 119;
//    void getPendingPushProvisioningToken(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 120;
//    void fun121(ITapAndPayServiceCallbacks callbacks, in ApiMetadata apiMetadata) = 121;
}
