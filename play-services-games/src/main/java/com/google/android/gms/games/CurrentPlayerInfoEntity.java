/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class CurrentPlayerInfoEntity extends AutoSafeParcelable implements CurrentPlayerInfo {
    @Field(1)
    @Player.FriendsListVisibilityStatus
    private int friendsListVisibilityStatus;

    public CurrentPlayerInfoEntity() {
    }

    public CurrentPlayerInfoEntity(CurrentPlayerInfo copy) {
        friendsListVisibilityStatus = copy.getFriendsListVisibilityStatus();
    }

    public CurrentPlayerInfoEntity(int friendsListVisibilityStatus) {
        this.friendsListVisibilityStatus = friendsListVisibilityStatus;
    }

    @Override
    @Player.FriendsListVisibilityStatus
    public int getFriendsListVisibilityStatus() {
        return friendsListVisibilityStatus;
    }

    @Override
    public CurrentPlayerInfo freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    public static final SafeParcelableCreatorAndWriter<CurrentPlayerInfoEntity> CREATOR = findCreator(CurrentPlayerInfoEntity.class);
}
