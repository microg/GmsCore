package com.google.android.gms.pay.internal;

import com.google.android.gms.pay.GetPayApiAvailabilityStatusRequest;
import com.google.android.gms.pay.internal.IPayServiceCallbacks;
import com.google.android.gms.pay.SavePassesRequest;
import com.google.android.gms.pay.SyncBundleRequest;
import com.google.android.gms.pay.CheckReadinessForEmoneyRequest;
import com.google.android.gms.pay.GetMdocCredentialRequest;
import com.google.android.gms.pay.GetPendingIntentForWalletOnWearRequest;
import com.google.android.gms.pay.NotifyCardTapEventRequest;
import com.google.android.gms.pay.PushEmoneyCardRequest;
import com.google.android.gms.pay.NotifyEmoneyCardStatusUpdateRequest;

interface IThirdPartyPayService {
       void getPayApiAvailabilityStatus(in GetPayApiAvailabilityStatusRequest request, in IPayServiceCallbacks callback) = 1;
       void savePasses(in SavePassesRequest request, in IPayServiceCallbacks callback) = 2;
       void syncBundle(in SyncBundleRequest request, in IPayServiceCallbacks callback) = 3;
       void getPendingForWalletOnWear(in GetPendingIntentForWalletOnWearRequest request,in IPayServiceCallbacks callback) = 4;
       void getMdocCredential(in GetMdocCredentialRequest request, in IPayServiceCallbacks callback) = 5;
       void notifyCardTapEvent(in NotifyCardTapEventRequest request, in IPayServiceCallbacks callback) = 6;
       void checkReadinessForEmoney(in CheckReadinessForEmoneyRequest request, in IPayServiceCallbacks callback) = 7;
       void pushEmoneyCard(in PushEmoneyCardRequest request, in IPayServiceCallbacks callback) = 8;
       void notifyEmoneyCardStatusUpdate(in NotifyEmoneyCardStatusUpdateRequest request, in IPayServiceCallbacks callback) = 9;
}