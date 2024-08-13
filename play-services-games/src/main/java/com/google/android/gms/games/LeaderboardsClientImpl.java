/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApi;
import com.google.android.gms.games.leaderboard.Leaderboard;
import com.google.android.gms.games.leaderboard.LeaderboardBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.tasks.Task;

public class LeaderboardsClientImpl extends GoogleApi<Games.GamesOptions> implements LeaderboardsClient {

    public LeaderboardsClientImpl(Context context, Games.GamesOptions options) {
        super(context, Games.API, options);
        Log.d(Games.TAG, "LeaderboardsClientImpl: options: " + options);
    }

    @Override
    public Task<Intent> getAllLeaderboardsIntent() {
        return null;
    }

    @Override
    public Task<Intent> getLeaderboardIntent(String leaderboardId, int timeSpan) {
        return null;
    }

    @Override
    public Task<Intent> getLeaderboardIntent(String leaderboardId) {
        return null;
    }

    @Override
    public Task<Intent> getLeaderboardIntent(String leaderboardId, int timeSpan, int collection) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScore>> loadCurrentPlayerLeaderboardScore(String leaderboardId, int span, int leaderboardCollection) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardBuffer>> loadLeaderboardMetadata(boolean forceReload) {
        return null;
    }

    @Override
    public Task<AnnotatedData<Leaderboard>> loadLeaderboardMetadata(String leaderboardId, boolean forceReload) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScores>> loadMoreScores(LeaderboardScoreBuffer buffer, int maxResults, int pageDirection) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScores>> loadPlayerCenteredScores(String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScores>> loadPlayerCenteredScores(String leaderboardId, int span, int leaderboardCollection, int maxResults) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScores>> loadTopScores(String leaderboardId, int span, int leaderboardCollection, int maxResults) {
        return null;
    }

    @Override
    public Task<AnnotatedData<LeaderboardScores>> loadTopScores(String leaderboardId, int span, int leaderboardCollection, int maxResults, boolean forceReload) {
        return null;
    }

    @Override
    public void submitScore(String leaderboardId, long score, String scoreTag) {

    }

    @Override
    public void submitScore(String leaderboardId, long score) {

    }
}
