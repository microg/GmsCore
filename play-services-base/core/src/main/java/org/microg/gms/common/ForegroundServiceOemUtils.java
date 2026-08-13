package org.microg.gms.common;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class ForegroundServiceOemUtils {
    private static final String TAG = "BatteryOptimizationOemUtils";

    private static final String[] KNOWN_RESTRICTED_MANUFACTURERS = {
            "huawei", "xiaomi", "oneplus", "samsung", "meizu", "asus", "wiko",
            "lenovo", "oppo", "vivo", "realme", "motorola", "blackview", "tecno",
            "sony", "unihertz"
    };

    public static String getDkmaSlug() {
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.ROOT);

        for (String brand : KNOWN_RESTRICTED_MANUFACTURERS) {
            if (manufacturer.contains(brand)) {
                return brand;
            }
        }

        return ""; // Oem not listed, hide dontkillmyapp option
    }

    public static Intent getDkmaIntent(String slug) {
        String url = "https://dontkillmyapp.com/" + slug;
        return new Intent(Intent.ACTION_VIEW, Uri.parse(url));
    }

    public static boolean isXiaomi() {
        return Build.MANUFACTURER.toLowerCase(Locale.ROOT).contains("xiaomi");
    }

    public static Intent getBatteryOptimizationIntent(Context context) {
        // Temporary fix issues: https://github.com/MorpheApp/MicroG-RE/issues/112
        if (isXiaomi()) {
            return new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        } else {
            return new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                    .setData(Uri.parse("package:" + context.getPackageName()));
        }
    }

    public interface IntentLauncher {
        void launch(Intent intent);
    }

    public static void openBatteryOptimizationSettings(Context context, IntentLauncher launcher) {
        Intent intent = getBatteryOptimizationIntent(context);
        try {
            launcher.launch(intent);
        } catch (Exception e) {
            if (!Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS.equals(intent.getAction())) {
                try {
                    launcher.launch(new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS));
                } catch (Exception e2) {
                    Log.w(TAG, "Failed to launch battery optimization settings", e2);
                }
            } else {
                Log.w(TAG, "Failed to launch battery optimization settings", e);
            }
        }
    }
}