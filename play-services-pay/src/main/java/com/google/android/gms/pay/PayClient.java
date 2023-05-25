/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.pay;

import android.app.Activity;
import android.app.PendingIntent;
import androidx.annotation.IntDef;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.HasApiKey;
import com.google.android.gms.tasks.Task;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Interface for Pay API.
 */
public interface PayClient extends HasApiKey<Api.ApiOptions.NotRequiredOptions> {
    /**
     * Debug message passed back in {@code onActivityResult()} when calling {@link #savePasses(String, Activity, int)} or
     * {@link #savePassesJwt(String, Activity, int)}.
     */
    String EXTRA_API_ERROR_MESSAGE = "extra_api_error_message";

    /**
     * Gets the {@link PayApiAvailabilityStatus} of the current user and device.
     *
     * @param requestType A {@link PayClient.RequestType} for how the API will be used.
     * @return One of the possible {@link PayApiAvailabilityStatus}.
     */
    Task<@PayApiAvailabilityStatus Integer> getPayApiAvailabilityStatus(@RequestType int requestType);

    /**
     * Create a {@link PendingIntent} for the Wear Wallet activity. May return an error if pay is not supported in this region or if the watch is not reachable.
     *
     * @param wearNodeId   The node id of the watch.
     * @param intentSource The {@link PayClient.WearWalletIntentSource} that launches the requested page.
     */
    Task<PendingIntent> getPendingIntentForWalletOnWear(String wearNodeId, @WearWalletIntentSource int intentSource);

    /**
     * Provides the product name in this market.
     */
    PayClient.ProductName getProductName();

    /**
     * Saves one or multiple passes in a JSON format.
     * <p>
     * Must be called from an {@code Activity}.
     *
     * @param json        A JSON string request to save one or multiple passes. The JSON format is consistent with the JWT save link format. Refer to
     *                    Google Pay API for Passes for an overview on how save links are generated. Only focus on how the JSON is formatted.
     *                    There is no need to sign the JSON string.
     * @param activity    The {@code Activity} that will receive the callback result.
     * @param requestCode An integer request code that will be passed back in {@code onActivityResult()}, allowing you to identify whom this result came from.
     */
    void savePasses(String json, Activity activity, int requestCode);

    /**
     * Saves one or multiple passes in a JWT format.
     * <p>
     * Must be called from an {@code Activity}.
     *
     * @param jwt         A JWT string token to save one or multiple passes. The token format is the same used in the JWT save link format. Refer to
     *                    Google Pay API for Passes for an overview on how save links are generated.
     * @param activity    The {@code Activity} that will receive the callback result.
     * @param requestCode An integer request code that will be passed back in {@code onActivityResult()}, allowing you to identify whom this result came from.
     */
    void savePassesJwt(String jwt, Activity activity, int requestCode);

    /**
     * Indicates what the product is called in this market
     */
    enum ProductName {
        GOOGLE_PAY,
        GOOGLE_WALLET
    }

    /**
     * All possible request types that will be used by callers of {@link PayClient#getPayApiAvailabilityStatus(int)}.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({RequestType.CARD_PROVISIONING_DEEP_LINK, RequestType.SAVE_PASSES, RequestType.SAVE_PASSES_JWT})
    @interface RequestType {
        /**
         * Checks support of card provisioning deep links.
         */
        int CARD_PROVISIONING_DEEP_LINK = 1;
        /**
         * Checks availability of the {@link PayClient#savePasses(String, Activity, int)} API.
         */
        int SAVE_PASSES = 2;
        /**
         * Checks availability of the {@link PayClient#savePassesJwt(String, Activity, int)} API.
         */
        int SAVE_PASSES_JWT = 3;
    }

    /**
     * Possible result codes passed back in {@code onActivityResult()} when calling {@link PayClient#savePasses(String, Activity, int)} or
     * {@link PayClient#savePassesJwt(String, Activity, int)}. These are in addition to {@link Activity#RESULT_OK} and {@link Activity#RESULT_CANCELED}.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({Activity.RESULT_OK, Activity.RESULT_CANCELED, SavePassesResult.API_UNAVAILABLE, SavePassesResult.SAVE_ERROR, SavePassesResult.INTERNAL_ERROR})
    @interface SavePassesResult {
        /**
         * The {@link PayClient#savePasses(String, Activity, int)} or {@link PayClient#savePassesJwt(String, Activity, int) API is unavailable.
         * Use {@link PayClient#getPayApiAvailabilityStatus(int)} before calling the API.
         */
        int API_UNAVAILABLE = 1;
        /**
         * An error occurred while saving the passes. Check {@code EXTRA_API_ERROR_MESSAGE} to debug the issue.
         */
        int SAVE_ERROR = 2;
        /**
         * Indicates that an internal error occurred while calling the API. Retry the API call. If the error persists assume that the API is not available.
         */
        int INTERNAL_ERROR = 3;
    }

    /**
     * Intent source for Wear Card Management Activity. Behavior may depend on the source.
     */
    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({WearWalletIntentSource.OOBE, WearWalletIntentSource.SETTINGS})
    @interface WearWalletIntentSource {
        /**
         * Start Wear Wallet for out of box experience
         */
        int OOBE = 20;
        /**
         * Start Wear Wallet from settings
         */
        int SETTINGS = 21;
    }
}
