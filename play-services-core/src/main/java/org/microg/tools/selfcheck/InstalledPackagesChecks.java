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

package org.microg.tools.selfcheck;

import android.content.Context;
import android.content.pm.PackageManager;

import com.google.android.gms.R;

import org.microg.gms.common.Constants;
import org.microg.gms.common.PackageUtils;
import org.microg.tools.selfcheck.SelfCheckGroup;

public class InstalledPackagesChecks implements SelfCheckGroup {

    @Override
    public String getGroupName(Context context) {
        return "Installed packages";
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        addPackageInstalledAndSignedResult(context, collector, "Play Services (GmsCore)", Constants.GMS_PACKAGE_NAME, Constants.GMS_PACKAGE_SIGNATURE_SHA1);
        addPackageInstalledAndSignedResult(context, collector, "Play Store (Phonesky)", "com.android.vending", Constants.GMS_PACKAGE_SIGNATURE_SHA1);
        addPackageInstalledResult(context, collector, "Services Framework (GSF)", "com.google.android.gsf");
    }

    private void addPackageInstalledAndSignedResult(Context context, ResultCollector collector, String nicePackageName, String androidPackageName, String signatureHash) {
        if (addPackageInstalledResult(context, collector, nicePackageName, androidPackageName)) {
            addPackageSignedResult(context, collector, nicePackageName, androidPackageName, signatureHash);
        }
    }

    private boolean addPackageSignedResult(Context context, ResultCollector collector, String nicePackageName, String androidPackageName, String signatureHash) {
        boolean hashMatches = signatureHash.equals(PackageUtils.firstSignatureDigest(context, androidPackageName));
        collector.addResult(context.getString(R.string.self_check_name_correct_sig, nicePackageName), hashMatches,
                context.getString(R.string.self_check_resolution_correct_sig, nicePackageName));
        return hashMatches;
    }

    private boolean addPackageInstalledResult(Context context, ResultCollector collector, String nicePackageName, String androidPackageName) {
        boolean packageExists = true;
        try {
            context.getPackageManager().getPackageInfo(androidPackageName, 0);
        } catch (PackageManager.NameNotFoundException e) {
            packageExists = false;
        }
        collector.addResult(context.getString(R.string.self_check_name_app_installed, nicePackageName), packageExists,
                context.getString(R.string.self_check_resolution_app_installed, nicePackageName));
        return packageExists;
    }
}
