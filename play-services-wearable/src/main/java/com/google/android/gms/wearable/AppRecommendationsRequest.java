package com.google.android.gms.wearable;

import org.microg.safeparcel.AutoSafeParcelable;
import org.microg.safeparcel.SafeParceled;

public class AppRecommendationsRequest extends AutoSafeParcelable {
    @SafeParceled(1)
    public String androidId;
    @SafeParceled(2)
    public int searchFlags;
    @SafeParceled(3)
    public int limit;

    public AppRecommendationsRequest() {
    }

    public AppRecommendationsRequest(String androidId, int searchFlags, int limit) {
        this.androidId = androidId;
        this.searchFlags = searchFlags;
        this.limit = limit;
    }

    @Override
    public String toString() {
        return "AppRecommendationsRequest{androidId='" + androidId + "', searchFlags=" + searchFlags + ", limit=" + limit + "}";
    }

    public static final Creator<AppRecommendationsRequest> CREATOR = new AutoCreator<>(AppRecommendationsRequest.class);
}

