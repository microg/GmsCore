package org.microg.gms.wearable.bluetooth;

import android.util.Log;

public class RetryStrategy {
    private static final String TAG = "RetryStrategy";

    public static final int POLICY_DEFAULT = 0;
    public static final int POLICY_AGGRESSIVE = 1;
    public static final int POLICY_LOW_POWER = 2;
    public static final int POLICY_OFF = 3;

    private static final RetryParams DEFAULT_PARAMS = new RetryParams(
            6,30000L, 320000L);

    private static final RetryParams AGGRESSIVE_PARAMS = new RetryParams(
            6,30000L, 320000L);

    private static final RetryParams LOW_POWER_PARAMS = new RetryParams(
            10,600000L, 1024000L);

    private static final RetryParams OFF_PARAMS = new RetryParams(
            0,0L, -1L);

    private final int maxRetryStep;
    private final long totalRetryTimeLimitMs;
    private final long retryDelayAtLimitMs;

    private long currentRetryCount = 0;
    private long cumulativeDelayMs = 0;
    private long lastResetTime = System.currentTimeMillis();

    public static RetryStrategy fromPolicy(int policy) {
        RetryParams params;
        switch (policy) {
            case POLICY_AGGRESSIVE:
                params = AGGRESSIVE_PARAMS;
                break;
            case POLICY_LOW_POWER:
                params = LOW_POWER_PARAMS;
                break;
            case POLICY_OFF:
                params = OFF_PARAMS;
                break;
            case POLICY_DEFAULT:
            default:
                params = DEFAULT_PARAMS;
                break;
        }

        Log.d(TAG, String.format("Created retry strategy: policy=%s, params=%s",
                policyToString(policy), params));

        return new RetryStrategy(params.maxRetryStep, params.totalRetryTimeLimit,
                params.retryDelayAtLimit);
    }

    public RetryStrategy(int maxRetryStep, long totalRetryTimeLimitMs, long retryDelayAtLimitMs) {
        this.maxRetryStep = maxRetryStep;
        this.totalRetryTimeLimitMs = totalRetryTimeLimitMs;
        this.retryDelayAtLimitMs = retryDelayAtLimitMs;
    }

    public long nextDelayMs() {
        if (retryDelayAtLimitMs < 0) {
            Log.d(TAG, "Retry strategy is OFF, returning -1");
            return -1;
        }

        long retryCount = Math.min(maxRetryStep, currentRetryCount + 1);
        currentRetryCount = retryCount;

        // exponential increase
        long delay = (1L << (int)(retryCount - 1)) * 1000L;

        long newCumulativeMs = cumulativeDelayMs + delay;
        cumulativeDelayMs = newCumulativeMs;

        Log.d(TAG, String.format("nextDelay: retryCount=%d, delay=%dms, cumulative=%dms/%dms",
                retryCount, delay, newCumulativeMs, totalRetryTimeLimitMs));

        if (totalRetryTimeLimitMs >= 0 && newCumulativeMs >= totalRetryTimeLimitMs) {
            Log.w(TAG, String.format(
                    "Cumulative retry time limit exceeded (%dms >= %dms), returning fallback delay: %dms",
                    newCumulativeMs, totalRetryTimeLimitMs, retryDelayAtLimitMs));

            return retryDelayAtLimitMs;
        }

        return delay;
    }

    public void disableRetries() {
        Log.d(TAG, "Disabling retries");
        currentRetryCount = maxRetryStep;
        cumulativeDelayMs = Math.max(cumulativeDelayMs, totalRetryTimeLimitMs);
    }

    public void reset() {
        Log.d(TAG, String.format("Resetting retry state (was: retryCount=%d, cumulative=%dms)",
                currentRetryCount, cumulativeDelayMs));

        currentRetryCount = 0;
        cumulativeDelayMs = 0;
        lastResetTime = System.currentTimeMillis();
    }

    public boolean isEnabled() {
        return retryDelayAtLimitMs >= 0;
    }

    public boolean hasExceededLimit() {
        return totalRetryTimeLimitMs >= 0 && cumulativeDelayMs >= totalRetryTimeLimitMs;
    }

    public long getRetryCount() {
        return currentRetryCount;
    }

    public long getCumulativeDelayMs() {
        return cumulativeDelayMs;
    }

    public long getTimeSinceResetMs() {
        return System.currentTimeMillis() - lastResetTime;
    }

    private static String policyToString(int policy) {
        switch (policy) {
            case POLICY_DEFAULT: return "DEFAULT";
            case POLICY_AGGRESSIVE: return "AGGRESSIVE";
            case POLICY_LOW_POWER: return "LOW_POWER";
            case POLICY_OFF: return "OFF";
            default: return "UNKNOWN(" + policy + ")";
        }
    }

    private static class RetryParams {
        final int maxRetryStep;
        final long totalRetryTimeLimit;
        final long retryDelayAtLimit;

        RetryParams(int maxRetryStep, long totalRetryTimeLimit, long retryDelayAtLimit) {
            this.maxRetryStep = maxRetryStep;
            this.totalRetryTimeLimit = totalRetryTimeLimit;
            this.retryDelayAtLimit = retryDelayAtLimit;
        }

        @Override
        public String toString() {
            return "RetryParams{maxStep=" + maxRetryStep +
                    ", timeLimit=" + totalRetryTimeLimit +
                    ", fallback=" + retryDelayAtLimit + "}";
        }
    }
}