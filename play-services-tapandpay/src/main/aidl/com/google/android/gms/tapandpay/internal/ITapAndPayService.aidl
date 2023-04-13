package com.google.android.gms.tapandpay.internal;

import com.google.android.gms.tapandpay.internal.ITapAndPayServiceCallbacks;

interface ITapAndPayService {
//    void setSelectedToken(in SetSelectedTokenRequest request, ITapAndPayServiceCallbacks callbacks) = 0;
//    void getAllCards(in GetAllCardsRequest request, ITapAndPayServiceCallbacks callbacks) = 1;
//    void deleteToken(in DeleteTokenRequest request, ITapAndPayServiceCallbacks callbacks) = 2;
//    void firstPartyTokenizePan(in FirstPartyTokenizePanRequest request, ITapAndPayServiceCallbacks callbacks) = 3;
//    void setActiveAccount(in SetActiveAccountRequest request, ITapAndPayServiceCallbacks callbacks) = 4;
//    void showSecurityPrompt(in ShowSecurityPromptRequest request, ITapAndPayServiceCallbacks callbacks) = 7;
//    void getActiveAccount(in GetActiveAccountRequest request, ITapAndPayServiceCallbacks callbacks) = 8;
    void registerDataChangedListener(ITapAndPayServiceCallbacks callbacks) = 9;
//    void isDeviceUnlockedForPayment(in IsDeviceUnlockedForPaymentRequest request, ITapAndPayServiceCallbacks callbacks) = 10;
//    void promptDeviceUnlockForPayment(in PromptDeviceUnlockForPaymentRequest request, ITapAndPayServiceCallbacks callbacks) = 11;
//    void sendTapEvent(in SendTapEventRequest request, ITapAndPayServiceCallbacks callbacks) = 12;
//    void getReceivesTransactionNotification(in GetReceivesTransactionNotificationsRequest request, ITapAndPayServiceCallbacks callbacks) = 13;
//    void setReceivesTransactionNotification(in SetReceivesTransactionNotificationsRequest request, ITapAndPayServiceCallbacks callbacks) = 14;
//    void retrieveInAppPaymentCredential(in RetrieveInAppPaymentCredentialRequest request, ITapAndPayServiceCallbacks callbacks) = 15;
//    void getActiveCardsForAccount(in GetActiveCardsForAccountRequest request, ITapAndPayServiceCallbacks callbacks) = 17;
//    void getAnalyticsContext(ITapAndPayServiceCallbacks callbacks) = 19;
//    void getActiveWalletId(ITapAndPayServiceCallbacks callbacks) = 20;
    void getTokenStatus(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks) = 21;
//    void issuerTokenize(int tokenProvider, String issuerTokenId, String s2, ITapAndPayServiceCallbacks callbacks) = 22;
//    void requestSelectToken(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks) = 23;
//    void requestDeleteToken(int tokenProvider, String issuerTokenId, ITapAndPayServiceCallbacks callbacks) = 24;
//    void isDeviceUnlockedForInAppPayment(in IsDeviceUnlockedForInAppPaymentRequest request, ITapAndPayServiceCallbacks callbacks) = 25;
//    void reportInAppTransactionCompleted(in ReportInAppTransactionCompletedRequest request, ITapAndPayServiceCallbacks callbacks) = 26;
//    void pushTokenize(in PushTokenizeRequest request, ITapAndPayServiceCallbacks callbacks) = 27;
//    void createWallet(ITapAndPayServiceCallbacks callbacks) = 28;
    void getStableHardwareId(ITapAndPayServiceCallbacks callbacks) = 29;
//    void getEnvironment(ITapAndPayServiceCallbacks callbacks) = 30;
//    void enablePayOnWear(in EnablePayOnWearRequest request, ITapAndPayServiceCallbacks callbacks) = 31;
//    void isPayPalAvailable(ITapAndPayServiceCallbacks callbacks) = 32;
//    void unknown34(ITapAndPayServiceCallbacks callbacks) = 33;
//    void getSecurityParams(ITapAndPayServiceCallbacks callbacks) = 34;
//    void getNotificationSettings(in GetNotificationSettingsRequest request, ITapAndPayServiceCallbacks callbacks) = 36;
//    void setNotificationSettings(in SetNotificationSettingsRequest request, ITapAndPayServiceCallbacks callbacks) = 37;
}
