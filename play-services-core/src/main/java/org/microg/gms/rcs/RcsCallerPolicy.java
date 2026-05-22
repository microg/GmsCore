/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.rcs;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.os.Build;
import android.util.Log;

import com.google.android.gms.common.internal.GetServiceRequest;

import org.microg.gms.common.PackageUtils;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Stock-parity caller allowlists for the internal RCS verification surfaces.
 * Matches stock GMS enforcement; relaxing this for third-party RCS clients
 * is out of scope for the bounty and can be revisited separately.
 */
public final class RcsCallerPolicy {
    private static final String TAG = "GmsRcsCallerPolicy";

    private static final Set<String> CONSTELLATION_ALLOWED_PACKAGES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "com.google.android.gms",
                    "com.google.android.apps.messaging",
                    "com.google.android.ims",
                    "com.google.android.apps.tachyon",
                    "com.google.android.dialer",
                    "com.google.android.apps.nbu.paisa.user.dev",
                    "com.google.android.apps.nbu.paisa.user.qa",
                    "com.google.android.apps.nbu.paisa.user.teamfood2",
                    "com.google.android.apps.nbu.paisa.user.partner",
                    "com.google.android.apps.nbu.paisa.user",
                    "com.google.android.gms.constellation.getiidtoken",
                    "com.google.android.gms.constellation.ondemandconsent",
                    "com.google.android.gms.constellation.ondemandconsentv2",
                    "com.google.android.gms.constellation.readphonenumber",
                    "com.google.android.gms.constellation.verifyphonenumberlite",
                    "com.google.android.gms.constellation.verifyphonenumber",
                    "com.google.android.gms.test",
                    "com.google.android.apps.stargate",
                    "com.google.android.gms.firebase.fpnv",
                    "com.google.firebase.pnv.testapp",
                    "com.google.firebase.pnv"
            )));

    private static final Set<String> ASTERISM_ALLOWED_PACKAGES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList(
                    "com.google.android.apps.messaging",
                    "com.google.android.apps.tachyon",
                    "com.google.android.ims",
                    "com.google.android.apps.nbu.paisa.user.dev",
                    "com.google.android.apps.nbu.paisa.user.qa",
                    "com.google.android.apps.nbu.paisa.user.teamfood2",
                    "com.google.android.apps.nbu.paisa.user.partner",
                    "com.google.android.apps.nbu.paisa.user"
            )));

    private RcsCallerPolicy() {}

    public static boolean isConstellationPackageAllowed(String packageName) {
        return CONSTELLATION_ALLOWED_PACKAGES.contains(packageName);
    }

    public static boolean isAsterismPackageAllowed(String packageName) {
        return ASTERISM_ALLOWED_PACKAGES.contains(packageName);
    }

    public static String checkConstellationCaller(Context context, GetServiceRequest request) {
        return checkAllowedCaller(context, request, CONSTELLATION_ALLOWED_PACKAGES, "Constellation");
    }

    public static String checkConstellationCaller(Context context, int callingUid, String suggestedPackageName) {
        return checkAllowedCaller(context, callingUid, suggestedPackageName, CONSTELLATION_ALLOWED_PACKAGES, "Constellation");
    }

    public static String checkAsterismCaller(Context context, GetServiceRequest request) {
        return checkAllowedCaller(context, request, ASTERISM_ALLOWED_PACKAGES, "Asterism");
    }

    public static String getPackageVersionSummary(Context context, String packageName) {
        if (packageName == null || packageName.isEmpty()) return "unknown";
        try {
            PackageInfo info = context.getPackageManager().getPackageInfo(packageName, 0);
            long versionCode = Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? info.getLongVersionCode() : info.versionCode;
            String versionName = info.versionName != null ? info.versionName : "unknown";
            return versionName + "(" + versionCode + ")";
        } catch (Exception e) {
            Log.w(TAG, "Failed to get version for package=" + packageName, e);
            return "unknown";
        }
    }

    private static String checkAllowedCaller(Context context, GetServiceRequest request, Set<String> allowedPackages, String serviceName) {
        if (request == null) {
            throw new IllegalArgumentException(serviceName + " request missing");
        }
        String callingPackage = PackageUtils.getAndCheckCallingPackage(context, request.packageName);
        if (callingPackage == null) {
            throw new SecurityException(serviceName + " caller package missing or invalid");
        }
        if (!allowedPackages.contains(callingPackage)) {
            Log.w(TAG, serviceName + " rejecting caller: " + callingPackage);
            throw new SecurityException(serviceName + " caller not allowed: " + callingPackage);
        }
        return callingPackage;
    }

    private static String checkAllowedCaller(Context context, int callingUid, String suggestedPackageName, Set<String> allowedPackages, String serviceName) {
        String callingPackage = PackageUtils.getAndCheckPackage(context, suggestedPackageName, callingUid);
        if (callingPackage == null) {
            throw new SecurityException(serviceName + " caller package missing or invalid for uid=" + callingUid);
        }
        if (!allowedPackages.contains(callingPackage)) {
            Log.w(TAG, serviceName + " rejecting caller: " + callingPackage + " uid=" + callingUid);
            throw new SecurityException(serviceName + " caller not allowed: " + callingPackage);
        }
        return callingPackage;
    }
}
