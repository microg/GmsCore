/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.reporting

import android.accounts.Account
import android.annotation.SuppressLint
import android.content.Context
import androidx.core.content.edit
import org.microg.gms.location.LocationSettings
import java.util.Random

private const val DEVICE_TAG_KEY_PREFIX = "deviceTag_"
private const val LEGACY_DEVICE_TAG_KEY_PREFIX = "device_tag_"
private const val DEVICE_INITIALIZED_KEY_PREFIX = "device_initialized_"
private const val DEVICE_HISTORY_ENABLED_KEY_PREFIX = "device_history_enabled_"
private const val DEVICE_REPORTING_ENABLED_KEY_PREFIX = "device_reporting_enabled_"
private const val DEVICE_MIGRATED_TO_ODLH_KEY_PREFIX = "device_migrated_to_odlh_"
private const val DEVICE_USER_RESTRICTION_KEY_PREFIX = "device_user_restriction_"
private const val PENDING_OPT_IN_KEY_PREFIX = "pending_opt_in_"
private const val PENDING_OPT_IN_SOURCE_KEY_PREFIX = "pending_opt_in_source_"
private const val PENDING_OPT_IN_AUDIT_TOKEN_KEY_PREFIX = "pending_opt_in_audit_token_"

internal data class PendingOptIn(
    val source: String,
    val auditToken: String?
)

internal data class DeviceRegistration(
    val deviceTag: Int,
    val initialized: Boolean,
    val lastHistoryEnabled: Boolean?,
    val lastUserRestriction: Int?,
    val lastMigratedToOdlh: Boolean?
)

internal fun DeviceRegistration.toLastSettings() = AccountApiSettings(
    modMillis = null,
    historyEnabled = lastHistoryEnabled,
    reportingEnabled = null,
    userRestriction = lastUserRestriction,
    migratedToOdlh = lastMigratedToOdlh,
    source = null,
    concurrencyType = null
)

internal object ReportingSettingsStore {
    private val lock = Any()
    private val deviceTagRandom = Random()

    // Reporting consent is local to this account and device. A missing server field must not reset it.
    fun getLocalReportingEnabled(context: Context, account: Account): Boolean = synchronized(lock) {
        val preferences = context.reportingPreferences
        val key = DEVICE_REPORTING_ENABLED_KEY_PREFIX + account.reportingPreferenceSuffix
        if (preferences.contains(key)) {
            preferences.getBoolean(key, false)
        } else {
            LocationSettings(context).mapsTimelineUpload.also {
                preferences.edit { putBoolean(key, it) }
            }
        }
    }

    fun setLocalReportingEnabled(context: Context, account: Account, enabled: Boolean): Boolean =
        synchronized(lock) {
            val preferences = context.reportingPreferences
            val key = DEVICE_REPORTING_ENABLED_KEY_PREFIX + account.reportingPreferenceSuffix
            val changed = !preferences.contains(key) || preferences.getBoolean(key, false) != enabled
            if (changed) preferences.edit { putBoolean(key, enabled) }
            changed
        }

    fun getPendingOptIn(context: Context, account: Account): PendingOptIn? = synchronized(lock) {
        val preferences = context.reportingPreferences
        val suffix = account.reportingPreferenceSuffix
        if (!preferences.getBoolean(PENDING_OPT_IN_KEY_PREFIX + suffix, false)) {
            null
        } else {
            val source = preferences.getString(PENDING_OPT_IN_SOURCE_KEY_PREFIX + suffix, null)
                ?: return@synchronized null
            PendingOptIn(
                source = source,
                auditToken = preferences.getString(PENDING_OPT_IN_AUDIT_TOKEN_KEY_PREFIX + suffix, null)
            )
        }
    }

    @SuppressLint("UseKtx")
    fun queuePendingOptIn(
        context: Context,
        account: Account,
        source: String,
        auditToken: String?
    ): Boolean = synchronized(lock) {
        val preferences = context.reportingPreferences
        val suffix = account.reportingPreferenceSuffix
        preferences.edit().apply {
            putBoolean(DEVICE_REPORTING_ENABLED_KEY_PREFIX + suffix, true)
            putBoolean(PENDING_OPT_IN_KEY_PREFIX + suffix, true)
            putString(PENDING_OPT_IN_SOURCE_KEY_PREFIX + suffix, source)
            if (auditToken.isNullOrBlank()) {
                remove(PENDING_OPT_IN_AUDIT_TOKEN_KEY_PREFIX + suffix)
            } else {
                putString(PENDING_OPT_IN_AUDIT_TOKEN_KEY_PREFIX + suffix, auditToken)
            }
        }.commit()
    }

    fun clearPendingOptIn(context: Context, account: Account) = synchronized(lock) {
        val suffix = account.reportingPreferenceSuffix
        context.reportingPreferences.edit {
            remove(PENDING_OPT_IN_KEY_PREFIX + suffix)
            remove(PENDING_OPT_IN_SOURCE_KEY_PREFIX + suffix)
            remove(PENDING_OPT_IN_AUDIT_TOKEN_KEY_PREFIX + suffix)
        }
    }

    fun getDeviceRegistration(context: Context, account: Account): DeviceRegistration = synchronized(lock) {
        val preferences = context.reportingPreferences
        val suffix = account.reportingPreferenceSuffix
        val deviceTagKey = DEVICE_TAG_KEY_PREFIX + account
        val legacyDeviceTagKey = LEGACY_DEVICE_TAG_KEY_PREFIX + suffix
        val initializedKey = DEVICE_INITIALIZED_KEY_PREFIX + suffix
        val historyEnabledKey = DEVICE_HISTORY_ENABLED_KEY_PREFIX + suffix
        val userRestrictionKey = DEVICE_USER_RESTRICTION_KEY_PREFIX + suffix
        val migratedToOdlhKey = DEVICE_MIGRATED_TO_ODLH_KEY_PREFIX + suffix
        val deviceTag = when {
            preferences.contains(deviceTagKey) -> preferences.getInt(deviceTagKey, 0)
            preferences.contains(legacyDeviceTagKey) -> preferences.getInt(legacyDeviceTagKey, 0).also {
                preferences.edit { putInt(deviceTagKey, it) }
            }
            preferences.contains("deviceTag") -> preferences.getInt("deviceTag", 0).also {
                preferences.edit {
                    remove("deviceTag")
                    putInt(deviceTagKey, it)
                }
            }
            else -> deviceTagRandom.nextInt().also {
                preferences.edit {
                    putInt(deviceTagKey, it)
                    putBoolean(initializedKey, false)
                }
            }
        }
        DeviceRegistration(
            deviceTag = deviceTag,
            initialized = preferences.getBoolean(initializedKey, false),
            lastHistoryEnabled = if (preferences.contains(historyEnabledKey)) {
                preferences.getBoolean(historyEnabledKey, false)
            } else null,
            lastUserRestriction = if (preferences.contains(userRestrictionKey)) {
                preferences.getInt(userRestrictionKey, 0)
            } else null,
            lastMigratedToOdlh = if (preferences.contains(migratedToOdlhKey)) {
                preferences.getBoolean(migratedToOdlhKey, false)
            } else null
        )
    }

    fun recordServerSettings(context: Context, account: Account, settings: AccountApiSettings) = synchronized(lock) {
        val suffix = account.reportingPreferenceSuffix
        context.reportingPreferences.edit {
            putBoolean(DEVICE_INITIALIZED_KEY_PREFIX + suffix, true)
            if (settings.historyEnabled == null) {
                remove(DEVICE_HISTORY_ENABLED_KEY_PREFIX + suffix)
            } else {
                putBoolean(DEVICE_HISTORY_ENABLED_KEY_PREFIX + suffix, settings.historyEnabled)
            }
            if (settings.userRestriction == null) {
                remove(DEVICE_USER_RESTRICTION_KEY_PREFIX + suffix)
            } else {
                putInt(DEVICE_USER_RESTRICTION_KEY_PREFIX + suffix, settings.userRestriction)
            }
            if (settings.migratedToOdlh == null) {
                remove(DEVICE_MIGRATED_TO_ODLH_KEY_PREFIX + suffix)
            } else {
                putBoolean(DEVICE_MIGRATED_TO_ODLH_KEY_PREFIX + suffix, settings.migratedToOdlh)
            }
        }
    }
}