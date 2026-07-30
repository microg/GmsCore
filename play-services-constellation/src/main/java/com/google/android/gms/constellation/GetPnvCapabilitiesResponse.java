/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.constellation;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Response containing Phone Number Verification capabilities.
 * Describes which verification methods the carrier supports for RCS.
 */
public class GetPnvCapabilitiesResponse implements Parcelable {

    public static final int CAPABILITY_SMS = 1;
    public static final int CAPABILITY_TS43 = 2;
    public static final int CAPABILITY_CARRIER_APP = 4;
    public static final int CAPABILITY_EAP_AKA = 8;
    public static final int CAPABILITY_GBA = 16;
    public static final int CAPABILITY_IMS = 32;
    public static final int CAPABILITY_RCS_CONFIG = 64;
    public static final int CAPABILITY_HTTP_DIGEST = 128;
    public static final int CAPABILITY_TLS_PSK = 256;
    public static final int CAPABILITY_MO_SMS = 512;
    public static final int CAPABILITY_MT_SMS = 1024;

    private final int supportedCapabilities;
    private final int recommendedCapability;
    @Nullable
    private final String carrierName;
    @Nullable
    private final String carrierConfigUrl;
    @Nullable
    private final List<String> supportedMethods;
    private final long cacheTtlSeconds;
    private final int statusCode;
    @Nullable
    private final String statusMessage;
    private final boolean requiresUserConsent;

    public GetPnvCapabilitiesResponse(int supportedCapabilities, int recommendedCapability,
                                       @Nullable String carrierName, @Nullable String carrierConfigUrl,
                                       @Nullable List<String> supportedMethods, long cacheTtlSeconds,
                                       int statusCode, @Nullable String statusMessage,
                                       boolean requiresUserConsent) {
        this.supportedCapabilities = supportedCapabilities;
        this.recommendedCapability = recommendedCapability;
        this.carrierName = carrierName;
        this.carrierConfigUrl = carrierConfigUrl;
        this.supportedMethods = supportedMethods;
        this.cacheTtlSeconds = cacheTtlSeconds;
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.requiresUserConsent = requiresUserConsent;
    }

    protected GetPnvCapabilitiesResponse(Parcel in) {
        supportedCapabilities = in.readInt();
        recommendedCapability = in.readInt();
        carrierName = in.readString();
        carrierConfigUrl = in.readString();
        supportedMethods = in.createStringArrayList();
        cacheTtlSeconds = in.readLong();
        statusCode = in.readInt();
        statusMessage = in.readString();
        requiresUserConsent = in.readByte() != 0;
    }

    public int getSupportedCapabilities() { return supportedCapabilities; }
    public int getRecommendedCapability() { return recommendedCapability; }
    @Nullable
    public String getCarrierName() { return carrierName; }
    @Nullable
    public String getCarrierConfigUrl() { return carrierConfigUrl; }
    @Nullable
    public List<String> getSupportedMethods() { return supportedMethods; }
    public long getCacheTtlSeconds() { return cacheTtlSeconds; }
    public int getStatusCode() { return statusCode; }
    @Nullable
    public String getStatusMessage() { return statusMessage; }
    public boolean isRequiresUserConsent() { return requiresUserConsent; }

    public boolean hasCapability(int capability) { return (supportedCapabilities & capability) != 0; }
    public boolean supportsSms() { return hasCapability(CAPABILITY_SMS); }
    public boolean supportsTs43() { return hasCapability(CAPABILITY_TS43); }
    public boolean supportsEapAka() { return hasCapability(CAPABILITY_EAP_AKA); }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        dest.writeInt(supportedCapabilities);
        dest.writeInt(recommendedCapability);
        dest.writeString(carrierName);
        dest.writeString(carrierConfigUrl);
        dest.writeStringList(supportedMethods);
        dest.writeLong(cacheTtlSeconds);
        dest.writeInt(statusCode);
        dest.writeString(statusMessage);
        dest.writeByte((byte) (requiresUserConsent ? 1 : 0));
    }

    @Override
    public int describeContents() { return 0; }

    public static final Creator<GetPnvCapabilitiesResponse> CREATOR = new Creator<GetPnvCapabilitiesResponse>() {
        @Override
        public GetPnvCapabilitiesResponse createFromParcel(Parcel in) { return new GetPnvCapabilitiesResponse(in); }
        @Override
        public GetPnvCapabilitiesResponse[] newArray(int size) { return new GetPnvCapabilitiesResponse[size]; }
    };

    @NonNull
    @Override
    public String toString() {
        return "GetPnvCapabilitiesResponse{capabilities=" + supportedCapabilities
                + ", carrier='" + carrierName + '\'' + ", recommended=" + recommendedCapability + '}';
    }
}
