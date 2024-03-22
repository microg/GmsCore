package org.microg.gms.ui

import android.accounts.AccountManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import org.microg.gms.auth.AuthConstants

class AccountsFragment : PreferenceFragmentCompat() {

    private val TAG = AccountsFragment::class.java.simpleName

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_accounts)

        val accountManager = AccountManager.get(requireContext())
        val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE)

        findPreference<Preference>("pref_current_accounts_none")?.isVisible = accounts.isEmpty()
        val preferenceCategory = findPreference<PreferenceCategory>("prefcat_current_accounts")
        accounts.forEach {
            Preference(requireContext()).apply {
                title = it.name
                icon = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.proprietary_auth_gls_ic_google_minitab_selected
                )
                preferenceCategory?.addPreference(this)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        findPreference<Preference>("pref_manage_accounts")?.setOnPreferenceClickListener {
            try {
                startActivity(Intent(Settings.ACTION_SYNC_SETTINGS))
            } catch (activityNotFoundException: ActivityNotFoundException) {
                Log.e(TAG, "Failed to launch sync settings", activityNotFoundException)
            }
            true
        }
    }
}
