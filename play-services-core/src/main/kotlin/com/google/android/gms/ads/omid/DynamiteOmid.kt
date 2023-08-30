/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.ads.omid

import android.os.RemoteException
import android.util.Log
import androidx.annotation.Keep
import com.google.android.gms.dynamic.IObjectWrapper
import com.google.android.gms.dynamic.ObjectWrapper

private const val TAG = "Omid"

@Keep
class DynamiteOmid : IOmid.Stub() {
    override fun initializeOmid(context: IObjectWrapper?): Boolean {
        Log.d(TAG, "initializeOmid")
        return true
    }

    override fun createHtmlAdSession(version: String, webView: IObjectWrapper?, customReferenceData: String, impressionOwner: String, altImpressionOwner: String): IObjectWrapper {
        return createHtmlAdSessionWithPartnerName(version, webView, customReferenceData, impressionOwner, altImpressionOwner, "Google")
    }

    override fun startAdSession(adSession: IObjectWrapper?) {
        Log.d(TAG, "startAdSession")
    }

    override fun registerAdView(adSession: IObjectWrapper?, view: IObjectWrapper?) {
        Log.d(TAG, "registerAdView")
    }

    override fun getVersion(): String {
        Log.d(TAG, "getVersion")
        return "1.5.0"
    }

    override fun finishAdSession(adSession: IObjectWrapper?) {
        Log.d(TAG, "finishAdSession")
    }

    override fun addFriendlyObstruction(adSession: IObjectWrapper?, view: IObjectWrapper?) {
        Log.d(TAG, "addFriendlyObstruction")
    }

    override fun createHtmlAdSessionWithPartnerName(version: String, webView: IObjectWrapper?, customReferenceData: String, impressionOwner: String, altImpressionOwner: String, partnerName: String): IObjectWrapper {
        Log.d(TAG, "createHtmlAdSessionWithPartnerName($version, $customReferenceData, $impressionOwner, $altImpressionOwner, $partnerName)")
        return ObjectWrapper.wrap(AdSession())
    }

    override fun createJavascriptAdSessionWithPartnerNameImpressionCreativeType(version: String, webView: IObjectWrapper?, customReferenceData: String, impressionOwner: String, altImpressionOwner: String, partnerName: String, impressionType: String, creativeType: String, contentUrl: String): IObjectWrapper {
        Log.d(TAG, "createJavascriptAdSessionWithPartnerNameImpressionCreativeType($version, $customReferenceData, $impressionOwner, $altImpressionOwner, $partnerName, $impressionType, $creativeType, $contentUrl)")
        return ObjectWrapper.wrap(AdSession())
    }

    override fun createHtmlAdSessionWithPartnerNameImpressionCreativeType(version: String, webView: IObjectWrapper?, customReferenceData: String, impressionOwner: String, altImpressionOwner: String, partnerName: String, impressionType: String, creativeType: String, contentUrl: String): IObjectWrapper {
        Log.d(TAG, "createHtmlAdSessionWithPartnerNameImpressionCreativeType($version, $customReferenceData, $impressionOwner, $altImpressionOwner, $partnerName, $impressionType, $creativeType, $contentUrl)")
        return ObjectWrapper.wrap(AdSession())
    }
}
