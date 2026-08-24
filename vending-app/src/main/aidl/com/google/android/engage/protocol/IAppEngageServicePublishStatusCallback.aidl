/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.engage.protocol;

import android.os.Bundle;

/**
 * Callback interface for updatePublishStatus operation.
 * This callback is used to receive the result of a status update operation.
 */
interface IAppEngageServicePublishStatusCallback {
    /**
     * Called when the status update operation has completed.
     *
     * @param result Bundle containing the result of the status update operation
     */
    void onResult(in Bundle result);
}