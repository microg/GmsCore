/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.blockstore;

import android.content.Context;

/**
 * Entry point for Block Store API.
 * <p>
 * Allows apps to transfer small amounts of data via device-to-device restore. This enables a seamless sign-in when users start using a new
 * device.
 */
public class Blockstore {
    /**
     * Creates a new instance of {@link BlockstoreClient}.
     */
    public static BlockstoreClient getClient(Context context) {
        throw new UnsupportedOperationException();
    }
}
