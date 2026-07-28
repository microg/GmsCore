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
    static final String KEY_SESSION_ID = "sessionId";
    static final String KEY_MULTI_STEP = "isMultiStep";
    static final String KEY_STEP_NUMBER = "stepNumber";
    static final String KEY_TOTAL_STEPS = "totalSteps";

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

    // ---- Multi-step Play Integrity metadata ----

    public long getSessionId() {
        return bundle.getLong(KEY_SESSION_ID, -1L);
    }

    public DroidGuardResultsRequest setSessionId(long sessionId) {
        bundle.putLong(KEY_SESSION_ID, sessionId);
        return this;
    }

    public boolean getMultiStep() {
        return bundle.getBoolean(KEY_MULTI_STEP, false);
    }

    public DroidGuardResultsRequest setMultiStep(boolean multiStep) {
        bundle.putBoolean(KEY_MULTI_STEP, multiStep);
        return this;
    }

    public int getStepNumber() {
        return bundle.getInt(KEY_STEP_NUMBER, -1);
    }

    public DroidGuardResultsRequest setStepNumber(int stepNumber) {
        bundle.putInt(KEY_STEP_NUMBER, stepNumber);
        return this;
    }

    public int getTotalSteps() {
        return bundle.getInt(KEY_TOTAL_STEPS, -1);
    }

    public DroidGuardResultsRequest setTotalSteps(int totalSteps) {
        bundle.putInt(KEY_TOTAL_STEPS, totalSteps);
        return this;
    }

    /** Kotlin-friendly field-style accessors to preserve compatibility with
     *  Kotlin property syntax that calls get_/set_ on non-standard names. */
    public int get_totalSteps() {
        return getTotalSteps();
    }

    public DroidGuardResultsRequest set_totalSteps(int totalSteps) {
        return setTotalSteps(totalSteps);
    }

    /** Create a shallow copy of this request, cloning the backing Bundle. */
    public DroidGuardResultsRequest copy() {
        DroidGuardResultsRequest copy = new DroidGuardResultsRequest();
        copy.bundle.putAll(bundle);
        return copy;
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
