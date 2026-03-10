/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.ims.rcs.engine;

import android.os.Parcel;
import android.os.Parcelable;

public class RcsEngineLifecycleServiceResult implements Parcelable {
    public static final int SUCCESS = 0;
    public static final int ERROR_UNKNOWN = 1;
    public static final int ERROR_NOT_INITIALIZED = 2;
    public static final int ERROR_NETWORK_FAILURE = 3;
    public static final int ERROR_CONNECTION_OFFLINE = 4;
    
    private int code;
    private String description;

    public RcsEngineLifecycleServiceResult(int code) {
        this.code = code;
        this.description = null;
    }

    public RcsEngineLifecycleServiceResult(int code, String description) {
        this.code = code;
        this.description = description;
    }

    protected RcsEngineLifecycleServiceResult(Parcel in) {
        this.code = in.readInt();
        this.description = in.readString();
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public boolean succeeded() {
        return code == SUCCESS;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(code);
        dest.writeString(description);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<RcsEngineLifecycleServiceResult> CREATOR = new Creator<RcsEngineLifecycleServiceResult>() {
        @Override
        public RcsEngineLifecycleServiceResult createFromParcel(Parcel in) {
            return new RcsEngineLifecycleServiceResult(in);
        }

        @Override
        public RcsEngineLifecycleServiceResult[] newArray(int size) {
            return new RcsEngineLifecycleServiceResult[size];
        }
    };

    @Override
    public String toString() {
        if (code == SUCCESS) {
            return "OK";
        }
        StringBuilder sb = new StringBuilder("Error: (code=");
        sb.append(code);
        sb.append(")");
        if (description != null) {
            sb.append(", description: (");
            sb.append(description);
            sb.append(")");
        }
        return sb.toString();
    }
}
