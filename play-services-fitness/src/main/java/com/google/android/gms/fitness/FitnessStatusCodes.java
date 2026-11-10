/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fitness;

import android.Manifest;
import android.app.PendingIntent;

import com.google.android.gms.common.api.CommonStatusCodes;

import org.microg.gms.common.PublicApi;

/**
 * Fitness specific status codes, for use in {@link com.google.android.gms.common.api.Status#getStatusCode()}.
 */
public class FitnessStatusCodes extends CommonStatusCodes {
    /**
     * The subscribe request succeeded, but no data sources are currently available that match it. Recording of data will start
     * when data sources become available.
     */
    public static final int SUCCESS_NO_DATA_SOURCES = -5000;
    /**
     * The subscribe request succeeded, but the subscription already existed, so it was a no-op.
     */
    public static final int SUCCESS_ALREADY_SUBSCRIBED = -5001;
    /**
     * The unclaim request succeeded, but no matching claimed device was found. No changes were made.
     */
    public static final int SUCCESS_NO_CLAIMED_DEVICE = -5002;
    /**
     * The {@link PendingIntent} sent by the client, to unregister itself from listening to changes to fitness store, was not found in
     * registered listeners listening to fitness store updates.
     */
    public static final int SUCCESS_LISTENER_NOT_REGISTERED_FOR_FITNESS_DATA_UPDATES = -5003;
    /**
     * Status code denotes that the request is missing desired OAuth permissions.
     * <p>
     * If an app does not have the required OAuth access for a specific API request, the request will fail with this status code.
     * When this occurs, apps can use the pending intent inside the status object to request the necessary access before
     * retrying the request.
     */
    public static final int NEEDS_OAUTH_PERMISSIONS = 5000;
    /**
     * Status code denotes that the app attempted to insert a conflicting DataType, that is, there is an existing DataType with
     * the same name and different fields.
     */
    public static final int CONFLICTING_DATA_TYPE = 5001;
    /**
     * Status code denotes that the app attempted to insert a DataType whose name does not match the app's package name.
     */
    public static final int INCONSISTENT_DATA_TYPE = 5002;
    /**
     * Status code denotes that the requested data type was not found.
     */
    public static final int DATA_TYPE_NOT_FOUND = 5003;
    /**
     * Status code denotes that the app tried to insert data from the wrong app.
     */
    public static final int APP_MISMATCH = 5004;
    /**
     * Status code denotes that an unknown error occurred while trying to obtain an OAuth token.
     */
    public static final int UNKNOWN_AUTH_ERROR = 5005;
    /**
     * Status code denotes that the app is missing the required Bluetooth permissions.
     *
     * @deprecated Operations that require {@link Manifest.permission#BLUETOOTH_ADMIN} will throw a {@link SecurityException} if it's not granted.
     */
    @Deprecated
    public static final int MISSING_BLE_PERMISSION = 5006;
    /**
     * Status code denotes that the operation is not supported by Google Fit.
     */
    public static final int UNSUPPORTED_PLATFORM = 5007;
    /**
     * Status code denotes that there was a transient error accessing Google Fit services. Clients may retry.
     */
    public static final int TRANSIENT_ERROR = 5008;
    /**
     * Status code denotes that a session could not be started because an equivalent session has already ended.
     */
    public static final int EQUIVALENT_SESSION_ENDED = 5009;
    /**
     * Status code denotes that an app was not found in the list of connected apps in Google Fit. Signifies that either access to
     * the app was already revoked, or the app is not registered on the developer's console.
     */
    public static final int APP_NOT_FIT_ENABLED = 5010;
    /**
     * Status code denotes that an API call failed.
     */
    public static final int API_EXCEPTION = 5011;
    /**
     * Status code denotes that a read request specified an un-supported data type as input for aggregation.
     */
    public static final int AGGREGATION_NOT_SUPPORTED = 5012;
    /**
     * Status code denotes that the account is not supported.
     */
    public static final int UNSUPPORTED_ACCOUNT = 5013;
    /**
     * Status code denotes that Bluetooth is currently disabled.
     */
    public static final int DISABLED_BLUETOOTH = 5014;
    /**
     * Status code denotes that app attempted to insert data for a DataSource that does not match the app's package name.
     */
    public static final int INCONSISTENT_PACKAGE_NAME = 5015;
    /**
     * Status code denotes that there is no local datasource available to subscribe.
     */
    public static final int DATA_SOURCE_NOT_FOUND = 5018;
    /**
     * Status code denotes that app attempted to update data with new dataSet's timestamp outside of update request time
     * range.
     */
    public static final int DATASET_TIMESTAMP_INCONSISTENT_WITH_UPDATE_TIME_RANGE = 5019;
    /**
     * Status code denotes that the app attempted to add a session with start time later than end time.
     */
    public static final int INVALID_SESSION_TIMESTAMPS = 5020;
    /**
     * Status code denotes that the app attempted to add invalid data.
     */
    public static final int INVALID_DATA_POINT = 5021;
    /**
     * Status code denotes that the timestamp in the query is bad. This may be due to a bad time specified in the request or
     * bad system time.
     */
    public static final int INVALID_TIMESTAMP = 5022;
    /**
     * Status code denotes that the data type provided isn't supported by this API call.
     */
    public static final int DATA_TYPE_NOT_ALLOWED_FOR_API = 5023;
    /**
     * Status code denotes that access to a requested data type was denied because the app has not been whitelisted by
     * Google for access to that type.
     */
    public static final int REQUIRES_APP_WHITELISTING = 5024;
    /**
     * Status code denotes that access to a requested data type was denied because the app has not been granted
     * install/runtime permission.
     */
    public static final int INVALID_PERMISSION = 5025;
    /**
     * Status code denoting that access is blocked due to violation of the Fit platform user data policy
     * or out of compliance with Google Fit APIs terms of services.
     */
    public static final int ACCESS_BLOCKED = 5026;
    /**
     * Status code denoting that the given API doesn't support the supplied activity type.
     */
    public static final int INVALID_ACTIVITY_TYPE = 5027;


}
