/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import android.database.CharArrayBuffer;
import android.net.Uri;

import com.google.android.gms.common.data.DataBufferRef;
import com.google.android.gms.common.data.DataHolder;

import java.util.ArrayList;

public final class LeaderboardRef extends DataBufferRef implements Leaderboard {

    private final int variantSize;

    LeaderboardRef(DataHolder dataHolder, int dataRow, int variantSize) {
        super(dataHolder, dataRow);
        this.variantSize = variantSize;
    }

    public String getLeaderboardId() {
        return this.getString(LeaderboardColumns.DB_FIELD_EXTERNAL_LEADERBOARD_ID);
    }

    public String getDisplayName() {
        return this.getString(LeaderboardColumns.DB_FIELD_NAME);
    }

    public void getDisplayName(CharArrayBuffer var1) {
        this.copyToBuffer(LeaderboardColumns.DB_FIELD_NAME, var1);
    }

    public Uri getIconImageUri() {
        return this.parseUri(LeaderboardColumns.DB_FIELD_BOARD_ICON_IMAGE_URI);
    }

    public int getScoreOrder() {
        return this.getInteger(LeaderboardColumns.DB_FIELD_SCORE_ORDER);
    }

    public ArrayList<LeaderboardVariant> getVariants() {
        ArrayList<LeaderboardVariant> leaderboardVariants = new ArrayList<>(this.variantSize);

        for (int i = 0; i < this.variantSize; ++i) {
            leaderboardVariants.add(new LeaderboardVariantRef(this.dataHolder, this.dataRow + i));
        }

        return leaderboardVariants;
    }

    @Override
    public Leaderboard freeze() {
        return new LeaderboardEntity(this);
    }
}
