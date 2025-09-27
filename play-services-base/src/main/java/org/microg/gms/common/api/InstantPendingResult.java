/*
 * Copyright (C) 2017 microG Project Team
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

package org.microg.gms.common.api;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import java.util.concurrent.TimeUnit;

public class InstantPendingResult<R extends Result> extends PendingResult<R> {
    R value;

    public InstantPendingResult(R value) {
        this.value = value;
    }

    @Override
    public R await() {
        return value;
    }

    @Override
    public R await(long time, TimeUnit unit) {
        return value;
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setResultCallback(ResultCallback<R> callback, long time, TimeUnit unit) {
        callback.onResult(value);
    }

    @Override
    public void setResultCallback(ResultCallback<R> callback) {
        callback.onResult(value);
    }
}
