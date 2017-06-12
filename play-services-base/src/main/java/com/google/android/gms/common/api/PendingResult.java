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

package com.google.android.gms.common.api;

import java.util.concurrent.TimeUnit;

/**
 * Represents a pending result from calling an API method in Google Play services. The final result
 * object from a PendingResult is of type R, which can be retrieved in one of two ways.
 * <p/>
 * <ul>
 * <li>via blocking calls to {@link #await()}, or {@link #await(long, TimeUnit)}, or</li>
 * <li>via a callback by passing in an object implementing interface {@link ResultCallback} to
 * {@link #setResultCallback(ResultCallback)}.</li>
 * </ul>
 * After the result has been retrieved using {@link #await()} or delivered to the result callback,
 * it is an error to attempt to retrieve the result again. It is the responsibility of the caller
 * or callback receiver to release any resources associated with the returned result. Some result
 * types may implement {@link Releasable}, in which case {@link Releasable#release()} should be
 * used to free the associated resources.
 * <p/>
 * TODO: Docs
 */
public interface PendingResult<R extends Result> {
    /**
     * Blocks until the task is completed. This is not allowed on the UI thread. The returned
     * result object can have an additional failure mode of INTERRUPTED.
     */
    public R await();

    /**
     * Blocks until the task is completed or has timed out waiting for the result. This is not
     * allowed on the UI thread. The returned result object can have an additional failure mode
     * of either INTERRUPTED or TIMEOUT.
     */
    public R await(long time, TimeUnit unit);

    public void cancel();

    public boolean isCanceled();

    public void setResultCallback(ResultCallback<R> callback, long time, TimeUnit unit);

    public void setResultCallback(ResultCallback<R> callback);
}
