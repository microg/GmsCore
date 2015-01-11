package com.google.android.gms.common.api;

/**
 * An interface for receiving a {@link Result} from a {@link PendingResult} as an asynchronous callback.
 */
public interface ResultCallback<R extends Result> {
    /**
     * Called when the {@link Result} is ready. It is the responsibility of each callback to
     * release any resources associated with the result. Some result types may implement
     * {@link Releasable}, in which case {@link Releasable#release()} should be used to free the
     * associated resources.
     *
     * @param result The result from the API call. May not be null.
     */
    public void onResult(R result);
}
