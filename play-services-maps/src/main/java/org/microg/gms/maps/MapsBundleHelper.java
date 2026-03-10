/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.maps;

import android.os.Bundle;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class MapsBundleHelper {
    @NonNull
    private static ClassLoader getClassLoader() {
        return MapsBundleHelper.class.getClassLoader();
    }

    public static Parcelable getParcelableFromMapStateBundle(@Nullable Bundle bundle, String key) {
        ClassLoader classLoader = getClassLoader();
        bundle.setClassLoader(classLoader);
        Bundle mapStateBundle = bundle.getBundle("map_state");
        if (mapStateBundle == null) {
            return null;
        }
        mapStateBundle.setClassLoader(classLoader);
        return mapStateBundle.getParcelable(key);
    }

    public static void setParcelableInMapStateBundle(Bundle bundle, String key, @Nullable Parcelable parcelable) {
        ClassLoader classLoader = getClassLoader();
        bundle.setClassLoader(classLoader);
        Bundle mapStateBundle = bundle.getBundle("map_state");
        if (mapStateBundle == null) {
            mapStateBundle = new Bundle();
        }
        mapStateBundle.setClassLoader(classLoader);
        mapStateBundle.putParcelable(key, parcelable);
        bundle.putBundle("map_state", mapStateBundle);
    }

    public static void transfer(@Nullable Bundle src, @Nullable Bundle dest) {
        if (src == null || dest == null) {
            return;
        }
        Parcelable parcelableFromMapStateBundle = getParcelableFromMapStateBundle(src, "MapOptions");
        if (parcelableFromMapStateBundle != null) {
            setParcelableInMapStateBundle(dest, "MapOptions", parcelableFromMapStateBundle);
        }
        Parcelable parcelableFromMapStateBundle2 = getParcelableFromMapStateBundle(src, "StreetViewPanoramaOptions");
        if (parcelableFromMapStateBundle2 != null) {
            setParcelableInMapStateBundle(dest, "StreetViewPanoramaOptions", parcelableFromMapStateBundle2);
        }
        Parcelable parcelableFromMapStateBundle3 = getParcelableFromMapStateBundle(src, "camera");
        if (parcelableFromMapStateBundle3 != null) {
            setParcelableInMapStateBundle(dest, "camera", parcelableFromMapStateBundle3);
        }
        if (src.containsKey("position")) {
            dest.putString("position", src.getString("position"));
        }
        if (src.containsKey("com.google.android.wearable.compat.extra.LOWBIT_AMBIENT")) {
            dest.putBoolean("com.google.android.wearable.compat.extra.LOWBIT_AMBIENT", src.getBoolean("com.google.android.wearable.compat.extra.LOWBIT_AMBIENT", false));
        }
    }
}
