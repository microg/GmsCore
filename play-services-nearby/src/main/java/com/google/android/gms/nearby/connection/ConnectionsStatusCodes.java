/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.connection;

import com.google.android.gms.common.api.CommonStatusCodes;
import org.microg.gms.common.PublicApi;

@PublicApi
public final class ConnectionsStatusCodes extends CommonStatusCodes {
    public static final int STATUS_OK = 0;
    public static final int STATUS_NETWORK_NOT_CONNECTED = 8000;
    public static final int STATUS_NOT_CONNECTED_TO_ENDPOINT = 8001;
    public static final int STATUS_ALREADY_CONNECTED_TO_ENDPOINT = 8002;
    public static final int STATUS_CONNECTION_REJECTED = 8003;
    public static final int STATUS_ALREADY_ADVERTISING = 8004;
    public static final int STATUS_ALREADY_DISCOVERING = 8005;
    public static final int STATUS_ENDPOINT_IO_ERROR = 8012;
    public static final int STATUS_PAYLOAD_IO_ERROR = 8013;
    public static final int STATUS_OUT_OF_ORDER_API_CALL = 8009;

    private ConnectionsStatusCodes() {}

    public static String getStatusCodeString(int statusCode) {
        switch (statusCode) {
            case STATUS_OK:
                return "STATUS_OK";
            case STATUS_NETWORK_NOT_CONNECTED:
                return "STATUS_NETWORK_NOT_CONNECTED";
            case STATUS_NOT_CONNECTED_TO_ENDPOINT:
                return "STATUS_NOT_CONNECTED_TO_ENDPOINT";
            case STATUS_ALREADY_CONNECTED_TO_ENDPOINT:
                return "STATUS_ALREADY_CONNECTED_TO_ENDPOINT";
            case STATUS_CONNECTION_REJECTED:
                return "STATUS_CONNECTION_REJECTED";
            case STATUS_ALREADY_ADVERTISING:
                return "STATUS_ALREADY_ADVERTISING";
            case STATUS_ALREADY_DISCOVERING:
                return "STATUS_ALREADY_DISCOVERING";
            case STATUS_ENDPOINT_IO_ERROR:
                return "STATUS_ENDPOINT_IO_ERROR";
            case STATUS_PAYLOAD_IO_ERROR:
                return "STATUS_PAYLOAD_IO_ERROR";
            case STATUS_OUT_OF_ORDER_API_CALL:
                return "STATUS_OUT_OF_ORDER_API_CALL";
            default:
                return CommonStatusCodes.getStatusCodeString(statusCode);
        }
    }
}
