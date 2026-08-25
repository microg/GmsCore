/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.droidguard.internal;

import android.net.Network;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.microg.gms.common.Constants;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

public class DroidGuardResultsRequest extends AutoSafeParcelable {
    private static final String KEY_APP_ARCHITECTURE = "appArchitecture";
    private static final String KEY_CLIENT_VERSION = "clientVersion";
    private static final String KEY_FD = "fd";
    private static final String KEY_NETWORK_TO_USE = "networkToUse";
    private static final String KEY_TIMEOUT_MS = "timeoutMs";
    public static final String KEY_OPEN_HANDLES = "openHandles";

    @Field(2)
    public Bundle bundle;

    public DroidGuardResultsRequest() {
        bundle = new Bundle();
        String arch;
        try {
            arch = System.getProperty("os.arch");
        } catch (Exception ignored) {
            arch = "?";
        }
        bundle.putString(KEY_APP_ARCHITECTURE, arch);
        setClientVersion(Constants.GMS_VERSION_CODE);
    }

    public String getAppArchitecture() {
        return bundle.getString(KEY_APP_ARCHITECTURE);
    }

    public int getTimeoutMillis() {
        return bundle.getInt(KEY_TIMEOUT_MS, 60000);
    }

    public DroidGuardResultsRequest setTimeoutMillis(int millis) {
        bundle.putInt(KEY_TIMEOUT_MS, millis);
        return this;
    }

    public int getClientVersion() {
        return bundle.getInt(KEY_CLIENT_VERSION);
    }

    public DroidGuardResultsRequest setClientVersion(int clientVersion) {
        bundle.putInt(KEY_CLIENT_VERSION, clientVersion);
        return this;
    }

    public ParcelFileDescriptor getFd() {
        return bundle.getParcelable(KEY_FD);
    }

    public DroidGuardResultsRequest setFd(ParcelFileDescriptor fd) {
        bundle.putParcelable(KEY_FD, fd);
        return this;
    }

    public int getOpenHandles() {
        return bundle.getInt(KEY_OPEN_HANDLES);
    }

    public DroidGuardResultsRequest setOpenHandles(int openHandles) {
        bundle.putInt(KEY_OPEN_HANDLES, openHandles);
        return this;
    }

    @RequiresApi(api = 21)
    public Network getNetworkToUse() {
        return bundle.getParcelable(KEY_NETWORK_TO_USE);
    }

    @RequiresApi(api = 21)
    public DroidGuardResultsRequest setNetworkToUse(Network networkToUse) {
        bundle.putParcelable(KEY_NETWORK_TO_USE, networkToUse);
        return this;
    }

    @NonNull
    @Override
    public String toString() {
        ToStringHelper helper = ToStringHelper.name("DroidGuardResultsRequest");
        for (String key : bundle.keySet()) {
            helper.field(key, bundle.get(key));
        }
        return helper.end();
    }

    public static final Creator<DroidGuardResultsRequest> CREATOR = new AutoCreator<>(DroidGuardResultsRequest.class);
}
