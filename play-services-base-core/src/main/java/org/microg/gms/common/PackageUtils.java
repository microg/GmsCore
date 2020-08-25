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

import androidx.annotation.Nullable;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;
import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GMS_PACKAGE_SIGNATURE_SHA1;

public class PackageUtils {

    private static final String GOOGLE_PLATFORM_KEY = GMS_PACKAGE_SIGNATURE_SHA1;
    private static final String GOOGLE_APP_KEY = "24bb24c05e47e0aefa68a58a766179d9b613a600";
    private static final String GOOGLE_LEGACY_KEY = "58e1c4133f7441ec3d2c270270a14802da47ba0e"; // Seems to be no longer used.
    private static final String[] GOOGLE_PRIMARY_KEYS = {GOOGLE_PLATFORM_KEY, GOOGLE_APP_KEY};

    private static final Map<String, String> KNOWN_GOOGLE_PACKAGES;

    static {
        KNOWN_GOOGLE_PACKAGES = new HashMap<>();
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.classroom", "46f6c8987311e131f4f558d8e0ae145bebab6da3");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.inbox", "aa87ce1260c008d801197bb4ecea4ab8929da246");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.playconsole", "d6c35e55b481aefddd74152ca7254332739a81d6");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.travel.onthego", "0cbe08032217d45e61c0bc72f294395ee9ecb5d5");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.tycho", "01b844184e360686aa98b48eb16e05c76d4a72ad");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.contacts", "ee3e2b5d95365c5a1ccc2d8dfe48d94eb33b3ebe");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.wearable.app", "a197f9212f2fed64f0ff9c2a4edf24b9c8801c8c");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.youtube.music", "afb0fed5eeaebdd86f56a97742f4b6b33ef59875");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.vr.home", "fc1edc68f7e3e4963c998e95fc38f3de8d1bfc96");
        KNOWN_GOOGLE_PACKAGES.put("com.google.vr.cyclops", "188c5ca3863fa121216157a5baa80755ceda70ab");
        KNOWN_GOOGLE_PACKAGES.put("com.waze", "35b438fe1bc69d975dc8702dc16ab69ebf65f26f");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.wellbeing", "4ebdd02380f1fa0b6741491f0af35625dba76e9f");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.village.boond", "48e7985b8f901df335b5d5223579c81618431c7b");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.subscriptions.red", "de8304ace744ae4c4e05887a27a790815e610ff0");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.meetings", "47a6936b733dbdb45d71997fbe1d610eca36b8bf");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.nbu.paisa.user", "80df78bb700f9172bc671779b017ddefefcbf552");
    }

    public static boolean isGooglePackage(Context context, String packageName) {
        String signatureDigest = firstSignatureDigest(context, packageName);
        return isGooglePackage(packageName, signatureDigest);
    }

    public static boolean isGooglePackage(PackageManager packageManager, String packageName) {
        String signatureDigest = firstSignatureDigest(packageManager, packageName);
        return isGooglePackage(packageName, signatureDigest);
    }

    public static boolean isGooglePackage(String packageName, byte[] bytes) {
        return isGooglePackage(packageName, sha1sum(bytes));
    }

    public static boolean isGooglePackage(String packageName, String signatureDigest) {
        if (signatureDigest == null) return false;
        if (Arrays.asList(GOOGLE_PRIMARY_KEYS).contains(signatureDigest)) return true;
        if (!KNOWN_GOOGLE_PACKAGES.containsKey(packageName)) return false;
        return KNOWN_GOOGLE_PACKAGES.get(packageName).equals(signatureDigest);
    }

    public static void assertExtendedAccess(Context context) {
        if (!callerHasExtendedAccess(context))
            throw new SecurityException("Access denied, missing EXTENDED_ACCESS permission");
    }

    public static boolean callerHasExtendedAccess(Context context) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null && packagesForUid.length != 0) {
            for (String packageName : packagesForUid) {
                if (isGooglePackage(context, packageName) || GMS_PACKAGE_NAME.equals(packageName))
                    return true;
            }
        }
        return context.checkCallingPermission("org.microg.gms.EXTENDED_ACCESS") == PackageManager.PERMISSION_GRANTED;
    }

    public static void checkPackageUid(Context context, String packageName, int callingUid) {
        getAndCheckPackage(context, packageName, callingUid, 0);
    }

    @Nullable
    public static String firstSignatureDigest(Context context, String packageName) {
        return firstSignatureDigest(context.getPackageManager(), packageName);
    }

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

    @Nullable
    public static String getCallingPackage(Context context) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName == null) {
            packageName = firstPackageFromUserId(context, callingUid);
        }
        return packageName;
    }

    @Nullable
    public static String getAndCheckCallingPackage(Context context, String suggestedPackageName) {
        return getAndCheckCallingPackage(context, suggestedPackageName, 0);
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
        return getAndCheckPackage(context, suggestedPackageName, callingUid, Binder.getCallingPid());
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
        if (SDK_INT < android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
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

    public static int versionCode(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }

    public static String versionName(Context context, String packageName) {
        try {
            return context.getPackageManager().getPackageInfo(packageName, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    public static int targetSdkVersion(Context context, String packageName) {
        try {
            return context.getPackageManager().getApplicationInfo(packageName, 0).targetSdkVersion;
        } catch (PackageManager.NameNotFoundException e) {
            return -1;
        }
    }
}
