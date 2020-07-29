/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import java.util.concurrent.Executor;

public abstract class UpdateExecutor<TResult> implements UpdateListener<TResult>, Executor {
    private Executor executor;

    public UpdateExecutor(Executor executor) {
        this.executor = executor;
    }

    @Override
    public void execute(Runnable runnable) {
        if (executor == null) return;
        executor.execute(runnable);
    }

    @Override
    public void cancel() {
        executor = null;
    }
}
