/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.internal.Preconditions;
import com.google.android.gms.common.util.DataUtils;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerEntity;

public final class LeaderboardScoreEntity implements LeaderboardScore {
    private final long rank;
    private final String displayRank;
    private final String displayScore;
    private final long rawScore;
    private final long timestampMillis;
    private final String scoreHolderDisplayName;
    private final Uri scoreHolderIconImageUri;
    private final Uri scoreHolderHiResImageUri;
    private final PlayerEntity playerEntity;
    private final String scoreTag;

    public LeaderboardScoreEntity(LeaderboardScore leaderboardScore) {
        this.rank = leaderboardScore.getRank();
        this.displayRank = (String) Preconditions.checkNotNull(leaderboardScore.getDisplayRank());
        this.displayScore = (String) Preconditions.checkNotNull(leaderboardScore.getDisplayScore());
        this.rawScore = leaderboardScore.getRawScore();
        this.timestampMillis = leaderboardScore.getTimestampMillis();
        this.scoreHolderDisplayName = leaderboardScore.getScoreHolderDisplayName();
        this.scoreHolderIconImageUri = leaderboardScore.getScoreHolderIconImageUri();
        this.scoreHolderHiResImageUri = leaderboardScore.getScoreHolderHiResImageUri();
        Player player = leaderboardScore.getScoreHolder();
        this.playerEntity = player == null ? null : (PlayerEntity) player.freeze();
        this.scoreTag = leaderboardScore.getScoreTag();
    }

    public long getRank() {
        return this.rank;
    }

    public String getDisplayRank() {
        return this.displayRank;
    }

    public void getDisplayRank(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.displayRank, var1);
    }

    public String getDisplayScore() {
        return this.displayScore;
    }

    public void getDisplayScore(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.displayScore, var1);
    }

    public long getRawScore() {
        return this.rawScore;
    }

    public long getTimestampMillis() {
        return this.timestampMillis;
    }

    public String getScoreHolderDisplayName() {
        return this.playerEntity == null ? this.scoreHolderDisplayName : this.playerEntity.getDisplayName();
    }

    public void getScoreHolderDisplayName(CharArrayBuffer var1) {
        if (this.playerEntity == null) {
            DataUtils.copyStringToBuffer(this.scoreHolderDisplayName, var1);
        } else {
            this.playerEntity.getDisplayName(var1);
        }
    }

    public Uri getScoreHolderIconImageUri() {
        return this.playerEntity == null ? this.scoreHolderIconImageUri : this.playerEntity.getIconImageUri();
    }

    public Uri getScoreHolderHiResImageUri() {
        return this.playerEntity == null ? this.scoreHolderHiResImageUri : this.playerEntity.getHiResImageUri();
    }

    public Player getScoreHolder() {
        return this.playerEntity;
    }

    public String getScoreTag() {
        return this.scoreTag;
    }

    public boolean isDataValid() {
        return true;
    }

    @Override
    public LeaderboardScore freeze() {
        return this;
    }

}
