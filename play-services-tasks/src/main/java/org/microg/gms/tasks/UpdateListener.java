/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.tasks;

import com.google.android.gms.tasks.Task;

public interface UpdateListener<TResult> {
    void onTaskUpdate(Task<TResult> task);

    void cancel();
}
