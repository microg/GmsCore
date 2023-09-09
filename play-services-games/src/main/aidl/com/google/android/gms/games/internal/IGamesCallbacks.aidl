package com.google.android.gms.games.internal;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;

interface IGamesCallbacks {
  /* @deprecated */ void onAuthTokenLoaded(int statusCode, String authToken) = 5000;
  void onAchievementsLoaded(in DataHolder data) = 5001;
  void onAchievementUpdated(int statusCode, String achievementId) = 5002;
  void onLeaderboardsLoaded(in DataHolder data) = 5003;
  void onLeaderboardScoresLoaded(in DataHolder leaderboard, in DataHolder scores) = 5004;
  void onScoreSubmitted(in DataHolder data) = 5005;
  void onPlayersLoaded(in DataHolder data) = 5006;
  void onExtendedPlayersLoaded(in DataHolder data) = 5007;
  /* @deprecated */ void onGamesLoaded(in DataHolder data) = 5008;
  /* @deprecated */ void onExtendedGamesLoaded(in DataHolder data) = 5009;
  /* @deprecated */ void onGameInstancesLoaded(in DataHolder data) = 5010;
  /* @deprecated */ void onGameplayAclLoaded(in DataHolder data) = 5011;
  /* @deprecated */ void onGameplayAclUpdated(int statusCode) = 5012;
  /* @deprecated */ void onFAclLoaded(in DataHolder data) = 5013;
  /* @deprecated */ void onFAclUpdated(int statusCode) = 5014;
  void onSignOutComplete() = 5015;
  /* @deprecated */ void onInvitationsLoaded(in DataHolder data) = 5016;
  /* @deprecated */ void onRoomCreated(in DataHolder data) = 5017;
  /* @deprecated */ void onJoinedRoom(in DataHolder data) = 5018;
  /* @deprecated */ void onLeftRoom(int statusCode, String roomId) = 5019;
  /* @deprecated */ void onRoomConnecting(in DataHolder data) = 5020;
  /* @deprecated */ void onRoomAutoMatching(in DataHolder data) = 5021;
  /* @deprecated */ void onRoomConnected(in DataHolder data) = 5022;
  /* @deprecated */ void onConnectedToRoom(in DataHolder data) = 5023;
  /* @deprecated */ void onDisconnectedFromRoom(in DataHolder data) = 5024;
  /* @deprecated */ void onPeerInvitedToRoom(in DataHolder data, in String[] participantIds) = 5025;
  /* @deprecated */ void onPeerJoinedRoom(in DataHolder data, in String[] participantIds) = 5026;
  /* @deprecated */ void onPeerLeftRoom(in DataHolder data, in String[] participantIds) = 5027;
  /* @deprecated */ void onPeerDeclined(in DataHolder data, in String[] participantIds) = 5028;
  /* @deprecated */ void onPeerConnected(in DataHolder data, in String[] participantIds) = 5029;
  /* @deprecated */ void onPeerDisconnected(in DataHolder data, in String[] participantIds) = 5030;
  /* @deprecated */ void onRealTimeMessageReceived(in RealTimeMessage message) = 5031;
  /* @deprecated */ void onMessageSent(int statusCode, int messageId, String recipientParticipantId) = 5032;
  /* @deprecated */ void onGameMuteStatusChanged(int statusCode, String externalGameId, boolean isMuted) = 5033;
  /* @deprecated */ void onNotifyAclLoaded(in DataHolder data) = 5034;
  /* @deprecated */ void onNotifyAclUpdated(int statusCode) = 5035;
  /* @deprecated */ void onInvitationReceived(in DataHolder data) = 5036;
  /* @deprecated */ void onGameMuteStatusLoaded(in DataHolder data) = 5037;
  /* @deprecated */ void onContactSettingsLoaded(in DataHolder data) = 5038;
  /* @deprecated */ void onContactSettingsUpdated(int statusCode) = 5039;
  void onServerAuthCode(in Status status, String serverAuthCode) = 25002;
}
