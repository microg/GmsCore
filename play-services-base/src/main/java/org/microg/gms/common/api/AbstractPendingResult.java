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

package org.microg.gms.common.api;

import android.os.Looper;

import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Result;
import com.google.android.gms.common.api.ResultCallback;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class AbstractPendingResult<R extends Result> implements PendingResult<R> {
    private final Object lock = new Object();
    private final CountDownLatch countDownLatch = new CountDownLatch(1);
    private final ResultCallbackHandler<R> handler;
    private boolean canceled;
    private R result;
    private ResultCallback<R> resultCallback;

    public AbstractPendingResult(Looper looper) {
        handler = new ResultCallbackHandler<R>(looper);
    }

    private R getResult() {
        synchronized (lock) {
            return result;
        }
    }

    @Override
    public R await() {
        try {
            countDownLatch.await();
        } catch (InterruptedException ignored) {
        }
        return getResult();
    }

    @Override
    public R await(long time, TimeUnit unit) {
        try {
            countDownLatch.await(time, unit);
        } catch (InterruptedException ignored) {
        }
        return getResult();
    }

    @Override
    public void cancel() {
        // TODO
    }

    @Override
    public boolean isCanceled() {
        synchronized (lock) {
            return canceled;
        }
    }

    public boolean isReady() {
        return this.countDownLatch.getCount() == 0L;
    }

    @Override
    public void setResultCallback(ResultCallback<R> callback, long time, TimeUnit unit) {
        synchronized (lock) {
            if (!isCanceled()) {
                if (isReady()) {
                    handler.sendResultCallback(callback, getResult());
                } else {
                    handler.sendTimeoutResultCallback(this, unit.toMillis(time));
                }
            }
        }
    }

    @Override
    public void setResultCallback(ResultCallback<R> callback) {
        synchronized (lock) {
            if (!isCanceled()) {
                if (isReady()) {
                    handler.sendResultCallback(callback, getResult());
                } else {
                    resultCallback = callback;
                }
            }
        }
    }

    public void deliverResult(R result) {
        this.result = result;
        countDownLatch.countDown();

    }
}
