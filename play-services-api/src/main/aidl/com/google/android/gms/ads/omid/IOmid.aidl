package com.google.android.gms.ads.omid;

import com.google.android.gms.dynamic.IObjectWrapper;

interface IOmid {
    boolean initializeOmid(IObjectWrapper context) = 1;
    IObjectWrapper createHtmlAdSession(String version, IObjectWrapper webView, String customReferenceData, String impressionOwner, String altImpressionOwner) = 2;
    void startAdSession(IObjectWrapper adSession) = 3;
    void registerAdView(IObjectWrapper adSession, IObjectWrapper view) = 4;
    String getVersion() = 5;
    void finishAdSession(IObjectWrapper adSession) = 6;
    void addFriendlyObstruction(IObjectWrapper adSession, IObjectWrapper view) = 7;
    IObjectWrapper createHtmlAdSessionWithPartnerName(String version, IObjectWrapper webView, String customReferenceData, String impressionOwner, String altImpressionOwner, String parterName) = 8;
    IObjectWrapper createJavascriptAdSessionWithPartnerNameImpressionCreativeType(String version, IObjectWrapper webView, String customReferenceData, String impressionOwner, String altImpressionOwner, String parterName, String impressionType, String creativeType, String contentUrl) = 9;
    IObjectWrapper createHtmlAdSessionWithPartnerNameImpressionCreativeType(String version, IObjectWrapper webView, String customReferenceData, String impressionOwner, String altImpressionOwner, String parterName, String impressionType, String creativeType, String contentUrl) = 10;
}