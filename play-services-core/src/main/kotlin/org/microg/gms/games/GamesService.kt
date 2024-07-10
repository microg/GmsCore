/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.gms.games

import android.accounts.Account
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.os.Parcel
import android.util.Log
import androidx.core.app.PendingIntentCompat
import androidx.core.os.bundleOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.common.api.Scope
import com.google.android.gms.common.api.Status
import com.google.android.gms.common.data.DataHolder
import com.google.android.gms.common.internal.ConnectionInfo
import com.google.android.gms.common.internal.GetServiceRequest
import com.google.android.gms.common.internal.IGmsCallbacks
import com.google.android.gms.games.Player
import com.google.android.gms.games.PlayerColumns
import com.google.android.gms.games.PlayerEntity
import com.google.android.gms.games.internal.IGamesCallbacks
import com.google.android.gms.games.internal.IGamesClient
import com.google.android.gms.games.internal.IGamesService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.microg.gms.BaseService
import org.microg.gms.auth.AuthConstants
import org.microg.gms.auth.AuthManager
import org.microg.gms.auth.AuthPrefs
import org.microg.gms.common.GmsService
import org.microg.gms.common.PackageUtils
import org.microg.gms.utils.warnOnTransactionIssues

private const val TAG = "GamesService"

class GamesService : BaseService(TAG, GmsService.GAMES) {
    override fun handleServiceRequest(callback: IGmsCallbacks, request: GetServiceRequest, service: GmsService) {
        val packageName = PackageUtils.getAndCheckCallingPackageOrImpersonation(this, request.packageName)
            ?: throw IllegalArgumentException("Missing package name")

        fun sendSignInRequired() {
            Log.d(TAG, "Sending SIGN_IN_REQUIRED to $packageName")
            callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.SIGN_IN_REQUIRED, null, ConnectionInfo().apply {
                params = bundleOf(
                    "pendingIntent" to PendingIntentCompat.getActivity(
                        this@GamesService,
                        packageName.hashCode(),
                        Intent(this@GamesService, GamesSignInActivity::class.java).apply {
                            putExtra(EXTRA_GAME_PACKAGE_NAME, request.packageName)
                            putExtra(EXTRA_ACCOUNT, request.account)
                            putExtra(EXTRA_SCOPES, request.scopes)
                        },
                        PendingIntent.FLAG_UPDATE_CURRENT,
                        false
                    )
                )
            })
        }

        lifecycleScope.launchWhenStarted {
            try {
                val account = request.account?.takeIf { it.name != AuthConstants.DEFAULT_ACCOUNT }
                    ?: GamesConfigurationService.getDefaultAccount(this@GamesService, packageName)
                    ?: return@launchWhenStarted sendSignInRequired()

                val scopes = request.scopes.toList().realScopes
                Log.d(TAG, "handleServiceRequest scopes to ${scopes.joinToString(" ")}")

                val authManager = AuthManager(this@GamesService, account.name, packageName, "oauth2:${scopes.joinToString(" ")}")
                if (!authManager.isPermitted && !AuthPrefs.isTrustGooglePermitted(this@GamesService)) {
                    Log.d(TAG, "Not permitted to use $account for ${scopes.toList()}, sign in required")
                    return@launchWhenStarted sendSignInRequired()
                }

                if (!performGamesSignIn(this@GamesService, packageName, account, scopes = scopes)) {
                    Log.d(TAG, "performGamesSignIn fail, sign in required")
                    return@launchWhenStarted sendSignInRequired()
                }

                val player = JSONObject(GamesConfigurationService.getPlayer(this@GamesService, packageName, account)).toPlayer()

                callback.onPostInitCompleteWithConnectionInfo(
                    CommonStatusCodes.SUCCESS,
                    GamesServiceImpl(this@GamesService, lifecycle, packageName, account, player),
                    ConnectionInfo()
                )
            } catch (e: Exception) {
                Log.w(TAG, e)
                runCatching { callback.onPostInitCompleteWithConnectionInfo(ConnectionResult.INTERNAL_ERROR, null, null) }
            }
        }
    }
}

class GamesServiceImpl(val context: Context, override val lifecycle: Lifecycle, val packageName: String, val account: Account, val player: Player) :
    IGamesService.Stub(), LifecycleOwner {

    override fun clientDisconnecting(clientId: Long) {
        Log.d(TAG, "Not yet implemented: clientDisconnecting($clientId)")
    }

    override fun signOut(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "signOut called")
        lifecycleScope.launchWhenStarted {
            GamesConfigurationService.setDefaultAccount(context, packageName, null)
            callbacks?.onSignOutComplete()
        }
    }

    override fun getAppId(): String? {
        Log.d(TAG, "Not yet implemented: getAppId")
        return null
    }

    override fun getConnectionHint(): Bundle? {
        return null
    }

    override fun showWelcomePopup(windowToken: IBinder?, extraArgs: Bundle?) {
        runCatching { extraArgs?.keySet() }
        Log.d(TAG, "Not yet implemented: showWelcomePopup($windowToken, $extraArgs)")
    }

    override fun cancelPopups() {
        Log.d(TAG, "Not yet implemented: cancelPopups")
    }

    override fun getCurrentAccountName(): String? {
        Log.d(TAG, "getCurrentAccountName called: ${account.name}")
        return account.name
    }

    override fun loadGameplayAclInternal(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadGameplayAclInternal($gameId)")
    }

    override fun updateGameplayAclInternal(callbacks: IGamesCallbacks?, gameId: String?, aclData: String?) {
        Log.d(TAG, "Not yet implemented: updateGameplayAclInternal($gameId, $aclData)")
    }

    override fun loadFAclInternal(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadFAclInternal($gameId)")
    }

    override fun updateFAclInternal(callbacks: IGamesCallbacks?, gameId: String?, allCirclesVisible: Boolean, circleIds: LongArray?) {
        Log.d(TAG, "Not yet implemented: updateFAclInternal($gameId, $allCirclesVisible, $circleIds)")
    }

    override fun getCurrentPlayerId(): String? {
        Log.d(TAG, "getCurrentPlayerId called: ${player.playerId}")
        return player.playerId
    }

    override fun getCurrentPlayer(): DataHolder? {
        return if (player is PlayerEntity) {
            DataHolder.builder(PlayerColumns.CURRENT_PLAYER_COLUMNS.toTypedArray()).withRow(player.toContentValues()).build(CommonStatusCodes.SUCCESS)
        } else {
            DataHolder.builder(PlayerColumns.CURRENT_PLAYER_COLUMNS.toTypedArray()).build(CommonStatusCodes.SIGN_IN_REQUIRED)
        }
    }

    override fun loadPlayer(callbacks: IGamesCallbacks?, playerId: String?) {
        Log.d(TAG, "Not yet implemented: loadPlayer($playerId)")
    }

    override fun loadInvitablePlayers(callbacks: IGamesCallbacks?, pageSize: Int, expandCachedData: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadInvitablePlayers($pageSize, $expandCachedData, $forceReload)")
    }

    override fun submitScore(callbacks: IGamesCallbacks?, leaderboardId: String?, score: Long) {
        Log.d(TAG, "Not yet implemented: submitScore($leaderboardId, $score)")
    }

    override fun loadLeaderboards(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadLeaderboards")
    }

    override fun loadLeaderboard(callbacks: IGamesCallbacks?, leaderboardId: String?) {
        Log.d(TAG, "Not yet implemented: loadLeaderboard($leaderboardId)")
    }

    override fun loadTopScores(
        callbacks: IGamesCallbacks?,
        leaderboardId: String?,
        span: Int,
        leaderboardCollection: Int,
        maxResults: Int,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadTopScores($leaderboardId, $span, $leaderboardCollection, $maxResults, $forceReload)")
    }

    override fun loadPlayerCenteredScores(
        callbacks: IGamesCallbacks?,
        leaderboardId: String?,
        span: Int,
        leaderboardCollection: Int,
        maxResults: Int,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadPlayerCenteredScores($leaderboardId, $span, $leaderboardCollection, $maxResults, $forceReload)")
    }

    override fun loadMoreScores(callbacks: IGamesCallbacks?, previousheader: Bundle?, maxResults: Int, pageDirection: Int) {
        runCatching { previousheader?.keySet() }
        Log.d(TAG, "Not yet implemented: loadMoreScores($previousheader, $maxResults, $pageDirection)")
    }

    override fun loadAchievements(callbacks: IGamesCallbacks?) {
        loadAchievementsV2(callbacks, false)
    }

    override fun revealAchievement(callbacks: IGamesCallbacks?, achievementId: String?, windowToken: IBinder?, extraArgs: Bundle?) {
        runCatching { extraArgs?.keySet() }
        Log.d(TAG, "Not yet implemented: revealAchievement($achievementId, $windowToken, $extraArgs)")
    }

    override fun unlockAchievement(callbacks: IGamesCallbacks?, achievementId: String?, windowToken: IBinder?, extraArgs: Bundle?) {
        runCatching { extraArgs?.keySet() }
        Log.d(TAG, "Not yet implemented: unlockAchievement($achievementId, $windowToken, $extraArgs")
    }

    override fun incrementAchievement(callbacks: IGamesCallbacks?, achievementId: String?, numSteps: Int, windowToken: IBinder?, extraArgs: Bundle?) {
        runCatching { extraArgs?.keySet() }
        Log.d(TAG, "Not yet implemented: incrementAchievement($achievementId, $numSteps, $windowToken, $extraArgs)")
    }

    override fun loadGame(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadGame")
    }

    override fun loadInvitations(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadInvitations")
    }

    override fun declineInvitation(invitationId: String?, invitationType: Int) {
        Log.d(TAG, "Not yet implemented: declineInvitation($invitationId, $invitationType)")
    }

    override fun dismissInvitation(invitationId: String?, invitationType: Int) {
        Log.d(TAG, "Not yet implemented: dismissInvitation($invitationId, $invitationType)")
    }

    override fun createRoom(
        callbacks: IGamesCallbacks?,
        processBinder: IBinder?,
        variant: Int,
        invitedPlayerIds: Array<out String>?,
        autoMatchCriteria: Bundle?,
        enableSockets: Boolean,
        clientId: Long
    ) {
        Log.d(TAG, "Not yet implemented: createRoom($variant, $invitedPlayerIds, $autoMatchCriteria, $enableSockets, $clientId)")
    }

    override fun joinRoom(callbacks: IGamesCallbacks?, processBinder: IBinder?, matchId: String?, enableSockets: Boolean, clientId: Long) {
        Log.d(TAG, "Not yet implemented: joinRoom($matchId, $enableSockets, $clientId)")
    }

    override fun leaveRoom(callbacks: IGamesCallbacks?, matchId: String?) {
        Log.d(TAG, "Not yet implemented: leaveRoom($matchId)")
    }

    override fun sendReliableMessage(callbacks: IGamesCallbacks?, messageData: ByteArray?, matchId: String?, recipientParticipantId: String?): Int {
        Log.d(TAG, "Not yet implemented: sendReliableMessage($messageData, $matchId, $recipientParticipantId)")
        return 0
    }

    override fun sendUnreliableMessage(messageData: ByteArray?, matchId: String?, recipientParticipantIds: Array<out String>?): Int {
        Log.d(TAG, "Not yet implemented: sendUnreliableMessage($messageData, $matchId, $recipientParticipantIds)")
        return 0
    }

    override fun createSocketConnection(participantId: String?): String? {
        Log.d(TAG, "Not yet implemented: createSocketConnection($participantId)")
        return null
    }

    override fun clearNotifications(notificationTypes: Int) {
        Log.d(TAG, "Not yet implemented: clearNotifications($notificationTypes)")
    }

    override fun loadLeaderboardsFirstParty(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadLeaderboardsFirstParty($gameId)")
    }

    override fun loadLeaderboardFirstParty(callbacks: IGamesCallbacks?, gameId: String?, leaderboardId: String?) {
        Log.d(TAG, "Not yet implemented: loadLeaderboardFirstParty($gameId, $leaderboardId)")
    }

    override fun loadTopScoresFirstParty(
        callbacks: IGamesCallbacks?,
        gameId: String?,
        leaderboardId: String?,
        span: Int,
        leaderboardCollection: Int,
        maxResults: Int,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadTopScoresFirstParty($gameId, $leaderboardId, $span, $leaderboardCollection, $maxResults, $forceReload)")
    }

    override fun loadPlayerCenteredScoresFirstParty(
        callbacks: IGamesCallbacks?,
        gameId: String?,
        leaderboardId: String?,
        span: Int,
        leaderboardCollection: Int,
        maxResults: Int,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadPlayerCenteredScoresFirstParty($gameId, $leaderboardId, $span, $leaderboardCollection, $maxResults, $forceReload)")
    }

    override fun loadAchievementsFirstParty(callbacks: IGamesCallbacks?, playerId: String?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadAchievementsFirstParty($playerId, $gameId)")
    }

    override fun loadGameFirstParty(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadGameFirstParty($gameId)")
    }

    override fun loadGameInstancesFirstParty(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadGameInstancesFirstParty($gameId)")
    }

    override fun loadGameCollectionFirstParty(
        callbacks: IGamesCallbacks?,
        pageSize: Int,
        collectionType: Int,
        expandCachedData: Boolean,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadGameCollectionFirstParty($pageSize, $collectionType, $expandCachedData, $forceReload)")
    }

    override fun loadRecentlyPlayedGamesFirstParty(
        callbacks: IGamesCallbacks?,
        externalPlayerId: String?,
        pageSize: Int,
        expandCachedData: Boolean,
        forceReload: Boolean
    ) {
        Log.d(TAG, "Not yet implemented: loadRecentlyPlayedGamesFirstParty($externalPlayerId, $pageSize, $expandCachedData, $forceReload)")
    }

    override fun loadInvitablePlayersFirstParty(callbacks: IGamesCallbacks?, pageSize: Int, expandCachedData: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadInvitablePlayersFirstParty($pageSize, $expandCachedData, $forceReload)")
    }

    override fun loadRecentPlayersFirstParty(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadRecentPlayersFirstParty")
    }

    override fun loadCircledPlayersFirstParty(callbacks: IGamesCallbacks?, pageSize: Int, expandCachedData: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadCircledPlayersFirstParty($pageSize, $expandCachedData, $forceReload)")
    }

    override fun loadSuggestedPlayersFirstParty(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadSuggestedPlayersFirstParty")
    }

    override fun dismissPlayerSuggestionFirstParty(playerIdToDismiss: String?) {
        Log.d(TAG, "Not yet implemented: dismissPlayerSuggestionFirstParty($playerIdToDismiss)")
    }

    override fun declineInvitationFirstParty(gameId: String?, invitationId: String?, invitationType: Int) {
        Log.d(TAG, "Not yet implemented: declineInvitationFirstParty($gameId, $invitationId, $invitationType)")
    }

    override fun loadInvitationsFirstParty(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: loadInvitationsFirstParty($gameId)")
    }

    override fun registerWaitingRoomListenerRestricted(callbacks: IGamesCallbacks?, roomId: String?): Int {
        Log.d(TAG, "Not yet implemented: registerWaitingRoomListenerRestricted($roomId)")
        return 0
    }

    override fun setGameMuteStatusInternal(callbacks: IGamesCallbacks?, gameId: String?, muted: Boolean) {
        Log.d(TAG, "Not yet implemented: setGameMuteStatusInternal($gameId, $muted)")
    }

    override fun clearNotificationsFirstParty(gameId: String?, notificationTypes: Int) {
        Log.d(TAG, "Not yet implemented: clearNotificationsFirstParty($gameId, $notificationTypes)")
    }

    override fun loadNotifyAclInternal(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadNotifyAclInternal")
    }

    override fun updateNotifyAclInternal(callbacks: IGamesCallbacks?, aclData: String?) {
        Log.d(TAG, "Not yet implemented: updateNotifyAclInternal($aclData)")
    }

    override fun registerInvitationListener(callbacks: IGamesCallbacks?, clientId: Long) {
        Log.d(TAG, "Not yet implemented: registerInvitationListener($clientId)")
    }

    override fun unregisterInvitationListener(clientId: Long) {
        Log.d(TAG, "Not yet implemented: unregisterInvitationListener($clientId)")
    }

    override fun unregisterWaitingRoomListenerRestricted(roomId: String?): Int {
        Log.d(TAG, "Not yet implemented: unregisterWaitingRoomListenerRestricted($roomId)")
        return 0
    }

    override fun isGameMutedInternal(callbacks: IGamesCallbacks?, gameId: String?) {
        Log.d(TAG, "Not yet implemented: isGameMutedInternal($gameId)")
    }

    override fun loadContactSettingsInternal(callbacks: IGamesCallbacks?) {
        Log.d(TAG, "Not yet implemented: loadContactSettingsInternal")
    }

    override fun updateContactSettingsInternal(callbacks: IGamesCallbacks?, enableMobileNotifications: Boolean) {
        Log.d(TAG, "Not yet implemented: updateContactSettingsInternal($enableMobileNotifications)")
    }

    override fun getSelectedAccountForGameFirstParty(gamePackageName: String?): String? {
        Log.d(TAG, "Not yet implemented: getSelectedAccountForGameFirstParty($gamePackageName)")
        return null
    }

    override fun updateSelectedAccountForGameFirstParty(gamePackageName: String?, accountName: String?) {
        Log.d(TAG, "Not yet implemented: updateSelectedAccountForGameFirstParty($gamePackageName, $accountName)")
    }

    override fun getGamesContentUriRestricted(gameId: String?): Uri? {
        Log.d(TAG, "Not yet implemented: getGamesContentUriRestricted($gameId)")
        return null
    }

    override fun shouldUseNewPlayerNotificationsFirstParty(): Boolean {
        Log.d(TAG, "Not yet implemented: shouldUseNewPlayerNotificationsFirstParty")
        return false
    }

    override fun setUseNewPlayerNotificationsFirstParty(newPlayerStyle: Boolean) {
        Log.d(TAG, "Not yet implemented: setUseNewPlayerNotificationsFirstParty($newPlayerStyle)")
    }

    override fun searchForPlayersFirstParty(callbacks: IGamesCallbacks?, query: String?, pageSize: Int, expandCachedData: Boolean, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: searchForPlayersFirstParty($query, $pageSize, $expandCachedData, $forceReload)")
    }

    override fun getCurrentGame(): DataHolder? {
        Log.d(TAG, "Not yet implemented: getCurrentGame")
        return null
    }

    override fun loadAchievementsV2(callbacks: IGamesCallbacks?, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadAchievementsV2($forceReload)")
        callbacks?.onAchievementsLoaded(DataHolder.empty(CommonStatusCodes.SUCCESS))
    }

    override fun submitLeaderboardScore(callbacks: IGamesCallbacks?, leaderboardId: String?, score: Long, scoreTag: String?) {
        Log.d(TAG, "Not yet implemented: submitLeaderboardScore($leaderboardId, $score, $scoreTag)")
    }

    override fun setAchievementSteps(callbacks: IGamesCallbacks?, id: String?, numSteps: Int, windowToken: IBinder?, extras: Bundle?) {
        runCatching { extras?.keySet() }
        Log.d(TAG, "Not yet implemented: setAchievementSteps($id, $numSteps, $windowToken, $extras)")
    }

    private fun getGamesIntent(action: String, block: Intent.() -> Unit = {}) = Intent(action).apply {
        setPackage(GAMES_PACKAGE_NAME)
        putExtra(EXTRA_GAME_PACKAGE_NAME, packageName)
        addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        block()
    }

    override fun getAllLeaderboardsIntent(): Intent = getGamesIntent(ACTION_VIEW_LEADERBOARDS)

    override fun getAchievementsIntent(): Intent = getGamesIntent(ACTION_VIEW_ACHIEVEMENTS)

    override fun getPlayerSearchIntent(): Intent = getGamesIntent(ACTION_PLAYER_SEARCH)

    override fun loadEvents(callbacks: IGamesCallbacks?, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadEvents($forceReload)")
    }

    override fun incrementEvent(eventId: String?, incrementAmount: Int) {
        Log.d(TAG, "Not yet implemented: incrementEvent($eventId, $incrementAmount)")
    }

    override fun loadEventsById(callbacks: IGamesCallbacks?, forceReload: Boolean, eventsIds: Array<out String>?) {
        Log.d(TAG, "Not yet implemented: loadEventsById($forceReload, $eventsIds)")
    }

    override fun getMaxDataSize(): Int {
        return 3 * 1024 * 1024
    }

    override fun getMaxCoverImageSize(): Int {
        return 800 * 1024
    }

    override fun registerEventClient(callback: IGamesClient?, l: Long) {
        Log.d(TAG, "Not yet implemented: registerEventClient($l)")
    }

    private fun getCompareProfileIntent(playerId: String, block: Intent.() -> Unit = {}): Intent = getGamesIntent(ACTION_VIEW_PROFILE) {
        putExtra(EXTRA_IS_SELF, playerId == currentPlayerId)
        putExtra(EXTRA_ACCOUNT, currentAccount)
        block()
    }

    override fun getCompareProfileIntentForPlayer(player: PlayerEntity): Intent = getCompareProfileIntent(player.playerId) {
        putExtra(EXTRA_PLAYER, player)
    }

    override fun loadPlayerStats(callbacks: IGamesCallbacks?, forceReload: Boolean) {
        Log.d(TAG, "Not yet implemented: loadPlayerStats($forceReload)")
    }

    override fun getCurrentAccount(): Account? {
        Log.d(TAG, "Not yet implemented: getCurrentAccount")
        return account
    }

    override fun isTelevision(): Boolean {
        Log.d(TAG, "Not yet implemented: isTelevision")
        return false
    }

    override fun getCompareProfileIntentWithAlternativeNameHints(
        otherPlayerId: String,
        otherPlayerInGameName: String?,
        currentPlayerInGameName: String?
    ): Intent = getCompareProfileIntent(otherPlayerId) {
        putExtra(EXTRA_PLAYER_ID, otherPlayerId)
        putExtra(EXTRA_OTHER_PLAYER_IN_GAME_NAME, otherPlayerInGameName)
        putExtra(EXTRA_SELF_IN_GAME_NAME, currentPlayerInGameName)
    }

    override fun requestServerSideAccess(callbacks: IGamesCallbacks, serverClientId: String, forceRefreshToken: Boolean) {
        lifecycleScope.launchWhenStarted {
            try {
                val serverAuthTokenResponse = withContext(Dispatchers.IO) {
                    val serverAuthTokenManager = AuthManager(context, account.name, packageName, "oauth2:server:client_id:${serverClientId}:api_scope:${Scopes.GAMES_LITE}")
                    serverAuthTokenManager.setOauth2Prompt(if (forceRefreshToken) "consent" else "auto")
                    serverAuthTokenManager.setItCaveatTypes("2")
                    serverAuthTokenManager.isPermitted = true
                    serverAuthTokenManager.invalidateAuthToken()
                    serverAuthTokenManager.requestAuth(true)
                }
                if (serverAuthTokenResponse.auth != null) {
                    callbacks.onServerAuthCode(Status(CommonStatusCodes.SUCCESS), serverAuthTokenResponse.auth)
                } else {
                    callbacks.onServerAuthCode(Status(CommonStatusCodes.SIGN_IN_REQUIRED), null)
                }
            } catch (e: Exception) {
                Log.w(TAG, e)
                runCatching { callbacks.onServerAuthCode(Status(CommonStatusCodes.INTERNAL_ERROR), null) }
            }
        }
    }

    override fun onTransact(code: Int, data: Parcel, reply: Parcel?, flags: Int): Boolean =
        warnOnTransactionIssues(code, reply, flags, TAG) { super.onTransact(code, data, reply, flags) }
}