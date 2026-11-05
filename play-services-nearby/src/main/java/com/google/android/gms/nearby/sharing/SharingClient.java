/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.nearby.sharing;

import com.google.android.gms.tasks.Task;

import org.microg.gms.common.PublicApi;

import java.util.List;

/**
 * Interface for the Nearby Share API, which allows exchanging content with nearby devices.
 */
@PublicApi
public interface SharingClient {
    /**
     * Service action through which the nearby sharing backend is bound.
     */
    String ACTION_START_SERVICE = "com.google.android.gms.nearby.sharing.START_SERVICE";

    /**
     * Starts broadcasting this device so that nearby devices can discover and send shares to it.
     */
    Task<Void> startAdvertising(LocalShare share);

    /**
     * Stops broadcasting this device.
     */
    Task<Void> stopAdvertising();

    /**
     * Starts scanning for nearby devices that are willing to receive shares.
     */
    Task<Void> startDiscovery();

    /**
     * Stops scanning for nearby devices.
     */
    Task<Void> stopDiscovery();

    /**
     * Returns the devices that were discovered by the most recent discovery run.
     */
    Task<List<ShareTarget>> getDiscoveredShareTargets();

    /**
     * Sends {@code content} to the given {@code target}.
     */
    Task<Void> send(ShareTarget target, SharedContent content);

    /**
     * Returns the shares that were received while advertising.
     */
    Task<List<ReceivedShare>> getReceivedShares();
}
