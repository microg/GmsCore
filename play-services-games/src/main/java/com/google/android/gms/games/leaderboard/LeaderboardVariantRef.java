/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;

public class LeaderboardVariantRef extends DataBufferRef implements LeaderboardVariant {

    LeaderboardVariantRef(DataHolder dataHolder, int dataRow) {
        super(dataHolder, dataRow);
    }

    public final int getTimeSpan() {
        return this.getInteger("timespan");
    }

    public final int getCollection() {
        return this.getInteger("collection");
    }

    public final boolean hasPlayerInfo() {
        return !this.hasNull("player_raw_score");
    }

    public final long getRawPlayerScore() {
        return this.hasNull("player_raw_score") ? -1L : this.getLong("player_raw_score");
    }

    public final String getDisplayPlayerScore() {
        return this.getString("player_display_score");
    }

    public final long getPlayerRank() {
        return this.hasNull("player_rank") ? -1L : this.getLong("player_rank");
    }

    public final String getDisplayPlayerRank() {
        return this.getString("player_display_rank");
    }

    public final String getPlayerScoreTag() {
        return this.getString("player_score_tag");
    }

    public final long getNumScores() {
        return this.hasNull("total_scores") ? -1L : this.getLong("total_scores");
    }

    @Override
    public LeaderboardVariant freeze() {
        return new LeaderboardVariantEntity(this);
    }
}