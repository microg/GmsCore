/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

public class LeaderboardVariantEntity implements LeaderboardVariant {
    private final int timeSpan;
    private final int collection;
    private final boolean hasPlayerInfo;
    private final long rawPlayerScore;
    private final String displayPlayerScore;
    private final long playerRank;
    private final String displayPlayerRank;
    private final String playerScoreTag;
    private final long numScores;

    public LeaderboardVariantEntity(LeaderboardVariant variant) {
        this.timeSpan = variant.getTimeSpan();
        this.collection = variant.getCollection();
        this.hasPlayerInfo = variant.hasPlayerInfo();
        this.rawPlayerScore = variant.getRawPlayerScore();
        this.displayPlayerScore = variant.getDisplayPlayerScore();
        this.playerRank = variant.getPlayerRank();
        this.displayPlayerRank = variant.getDisplayPlayerRank();
        this.playerScoreTag = variant.getPlayerScoreTag();
        this.numScores = variant.getNumScores();
    }

    public int getTimeSpan() {
        return this.timeSpan;
    }

    public int getCollection() {
        return this.collection;
    }

    public boolean hasPlayerInfo() {
        return this.hasPlayerInfo;
    }

    public long getRawPlayerScore() {
        return this.rawPlayerScore;
    }

    public String getDisplayPlayerScore() {
        return this.displayPlayerScore;
    }

    public long getPlayerRank() {
        return this.playerRank;
    }

    public String getDisplayPlayerRank() {
        return this.displayPlayerRank;
    }

    public String getPlayerScoreTag() {
        return this.playerScoreTag;
    }

    public long getNumScores() {
        return this.numScores;
    }

    @Override
    public LeaderboardVariant freeze() {
        return this;
    }

    public boolean isDataValid() {
        return true;
    }

}
