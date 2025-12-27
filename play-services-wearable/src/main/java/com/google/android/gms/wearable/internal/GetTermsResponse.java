package com.google.android.gms.wearable.internal;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

import java.util.List;

public class GetTermsResponse extends AutoSafeParcelable {
    @SafeParceled(1)
    public final int statusCode;
    @SafeParceled(2)
    public final List consents; // correct name is unknown, but assuming this is a consent list

    public GetTermsResponse(int statusCode, List consents) {
        this.statusCode = statusCode;
        this.consents = consents;
    }

    public static final Creator<GetTermsResponse> CREATOR = new AutoCreator<GetTermsResponse>(GetTermsResponse.class);
}
