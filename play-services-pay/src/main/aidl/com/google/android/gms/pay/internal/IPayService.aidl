package com.google.android.gms.pay.internal;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.pay.internal.IPayServiceCallbacks;
import com.google.android.gms.pay.DataChangeListenerRequest;
import com.google.android.gms.pay.GetBulletinsRequest;
import com.google.android.gms.pay.GetClosedLoopCardsRequest;
import com.google.android.gms.pay.GetClosedLoopCardsFromServerRequest;
import com.google.android.gms.pay.GetDigitalCarKeysRequest;
import com.google.android.gms.pay.GetOnboardingInfoRequest;
import com.google.android.gms.pay.GetPayCapabilitiesRequest;
import com.google.android.gms.pay.GetPaymentMethodsRequest;
import com.google.android.gms.pay.GetSortOrderRequest;
import com.google.android.gms.pay.GetValuablesRequest;
import com.google.android.gms.pay.GetValuablesFromServerRequest;

interface IPayService {



    void getValuables(in GetValuablesRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 4;
    void getValuablesFromServer(in GetValuablesFromServerRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 5;

    void getClosedLoopCards(in GetClosedLoopCardsRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 7;
    void getClosedLoopCardsFromServer(in GetClosedLoopCardsFromServerRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 8;


    void registerDataChangedListener(in DataChangeListenerRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 11;












    void getSortOrder(in GetSortOrderRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 23;





    void getPaymentMethods(in GetPaymentMethodsRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 29;

























    void getOnboardingInfo(in GetOnboardingInfoRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 54;























    void getPayCapabilities(in GetPayCapabilitiesRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 80;


















    void getDigitalCarKeys(in GetDigitalCarKeysRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 101;

















    void getWalletBulletins(in GetBulletinsRequest request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 114;












    void performIdCard(in byte[] request, IPayServiceCallbacks callbacks, in ApiMetadata metadata) = 128;
}