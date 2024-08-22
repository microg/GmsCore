package com.google.android.gms.games.internal;

import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.gms.common.data.DataHolder;
//import com.google.android.gms.drive.Contents;
import com.google.android.gms.games.PlayerEntity;
import com.google.android.gms.games.internal.IGamesCallbacks;
import com.google.android.gms.games.internal.IGamesClient;

interface IGamesService {
    void clientDisconnecting(long clientId) = 5000;
    void signOut(IGamesCallbacks callbacks) = 5001;
    String getAppId() = 5002;
    Bundle getConnectionHint() = 5003;
    void showWelcomePopup(IBinder windowToken, in Bundle extras) = 5004;
    void cancelPopups() = 5005;
    String getCurrentAccountName() = 5006;
    void loadGameplayAclInternal(IGamesCallbacks callbacks, String gameId) = 5007;
    void updateGameplayAclInternal(IGamesCallbacks callbacks, String gameId, String aclData) = 5008;
    void loadFAclInternal(IGamesCallbacks callbacks, String gameId) = 5009;
    void updateFAclInternal(IGamesCallbacks callbacks, String gameId, boolean allCirclesVisible, in long[] circleIds) = 5010;
    String getCurrentPlayerId() = 5011;
    DataHolder getCurrentPlayer() = 5012;
    void loadPlayer(IGamesCallbacks callbacks, String playerId) = 5013;
    void loadInvitablePlayers(IGamesCallbacks callbacks, int pageSize, boolean expandCachedData, boolean forceReload) = 5014;
    void submitScore(IGamesCallbacks callbacks, String leaderboardId, long score) = 5015;
    void loadLeaderboards(IGamesCallbacks callbacks) = 5016;
    void loadLeaderboard(IGamesCallbacks callbacks, String leaderboardId) = 5017;
    void loadTopScores(IGamesCallbacks callbacks, String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) = 5018;
    void loadPlayerCenteredScores(IGamesCallbacks callbacks, String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) = 5019;
    void loadMoreScores(IGamesCallbacks callbacks, in Bundle previousheader, int maxResults, int pageDirection) = 5020;
    void loadAchievements(IGamesCallbacks callbacks) = 5021;
    void revealAchievement(IGamesCallbacks callbacks, String achievementId, IBinder windowToken, in Bundle extras) = 5022;
    void unlockAchievement(IGamesCallbacks callbacks, String achievementId, IBinder windowToken, in Bundle extras) = 5023;
    void incrementAchievement(IGamesCallbacks callbacks, String achievementId, int numSteps, IBinder windowToken, in Bundle extras) = 5024;
    void loadGame(IGamesCallbacks callbacks) = 5025;
    void loadInvitations(IGamesCallbacks callbacks) = 5026;
    void declineInvitation(String invitationId, int invitationType) = 5027;
    void dismissInvitation(String invitationId, int invitationType) = 5028;
    void createRoom(IGamesCallbacks callbacks, IBinder processBinder, int variant, in String[] invitedPlayerIds, in Bundle autoMatchCriteria, boolean enableSockets, long clientId) = 5029;
    void joinRoom(IGamesCallbacks callbacks, IBinder processBinder, String matchId, boolean enableSockets, long clientId) = 5030;
    void leaveRoom(IGamesCallbacks callbacks, String matchId) = 5031;
    int sendReliableMessage(IGamesCallbacks callbacks, in byte[] messageData, String matchId, String recipientParticipantId) = 5032;
    int sendUnreliableMessage(in byte[] messageData, String matchId, in String[] recipientParticipantIds) = 5033;
    String createSocketConnection(String participantId) = 5034;
    void clearNotifications(int notificationTypes) = 5035;
    void loadLeaderboardsFirstParty(IGamesCallbacks callbacks, String gameId) = 5036;
    void loadLeaderboardFirstParty(IGamesCallbacks callbacks, String gameId, String leaderboardId) = 5037;
    void loadTopScoresFirstParty(IGamesCallbacks callbacks, String gameId, String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) = 5038;
    void loadPlayerCenteredScoresFirstParty(IGamesCallbacks callbacks, String gameId, String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) = 5039;
    void loadAchievementsFirstParty(IGamesCallbacks callbacks, String playerId, String gameId) = 5040;
    void loadGameFirstParty(IGamesCallbacks callbacks, String gameId) = 5041;
    void loadGameInstancesFirstParty(IGamesCallbacks callbacks, String gameId) = 5042;
    void loadGameCollectionFirstParty(IGamesCallbacks callbacks, int pageSize, int collectionType, boolean expandCachedData, boolean forceReload) = 5043;
    void loadRecentlyPlayedGamesFirstParty(IGamesCallbacks callbacks, String externalPlayerId, int pageSize, boolean expandCachedData, boolean forceReload) = 5044;
    void loadInvitablePlayersFirstParty(IGamesCallbacks callbacks, int pageSize, boolean expandCachedData, boolean forceReload) = 5045;
    void loadRecentPlayersFirstParty(IGamesCallbacks callbacks) = 5046;
    void loadCircledPlayersFirstParty(IGamesCallbacks callbacks, int pageSize, boolean expandCachedData, boolean forceReload) = 5047;
    void loadSuggestedPlayersFirstParty(IGamesCallbacks callbacks) = 5048;
    void dismissPlayerSuggestionFirstParty(String playerIdToDismiss) = 5049;
    void declineInvitationFirstParty(String gameId, String invitationId, int invitationType) = 5050;
    void loadInvitationsFirstParty(IGamesCallbacks callbacks, String gameId) = 5051;
    int registerWaitingRoomListenerRestricted(IGamesCallbacks callbacks, String roomId) = 5052;
    void setGameMuteStatusInternal(IGamesCallbacks callbacks, String gameId, boolean muted) = 5053;
    void clearNotificationsFirstParty(String gameId, int notificationTypes) = 5054;
    void loadNotifyAclInternal(IGamesCallbacks callbacks) = 5055;
    void updateNotifyAclInternal(IGamesCallbacks callbacks, String aclData) = 5056;
    void registerInvitationListener(IGamesCallbacks callbacks, long clientId) = 5057;
    void unregisterInvitationListener(long clientId) = 5058;
    int unregisterWaitingRoomListenerRestricted(String roomId) = 5059;
    void isGameMutedInternal(IGamesCallbacks callbacks, String gameId) = 5060;
    void loadContactSettingsInternal(IGamesCallbacks callbacks) = 5061;
    void updateContactSettingsInternal(IGamesCallbacks callbacks, boolean enableMobileNotifications) = 5062;
    String getSelectedAccountForGameFirstParty(String gamePackageName) = 5063;
    void updateSelectedAccountForGameFirstParty(String gamePackageName, String accountName) = 5064;
    Uri getGamesContentUriRestricted(String gameId) = 5065;
    boolean shouldUseNewPlayerNotificationsFirstParty() = 5066;
    void setUseNewPlayerNotificationsFirstParty(boolean newPlayerStyle) = 5067;

    void searchForPlayersFirstParty(IGamesCallbacks callbacks, String query, int pageSize, boolean expandCachedData, boolean forceReload) = 5500;
    DataHolder getCurrentGame() = 5501;

    void loadAchievementsV2(IGamesCallbacks callbacks, boolean forceReload) = 6000;

    void submitLeaderboardScore(IGamesCallbacks callbacks, String leaderboardId, long score, @nullable String scoreTag) = 7001;
    void setAchievementSteps(IGamesCallbacks callbacks, String id, int numSteps, IBinder windowToken, in Bundle extras) = 7002;

    Intent getAllLeaderboardsIntent() = 9002;
    Intent getAchievementsIntent() = 9004;
    Intent getPlayerSearchIntent() = 9009;

//    void getSelectSnapshotIntent(String str, boolean z, boolean z2, int i) = 12001;
//    void loadSnapshotsResult(IGamesCallbacks callbacks, boolean forceReload) = 12002;
    void loadEvents(IGamesCallbacks callbacks, boolean forceReload) = 12015;
    void incrementEvent(String eventId, int incrementAmount) = 12016;
//    void discardAndCloseSnapshot(in Contents contents) = 12018;
    void loadEventsById(IGamesCallbacks callbacks, boolean forceReload, in String[] eventsIds) = 12030;
//    void resolveSnapshotConflict(IGamesCallbacks callbacks, String conflictId, String snapshotId, in SnapshotMetadataChangeEntity metadata, in Contents contents) = 12032;
    int getMaxDataSize() = 12034;
    int getMaxCoverImageSize() = 12035;

    void registerEventClient(IGamesClient callback, long l) = 15500;
    Intent getCompareProfileIntentForPlayer(in PlayerEntity player) = 15502;

    void loadPlayerStats(IGamesCallbacks callbacks, boolean forceReload) = 17000;

    Account getCurrentAccount() = 21000;

    boolean isTelevision() = 22029;

    Intent getCompareProfileIntentWithAlternativeNameHints(String otherPlayerId, String otherPlayerInGameName, String currentPlayerInGameName) = 25015;

    void requestServerSideAccess(IGamesCallbacks callbacks, String serverClientId, boolean forceRefreshToken) = 27002;

}
