/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package org.microg.gms.location.reporting

import android.accounts.Account
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.location.reporting.ReportingState
import org.microg.gms.auth.AuthConstants
import userlocation.ApiSettings
import userlocation.ClientAuthTokens
import userlocation.GetApiSettingsRequest
import userlocation.SetApiSettingsRequest
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

private const val CACHE_TTL_MS = 5 * 60_000L
private const val FAILURE_CACHE_TTL_MS = 30_000L
private const val NOT_DIRTY_SOURCE = "com.google.android.gms+not-dirty"
private const val ACCOUNT_SETTINGS_SOURCE = "com.google.android.gms+settings+com.google.android.gms"

private const val API_SETTINGS_SOURCE_CONCURRENT = 3
private const val API_SETTINGS_CONCURRENCY_UNKNOWN_DEVICE_TAG = 2

private const val OPT_IN_RESULT_NOT_ALLOWED = 7
private const val OPT_IN_RESULT_UNSUPPORTED_GEO = 13
private const val OPT_IN_RESULT_UNSUPPORTED_FORM_FACTOR = 14

internal data class AccountApiSettings(
    val modMillis: Long?,
    val historyEnabled: Boolean?,
    val reportingEnabled: Boolean?,
    val userRestriction: Int?,
    val migratedToOdlh: Boolean?,
    val source: Int?,
    val concurrencyType: Int?
)

private data class CachedApiSettings(
    val settings: AccountApiSettings?,
    val expiresAt: Long
)

private data class FetchApiSettingsResult(
    val settings: AccountApiSettings?,
    val retrySoon: Boolean = false
)

private data class SetApiSettingsResult(
    val settings: AccountApiSettings?,
    val applied: Boolean
)

data class EffectiveAccountLocationSettings(
    val timelineEnabled: Boolean,
    val uploadAllowed: Boolean,
    val historyStatus: Int,
    val reportingStatus: Int,
    val allowed: Boolean,
    val active: Boolean,
    val migratedToOdlh: Boolean,
    private val standardOptInResult: Int,
    private val gmsOptInResult: Int
) {
    fun expectedOptInResult(
        callerAllowed: Boolean = true,
        isGmsCaller: Boolean = false
    ): Int = when {
        standardOptInResult == OPT_IN_RESULT_INVALID_ACCOUNT -> OPT_IN_RESULT_INVALID_ACCOUNT
        !callerAllowed -> OPT_IN_RESULT_CALLER_NOT_ALLOWED
        isGmsCaller -> gmsOptInResult
        else -> standardOptInResult
    }

    fun toReportingState(
        deviceTag: Int?,
        canAccessSettings: Boolean = false,
        optInResult: Int = expectedOptInResult()
    ) = ReportingState(
        reportingStatus,
        historyStatus,
        allowed,
        active,
        optInResult,
        optInResult,
        deviceTag,
        canAccessSettings,
        migratedToOdlh
    )
}

data class ManagedAccountLocationSettings(
    val historyEnabled: Boolean?,
    val reportingEnabled: Boolean?,
    val userRestriction: Int?
) {
    val timelineEnabled: Boolean
        get() = historyEnabled == true
}

data class ManagedAccountLocationSettingsUpdate(
    val success: Boolean,
    val settings: ManagedAccountLocationSettings?
)

private val apiSettingsCache = ConcurrentHashMap<Account, CachedApiSettings>()
private val apiSettingsLocks = ConcurrentHashMap<Account, Any>()

internal fun setLocalAccountReportingEnabled(
    context: Context,
    account: Account,
    enabled: Boolean
): Boolean {
    if (!isGoogleAccountOnDevice(context, account)) return false
    if (ReportingSettingsStore.setLocalReportingEnabled(context, account, enabled)) {
        notifyReportingSettingsChanged(context)
    }
    return true
}

fun isLocalAccountReportingEnabled(context: Context, account: Account): Boolean =
    isGoogleAccountOnDevice(context, account) &&
            ReportingSettingsStore.getLocalReportingEnabled(context, account)

fun hasAnyLocalAccountReportingEnabled(context: Context): Boolean = runCatching {
    context.googleAccounts.any { ReportingSettingsStore.getLocalReportingEnabled(context, it) }
}.getOrDefault(false)

private fun AccountApiSettings?.withPendingOptIn(
    context: Context,
    account: Account
): AccountApiSettings? {
    if (ReportingSettingsStore.getPendingOptIn(context, account) == null) return this
    return (this ?: ReportingSettingsStore.getDeviceRegistration(context, account).toLastSettings()).copy(
        historyEnabled = true,
        reportingEnabled = true
    )
}

internal fun queueAccountOptIn(
    context: Context,
    account: Account,
    source: String,
    auditToken: String?
): Boolean {
    if (!isGoogleAccountOnDevice(context, account)) return false
    if (!ReportingSettingsStore.queuePendingOptIn(context, account, source, auditToken)) return false
    notifyReportingSettingsChanged(context)
    return true
}

internal fun getReportingDeviceTag(context: Context, account: Account): Int =
    ReportingSettingsStore.getDeviceRegistration(context, account).deviceTag

private fun ApiSettings.toAccountApiSettings(): AccountApiSettings =
    AccountApiSettings(
        modMillis = modMillis,
        historyEnabled = historyEnabled,
        reportingEnabled = reportingEnabled,
        userRestriction = userRestriction,
        migratedToOdlh = migratedToOdlh,
        source = source,
        concurrencyType = concurrencyType
    )

private fun AccountApiSettings.toManagedAccountLocationSettings(localReportingEnabled: Boolean) =
    ManagedAccountLocationSettings(
        historyEnabled = historyEnabled,
        reportingEnabled = localReportingEnabled,
        userRestriction = userRestriction
    )

private fun ApiSettings.toSetResponseSettings(
    fallback: AccountApiSettings?,
    historyRequested: Boolean?
): AccountApiSettings {
    val response = toAccountApiSettings()
    return response.copy(
        modMillis = response.modMillis ?: fallback?.modMillis,
        historyEnabled = response.historyEnabled
                ?: fallback?.historyEnabled?.takeIf {
                    historyRequested == null || it == historyRequested
                },
        userRestriction = response.userRestriction ?: fallback?.userRestriction,
        migratedToOdlh = response.migratedToOdlh ?: fallback?.migratedToOdlh,
        source = response.source ?: fallback?.source,
        concurrencyType = response.concurrencyType ?: fallback?.concurrencyType
    )
}

private fun AccountApiSettings.matchesRequestedSettings(
    historyEnabled: Boolean?,
    reportingEnabled: Boolean?
): Boolean =
    (historyEnabled == null || this.historyEnabled == historyEnabled) &&
            (reportingEnabled == null || this.reportingEnabled == null || this.reportingEnabled == reportingEnabled)

private fun AccountApiSettings.canRetryConcurrentUpdate(): Boolean =
    source == API_SETTINGS_SOURCE_CONCURRENT &&
            concurrencyType != API_SETTINGS_CONCURRENCY_UNKNOWN_DEVICE_TAG

private fun getApiSettings(session: ReportingApiSession): ApiSettings? {
    val request = GetApiSettingsRequest.Builder()
            .deviceTag(session.deviceTag)
            .device(session.device)
            .requestFlag(false)
            .build()
    return runCatching {
        session.client.GetApiSettings().executeBlocking(request).settings
                ?: throw IOException("GetApiSettings returned no settings")
    }.onFailure {
        Log.w(TAG, "GetApiSettings failed", it)
    }.getOrNull()
}

private fun setApiSettings(
    context: Context,
    account: Account,
    session: ReportingApiSession,
    historyEnabled: Boolean?,
    reportingEnabled: Boolean?,
    deviceReportingEnabled: Boolean,
    source: String,
    auditToken: String?,
    previousSettings: AccountApiSettings?
): SetApiSettingsResult {
    var baseSettings = previousSettings
    var lastResponse: AccountApiSettings? = null
    val requestAuditToken = auditToken?.takeIf { it.isNotBlank() }
    val historyChanged = historyEnabled != null && historyEnabled != previousSettings?.historyEnabled
    val reportingChanged = reportingEnabled != null && reportingEnabled != previousSettings?.reportingEnabled
    val historySource = if (historyChanged) source else NOT_DIRTY_SOURCE
    val reportingSource = if (reportingChanged) source else NOT_DIRTY_SOURCE
    for (attempt in 0..1) {
        val settingsBuilder = ApiSettings.Builder()
                .deviceCapabilities(buildDeviceCapabilities(
                    context,
                    historyEnabled == true && deviceReportingEnabled
                ))
        if (historyEnabled != null) settingsBuilder.historyEnabled(historyEnabled)
        if (reportingEnabled != null) settingsBuilder.reportingEnabled(reportingEnabled)
        baseSettings?.modMillis?.let { settingsBuilder.modMillis(it) }

        val authTokensBuilder = ClientAuthTokens.Builder()
                .historySource(historySource)
                .reportingSource(reportingSource)
        if (requestAuditToken != null) authTokensBuilder.auditToken(requestAuditToken)

        val request = SetApiSettingsRequest.Builder()
                .deviceTag(session.deviceTag)
                .device(session.device)
                .settings(settingsBuilder.build())
                .authTokens(authTokensBuilder.build())
                .requestFlag(baseSettings?.migratedToOdlh == true)
                .build()
        val result = runCatching {
            session.client.SetApiSettings().executeBlocking(request).settings
                    ?: throw IOException("SetApiSettings returned no settings")
        }.onFailure {
            Log.w(TAG, "SetApiSettings failed", it)
        }
        val response = result.getOrNull()?.toSetResponseSettings(baseSettings, historyEnabled)
        if (response == null) {
            lastResponse?.let { ReportingSettingsStore.recordServerSettings(context, account, it) }
            return SetApiSettingsResult(lastResponse, applied = false)
        }
        lastResponse = response

        if (response.matchesRequestedSettings(historyEnabled, reportingEnabled)) {
            ReportingSettingsStore.recordServerSettings(context, account, response)
            return SetApiSettingsResult(response, applied = true)
        }
        if (attempt == 0 && response.canRetryConcurrentUpdate()) {
            baseSettings = response
            continue
        }

        Log.w(TAG, "SetApiSettings did not apply the requested settings")
        ReportingSettingsStore.recordServerSettings(context, account, response)
        return SetApiSettingsResult(response, applied = false)
    }
    return SetApiSettingsResult(lastResponse, applied = false)
}

private fun fetchApiSettings(context: Context, account: Account): FetchApiSettingsResult {
    if (account.type != AuthConstants.DEFAULT_ACCOUNT_TYPE) return FetchApiSettingsResult(null)
    val registration = ReportingSettingsStore.getDeviceRegistration(context, account)
    val lastSettings = registration.toLastSettings()
    val session = openReportingApiSession(context, account, registration.deviceTag)
            ?: return FetchApiSettingsResult(lastSettings, retrySoon = true)

    val apiSettings = getApiSettings(session) ?: return FetchApiSettingsResult(lastSettings, retrySoon = true)

    val serverSettings = apiSettings.toAccountApiSettings()
    ReportingSettingsStore.recordServerSettings(context, account, serverSettings)
    return FetchApiSettingsResult(serverSettings)
}

internal fun setAccountLocationSettings(
    context: Context,
    account: Account,
    historyEnabled: Boolean?,
    reportingEnabled: Boolean?,
    deviceReportingEnabled: Boolean,
    source: String,
    auditToken: String? = null
): Boolean {
    if (account.type != AuthConstants.DEFAULT_ACCOUNT_TYPE) return false
    val lock = apiSettingsLocks.getOrPut(account) { Any() }
    return synchronized(lock) {
        val registration = ReportingSettingsStore.getDeviceRegistration(context, account)
        val session = openReportingApiSession(context, account, registration.deviceTag)
                ?: return@synchronized false
        val cachedSettings = apiSettingsCache[account]?.settings
        val previousSettings = if (!registration.initialized) {
            registration.toLastSettings()
        } else {
            cachedSettings?.takeIf { it.modMillis != null }
                    ?: getApiSettings(session)?.toAccountApiSettings()
                    ?: return@synchronized false
        }
        val result = setApiSettings(
            context = context,
            account = account,
            session = session,
            historyEnabled = historyEnabled,
            reportingEnabled = reportingEnabled,
            deviceReportingEnabled = deviceReportingEnabled,
            source = source,
            auditToken = auditToken,
            previousSettings = previousSettings
        )
        val settings = result.settings ?: previousSettings
        val ttl = if (result.applied) CACHE_TTL_MS else FAILURE_CACHE_TTL_MS
        apiSettingsCache[account] = CachedApiSettings(
            settings = settings,
            expiresAt = SystemClock.elapsedRealtime() + ttl
        )
        result.applied
    }
}

internal fun synchronizePendingAccountOptIn(context: Context, account: Account): Boolean {
    val lock = apiSettingsLocks.getOrPut(account) { Any() }
    return synchronized(lock) {
        val pendingOptIn = ReportingSettingsStore.getPendingOptIn(context, account)
                ?: return@synchronized true
        val applied = setAccountLocationSettings(
            context = context,
            account = account,
            historyEnabled = true,
            reportingEnabled = true,
            deviceReportingEnabled = ReportingSettingsStore.getLocalReportingEnabled(context, account),
            source = pendingOptIn.source,
            auditToken = pendingOptIn.auditToken
        )
        if (applied) {
            ReportingSettingsStore.clearPendingOptIn(context, account)
            notifyReportingSettingsChanged(context)
        }
        applied
    }
}

internal fun synchronizePendingAccountOptIns(context: Context) {
    context.googleAccounts.forEach { synchronizePendingAccountOptIn(context, it) }
}

private fun CachedApiSettings.isUsable(now: Long): Boolean = now < expiresAt

private fun fetchApiSettingsResultCached(
    context: Context,
    account: Account,
    forceRefresh: Boolean = false
): FetchApiSettingsResult {
    val now = SystemClock.elapsedRealtime()
    apiSettingsCache[account]?.takeIf { !forceRefresh && it.isUsable(now) }?.let {
        return FetchApiSettingsResult(it.settings)
    }
    val lock = apiSettingsLocks.getOrPut(account) { Any() }
    return synchronized(lock) {
        val lockedNow = SystemClock.elapsedRealtime()
        apiSettingsCache[account]?.takeIf { !forceRefresh && it.isUsable(lockedNow) }?.let {
            return@synchronized FetchApiSettingsResult(it.settings)
        }
        val result = fetchApiSettings(context, account)
        val ttl = if (result.retrySoon) FAILURE_CACHE_TTL_MS else CACHE_TTL_MS
        apiSettingsCache[account] = CachedApiSettings(
            result.settings,
            SystemClock.elapsedRealtime() + ttl
        )
        result
    }
}

private fun fetchApiSettingsCached(
    context: Context,
    account: Account,
    forceRefresh: Boolean = false
): AccountApiSettings? = fetchApiSettingsResultCached(context, account, forceRefresh).settings.also {
    disableLocalReportingForDisabledTimeline(context, account, it)
}

private fun disableLocalReportingForDisabledTimeline(
    context: Context,
    account: Account,
    settings: AccountApiSettings?
) {
    if (settings?.historyEnabled == false &&
        ReportingSettingsStore.getPendingOptIn(context, account) == null
    ) {
        setLocalAccountReportingEnabled(context, account, false)
    }
}

fun fetchManagedAccountLocationSettings(
    context: Context,
    account: Account,
    forceRefresh: Boolean = false
): ManagedAccountLocationSettings? {
    if (!isGoogleAccountOnDevice(context, account)) return null
    val settings = fetchApiSettingsCached(context, account, forceRefresh)
    return settings.withPendingOptIn(context, account)?.toManagedAccountLocationSettings(
        ReportingSettingsStore.getLocalReportingEnabled(context, account)
    )
}

fun updateManagedAccountLocationSettings(
    context: Context,
    account: Account,
    historyEnabled: Boolean,
    reportingEnabled: Boolean,
    synchronizeHistoryEnabled: Boolean
): ManagedAccountLocationSettingsUpdate {
    if (!isGoogleAccountOnDevice(context, account)) {
        return ManagedAccountLocationSettingsUpdate(false, null)
    }
    val normalizedReportingEnabled = historyEnabled && reportingEnabled
    setLocalAccountReportingEnabled(context, account, normalizedReportingEnabled)
    if (!synchronizeHistoryEnabled) {
        val storedSettings = apiSettingsCache[account]?.settings
                ?: ReportingSettingsStore.getDeviceRegistration(context, account).toLastSettings()
        return ManagedAccountLocationSettingsUpdate(
            success = true,
            settings = storedSettings.withPendingOptIn(context, account)
                    ?.toManagedAccountLocationSettings(normalizedReportingEnabled)
        )
    }
    val lock = apiSettingsLocks.getOrPut(account) { Any() }
    return synchronized(lock) {
        if (!historyEnabled) {
            ReportingSettingsStore.clearPendingOptIn(context, account)
        }
        val applied = setAccountLocationSettings(
            context = context,
            account = account,
            historyEnabled = historyEnabled,
            reportingEnabled = null,
            deviceReportingEnabled = normalizedReportingEnabled,
            source = ACCOUNT_SETTINGS_SOURCE
        )
        val confirmation = fetchApiSettingsResultCached(
            context,
            account,
            forceRefresh = !applied
        )
        disableLocalReportingForDisabledTimeline(context, account, confirmation.settings)
        val settings = confirmation.settings.withPendingOptIn(context, account)
                ?.toManagedAccountLocationSettings(
                    ReportingSettingsStore.getLocalReportingEnabled(context, account)
                )
        val confirmed = settings?.historyEnabled == historyEnabled
        val success = confirmed && !confirmation.retrySoon
        if (success) notifyReportingSettingsChanged(context)
        ManagedAccountLocationSettingsUpdate(
            success = success,
            settings = settings
        )
    }
}

fun fetchEffectiveAccountLocationSettings(
    context: Context,
    account: Account?,
    allowRemoteAccountSettings: Boolean = true,
    refreshRemoteAccountSettings: Boolean = true
): EffectiveAccountLocationSettings {
    val accountOnDevice = isGoogleAccountOnDevice(context, account)
    val storedAccountSettings = when {
        !allowRemoteAccountSettings || !accountOnDevice -> null
        refreshRemoteAccountSettings -> account?.let { fetchApiSettingsCached(context, it) }
        else -> account?.let {
            ReportingSettingsStore.getDeviceRegistration(context, it).toLastSettings()
        }
    }
    val accountSettings = if (allowRemoteAccountSettings && accountOnDevice && account != null) {
        storedAccountSettings.withPendingOptIn(context, account)
    } else {
        storedAccountSettings
    }
    val migratedToOdlh = accountSettings?.migratedToOdlh == true
    val timelineEnabled = accountSettings?.historyEnabled == true
    val localReportingEnabled = account?.takeIf {
        allowRemoteAccountSettings && accountOnDevice
    }?.let {
        ReportingSettingsStore.getLocalReportingEnabled(context, it)
    }
    val uploadAllowed = timelineEnabled && localReportingEnabled == true
    val conditions = getLocalReportingConditions(context)
    val allowed = allowRemoteAccountSettings &&
            accountOnDevice &&
            conditions.allowed &&
            (accountSettings?.userRestriction ?: 0) == 0
    val standardOptInResult = when {
        !accountOnDevice -> OPT_IN_RESULT_INVALID_ACCOUNT
        !allowed -> OPT_IN_RESULT_NOT_ALLOWED
        else -> OPT_IN_RESULT_SUCCESS
    }
    val gmsOptInResult = when {
        !accountOnDevice -> OPT_IN_RESULT_INVALID_ACCOUNT
        !conditions.deviceFormFactorSupported -> OPT_IN_RESULT_UNSUPPORTED_FORM_FACTOR
        !conditions.countryAllowed -> OPT_IN_RESULT_UNSUPPORTED_GEO
        !allowed -> OPT_IN_RESULT_NOT_ALLOWED
        else -> OPT_IN_RESULT_SUCCESS
    }
    return EffectiveAccountLocationSettings(
        timelineEnabled = timelineEnabled,
        uploadAllowed = uploadAllowed,
        historyStatus = accountSettings?.historyEnabled.toReportingStatus(),
        reportingStatus = localReportingEnabled.toReportingStatus(),
        allowed = allowed,
        active = allowed && uploadAllowed && conditions.locationEnabled,
        migratedToOdlh = migratedToOdlh,
        standardOptInResult = standardOptInResult,
        gmsOptInResult = gmsOptInResult
    )
}

fun fetchEffectiveTimelineEnabled(
    context: Context,
    account: Account?,
    allowRemoteAccountSettings: Boolean
): Boolean {
    if (!allowRemoteAccountSettings || !isGoogleAccountOnDevice(context, account)) return false
    return account?.let {
        fetchApiSettingsCached(context, it).withPendingOptIn(context, it)?.historyEnabled
    } == true
}
