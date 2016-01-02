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

public class RomSpoofSignatureChecks implements SelfCheckGroup {

    public static final String FAKE_SIGNATURE_PERMISSION = "android.permission.FAKE_PACKAGE_SIGNATURE";

    @Override
    public String getGroupName(Context context) {
        return "ROM spoof signature support";
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        if (addRomKnowsFakeSignaturePermission(context, collector)) {
            if (addSystemGrantsFakeSignaturePermission(context, collector)) {
                addSystemSpoofsSignature(context, collector);
            }
        }
    }

    private boolean addRomKnowsFakeSignaturePermission(Context context, ResultCollector collector) {
        boolean knowsPermission = true;
        try {
            context.getPackageManager().getPermissionInfo(FAKE_SIGNATURE_PERMISSION, 0);
        } catch (PackageManager.NameNotFoundException e) {
            knowsPermission = false;
        }
        collector.addResult(context.getString(R.string.self_check_name_fake_sig_perm), knowsPermission,
                context.getString(R.string.self_check_resolution_fake_sig_perm));
        return knowsPermission;
    }

    private boolean addSystemGrantsFakeSignaturePermission(Context context, ResultCollector collector) {
        boolean grantsPermission = context.checkCallingOrSelfPermission(FAKE_SIGNATURE_PERMISSION) == PackageManager.PERMISSION_GRANTED;
        collector.addResult(context.getString(R.string.self_check_name_perm_granted), grantsPermission,
                context.getString(R.string.self_check_resolution_perm_granted));
        return grantsPermission;
    }

    private boolean addSystemSpoofsSignature(Context context, ResultCollector collector) {
        boolean spoofsSignature = Constants.GMS_PACKAGE_SIGNATURE_SHA1.equals(PackageUtils.firstSignatureDigest(context, Constants.GMS_PACKAGE_NAME));
        collector.addResult(context.getString(R.string.self_check_name_system_spoofs), spoofsSignature,
                context.getString(R.string.self_check_resolution_system_spoofs));
        return spoofsSignature;
    }
}
