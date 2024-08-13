/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.util.DataUtils;

import java.util.ArrayList;

public final class LeaderboardEntity implements Leaderboard {
    private final String leaderboardId;
    private final String displayName;
    private final Uri icomImageUri;
    private final int scoreOrder;
    private final ArrayList<LeaderboardVariantEntity> leaderboardVariantEntities;

    public LeaderboardEntity(Leaderboard leaderboard) {
        this.leaderboardId = leaderboard.getLeaderboardId();
        this.displayName = leaderboard.getDisplayName();
        this.icomImageUri = leaderboard.getIconImageUri();
        this.scoreOrder = leaderboard.getScoreOrder();
        ArrayList<LeaderboardVariant> variants;
        int size = (variants = leaderboard.getVariants()).size();
        this.leaderboardVariantEntities = new ArrayList<>(size);
        for (int i = 0; i < size; ++i) {
            this.leaderboardVariantEntities.add((LeaderboardVariantEntity) ((LeaderboardVariant) variants.get(i)).freeze());
        }
    }

    public String getLeaderboardId() {
        return this.leaderboardId;
    }

    public String getDisplayName() {
        return this.displayName;
    }

    public void getDisplayName(CharArrayBuffer var1) {
        DataUtils.copyStringToBuffer(this.displayName, var1);
    }

    public Uri getIconImageUri() {
        return this.icomImageUri;
    }

    public int getScoreOrder() {
        return this.scoreOrder;
    }

    public ArrayList<LeaderboardVariant> getVariants() {
        return new ArrayList<>(this.leaderboardVariantEntities);
    }

    public boolean isDataValid() {
        return true;
    }

    @Override
    public Leaderboard freeze() {
        return this;
    }
}
