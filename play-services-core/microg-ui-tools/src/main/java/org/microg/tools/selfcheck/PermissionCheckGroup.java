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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.util.Log;

import androidx.fragment.app.Fragment;

import org.microg.tools.ui.R;

import static android.os.Build.VERSION_CODES.M;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

@TargetApi(M)
public class PermissionCheckGroup implements SelfCheckGroup {
    private static final String TAG = "SelfCheckPerms";

    private String[] permissions;

    public PermissionCheckGroup(String... permissions) {
        this.permissions = permissions;
    }

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_permissions);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        for (String permission : permissions) {
            doPermissionCheck(context, collector, permission);
        }
    }

    private void doPermissionCheck(Context context, ResultCollector collector, final String permission) {
        PackageManager pm = context.getPackageManager();
        try {
            PermissionInfo info = pm.getPermissionInfo(permission, 0);
            CharSequence permLabel = info.loadLabel(pm);
            collector.addResult(context.getString(R.string.self_check_name_permission, permLabel),
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED ? Positive : Negative,
                    context.getString(R.string.self_check_resolution_permission),
                    fragment -> fragment.requestPermissions(new String[]{permission}, 0));
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
        }
    }
}
