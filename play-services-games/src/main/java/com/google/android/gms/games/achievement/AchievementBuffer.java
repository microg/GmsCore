/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.achievement;

import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataHolder;

import org.microg.gms.common.Hide;

/**
 * Data structure providing access to a list of achievements.
 */
public class AchievementBuffer extends AbstractDataBuffer<Achievement> {

    @Hide
    public AchievementBuffer(DataHolder dataHolder) {
        super(dataHolder);
    }

    @Override
    public Achievement get(int position) {
        return new AchievementRef(dataHolder, position);
    }
}
