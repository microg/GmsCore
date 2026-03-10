/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.player;

import android.net.Uri;
import android.os.Parcelable;
import com.google.android.gms.common.data.Freezable;

public interface MostRecentGameInfo extends Freezable<MostRecentGameInfo>, Parcelable {
    long getActivityTimestampMillis();

    Uri getGameFeaturedImageUri();

    Uri getGameHiResImageUri();

    Uri getGameIconImageUri();

    String getGameId();

    String getGameName();
}
