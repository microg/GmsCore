/*
 * SPDX-FileCopyrightText: 2016, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

/**
 * Listener called when a {@link Task} completes successfully.
 *
 * @see Task#addOnSuccessListener(OnSuccessListener)
 */
@PublicApi
public interface OnSuccessListener<TResult> {
    /**
     * Called when the {@link Task} completes successfully.
     *
     * @param result the result of the Task
     */
    void onSuccess(TResult result);
}
