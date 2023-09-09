/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.games;

import com.google.android.gms.common.data.AbstractDataBuffer;
import com.google.android.gms.common.data.DataHolder;
import org.microg.gms.common.Hide;

/**
 * Data structure providing access to a list of players.
 */
public class PlayerBuffer extends AbstractDataBuffer<Player> {
    @Hide
    public PlayerBuffer(DataHolder dataHolder) {
        super(dataHolder);
    }

    public Player get(int position) {
        throw new UnsupportedOperationException();
        //return new PlayerRef(dataHolder, position);
    }
}
