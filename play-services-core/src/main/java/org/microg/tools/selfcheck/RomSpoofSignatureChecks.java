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

package org.microg.tools.selfcheck;

import android.content.Context;
import android.content.pm.PackageManager;

import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.gms.R;

import org.microg.gms.common.Constants;
import org.microg.gms.common.PackageUtils;

import static android.content.pm.PackageManager.PERMISSION_GRANTED;
import static org.microg.gms.common.Constants.GMS_PACKAGE_SIGNATURE_SHA1;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Unknown;

public class RomSpoofSignatureChecks implements SelfCheckGroup {

    public static final String FAKE_SIGNATURE_PERMISSION = "android.permission.FAKE_PACKAGE_SIGNATURE";

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_fake_sig);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        boolean hasPermission = addRomKnowsFakeSignaturePermission(context, collector);
        if (hasPermission) {
            addSystemGrantsFakeSignaturePermission(context, collector);
        }
        addSystemSpoofsSignature(context, collector);
    }

    private boolean addRomKnowsFakeSignaturePermission(Context context, ResultCollector collector) {
        boolean knowsPermission = true;
        try {
            context.getPackageManager().getPermissionInfo(FAKE_SIGNATURE_PERMISSION, 0);
        } catch (PackageManager.NameNotFoundException e) {
            knowsPermission = false;
        }
        collector.addResult(context.getString(R.string.self_check_name_fake_sig_perm), knowsPermission ? Positive : Unknown,
                context.getString(R.string.self_check_resolution_fake_sig_perm));
        return knowsPermission;
    }

    private boolean addSystemGrantsFakeSignaturePermission(Context context, ResultCollector collector) {
        boolean grantsPermission = ContextCompat.checkSelfPermission(context, FAKE_SIGNATURE_PERMISSION) == PERMISSION_GRANTED;
        collector.addResult(context.getString(R.string.self_check_name_perm_granted), grantsPermission ? Positive : Negative,
                context.getString(R.string.self_check_resolution_perm_granted), new CheckResolver() {
                    @Override
                    public void tryResolve(Fragment fragment) {
                        fragment.requestPermissions(new String[]{FAKE_SIGNATURE_PERMISSION}, 0);
                    }
                });
        return grantsPermission;
    }

    private boolean addSystemSpoofsSignature(Context context, ResultCollector collector) {
        boolean spoofsSignature = GMS_PACKAGE_SIGNATURE_SHA1.equals(PackageUtils.firstSignatureDigest(context, Constants.GMS_PACKAGE_NAME));
        collector.addResult(context.getString(R.string.self_check_name_system_spoofs), spoofsSignature ? Positive : Negative,
                context.getString(R.string.self_check_resolution_system_spoofs));
        return spoofsSignature;
    }
}
