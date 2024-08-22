package com.google.android.gms.pay.internal;

import android.app.PendingIntent;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.pay.EmoneyReadiness;
import com.google.android.gms.pay.GetWalletStatusResponse;
import com.google.android.gms.pay.PayApiError;

interface IPayServiceCallbacks {
    oneway void onStatus(in Status status) = 1;
//    oneway void onGetPayGlobalActionCardsResponse(in Status status, in GetPayGlobalActionCardsResponse response) = 2;
    oneway void onPendingIntentForWalletOnWear(in Status status, in PendingIntent pendingIntent) = 3;
//    oneway void onProtoSafeParcelable(in Status status, in ProtoSafeParcelable proto) = 4;
//    oneway void onDataChangeListenerResponse(in DataChangeListenerResponse response) = 5;
//    oneway void onGetSortOrderResponse(in Status status, in GetSortOrderResponse response) = 6;
    oneway void onStatusAndBoolean(in Status status, boolean b) = 7;
//    oneway void onGetTransactionsResponse(in Status status, in GetTransactionsResponse response) = 8;
    oneway void onPayApiError(in PayApiError error) = 9;
//    oneway void onGetOutstandingPurchaseOrderIdResponse(in Status status, in GetOutstandingPurchaseOrderIdResponse response) = 10;
//    oneway void onGetClosedLoopBundleResponse(in Status status, in GetClosedLoopBundleResponse response) = 11;
//    oneway void onGetPayCardArtResponse(in Status status, in GetPayCardArtResponse response) = 12;
//    oneway void onSyncTransactionsResponse(in Status status, in SyncTransactionsResponse response) = 13;
    oneway void onStatusAndByteArray(in Status status, in byte[] bArr) = 14;
//    oneway void onGetPassesResponse(in Status status, in GetPassesResponse response) = 15;
    oneway void onStatusAndLong(in Status status, long l) = 16;
    oneway void onPendingIntent(in Status status) = 17;
//    oneway void onGp3SupportInfo(in Status status, Gp3SupportInfo info) = 18;
    oneway void onPayApiAvailabilityStatus(in Status status, int availabilityStatus) = 19;
//    oneway void onGetTransitCardsResponse(in Status status, in GetTransitCardsResponse response) = 20;
//    oneway void onGetWalletStatus(in Status status, in GetWalletStatusResponse getWalletStatusResponse) = 21;
//    oneway void onGetSeFeatureReadinessStatusResponse(in Status status, in GetSeFeatureReadinessStatusResponse response) = 22;
//    oneway void onSyncTransactionByIdResponse(in Status status, in SyncTransactionByIdResponse response) = 23;
//    oneway void onGetImagesForValuableResponse(in Status status, in GetImagesForValuableResponse response) = 24;
    oneway void onEmoneyReadiness(in Status status, in EmoneyReadiness emoneyReadiness) = 25;
}