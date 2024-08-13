/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import android.database.CharArrayBuffer;
import android.net.Uri;
import android.os.Parcelable;

import com.google.android.gms.common.data.Freezable;

public interface Game extends Parcelable, Freezable<Game> {
    String getApplicationId();

    String getDisplayName();

    void getDisplayName(CharArrayBuffer buffer);

    String getPrimaryCategory();

    String getSecondaryCategory();

    String getDescription();

    void getDescription(CharArrayBuffer buffer);

    String getDeveloperName();

    void getDeveloperName(CharArrayBuffer buffer);

    Uri getIconImageUri();

    /** @deprecated */
    @Deprecated
    String getIconImageUrl();

    Uri getHiResImageUri();

    /** @deprecated */
    @Deprecated
    String getHiResImageUrl();

    Uri getFeaturedImageUri();

    /** @deprecated */
    @Deprecated
    String getFeaturedImageUrl();

    boolean isPlayEnabledGame();

    boolean isMuted();

    boolean isIdentitySharingConfirmed();

    boolean isInstanceInstalled();

    String getInstancePackageName();

    int getGameplayAclStatus();

    int getAchievementTotalCount();

    int getLeaderboardCount();

    boolean isRealTimeMultiplayerEnabled();

    boolean isTurnBasedMultiplayerEnabled();

    boolean areSnapshotsEnabled();

    String getThemeColor();

    boolean hasGamepadSupport();
}
