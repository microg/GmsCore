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
import android.content.Intent;
import android.net.Uri;
import android.os.PowerManager;
import android.provider.Settings;

import androidx.fragment.app.Fragment;

import com.google.android.gms.R;

import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Negative;
import static org.microg.tools.selfcheck.SelfCheckGroup.Result.Positive;

@TargetApi(23)
public class SystemChecks implements SelfCheckGroup, SelfCheckGroup.CheckResolver {

    public static final int REQUEST_IGNORE_BATTERY_OPTIMIZATIONS = 417;

    @Override
    public String getGroupName(Context context) {
        return context.getString(R.string.self_check_cat_system);
    }

    @Override
    public void doChecks(Context context, ResultCollector collector) {
        isBatterySavingDisabled(context, collector);
    }

    private void isBatterySavingDisabled(final Context context, ResultCollector collector) {
        PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        collector.addResult(context.getString(R.string.self_check_name_battery_optimizations),
                pm.isIgnoringBatteryOptimizations(context.getPackageName()) ? Positive : Negative,
                context.getString(R.string.self_check_resolution_battery_optimizations), this);
    }

    @Override
    public void tryResolve(Fragment fragment) {
        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
        intent.setData(Uri.parse("package:" + fragment.getActivity().getPackageName()));
        fragment.startActivityForResult(intent, REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
    }
}
