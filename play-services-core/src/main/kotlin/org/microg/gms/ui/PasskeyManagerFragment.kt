/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.ui

import android.content.Context
import android.os.Bundle
import android.text.format.DateUtils
import android.util.Base64
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.lifecycle.lifecycleScope
import androidx.preference.Preference
import androidx.preference.PreferenceCategory
import androidx.preference.PreferenceFragmentCompat
import com.google.android.gms.R
import com.google.android.gms.fido.fido2.api.common.PublicKeyCredentialUserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.microg.gms.fido.core.Database
import org.microg.gms.fido.core.KnownRegistration
import org.microg.gms.fido.core.transport.Transport
import org.microg.gms.fido.core.transport.screenlock.ScreenLockCredentialStore
import org.microg.gms.profile.Build

class PasskeyManagerFragment : PreferenceFragmentCompat() {

    private lateinit var category: PreferenceCategory
    private lateinit var emptyPlaceholder: Preference

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.preferences_passkey_manager)
        category = preferenceScreen.findPreference(PREFCAT_PASSKEYS) ?: return
        emptyPlaceholder = preferenceScreen.findPreference(PREF_PASSKEYS_NONE) ?: return
    }

    override fun onResume() {
        super.onResume()
        updateContent()
    }

    private fun updateContent() {
        val ctx = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val list = withContext(Dispatchers.IO) {
                runCatching { Database(ctx).getAllKnownRegistrations() }
                    .onFailure { Log.w(TAG, "Failed to load passkeys", it) }
                    .getOrDefault(emptyList())
            }
            category.removeAll()
            if (list.isEmpty()) {
                category.addPreference(emptyPlaceholder)
            } else {
                list.forEachIndexed { index, item ->
                    category.addPreference(buildPasskeyPreference(ctx, item, index))
                }
            }
        }
    }

    private fun buildPasskeyPreference(ctx: Context, item: KnownRegistration, order: Int): Preference =
        Preference(ctx).apply {
            key = "pref_passkey_${item.rpId}_${item.credentialId}"
            this.order = order
            isIconSpaceReserved = false
            widgetLayoutResource = R.layout.widget_passkey_delete
            title = item.rpId
            summary = buildSummary(ctx, item)
            setOnPreferenceClickListener {
                confirmDelete(item)
                true
            }
        }

    private fun confirmDelete(item: KnownRegistration) {
        val displayUser = formatPasskeyUser(requireContext(), item.userJson)
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.pref_passkey_manager_delete_dialog_title)
            .setMessage(getString(R.string.pref_passkey_manager_delete_dialog_message, item.rpId, displayUser))
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.pref_passkey_manager_delete_dialog_confirm) { _, _ ->
                performDelete(item)
            }
            .show()
    }

    private fun performDelete(item: KnownRegistration) {
        val ctx = requireContext().applicationContext
        lifecycleScope.launchWhenResumed {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    if (Build.VERSION.SDK_INT >= 23) {
                        val keyId = Base64.decode(item.credentialId, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                        ScreenLockCredentialStore(ctx).deleteKey(item.rpId, keyId)
                    }
                    Database(ctx).deleteKnownRegistration(item.rpId, item.credentialId)
                }.onFailure { Log.w(TAG, "Failed to delete passkey", it) }.isSuccess
            }
            Log.d(TAG, "performDelete ok? = $ok")
            updateContent()
        }
    }

    companion object {
        private const val TAG = "PasskeyManager"
        private const val PREFCAT_PASSKEYS = "prefcat_passkeys"
        private const val PREF_PASSKEYS_NONE = "pref_passkeys_none"
    }
}

internal fun formatPasskeyUser(context: Context, userJson: String?): String {
    if (userJson.isNullOrBlank()) return context.getString(R.string.pref_passkey_manager_unknown_user)
    return try {
        val entity = PublicKeyCredentialUserEntity.parseJson(userJson)
        val displayName = entity.displayName?.takeIf { it.isNotBlank() }
        val name = entity.name?.takeIf { it.isNotBlank() }
        when {
            displayName != null && name != null && displayName != name -> "$displayName ($name)"
            displayName != null -> displayName
            name != null -> name
            else -> context.getString(R.string.pref_passkey_manager_unknown_user)
        }
    } catch (e: Exception) {
        context.getString(R.string.pref_passkey_manager_unknown_user)
    }
}

private fun buildSummary(context: Context, item: KnownRegistration): String {
    val user = formatPasskeyUser(context, item.userJson)
    val time = DateUtils.getRelativeTimeSpanString(
        item.timestamp,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()
    val transportLabel = context.getString(transportLabelRes(item.transport))
    val credId = context.getString(
        R.string.pref_passkey_manager_credential_id_format_internal,
        truncateCredentialId(item.credentialId)
    )
    return "$user\n$time · $transportLabel\n$credId"
}

private fun transportLabelRes(transport: Transport): Int = when (transport) {
    Transport.SCREEN_LOCK -> R.string.pref_passkey_manager_transport_screen_lock
    Transport.USB -> R.string.pref_passkey_manager_transport_usb
    Transport.NFC -> R.string.pref_passkey_manager_transport_nfc
    Transport.BLUETOOTH -> R.string.pref_passkey_manager_transport_bluetooth
    Transport.HYBRID -> R.string.pref_passkey_manager_transport_hybrid
}

private fun truncateCredentialId(id: String): String {
    if (id.length <= 16) return id
    return id.substring(0, 6) + "…" + id.substring(id.length - 6)
}
