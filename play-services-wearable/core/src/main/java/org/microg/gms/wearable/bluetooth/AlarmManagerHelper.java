package org.microg.gms.wearable.bluetooth;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Build;
import android.os.SystemClock;
import android.util.Log;

public class AlarmManagerHelper {
    private static final String TAG = "AlarmManagerHelper";

    private final Context context;
    private final AlarmManager alarmManager;

    public AlarmManagerHelper(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager == null) {
            throw new IllegalStateException("AlarmManager not available");
        }
    }

    public void setExactAndAllowWhileIdle(String tag, int type, long triggerAtMillis,
                                          PendingIntent operation) {
        if (triggerAtMillis <= 0) {
            Log.w(TAG, String.format("Invalid trigger time: %d", triggerAtMillis));
            return;
        }

        long delayMs = triggerAtMillis - SystemClock.elapsedRealtime();
        Log.d(TAG, String.format("setExactAndAllowWhileIdle: tag=%s, type=%d, delay=%dms",
                tag, type, delayMs));

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation);
            } else {
                alarmManager.setExact(type, triggerAtMillis, operation);
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException setting alarm", e);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "Error setting alarm", e);
            throw new RuntimeException("Failed to set alarm", e);
        }
    }

    public void setWindow(String tag, int type, long triggerAtMillis, long windowMs,
                          PendingIntent operation) {
        if (triggerAtMillis <= 0) {
            Log.w(TAG, String.format("Invalid trigger time: %d", triggerAtMillis));
            return;
        }

        long delayMs = triggerAtMillis - SystemClock.elapsedRealtime();
        Log.d(TAG, String.format("setWindow: tag=%s, type=%d, delay=%dms, window=%dms",
                tag, type, delayMs, windowMs));

        try {
            alarmManager.setWindow(type, triggerAtMillis, windowMs, operation);
        } catch (Exception e) {
            Log.e(TAG, "Error setting windowed alarm", e);
            throw new RuntimeException("Failed to set windowed alarm", e);
        }
    }

    public void cancel(PendingIntent operation) {
        try {
            alarmManager.cancel(operation);
            Log.d(TAG, "Cancelled alarm");
        } catch (Exception e) {
            Log.w(TAG, "Error cancelling alarm", e);
        }
    }

    public static PendingIntent createPendingIntent(Context context, int requestCode,
                                                    android.content.Intent intent) {
        int flags = PendingIntent.FLAG_UPDATE_CURRENT;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getBroadcast(context, requestCode, intent, flags);
    }

    public static long elapsedRealtimeFromNow(long delayMs) {
        return SystemClock.elapsedRealtime() + delayMs;
    }

    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                return alarmManager.canScheduleExactAlarms();
            } catch (Exception e) {
                Log.w(TAG, "Error checking exact alarm permission", e);
                return false;
            }
        }
        return true;
    }
}