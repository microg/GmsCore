package org.microg.tools.updater;

import androidx.annotation.NonNull;

public class VersionUtils {

    public static int compareVersions(@NonNull String current, @NonNull String latest) {
        String[] cParts = current.replaceAll("[^0-9.]", "").split("\\.");
        String[] lParts = latest.replaceAll("[^0-9.]", "").split("\\.");

        int length = Math.max(cParts.length, lParts.length);
        for (int i = 0; i < length; i++) {
            int cv = (i < cParts.length && !cParts[i].isEmpty()) ? Integer.parseInt(cParts[i]) : 0;
            int lv = (i < lParts.length && !lParts[i].isEmpty()) ? Integer.parseInt(lParts[i]) : 0;

            if (cv != lv) return cv - lv;
        }
        return 0;
    }
}