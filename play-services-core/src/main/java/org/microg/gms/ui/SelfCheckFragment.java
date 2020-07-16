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

package org.microg.gms.ui;

import android.content.Intent;
import android.os.Build;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import org.microg.tools.selfcheck.InstalledPackagesChecks;
//import org.microg.tools.selfcheck.NlpOsCompatChecks;
//import org.microg.tools.selfcheck.NlpStatusChecks;
import org.microg.tools.selfcheck.PermissionCheckGroup;
import org.microg.tools.selfcheck.RomSpoofSignatureChecks;
import org.microg.tools.selfcheck.SelfCheckGroup;
import org.microg.tools.selfcheck.SystemChecks;
import org.microg.tools.ui.AbstractSelfCheckFragment;
import org.microg.tools.ui.AbstractSettingsActivity;

import java.util.List;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.GET_ACCOUNTS;
import static android.Manifest.permission.READ_PHONE_STATE;
import static android.Manifest.permission.WRITE_EXTERNAL_STORAGE;
import static android.os.Build.VERSION.SDK_INT;
import static android.os.Build.VERSION_CODES.LOLLIPOP_MR1;

public class SelfCheckFragment extends AbstractSelfCheckFragment {

    @Override
    protected void prepareSelfCheckList(List<SelfCheckGroup> checks) {
        checks.add(new RomSpoofSignatureChecks());
        checks.add(new InstalledPackagesChecks());
        if (SDK_INT > LOLLIPOP_MR1) {
            checks.add(new PermissionCheckGroup(ACCESS_COARSE_LOCATION, "android.permission.ACCESS_BACKGROUND_LOCATION", WRITE_EXTERNAL_STORAGE, GET_ACCOUNTS, READ_PHONE_STATE));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checks.add(new SystemChecks());
        }
//        checks.add(new NlpOsCompatChecks());
//        checks.add(new NlpStatusChecks());
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        reset(LayoutInflater.from(getContext()));
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == SystemChecks.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
            reset(LayoutInflater.from(getContext()));
        else
            super.onActivityResult(requestCode, resultCode, data);
    }

    public static class AsActivity extends AbstractSettingsActivity {
        public AsActivity() {
            showHomeAsUp = true;
        }

        @Override
        protected Fragment getFragment() {
            return new SelfCheckFragment();
        }
    }
}
