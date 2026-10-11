/* SPDX-License-Identifier: Apache-2.0 */
package org.microg.gms.wearable;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Binder;
import org.microg.gms.common.GooglePackagePermission;
import org.microg.gms.utils.ExtendedPackageInfo;
import java.util.Arrays;

/** Privileged companion identity captured before leaving the Binder thread. */
final class ConfigurationCaller {
    private final String packageName;
    private final int uid;
    private final byte[] certificate;

    private ConfigurationCaller(String packageName, int uid, byte[] certificate) {
        this.packageName = packageName;
        this.uid = uid;
        this.certificate = certificate.clone();
    }

    static ConfigurationCaller capture(Context context, String packageName) {
        if (!"com.google.android.apps.wear.companion".equals(packageName)
                && !"com.google.android.wearable.app".equals(packageName)) {
            throw new SecurityException("Unsupported connection manager");
        }
        ConfigurationCaller caller = new ConfigurationCaller(packageName, Binder.getCallingUid(), certificate(context, packageName));
        caller.enforceInstalled(context);
        return caller;
    }

    void enforceInstalled(Context context) {
        try {
            if (context.getPackageManager().getApplicationInfo(packageName, 0).uid != uid
                    || !Arrays.equals(certificate, certificate(context, packageName))) {
                throw new SecurityException("Connection manager changed");
            }
        } catch (PackageManager.NameNotFoundException e) {
            throw new SecurityException("Connection manager unavailable");
        }
    }

    private static byte[] certificate(Context context, String packageName) {
        ExtendedPackageInfo info = new ExtendedPackageInfo(context, packageName);
        // Reuse the existing package-and-certificate allowlist; this grants no account access.
        if (!info.hasGooglePackagePermission(GooglePackagePermission.ACCOUNT)
                || info.getCertificates().size() != 1 || info.getFirstCertificateSha256() == null) {
            throw new SecurityException("Untrusted connection manager");
        }
        return info.getFirstCertificateSha256();
    }
}
