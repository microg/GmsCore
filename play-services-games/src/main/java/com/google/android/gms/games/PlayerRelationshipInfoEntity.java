/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games;

import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.safeparcel.AutoSafeParcelable;

@Hide
public class PlayerRelationshipInfoEntity extends AutoSafeParcelable implements PlayerRelationshipInfo {
    @Field(1)
    @Player.PlayerFriendStatus
    private int friendStatus;
    @Field(2)
    private String nickname;
    @Field(3)
    private String invitationNickname;
    @Field(4)
    private String nicknameAbuseReportToken;

    public PlayerRelationshipInfoEntity() {
    }

    public PlayerRelationshipInfoEntity(PlayerRelationshipInfo copy) {
        friendStatus = copy.getFriendStatus();
    }

    public PlayerRelationshipInfoEntity(int friendStatus, String nickname, String invitationNickname, String nicknameAbuseReportToken) {
        this.friendStatus = friendStatus;
        this.nickname = nickname;
        this.invitationNickname = invitationNickname;
        this.nicknameAbuseReportToken = nicknameAbuseReportToken;
    }

    @Override
    @Player.PlayerFriendStatus
    public int getFriendStatus() {
        return friendStatus;
    }

    @Override
    public PlayerRelationshipInfo freeze() {
        return this;
    }

    @Override
    public boolean isDataValid() {
        return true;
    }

    public String getNickname() {
        return nickname;
    }

    public String getInvitationNickname() {
        return invitationNickname;
    }

    public String getNicknameAbuseReportToken() {
        return nicknameAbuseReportToken;
    }

    public static final SafeParcelableCreatorAndWriter<PlayerRelationshipInfoEntity> CREATOR = findCreator(PlayerRelationshipInfoEntity.class);
}
