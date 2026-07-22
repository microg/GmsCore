/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.engage.protocol;

import android.os.Bundle;

/**
 * Callback interface for deleteClusters operation.
 * This callback is used to receive the result of a cluster deletion operation.
 */
interface IAppEngageServiceDeleteClustersCallback {
    /**
     * Called when the delete operation has completed.
     *
     * @param result Bundle containing the result of the delete operation
     */
    void onResult(in Bundle result);
}