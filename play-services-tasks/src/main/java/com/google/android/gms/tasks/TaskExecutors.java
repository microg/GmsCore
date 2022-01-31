/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.tasks;

import android.os.Handler;
import android.os.Looper;

import org.microg.gms.common.PublicApi;

import java.util.concurrent.Executor;

/**
 * Standard {@link Executor} instances for use with {@link Task}.
 */
@PublicApi
public final class TaskExecutors {
    /**
     * An Executor that uses the main application thread.
     */
    public static final Executor MAIN_THREAD = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());
        @Override
        public void execute(Runnable command) {
            handler.post(command);
        }
    };
}
