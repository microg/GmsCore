/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R

class SafetyNetPreferencesFragment : PreferenceFragmentCompat() {

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_safetynet)
    }
}
