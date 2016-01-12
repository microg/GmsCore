/*
 * Copyright 2013-2016 microG Project Team
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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.os.Build;
import android.support.v4.app.Fragment;
import android.util.Log;

import com.google.android.gms.R;

import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

@TargetApi(Build.VERSION_CODES.M)
public class PermissionChecks implements SelfCheckGroup {
    private static final String TAG = "SelfCheckPerms";

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_permissions);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        doPermissionCheck(context, collector, Manifest.permission.ACCESS_COARSE_LOCATION);
        doPermissionCheck(context, collector, Manifest.permission.ACCESS_FINE_LOCATION);
        doPermissionCheck(context, collector, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        doPermissionCheck(context, collector, Manifest.permission.GET_ACCOUNTS);
        doPermissionCheck(context, collector, Manifest.permission.READ_PHONE_STATE);
        doPermissionCheck(context, collector, com.google.android.gms.Manifest.permission.SEND);
    }

    private void doPermissionCheck(Context context, ResultCollector collector, final String permission) {
        PackageManager pm = context.getPackageManager();
        try {
            PermissionInfo info = pm.getPermissionInfo(permission, 0);
            PermissionGroupInfo groupInfo = info.group != null ? pm.getPermissionGroupInfo(info.group, 0) : null;
            CharSequence permLabel = info.loadLabel(pm);
            CharSequence groupLabel = groupInfo != null ? groupInfo.loadLabel(pm) : permLabel;
            collector.addResult(context.getString(R.string.self_check_name_permission, permLabel),
                    context.checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED ? Positive : Negative,
                    context.getString(R.string.self_check_resolution_permission, groupLabel),
                    new SelfCheckGroup.CheckResolver() {

                        @Override
                        public void tryResolve(Fragment fragment) {
                            fragment.requestPermissions(new String[]{permission}, 0);
                        }
                    });
        } catch (PackageManager.NameNotFoundException e) {
            Log.w(TAG, e);
        }
    }
}
