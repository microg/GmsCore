/*
 * Copyright (C) 2023 microG Project Team
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

package org.microg.gms.wearable;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import java.util.concurrent.TimeUnit;

public class ImmediatePendingResult<R extends Result> extends PendingResult<R> {

    private final R result;

    public ImmediatePendingResult(R result) {
        this.result = result;
    }

    @Override
    public R await() {
        return result;
    }

    @Override
    public R await(long time, TimeUnit units) {
        return result;
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isCanceled() {
        return false;
    }

    @Override
    public void setResultCallback(ResultCallback<? super R> callback) {
        callback.onResult(result);
    }

    @Override
    public void setResultCallback(ResultCallback<? super R> callback, long time, TimeUnit units) {
        callback.onResult(result);
    }
}
