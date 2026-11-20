/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.vision.text.ui

import android.content.Intent
import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import org.microg.gms.vision.core.R

open class DynamicModulesFragment : PreferenceFragmentCompat() {
    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        setPreferencesFromResource(R.xml.preferences_dynamic_modules, rootKey)
        findPreference<Preference>("pref_dynamic_modules_text_recognition")!!.intent =
            Intent(requireContext(), OcrModelsActivity::class.java)
    }
}
