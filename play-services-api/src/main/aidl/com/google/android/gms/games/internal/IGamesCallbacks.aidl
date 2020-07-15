package com.google.android.gms.games.internal;

import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;

interface IGamesCallbacks {
  void onAuthTokenLoaded(int statusCode, String authToken) = 5000;
  void onAchievementsLoaded(in DataHolder data) = 5001;
  void onAchievementUpdated(int statusCode, String achievementId) = 5002;
  void onLeaderboardsLoaded(in DataHolder data) = 5003;
  void onLeaderboardScoresLoaded(in DataHolder leaderboard, in DataHolder scores) = 5004;
  void onScoreSubmitted(in DataHolder data) = 5005;
  void onPlayersLoaded(in DataHolder data) = 5006;
  void onExtendedPlayersLoaded(in DataHolder data) = 5007;
  void onGamesLoaded(in DataHolder data) = 5008;
  void onExtendedGamesLoaded(in DataHolder data) = 5009;
  void onGameInstancesLoaded(in DataHolder data) = 5010;
  void onGameplayAclLoaded(in DataHolder data) = 5011;
  void onGameplayAclUpdated(int statusCode) = 5012;
  void onFAclLoaded(in DataHolder data) = 5013;
  void onFAclUpdated(int statusCode) = 5014;
  void onSignOutComplete() = 5015;
  void onInvitationsLoaded(in DataHolder data) = 5016;
  void onRoomCreated(in DataHolder data) = 5017;
  void onJoinedRoom(in DataHolder data) = 5018;
  void onLeftRoom(int statusCode, String roomId) = 5019;
  void onRoomConnecting(in DataHolder data) = 5020;
  void onRoomAutoMatching(in DataHolder data) = 5021;
  void onRoomConnected(in DataHolder data) = 5022;
  void onConnectedToRoom(in DataHolder data) = 5023;
  void onDisconnectedFromRoom(in DataHolder data) = 5024;
  void onPeerInvitedToRoom(in DataHolder data, in String[] participantIds) = 5025;
  void onPeerJoinedRoom(in DataHolder data, in String[] participantIds) = 5026;
  void onPeerLeftRoom(in DataHolder data, in String[] participantIds) = 5027;
  void onPeerDeclined(in DataHolder data, in String[] participantIds) = 5028;
  void onPeerConnected(in DataHolder data, in String[] participantIds) = 5029;
  void onPeerDisconnected(in DataHolder data, in String[] participantIds) = 5030;
  void onRealTimeMessageReceived(in RealTimeMessage message) = 5031;
  void onMessageSent(int statusCode, int messageId, String recipientParticipantId) = 5032;
  void onGameMuteStatusChanged(int statusCode, String externalGameId, boolean isMuted) = 5033;
  void onNotifyAclLoaded(in DataHolder data) = 5034;
  void onNotifyAclUpdated(int statusCode) = 5035;
  void onInvitationReceived(in DataHolder data) = 5036;
  void onGameMuteStatusLoaded(in DataHolder data) = 5037;
  void onContactSettingsLoaded(in DataHolder data) = 5038;
  void onContactSettingsUpdated(int statusCode) = 5039;
}
