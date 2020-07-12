/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

/**
 * Listener called when a {@link CancellationToken} is canceled successfully.
 *
 * @see CancellationToken#onCanceledRequested(OnTokenCanceledListener)
 */
@PublicApi
public interface OnTokenCanceledListener {
    /**
     * Called when the {@link CancellationToken} is canceled successfully.
     */
    void onCanceled();
}
