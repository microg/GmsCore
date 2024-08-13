/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayerRef;

public class LeaderboardScoreRef extends DataBufferRef implements LeaderboardScore {
    private final PlayerRef playerRef;

    LeaderboardScoreRef(DataHolder dataHolder, int dataRow) {
        super(dataHolder, dataRow);
        this.playerRef = new PlayerRef(dataHolder, dataRow);
    }

    public final long getRank() {
        return this.getLong(LeaderboardScoreColumns.DB_FIELD_RANK);
    }

    public final String getDisplayRank() {
        return this.getString(LeaderboardScoreColumns.DB_FIELD_DISPLAY_RANK);
    }

    public final void getDisplayRank(CharArrayBuffer var1) {
        this.copyToBuffer(LeaderboardScoreColumns.DB_FIELD_DISPLAY_RANK, var1);
    }

    public final String getDisplayScore() {
        return this.getString(LeaderboardScoreColumns.DB_FIELD_DISPLAY_SCORE);
    }

    public final void getDisplayScore(CharArrayBuffer var1) {
        this.copyToBuffer(LeaderboardScoreColumns.DB_FIELD_DISPLAY_SCORE, var1);
    }

    public final long getRawScore() {
        return this.getLong(LeaderboardScoreColumns.DB_FIELD_RAW_SCORE);
    }

    public final long getTimestampMillis() {
        return this.getLong(LeaderboardScoreColumns.DB_FIELD_ACHIEVED_TIMESTAMP);
    }

    public final String getScoreHolderDisplayName() {
        return this.hasNull(LeaderboardScoreColumns.DB_FIELD_EXTERNAL_PLAYER_ID) ? this.getString(LeaderboardScoreColumns.DB_FIELD_DEFAULT_DISPLAY_NAME) : this.playerRef.getDisplayName();
    }

    public final void getScoreHolderDisplayName(CharArrayBuffer var1) {
        if (this.hasNull(LeaderboardScoreColumns.DB_FIELD_EXTERNAL_PLAYER_ID)) {
            this.copyToBuffer(LeaderboardScoreColumns.DB_FIELD_DEFAULT_DISPLAY_NAME, var1);
        } else {
            this.playerRef.getDisplayName(var1);
        }
    }

    public final Uri getScoreHolderIconImageUri() {
        return this.hasNull(LeaderboardScoreColumns.DB_FIELD_EXTERNAL_PLAYER_ID) ? this.parseUri(LeaderboardScoreColumns.DB_FIELD_DEFAULT_DISPLAY_IMAGE_URI) : this.playerRef.getIconImageUri();
    }

    public final Uri getScoreHolderHiResImageUri() {
        return this.hasNull(LeaderboardScoreColumns.DB_FIELD_EXTERNAL_PLAYER_ID) ? null : this.playerRef.getHiResImageUri();
    }

    public final Player getScoreHolder() {
        return this.hasNull(LeaderboardScoreColumns.DB_FIELD_EXTERNAL_PLAYER_ID) ? null : this.playerRef;
    }

    public final String getScoreTag() {
        return this.getString(LeaderboardScoreColumns.DB_FIELD_SCORE_TAG);
    }

    @Override
    public LeaderboardScore freeze() {
        return new LeaderboardScoreEntity(this);
    }
}