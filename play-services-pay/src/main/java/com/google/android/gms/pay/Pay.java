/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.pay;

import android.app.Activity;
import android.content.Context;
import org.microg.gms.pay.PayClientImpl;

/**
 * Entry point for Pay API.
 */
public class Pay {
    /**
     * Creates a new instance of {@link PayClient} for use in an {@link Activity}. This client should not be used outside of the given {@code Activity}.
     */
    public static PayClient getClient(Activity activity) {
        return new PayClientImpl(activity);
    }

    /**
     * Creates a new instance of {@link PayClient} for use in a non-{@code Activity} {@link Context}.
     */
    public static PayClient getClient(Context context) {
        return new PayClientImpl(context);
    }
}
