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

package org.microg.gms.common;

import android.app.ActivityManager;
import android.app.Application;
import android.app.PendingIntent;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;
import android.util.Log;

import androidx.annotation.Nullable;
import org.microg.gms.utils.ExtendedPackageInfo;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GMS_PACKAGE_SIGNATURE_SHA1;
import static org.microg.gms.common.Constants.GMS_SECONDARY_PACKAGE_SIGNATURE_SHA1;

public class PackageUtils {

    private static final String GOOGLE_PLATFORM_KEY = GMS_PACKAGE_SIGNATURE_SHA1;
    private static final String GOOGLE_PLATFORM_KEY_2 = GMS_SECONDARY_PACKAGE_SIGNATURE_SHA1;
    private static final String GOOGLE_APP_KEY = "24bb24c05e47e0aefa68a58a766179d9b613a600";
    private static final String GOOGLE_LEGACY_KEY = "58e1c4133f7441ec3d2c270270a14802da47ba0e"; // Seems to be no longer used.
    private static final String[] GOOGLE_PRIMARY_KEYS = {GOOGLE_PLATFORM_KEY, GOOGLE_PLATFORM_KEY_2, GOOGLE_APP_KEY};

    @Deprecated
    public static boolean isGooglePackage(Context context, String packageName) {
        return new ExtendedPackageInfo(context, packageName).isGoogleOrPlatformPackage();
    }

    /**
     * @deprecated Extended access is a deprecated concept
     */
    @Deprecated
    public static boolean callerHasExtendedAccessPermission(Context context) {
        return context.checkCallingPermission("org.microg.gms.EXTENDED_ACCESS") == PackageManager.PERMISSION_GRANTED;
    }

    public static void assertGooglePackagePermission(Context context, GooglePackagePermission permission) {
        try {
            if (!callerHasGooglePackagePermission(context, permission))
                throw new SecurityException("Access denied, missing google package permission for " + permission.name());
        } catch (SecurityException e) {
            Log.w("ExtendedAccess", e);
            throw e;
        }
    }

    public static boolean callerHasGooglePackagePermission(Context context, GooglePackagePermission permission) {
        for (String packageCandidate : getCallingPackageCandidates(context)) {
            if (new ExtendedPackageInfo(context, packageCandidate).hasGooglePackagePermission(permission)) {
                return true;
            }
        }

        // TODO: Replace with explicit permission instead of generic "extended access"
        if (callerHasExtendedAccessPermission(context)) return true;

        return false;
    }

    public static void checkPackageUid(Context context, String packageName, int callingUid) {
        getAndCheckPackage(context, packageName, callingUid, 0);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(Context context, String packageName) {
        return firstSignatureDigest(context.getPackageManager(), packageName);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(PackageManager packageManager, String packageName) {
        final PackageInfo info;
        try {
            info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        if (info != null && info.signatures != null && info.signatures.length > 0) {
            for (Signature sig : info.signatures) {
                String digest = sha1sum(sig.toByteArray());
                if (digest != null) {
                    return digest;
                }
            }
        }
        return null;
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static byte[] firstSignatureDigestBytes(Context context, String packageName) {
        return firstSignatureDigestBytes(context.getPackageManager(), packageName);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static byte[] firstSignatureDigestBytes(PackageManager packageManager, String packageName) {
        final PackageInfo info;
        try {
            info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        if (info != null && info.signatures != null && info.signatures.length > 0) {
            for (Signature sig : info.signatures) {
                byte[] digest = sha1bytes(sig.toByteArray());
                if (digest != null) {
                    return digest;
                }
            }
        }
        return null;
    }

    @Nullable
    public static String getCallingPackage(Context context) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName == null) {
            packageName = firstPackageFromUserId(context, callingUid);
        }
        return packageName;
    }

    public static String[] getCallingPackageCandidates(Context context) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName != null) return new String[]{packageName};
        String[] candidates = context.getPackageManager().getPackagesForUid(callingUid);
        if (candidates == null) return new String[0];
        return candidates;
    }

    @Nullable
    public static String getAndCheckCallingPackage(Context context, String suggestedPackageName) {
        return getAndCheckCallingPackage(context, suggestedPackageName, 0);
    }

    @Nullable
    public static String getAndCheckCallingPackageOrImpersonation(Context context, String suggestedPackageName) {
        try {
            return getAndCheckCallingPackage(context, suggestedPackageName, 0);
        } catch (Exception e) {
            if (callerHasGooglePackagePermission(context, GooglePackagePermission.IMPERSONATE)) {
                return suggestedPackageName;
            }
            throw e;
        }
    }

    @Nullable
    public static String getAndCheckCallingPackage(Context context, int suggestedCallerUid) {
        return getAndCheckCallingPackage(context, null, suggestedCallerUid);
    }

    @Nullable
    public static String getAndCheckCallingPackage(Context context, String suggestedPackageName, int suggestedCallerUid) {
        return getAndCheckCallingPackage(context, suggestedPackageName, suggestedCallerUid, 0);
    }

    @Nullable
    public static String getAndCheckCallingPackage(Context context, String suggestedPackageName, int suggestedCallerUid, int suggestedCallerPid) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        if (suggestedCallerUid > 0 && suggestedCallerUid != callingUid) {
            throw new SecurityException("suggested UID [" + suggestedCallerUid + "] and real calling UID [" + callingUid + "] mismatch!");
        }
        if (suggestedCallerPid > 0 && suggestedCallerPid != callingPid) {
            throw new SecurityException("suggested PID [" + suggestedCallerPid + "] and real calling PID [" + callingPid + "] mismatch!");
        }
        return getAndCheckPackage(context, suggestedPackageName, callingUid, callingPid);
    }

    @Nullable
    public static String getAndCheckPackage(Context context, String suggestedPackageName, int callingUid) {
        return getAndCheckPackage(context, suggestedPackageName, callingUid, 0);
    }

    @Nullable
    public static String getAndCheckPackage(Context context, String suggestedPackageName, int callingUid, int callingPid) {
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName == null) {
            String[] packagesForUid = context.getPackageManager().getPackagesForUid(callingUid);
            if (packagesForUid != null && packagesForUid.length != 0) {
                if (packagesForUid.length == 1) {
                    packageName = packagesForUid[0];
                } else if (Arrays.asList(packagesForUid).contains(suggestedPackageName)) {
                    packageName = suggestedPackageName;
                } else {
                    packageName = packagesForUid[0];
                }
            }
        }
        if (packageName != null && suggestedPackageName != null && !packageName.equals(suggestedPackageName)) {
            throw new SecurityException("UID [" + callingUid + "] is not related to packageName [" + suggestedPackageName + "] (seems to be " + packageName + ")");
        }
        return packageName;
    }

    @Nullable
    @Deprecated
    public static String packageFromProcessId(Context context, int pid) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager == null) return null;
        if (pid <= 0) return null;
        List<ActivityManager.RunningAppProcessInfo> runningAppProcesses = manager.getRunningAppProcesses();
        if (runningAppProcesses != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : runningAppProcesses) {
                if (processInfo.pid == pid && processInfo.pkgList.length == 1) {
                    return processInfo.pkgList[0];
                }
            }
        }
        return null;
    }

    @Nullable
    public static String firstPackageFromUserId(Context context, int uid) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(uid);
        if (packagesForUid != null && packagesForUid.length != 0) {
            return packagesForUid[0];
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static String packageFromPendingIntent(PendingIntent pi) {
        if (pi == null) return null;
        if (SDK_INT < 17) {
            return pi.getTargetPackage();
        } else {
            return pi.getCreatorPackage();
        }
    }

    public static String getProcessName() {
        if (android.os.Build.VERSION.SDK_INT >= 28)
            return Application.getProcessName();
        try {
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            String methodName = android.os.Build.VERSION.SDK_INT >= 18 ? "currentProcessName" : "currentPackageName";
            Method getProcessName = activityThread.getDeclaredMethod(methodName);
            return (String) getProcessName.invoke(null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static boolean isPersistentProcess() {
        String processName = getProcessName();
        if (processName == null) {
            Log.w("GmsPackageUtils", "Can't determine process name of current process");
            return false;
        }
        return processName.endsWith(":persistent");
    }

    public static boolean isMainProcess(Context context) {
        String processName = getProcessName();
        if (processName == null) {
            Log.w("GmsPackageUtils", "Can't determine process name of current process");
            return false;
        }
        return processName.equals(context.getPackageName());
    }

    public static void warnIfNotPersistentProcess(Class<?> clazz) {
        if (!isPersistentProcess()) {
            Log.w("GmsPackageUtils", clazz.getSimpleName() + " initialized outside persistent process", new RuntimeException());
        }
    }

    public static void warnIfNotMainProcess(Context context, Class<?> clazz) {
        if (!isMainProcess(context)) {
            Log.w("GmsPackageUtils", clazz.getSimpleName() + " initialized outside main process", new RuntimeException());
        }
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    public static String sha1sum(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (final NoSuchAlgorithmException e) {
            return null;
        }
        if (md != null) {
            bytes = md.digest(bytes);
            if (bytes != null) {
                StringBuilder sb = new StringBuilder(2 * bytes.length);
                for (byte b : bytes) {
                    sb.append(String.format("%02x", b));
                }
                return sb.toString();
            }
        }
        return null;
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    public static byte[] sha1bytes(byte[] bytes) {
        MessageDigest md;
        try {
            md = MessageDigest.getInstance("SHA1");
        } catch (final NoSuchAlgorithmException e) {
            return null;
        }
        if (md != null) {
            return md.digest(bytes);
        }
        return null;
    }

    @Deprecated
    public static int versionCode(Context context, String packageName) {
        return new ExtendedPackageInfo(context, packageName).getShortVersionCode();
    }

    @Deprecated
    public static String versionName(Context context, String packageName) {
        return new ExtendedPackageInfo(context, packageName).getVersionName();
    }

    @Deprecated
    public static int targetSdkVersion(Context context, String packageName) {
        return new ExtendedPackageInfo(context, packageName).getTargetSdkVersion();
    }
}
