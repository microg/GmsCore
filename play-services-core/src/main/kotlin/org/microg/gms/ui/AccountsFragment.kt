/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.TwoStatePreference
import com.google.android.gms.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.color.MaterialColors
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.microg.gms.account.AccountPreference
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.login.LoginActivity
import org.microg.gms.common.Constants
import org.microg.gms.gcm.ACTION_GCM_REGISTER_ALL_ACCOUNTS
import org.microg.gms.people.DatabaseHelper
import org.microg.gms.people.PeopleManager
import org.microg.gms.settings.SettingsContract
import org.microg.gms.settings.SettingsContract.Auth

const val PREF_ACCOUNTS_NONE = "pref_current_accounts_none"
const val PREF_ACCOUNTS_ADD = "pref_current_accounts_add"
const val PREFCAT_ACCOUNTS = "prefcat_current_accounts"
val TWO_STATE_SETTINGS = listOf(
    Auth.TRUST_GOOGLE,
    Auth.VISIBLE,
    Auth.INCLUDE_ANDROID_ID,
    Auth.STRIP_DEVICE_NAME,
    Auth.TWO_STEP_VERIFICATION,
    Auth.FIND_DEVICES,
)

class AccountsFragment : PreferenceFragmentCompat() {

    private lateinit var fab: ExtendedFloatingActionButton

    // TODO: This should use some better means of accessing the database
    private fun getDisplayName(account: Account): String? {
        val databaseHelper = DatabaseHelper(context)
        val cursor = databaseHelper.getOwner(account.name)
        return try {
            if (cursor.moveToNext()) {
                cursor.getColumnIndex("display_name").takeIf { it >= 0 }?.let { cursor.getString(it) }.takeIf { !it.isNullOrBlank() }
            } else null
        } finally {
            cursor.close()
            databaseHelper.close()
        }
    }

    private fun getCircleDrawable(bitmap: Bitmap?): Drawable {
        return bitmap?.let {
            RoundedBitmapDrawableFactory.create(resources, it).apply { isCircular = true }
        } ?: AppCompatResources.getDrawable(requireContext(), R.drawable.ic_account_avatar)!!
    }

    private fun registerGcmInGms() {
        Intent(ACTION_GCM_REGISTER_ALL_ACCOUNTS).apply {
            `package` = Constants.GMS_PACKAGE_NAME
        }.let { requireContext().sendBroadcast(it) }
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_accounts)
        updateSettings()
        for (setting in TWO_STATE_SETTINGS) {
            findPreference<TwoStatePreference>(setting)?.setOnPreferenceChangeListener { preference, newValue ->
                if (newValue is Boolean && preference.key in TWO_STATE_SETTINGS) {
                    SettingsContract.setSettings(requireContext(), Auth.getContentUri(requireContext())) { put(preference.key, newValue) }
                    updateSettings()
                    if (preference.key == Auth.TWO_STEP_VERIFICATION && newValue) registerGcmInGms()
                    if (preference.key == Auth.FIND_DEVICES && newValue) registerGcmInGms()
                    true
                } else false
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.setBackgroundColor(MaterialColors.getColor(view, android.R.attr.colorBackground))
        addAccountFab()
    }

    override fun onStart() {
        super.onStart()
        fab.show()
    }

    override fun onStop() {
        super.onStop()
        fab.hide()
    }

    override fun onResume() {
        super.onResume()
        updateSettings()
        fab.show()
    }

    private fun addAccountFab() {
        fab = requireActivity().findViewById(R.id.preference_fab)
        fab.text = getString(R.string.pref_add_account_summary)
        fab.setIconResource(R.drawable.ic_add)
        fab.setOnClickListener {
            startActivity(Intent(requireContext(), LoginActivity::class.java))
        }
    }

    private fun updateSettings() {
        val context = requireContext()

        val accountManager = AccountManager.get(context)
        val accounts = accountManager.getAccountsByType(AuthConstants.DEFAULT_ACCOUNT_TYPE).toList()

        findPreference<Preference>(PREF_ACCOUNTS_NONE)?.isVisible = accounts.isEmpty()
        val preferenceCategory = findPreference<PreferenceCategory>(PREFCAT_ACCOUNTS) ?: return
        // Keep the add and none
        while (preferenceCategory.preferenceCount > 2) {
            preferenceCategory.removePreference(preferenceCategory.getPreference(0))
        }
        accounts.forEachIndexed { index, account ->
            val displayName = getDisplayName(account)
            val photo = PeopleManager.getOwnerAvatarBitmap(context, account.name, false)

            val preference = AccountPreference(context).apply {
                title = displayName ?: account.name
                summary = account.name
                key = "account:${account.name}"
                order = 0
                position = index
                itemCount = accounts.size

                accountAvatar = getCircleDrawable(photo)
                onRemoveListener = { showRemovalDialog(account) }
            }
            preferenceCategory.addPreference(preference)

            if (photo == null) {
                viewLifecycleOwner.lifecycleScope.launch {
                    val hdPhoto = withContext(Dispatchers.IO) {
                        PeopleManager.getOwnerAvatarBitmap(context, account.name, true)
                    }
                    if (hdPhoto != null) {
                        preference.accountAvatar = getCircleDrawable(hdPhoto)
                    }
                }
            }
        }

        for (setting in TWO_STATE_SETTINGS) {
            findPreference<TwoStatePreference>(setting)?.isChecked =
                SettingsContract.getSettings(context, Auth.getContentUri(context), arrayOf(setting)) { c -> c.getInt(0) != 0 }
        }
    }

    private fun showRemovalDialog(account: Account) {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.account_remove_dialog, null)

        dialogView.findViewById<MaterialTextView>(R.id.account_name).text = getDisplayName(account) ?: account.name
        dialogView.findViewById<MaterialTextView>(R.id.account_email).text = account.name

        dialogView.findViewById<MaterialTextView>(R.id.dialog_title).text = getString(R.string.dialog_title_remove_account)
        dialogView.findViewById<MaterialTextView>(R.id.dialog_remove_message).text = getString(R.string.dialog_message_remove_account)
        dialogView.findViewById<MaterialButton>(R.id.dialog_remove_button).text = getString(R.string.dialog_confirm_button)
        dialogView.findViewById<MaterialButton>(R.id.dialog_cancel_button).text = getString(R.string.dialog_cancel_button)

        val buttonRemove = dialogView.findViewById<MaterialButton>(R.id.dialog_remove_button)
        val buttonCancel = dialogView.findViewById<MaterialButton>(R.id.dialog_cancel_button)

        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            val bmp = PeopleManager.getOwnerAvatarBitmap(requireContext(), account.name, true)
            withContext(Dispatchers.Main) {
                dialogView.findViewById<ShapeableImageView>(R.id.account_avatar)
                    .setImageDrawable(getCircleDrawable(bmp))
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext()).setView(dialogView).create()
        buttonRemove.setOnClickListener {
            removeAccount(account)
            dialog.dismiss()
        }
        buttonCancel.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    private fun removeAccount(account: Account) {
        val rootView = view ?: return
        val am = AccountManager.get(requireContext())
        var undoRequested = false

        val snack = Snackbar.make(
            rootView,
            getString(R.string.snackbar_remove_account, account.name),
            Snackbar.LENGTH_LONG
        ).setAction(R.string.snackbar_undo_button) { undoRequested = true }

        snack.addCallback(object : Snackbar.Callback() {
            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                if (!undoRequested && isAdded) {
                    viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
                        if (am.removeAccountExplicitly(account)) {
                            withContext(Dispatchers.Main) { updateSettings() }
                        }
                    }
                }
            }
        })
        snack.show()
    }

    init {
        setHasOptionsMenu(true)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(0, MENU_GAMES_MANAGED, 0, org.microg.gms.base.core.R.string.menu_game_managed)
        menu.add(0, MENU_PASSKEY_MANAGER, 1, R.string.pref_passkey_manager_title)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            MENU_GAMES_MANAGED -> {
                findNavController().navigate(requireContext(), R.id.openGameManagerSettings)
                true
            }

            MENU_PASSKEY_MANAGER -> {
                findNavController().navigate(requireContext(), R.id.openPasskeyManagerSettings)
                true
            }

            else -> super.onOptionsItemSelected(item)
        }
    }

    companion object {
        private const val MENU_GAMES_MANAGED = Menu.FIRST
        private const val MENU_PASSKEY_MANAGER = Menu.FIRST + 1
    }
}
