/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.auth.blockstore;

import com.google.android.gms.common.api.CommonStatusCodes;

/**
 * Block Store specific status codes.
 * <p>
 * Codes are allocated from the range 40000 to 40499, allocated in {@link CommonStatusCodes}.
 */
public class BlockstoreStatusCodes extends CommonStatusCodes {

    /**
     * Attempting to use a Blockstore feature that is not (yet) supported on the given device.
     */
    public static final int FEATURE_NOT_SUPPORTED = 40002;

    /**
     * The available quota was exceeded.
     */
    public static final int MAX_SIZE_EXCEEDED = 40000;

    /**
     * Attempting to store a new key value pair after reaching the maximum number of entries allowed.
     */
    public static final int TOO_MANY_ENTRIES = 40001;

}
