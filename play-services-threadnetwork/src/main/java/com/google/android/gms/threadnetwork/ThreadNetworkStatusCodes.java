/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.threadnetwork;

import com.google.android.gms.common.api.CommonStatusCodes;
import com.google.android.gms.tasks.Task;

/**
 * Status codes for {@link ThreadNetworkClient} methods result {@link Task} failures.
 * <p>
 * The codes are supplementary to common status codes in {@link CommonStatusCodes}.
 */
public class ThreadNetworkStatusCodes extends CommonStatusCodes {
    /**
     * Operation is not permitted.
     */
    public static final int PERMISSION_DENIED = 44000;
    /**
     * Local Wi-Fi or Ethernet network is not connected.
     */
    public static final int LOCAL_NETWORK_NOT_CONNECTED = 44001;
    /**
     * Operation is not supported on current platform.
     */
    public static final int PLATFORM_NOT_SUPPORTED = 44002;
    /**
     * Failed to add Thread network credentials because the storage quota is exceeded.
     */
    public static final int MAX_STORAGE_SIZE_EXCEEDED = 44003;
    public static final int THREAD_NETWORK_NOT_FOUND = 44004;
}
