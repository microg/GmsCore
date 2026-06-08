package org.microg.gms.wearable.channel;

import android.os.Handler;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

public class PendingOperation {
    private static final String TAG = "PendingOperation";

    private final Handler handler;
    private final Runnable timeoutRunnable;
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final String description;

    public PendingOperation(Handler handler, Runnable onTimeout, long timeoutMs, String description) {
        this.handler = handler;
        this.timeoutRunnable = onTimeout;
        this.description = description;

        handler.postDelayed(timeoutRunnable, timeoutMs);

        if (Log.isLoggable(TAG, Log.VERBOSE)) {
            Log.v(TAG, "Created pending operation: " + description + " (timeout=" + timeoutMs + "ms)");
        }
    }

    public boolean cancel() {
        if (cancelled.compareAndSet(false, true)) {
            handler.removeCallbacks(timeoutRunnable);
            if (Log.isLoggable(TAG, Log.VERBOSE)) {
                Log.v(TAG, "Cancelled pending operation: " + description);
            }
            return true;
        }
        return false;
    }

    public boolean isCancelled() {
        return cancelled.get();
    }

    @Override
    public String toString() {
        return "PendingOperation{" + description + ", cancelled=" + cancelled.get() + "}";
    }
}