/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.leaderboard;

import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataHolder;

public class LeaderboardScoreBuffer extends AbstractDataBuffer<LeaderboardScore> {

    public LeaderboardScoreBuffer(DataHolder dataHolder) {
        super(dataHolder);
    }

    @Override
    public LeaderboardScore get(int position) {
        return new LeaderboardScoreRef(dataHolder, position);
    }
}
