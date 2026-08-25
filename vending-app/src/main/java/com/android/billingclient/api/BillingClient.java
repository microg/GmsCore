/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.android.billingclient.api;

import android.app.Activity;
import android.os.Bundle;
import androidx.annotation.AnyThread;
import androidx.annotation.UiThread;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Main interface for communication between the library and user application code.
 * <p>
 * It provides convenience methods for in-app billing. You can create one instance of this class for your application and
 * use it to process in-app billing operations. It provides synchronous (blocking) and asynchronous (non-blocking)
 * methods for many common in-app billing operations.
 * <p>
 * It's strongly recommended that you instantiate only one BillingClient instance at one time to avoid multiple
 * {@link PurchasesUpdatedListener#onPurchasesUpdated} callbacks for a single event.
 * <p>
 * All methods annotated with {@link AnyThread} can be called from any thread and all the asynchronous callbacks will be
 * returned on the same thread. Methods annotated with {@link UiThread} should be called from the Ui thread and all the
 * asynchronous callbacks will be returned on the Ui thread as well.
 * <p>
 * After instantiating, you must perform setup in order to start using the object. To perform setup, call the
 * {@link #startConnection} method and provide a listener; that listener will be notified when setup is complete, after which
 * (and not before) you may start calling other methods. After setup is complete, you will typically want to request an
 * inventory of owned items and subscriptions. See {@link #queryPurchasesAsync} and {@link #queryProductDetailsAsync}.
 * <p>
 * When you are done with this object, don't forget to call {@link #endConnection} to ensure proper cleanup. This object holds a
 * binding to the in-app billing service and the manager to handle broadcast events, which will leak unless you dispose it
 * correctly. If you created the object inside the {@link Activity#onCreate(Bundle)}  method, then the recommended place to dispose is
 * the {@link Activity#onDestroy()} method. After cleanup, it cannot be reused again for connection.
 * <p>
 * To get library logs inside Android logcat, set corresponding logging level. E.g.: {@code adb shell setprop
 * log.tag.BillingClient VERBOSE}
 */
public abstract class BillingClient {

    /**
     * Possible response codes.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface BillingResponseCode {
        /**
         * The request has reached the maximum timeout before Google Play responds.
         * <p>
         * Since this state is transient, your app should automatically retry (e.g. with exponential back off) to recover from this
         * error. Be mindful of how long you retry if the retry is happening during a user interaction.
         *
         * @deprecated See {@link #SERVICE_UNAVAILABLE} which will be used instead of this code.
         */
        @Deprecated
        int SERVICE_TIMEOUT = -3;
        /**
         * The requested feature is not supported by the Play Store on the current device.
         * <p>
         * If your app would like to check if a feature is supported before trying to use the feature your app can call
         * {@link #isFeatureSupported} to check if a feature is supported. For a list of feature types that can be supported, see
         * {@link FeatureType}.
         * <p>
         * For example: Before calling {@link #showInAppMessages} API, you can call {@link #isFeatureSupported} with the
         * {@link FeatureType#IN_APP_MESSAGING} featureType to check if it is supported.
         */
        int FEATURE_NOT_SUPPORTED = -2;

        /**
         * The app is not connected to the Play Store service via the Google Play Billing Library.
         * <p>
         * Examples where this error may occur:
         * <ul>
         * <li>The Play Store could have been updated in the background while your app was still running and the library lost
         *     connection.</li>
         * <li>{@link #startConnection} was never called or has not completed yet.</li>
         * </ul>
         * Since this state is transient, your app should automatically retry (e.g. with exponential back off) to recover from this
         * error. Be mindful of how long you retry if the retry is happening during a user interaction. The retry should lead to a
         * call to {@link #startConnection} right after or in some time after you received this code.
         */
        int SERVICE_DISCONNECTED = -1;

        /**
         * Success.
         */
        int OK = 0;

        /**
         * Transaction was canceled by the user.
         */
        int USER_CANCELED = 1;

        /**
         * The service is currently unavailable.
         * <p>
         * Since this state is transient, your app should automatically retry (e.g. with exponential back off) to recover from this
         * error. Be mindful of how long you retry if the retry is happening during a user interaction.
         */
        int SERVICE_UNAVAILABLE = 2;

        /**
         * A user billing error occurred during processing.
         * <p>
         * Examples where this error may occur:
         * <ul>
         * <li>The Play Store app on the user's device is out of date.</li>
         * <li>The user is in an unsupported country.</li>
         * <li>The user is an enterprise user and their enterprise admin has disabled users from making purchases.</li>
         * <li>Google Play is unable to charge the user?s payment method.</li>
         * </ul>
         * Letting the user retry may succeed if the condition causing the error has changed (e.g. An enterprise user's admin has allowed purchases for the organization).
         */
        int BILLING_UNAVAILABLE = 3;

        /**
         * The requested product is not available for purchase.
         * <p>
         * Please ensure the product is available in the user?s country. If you recently changed the country availability and are
         * still receiving this error then it may be because of a propagation delay.
         */
        int ITEM_UNAVAILABLE = 4;

        /**
         * Error resulting from incorrect usage of the API.
         * <p>
         * Examples where this error may occur:
         * <ul>
         * <li>Invalid arguments such as providing an empty product list where required.</li>
         * <li>Misconfiguration of the app such as not signing the app or not having the necessary permissions in the manifest.</li>
         * </ul>
         */
        int DEVELOPER_ERROR = 5;

        /**
         * Fatal error during the API action.
         * <p>
         * This is an internal Google Play error that may be transient or due to an unexpected condition during processing. You
         * can automatically retry (e.g. with exponential back off) for this case and contact Google Play if issues persist. Be
         * mindful of how long you retry if the retry is happening during a user interaction.
         */
        int ERROR = 6;

        /**
         * The purchase failed because the item is already owned.
         * <p>
         * Make sure your app is up-to-date with recent purchases using guidance in the Fetching purchases section in the
         * integration guide. If this error occurs despite making the check for recent purchases, then it may be due to stale
         * purchase information that was cached on the device by Play. When you receive this error, the cache should get
         * updated. After this, your purchases should be reconciled, and you can process them as outlined in the processing
         * purchases section in the integration guide.
         */
        int ITEM_ALREADY_OWNED = 7;

        /**
         * Requested action on the item failed since it is not owned by the user.
         * <p>
         * Make sure your app is up-to-date with recent purchases using guidance in the Fetching purchases section in the
         * integration guide. If this error occurs despite making the check for recent purchases, then it may be due to stale
         * purchase information cached on the device by Play. When you receive this error, the cache should get updated. After
         * this, your purchases should be reconciled, and you can process the purchases accordingly. For example, if you are
         * trying to consume an item and if the updated purchase information says it is already consumed, you can ignore the
         * error now.
         */
        int ITEM_NOT_OWNED = 8;

        int EXPIRED_OFFER_TOKEN = 11;

        /**
         * A network error occurred during the operation.
         * <p>
         * This error indicates that there was a problem with the network connection between the device and Play systems. This
         * could potentially also be due to the user not having an active network connection.
         */
        int NETWORK_ERROR = 12;

        int RESPONSE_CODE_UNSPECIFIED = -999;
    }

    /**
     * Supported Product types.
     */
    @Retention(RetentionPolicy.SOURCE)
    public @interface ProductType {
        /**
         * A Product type for Android apps in-app products.
         */
        String INAPP = "inapp";
        /**
         * A Product type for Android apps subscriptions.
         */
        String SUBS = "subs";
    }

    /**
     * Supported SKU types.
     * @deprecated Use {@link ProductType} instead.
     */
    @Deprecated
    @Retention(RetentionPolicy.SOURCE)
    public @interface SkuType {
        /**
         * A type of SKU for Android apps in-app products.
         */
        String INAPP = ProductType.INAPP;
        /**
         * A type of SKU for Android apps subscriptions.
         */
        String SUBS = ProductType.SUBS;
    }
}
