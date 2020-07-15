/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.tasks;

import org.microg.gms.common.PublicApi;

/**
 * Provides the ability to create an incomplete {@link Task} and later complete it by either
 * calling {@link #setResult(TResult)} or {@link #setException(Exception)}.
 */
@PublicApi
public class TaskCompletionSource<TResult> {
    public TaskCompletionSource() {
    }

    /**
     * Returns the Task.
     */
    public Task<TResult> getTask() {
        return null;
    }

    /**
     * Completes the Task with the specified exception.
     * @throws IllegalStateException if the Task is already complete
     */
    public void setException(Exception e) {

    }

    /**
     * Completes the Task with the specified result.
     * @throws IllegalStateException if the Task is already complete
     */
    public void setResult(TResult result) {

    }
}
