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
     * Blocks until the task is completed. This is not allowed on the UI thread. The returned result object can have an additional failure mode of INTERRUPTED.
     */
    public R await();

    /**
     * Blocks until the task is completed or has timed out waiting for the result. This is not allowed on the UI thread. The returned result object can have an additional failure mode of either INTERRUPTED or TIMEOUT.
     */
    public R await(long time, TimeUnit unit);

    public void cancel();

    public boolean isCanceled();

    public void setResultCallback(ResultCallback<R> callback, long time, TimeUnit unit);

    public void setResultCallback(ResultCallback<R> callback);
}
