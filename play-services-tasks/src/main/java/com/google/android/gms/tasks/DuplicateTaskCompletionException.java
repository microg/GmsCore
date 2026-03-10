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
 * An exception indicating that something attempted to set a result, exception, or cancellation on a {@link Task} that was already completed.
 */
@PublicApi
public class DuplicateTaskCompletionException extends IllegalStateException {

    private DuplicateTaskCompletionException(String s) {
        super(s);
    }

    /**
     * Creates a DuplicateTaskCompletionException from a {@link Task}.
     *
     * The {@link Task} must be complete.
     */
    public static DuplicateTaskCompletionException of(Task<?> task) {
        if (!task.isComplete()) throw new IllegalStateException("Task is not yet completed");
        return new DuplicateTaskCompletionException("Task is already completed");
    }
}
