/*
 * Copyright (C) 2017 microG Project Team
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

package org.microg.tools.ui;

import android.os.Bundle;

import androidx.appcompat.widget.SwitchCompat;

public abstract class SwitchBarResourceSettingsFragment extends ResourceSettingsFragment implements SwitchBar.OnSwitchChangeListener {
    protected SwitchBar switchBar;
    private SwitchCompat switchCompat;
    private boolean listenerSetup = false;

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

//        switchBar = activity.getSwitchBar();
//        switchBar.show();
//        switchCompat = switchBar.getSwitch();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
//        switchBar.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!listenerSetup) {
//            switchBar.addOnSwitchChangeListener(this);
            listenerSetup = true;
        }
    }

    @Override
    public void onPause() {
        if (listenerSetup) {
//            switchBar.removeOnSwitchChangeListener(this);
            listenerSetup = false;
        }
        super.onPause();
    }

    @Override
    public void onSwitchChanged(SwitchCompat switchView, boolean isChecked) {
        if (switchView == switchCompat) {
            onSwitchBarChanged(isChecked);
        }
    }

    public abstract void onSwitchBarChanged(boolean isChecked);
}
