/*
 * Copyright 2013-2015 microG Project Team
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

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Binder;

import com.google.android.gms.Manifest;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.microg.gms.common.Constants.GMS_PACKAGE_NAME;
import static org.microg.gms.common.Constants.GMS_PACKAGE_SIGNATURE_SHA1;

public class PackageUtils {

    private static final String[] KNOWN_GOOGLE_SIGNATURES = {
            GMS_PACKAGE_SIGNATURE_SHA1 /* Google platform key */,
            "58e1c4133f7441ec3d2c270270a14802da47ba0e" /* Android Wear */,
            "46f6c8987311e131f4f558d8e0ae145bebab6da3" /* Google Classroom */,
            "24bb24c05e47e0aefa68a58a766179d9b613a600" /* Google Fit/Glass */,
            "aa87ce1260c008d801197bb4ecea4ab8929da246" /* Google Inbox */,
            "01b844184e360686aa98b48eb16e05c76d4a72ad" /* Project Fi */,
            "35b438fe1bc69d975dc8702dc16ab69ebf65f26f" /* Waze */};

    public static boolean isGoogleSignedPackages(Context context, String packageName) {
        return Arrays.asList(KNOWN_GOOGLE_SIGNATURES).contains(firstSignatureDigest(context, packageName));
    }

    public static void assertExtendedAccess(Context context) {
        if (!callerHasExtendedAccess(context))
            throw new SecurityException("Access denied, missing EXTENDED_ACCESS permission");
    }

    public static boolean callerHasExtendedAccess(Context context) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(Binder.getCallingUid());
        if (packagesForUid != null && packagesForUid.length != 0) {
            for (String packageName : packagesForUid) {
                if (isGoogleSignedPackages(context, packageName) || GMS_PACKAGE_NAME.equals(packageName))
                    return true;
            }
        }
        return context.checkCallingPermission(Manifest.permission.EXTENDED_ACCESS) == PackageManager.PERMISSION_GRANTED;
    }

    public static void checkPackageUid(Context context, String packageName, int callingUid) {
        String[] packagesForUid = context.getPackageManager().getPackagesForUid(callingUid);
        if (packagesForUid != null && !Arrays.asList(packagesForUid).contains(packageName)) {
            throw new SecurityException("callingUid [" + callingUid + "] is not related to packageName [" + packageName + "]");
        }
    }

    public static void checkPackageUid(Context context, String packageName, int callerUid, int callingUid) {
        if (callerUid != 0 && callerUid != callingUid) {
            throw new SecurityException("callerUid [" + callerUid + "] and real calling uid [" + callingUid + "] mismatch!");
        }
        checkPackageUid(context, packageName, callingUid);
    }

    public static String firstSignatureDigest(Context context, String packageName) {
        PackageManager packageManager = context.getPackageManager();
        final PackageInfo info;
        try {
            info = packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
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
            throw new RuntimeException(e);
        }
    }
}
