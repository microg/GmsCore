/*
 * SPDX-FileCopyrightText: 2026, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.constellation;

import android.content.Context;
import android.util.Log;

import com.google.android.gms.common.api.ApiMetadata;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.constellation.GetIidTokenRequest;
import com.google.android.gms.constellation.GetIidTokenResponse;
import com.google.android.gms.constellation.internal.IConstellationCallbacks;

/**
 * Handler for {@code getIidToken}: returns an IID token for the requested Google
 * project number, the device's Firebase installation id, and a signature over the
 * token, using microG's existing Instance ID implementation.
 */
public final class GetIidToken {
    private static final String TAG = "GmsConstellationIid";

    private GetIidToken() {
    }

    public static void handle(Context context, IConstellationCallbacks callbacks, GetIidTokenRequest request) {
        try {
            ConstellationAuth auth = new ConstellationAuth(context);
            String iidToken = auth.getIidToken(request.projectNumber);
            String fid = auth.getFid();
            ConstellationAuth.SignResult signed = auth.signIidToken(iidToken);
            callbacks.onIidTokenGenerated(Status.SUCCESS,
                    new GetIidTokenResponse(iidToken, fid, signed.signature, signed.timestampMillis),
                    ApiMetadata.DEFAULT);
        } catch (Exception e) {
            Log.e(TAG, "getIidToken failed", e);
            try {
                callbacks.onIidTokenGenerated(Status.INTERNAL_ERROR, null, ApiMetadata.DEFAULT);
            } catch (Exception ignored) {
            }
        }
    }
}
