package com.google.android.gms.games.client;

import com.google.android.gms.games.client.IPlayGamesCallbacks;
import com.google.android.gms.games.client.PlayGamesConsistencyTokens;

interface IPlayGamesService {
    void getGameCollection(IPlayGamesCallbacks callbacks, int maxResults, int gameCollectionType, boolean z, boolean forceReload) = 1000;
    void loadGames(IPlayGamesCallbacks callbacks, String playerId, int maxResults, boolean z, boolean forceReload) = 1002;

    PlayGamesConsistencyTokens getConsistencyTokens() = 5027;
    void updateConsistencyTokens(in PlayGamesConsistencyTokens tokens) = 5028;

    void fun5041(IPlayGamesCallbacks callbacks) = 5040;
}