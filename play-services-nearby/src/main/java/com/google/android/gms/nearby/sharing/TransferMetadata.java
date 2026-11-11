/*
 * SPDX-FileCopyrightText: 2023, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.nearby.sharing;

import org.microg.gms.common.PublicApi;

@PublicApi
public class TransferMetadata {
    public static final int STATUS_UNKNOWN = 0;
    public static final int STATUS_CONNECTING = 1;
    public static final int STATUS_AWAITING_LOCAL_CONFIRMATION = 2;
    public static final int STATUS_AWAITING_REMOTE_ACCEPTANCE = 3;
    public static final int STATUS_IN_PROGRESS = 4;
    public static final int STATUS_COMPLETE = 5;
    public static final int STATUS_FAILED = 6;
    public static final int STATUS_REJECTED = 7;
    public static final int STATUS_CANCELLED = 8;

    private final int status;
    private final float progress;
    private final String token;

    public TransferMetadata(int status, float progress, String token) {
        this.status = status;
        this.progress = Math.max(0.0f, Math.min(100.0f, progress));
        this.token = token;
    }

    public int getStatus() {
        return status;
    }

    public float getProgress() {
        return progress;
    }

    public String getToken() {
        return token;
    }
}
