package org.microg.gms.ui

import android.os.Bundle
import androidx.preference.PreferenceFragmentCompat
import org.microg.tools.ui.AbstractSettingsActivity

class AccountManagerActivity : AbstractSettingsActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun getFragment(): PreferenceFragmentCompat {
        return AccountsFragment()
    }
}
