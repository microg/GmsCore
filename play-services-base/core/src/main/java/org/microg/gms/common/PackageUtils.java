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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import org.microg.gms.utils.ExtendedPackageInfo;

import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.os.Build.VERSION.SDK_INT;
import org.microg.gms.base.core.BuildConfig;
import static org.microg.gms.common.Constants.GMS_PACKAGE_SIGNATURE_SHA1;
import static org.microg.gms.common.Constants.GMS_SECONDARY_PACKAGE_SIGNATURE_SHA1;

public class PackageUtils {

    private static final String GOOGLE_PLATFORM_KEY = GMS_PACKAGE_SIGNATURE_SHA1;
    private static final String GOOGLE_PLATFORM_KEY_2 = GMS_SECONDARY_PACKAGE_SIGNATURE_SHA1;
    private static final String GOOGLE_APP_KEY = "24bb24c05e47e0aefa68a58a766179d9b613a600";
    private static final String GOOGLE_LEGACY_KEY = "58e1c4133f7441ec3d2c270270a14802da47ba0e"; // Seems to be no longer used.
    private static final String[] GOOGLE_PRIMARY_KEYS = {GOOGLE_PLATFORM_KEY, GOOGLE_PLATFORM_KEY_2, GOOGLE_APP_KEY};

    // Additional Google packages whose (re-signed) forks are identified by their real
    // Google signature digest. Mirrors MicroG-RE so extended access / account access
    // works for apps like YouTube Music, whose digest is not in GOOGLE_PRIMARY_KEYS.
    public static final Map<String, String> KNOWN_GOOGLE_PACKAGES;

    static {
        KNOWN_GOOGLE_PACKAGES = new HashMap<>();
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.classroom", "46f6c8987311e131f4f558d8e0ae145bebab6da3");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.inbox", "aa87ce1260c008d801197bb4ecea4ab8929da246");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.playconsole", "d6c35e55b481aefddd74152ca7254332739a81d6");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.travel.onthego", "0cbe08032217d45e61c0bc72f294395ee9ecb5d5");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.tycho", "01b844184e360686aa98b48eb16e05c76d4a72ad");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.contacts", "ee3e2b5d95365c5a1ccc2d8dfe48d94eb33b3ebe");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.wearable.app", "a197f9212f2fed64f0ff9c2a4edf24b9c8801c8c");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.youtube", "24bb24c05e47e0aefa68a58a766179d9b613a600");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.youtube.music", "afb0fed5eeaebdd86f56a97742f4b6b33ef59875");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.vr.home", "fc1edc68f7e3e4963c998e95fc38f3de8d1bfc96");
        KNOWN_GOOGLE_PACKAGES.put("com.google.vr.cyclops", "188c5ca3863fa121216157a5baa80755ceda70ab");
        KNOWN_GOOGLE_PACKAGES.put("com.waze", "35b438fe1bc69d975dc8702dc16ab69ebf65f26f");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.wellbeing", "4ebdd02380f1fa0b6741491f0af35625dba76e9f");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.village.boond", "48e7985b8f901df335b5d5223579c81618431c7b");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.subscriptions.red", "de8304ace744ae4c4e05887a27a790815e610ff0");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.meetings", "47a6936b733dbdb45d71997fbe1d610eca36b8bf");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.nbu.paisa.user", "80df78bb700f9172bc671779b017ddefefcbf552");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.dynamite", "519c5a17a60596e6fe5933b9cb4285e7b0e5eb7b");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.projection.gearhead", "9ca91f9e704d630ef67a23f52bf1577a92b9ca5d");
        KNOWN_GOOGLE_PACKAGES.put("com.google.stadia.android", "133aad3b3d3b580e286573c37f20549f9d3d1cce");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.kids.familylink", "88652b8464743e5ce80da0d4b890d13f9b1873df");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.walletnfcrel", "82759e2db43f9ccbafce313bc674f35748fabd7a");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.recorder", "394d84cd2cf89d3453702c663f98ec6554afc3cd");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.messaging", "0980a12be993528c19107bc21ad811478c63cefc");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.tachyon", "a0bc09af527b6397c7a9ef171d6cf76f757becc3");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.tasks", "5af12649728293f2d9f5e8c61e455a8fea16e7d8");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.jam", "9c1de3b64590e313470bb920f9f4b7819ee06140");
        KNOWN_GOOGLE_PACKAGES.put("com.fitbit.FitbitMobile", "29a4514c3b90b90cb6badc79614262195c6a5747");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.magazines", "bd32424203e0fb25f36b57e5aa356f9bdd1da998");
        KNOWN_GOOGLE_PACKAGES.put("com.google.android.apps.photos", "24bb24c05e47e0aefa68a58a766179d9b613a600");
    }

    @Deprecated
    public static boolean isGooglePackage(@NonNull Context context, @Nullable String packageName) {
        if (packageName == null) return false;
        // Re-signed Google forks declare their original identity via SPOOFED_PACKAGE_NAME;
        // look the digest up under that name so KNOWN_GOOGLE_PACKAGES matches. Mirrors
        // MicroG-RE (which also handles known Google packages that aren't installed under
        // the real name, e.g. intent targets like com.google.android.youtube).
        String signatureDigest = firstSignatureDigest(context, packageName);
        String spoofedPackageName = PackageSpoofUtils.spoofPackageName(context.getPackageManager(), packageName);
        String checkPackage = spoofedPackageName != null ? spoofedPackageName : packageName;
        if (isGooglePackage(checkPackage, signatureDigest)) return true;
        if (signatureDigest == null && KNOWN_GOOGLE_PACKAGES.containsKey(checkPackage)) return true;
        return new ExtendedPackageInfo(context, packageName).isPlatformPackage();
    }

    /**
     * @deprecated Extended access is a deprecated concept
     */
    @Deprecated
    public static boolean callerHasExtendedAccessPermission(@NonNull Context context) {
        return context.checkCallingPermission(BuildConfig.BASE_PACKAGE_NAME + ".gms.EXTENDED_ACCESS") == PackageManager.PERMISSION_GRANTED;
    }

    public static void assertGooglePackagePermission(@NonNull Context context, GooglePackagePermission permission) {
        try {
            if (!callerHasGooglePackagePermission(context, permission))
                throw new SecurityException("Access denied, missing google package permission for " + permission.name());
        } catch (SecurityException e) {
            Log.w("ExtendedAccess", e);
            throw e;
        }
    }

    public static boolean callerHasGooglePackagePermission(@NonNull Context context, GooglePackagePermission permission) {
        for (String packageCandidate : getCallingPackageCandidates(context)) {
            if (new ExtendedPackageInfo(context, packageCandidate).hasGooglePackagePermission(permission)) {
                return true;
            }
        }

        // TODO: Replace with explicit permission instead of generic "extended access"
        // Fall back to the spoof-aware extended-access check (MicroG-RE parity) so
        // re-signed Google forks whose digest is not in GOOGLE_PRIMARY_KEYS (e.g.
        // YouTube Music) still pass GooglePackagePermission gates like ACCOUNT.
        if (callerHasExtendedAccess(context)) return true;

        return false;
    }

    public static void checkPackageUid(@NonNull Context context, @NonNull String packageName, int callingUid) {
        getAndCheckPackage(context, packageName, callingUid, 0);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(@NonNull Context context, @Nullable String packageName) {
        return firstSignatureDigest(context, packageName, false);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(@NonNull Context context, @Nullable String packageName, boolean useSigningInfo) {
        return firstSignatureDigest(context.getPackageManager(), packageName, useSigningInfo);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(@NonNull PackageManager packageManager, @Nullable String packageName) {
        return firstSignatureDigest(packageManager, packageName, false);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static String firstSignatureDigest(@NonNull PackageManager packageManager, String packageName, boolean useSigningInfo) {
        // Spoof the signature of known Google apps (ReVanced forks of Google packages
        // are re-signed, so their real digest no longer matches). Mirrors MicroG-RE.
        if (packageName.endsWith(".youtube")) {
            return GOOGLE_APP_KEY;
        } else if (packageName.endsWith(".youtube.music")) {
            return "afb0fed5eeaebdd86f56a97742f4b6b33ef59875";
        } else if (packageName.endsWith(".photos")) {
            return GOOGLE_APP_KEY;
        } else if (packageName.endsWith(".magazines")) {
            return "bd32424203e0fb25f36b57e5aa356f9bdd1da998";
        }
        String digest = bytesToSumString(firstSignatureDigestBytes(packageManager, packageName, useSigningInfo));
        // spoof or use real one
        return PackageSpoofUtils.spoofStringSignature(packageManager, packageName, digest);
    }

    @Deprecated
    public static boolean isGooglePackage(String packageName, String signatureDigest) {
        if (signatureDigest == null) return false;
        if (Arrays.asList(GOOGLE_PRIMARY_KEYS).contains(signatureDigest)) return true;
        if (!KNOWN_GOOGLE_PACKAGES.containsKey(packageName)) return false;
        return KNOWN_GOOGLE_PACKAGES.get(packageName).equals(signatureDigest);
    }

    public static void assertExtendedAccess(@NonNull Context context) {
        if (!callerHasExtendedAccess(context))
            throw new SecurityException("Access denied, missing EXTENDED_ACCESS permission");
    }

    /**
     * @deprecated Extended access is a deprecated concept
     */
    @Deprecated
    public static boolean callerHasExtendedAccess(@NonNull Context context) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null && packagesForUid.length != 0) {
            for (String packageName : packagesForUid) {
                // Re-signed Google forks (e.g. Morphe apps) declare their original Google
                // identity via SPOOFED_PACKAGE_NAME meta-data; look the digest up under the
                // spoofed name so KNOWN_GOOGLE_PACKAGES matches. Mirrors MicroG-RE.
                String spoofedPackageName = PackageSpoofUtils.spoofPackageName(context.getPackageManager(), packageName);
                if (isGooglePackage(spoofedPackageName != null ? spoofedPackageName : packageName, firstSignatureDigest(context, packageName))
                        || Constants.GMS_PACKAGE_NAME.equals(packageName))
                    return true;
            }
        }
        return callerHasExtendedAccessPermission(context);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static byte[] firstSignatureDigestBytes(@NonNull Context context, @Nullable String packageName) {
        return firstSignatureDigestBytes(context.getPackageManager(), packageName);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static byte[] firstSignatureDigestBytes(@NonNull PackageManager packageManager, @Nullable String packageName) {
        return firstSignatureDigestBytes(packageManager, packageName, false);
    }

    /**
     * @deprecated We should stop using SHA-1 for certificate fingerprints!
     */
    @Deprecated
    @Nullable
    public static byte[] firstSignatureDigestBytes(@NonNull PackageManager packageManager, @Nullable String packageName, boolean useSigningInfo) {
        if (packageName == null) return null;
        final PackageInfo info;
        try {
            info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES | (useSigningInfo && SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : 0));
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        if (info == null) return null;
        if (SDK_INT >= 28 && useSigningInfo && info.signingInfo != null) {
            if (!info.signingInfo.hasMultipleSigners()) {
                for (Signature sig : info.signingInfo.getSigningCertificateHistory()) {
                    byte[] digest = sha1bytes(sig.toByteArray());
                    if (digest != null) {
                        // spoof or use real one
                        return PackageSpoofUtils.spoofBytesSignature(packageManager, packageName, digest);
                    }
                }
            }
        }
        if (info.signatures != null) {
            for (Signature sig : info.signatures) {
                byte[] digest = sha1bytes(sig.toByteArray());
                if (digest != null) {
                    // spoof or use real one
                    return PackageSpoofUtils.spoofBytesSignature(packageManager, packageName, digest);
                }
            }
        }
        return null;
    }

    @Nullable
    public static String getCallingPackage(@NonNull Context context) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName == null) {
            packageName = firstPackageFromUserId(context, callingUid);
        }

        // spoof or use real one
        return PackageSpoofUtils.spoofPackageName(context.getPackageManager(), packageName);
    }

    public static String[] getCallingPackageCandidates(@NonNull Context context) {
        int callingUid = Binder.getCallingUid(), callingPid = Binder.getCallingPid();
        String packageName = packageFromProcessId(context, callingPid);
        if (packageName != null) return new String[]{packageName};
        String[] candidates = context.getPackageManager().getPackagesForUid(callingUid);
        if (candidates == null) return new String[0];
        return candidates;
    }

    @Nullable
    public static String getAndCheckCallingPackage(@NonNull Context context, @Nullable String suggestedPackageName) {
        return getAndCheckCallingPackage(context, suggestedPackageName, 0);
    }

    @Nullable
    public static String getAndCheckCallingPackageOrImpersonation(@NonNull Context context, @Nullable String suggestedPackageName) {
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
    public static String getAndCheckCallingPackage(@NonNull Context context, int suggestedCallerUid) {
        return getAndCheckCallingPackage(context, null, suggestedCallerUid);
    }

    @Nullable
    public static String getAndCheckCallingPackage(@NonNull Context context, @Nullable String suggestedPackageName, int suggestedCallerUid) {
        return getAndCheckCallingPackage(context, suggestedPackageName, suggestedCallerUid, 0);
    }

    @Nullable
    public static String getAndCheckCallingPackage(@NonNull Context context, @Nullable String suggestedPackageName, int suggestedCallerUid, int suggestedCallerPid) {
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
    public static String getAndCheckPackage(@NonNull Context context, @Nullable String suggestedPackageName, int callingUid, int callingPid) {
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

        // spoof or use real one
        return PackageSpoofUtils.spoofPackageName(context.getPackageManager(), packageName);
    }

    @Nullable
    @Deprecated
    public static String packageFromProcessId(@NonNull Context context, int pid) {
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
    public static String firstPackageFromUserId(@NonNull Context context, int uid) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(uid);
        if (packagesForUid != null && packagesForUid.length != 0) {
            return packagesForUid[0];
        }
        return null;
    }

    @SuppressWarnings("deprecation")
    public static String packageFromPendingIntent(@Nullable PendingIntent pi) {
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
        return bytesToSumString(sha1bytes(bytes));
    }

    @Nullable
    private static String bytesToSumString(@Nullable byte[] bytes) {
        if (bytes == null) return null;
        StringBuilder sb = new StringBuilder(2 * bytes.length);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
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
