/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.play.core.assetpacks;

import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Status;
import com.google.android.play.core.assetpacks.model.AssetPackErrorCode;
import org.microg.gms.common.Hide;

/**
 * An exception indicating something went wrong with the Asset Delivery API.
 * <p>
 * See {@link #getErrorCode()} for the specific problem.
 */
public class AssetPackException extends ApiException {
    @Hide
    public AssetPackException(@AssetPackErrorCode int errorCode) {
        super(new Status(errorCode, "Asset Pack Download Error(" + errorCode + ")"));
    }

    /**
     * Returns an error code value from {@link AssetPackErrorCode}.
     */
    @AssetPackErrorCode
    public int getErrorCode() {
        return super.getStatusCode();
    }

    /**
     * Returns the error code resulting from the operation. The value is one of the constants in {@link AssetPackErrorCode}.
     * getStatusCode() is unsupported by AssetPackException, please use getErrorCode() instead.
     */
    @Override
    public int getStatusCode() {
        return super.getStatusCode();
    }
}
