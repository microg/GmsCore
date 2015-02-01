package com.google.android.gms.common.api;

import org.microg.gms.common.PublicApi;

/**
 * Represents the final result of invoking an API method in Google Play Services.
 */
@PublicApi
public interface Result {
    public Status getStatus();
}
