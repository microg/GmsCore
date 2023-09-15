/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.net.Uri;
import androidx.annotation.NonNull;

/**
 * An abstract class representing browser-based request parameters.
 */
public abstract class BrowserRequestOptions extends RequestOptions {
    /**
     * Gets value of the client data hash.
     */
    @NonNull
    public abstract byte[] getClientDataHash();

    @NonNull
    public abstract Uri getOrigin();
}
