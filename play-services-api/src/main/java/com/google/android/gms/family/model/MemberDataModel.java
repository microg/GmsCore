/**
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.family.model;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class MemberDataModel extends AbstractSafeParcelable {
    @Field(1)
    public String memberId = "";
    @Field(2)
    public String email = "";
    @Field(3)
    public String displayName = "";
    @Field(4)
    public String hohGivenName = "";
    @Field(5)
    public String profilePhotoUrl = "";
    @Field(6)
    public String roleName = "";
    @Field(7)
    public int role = 0;
    @Field(8)
    public boolean isActive = false;
    @Field(9)
    public int supervisionType = 0;
    @Field(10)
    public long timestamp = 0;
    @Field(11)
    public boolean isInviteEntry = false;
    @Field(12)
    public int inviteSlots = 0;
    @Field(13)
    public boolean isInvited = false;
    @Field(14)
    public String invitationId = "";
    @Field(15)
    public long inviteState = 0;
    @Field(16)
    public String inviteSentDate = "";

    public static final SafeParcelableCreatorAndWriter<MemberDataModel> CREATOR = findCreator(MemberDataModel.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    @Override
    public String toString() {
        return "MemberDataModel{" +
                "memberId='" + memberId + '\'' +
                ", email='" + email + '\'' +
                ", displayName='" + displayName + '\'' +
                ", hohGivenName='" + hohGivenName + '\'' +
                ", profilePhotoUrl='" + profilePhotoUrl + '\'' +
                ", roleName='" + roleName + '\'' +
                ", role=" + role +
                ", isActive=" + isActive +
                ", supervisionType=" + supervisionType +
                ", timestamp=" + timestamp +
                ", isInviteEntry=" + isInviteEntry +
                ", inviteSlots=" + inviteSlots +
                ", isInvited=" + isInvited +
                ", invitationId='" + invitationId + '\'' +
                ", inviteState=" + inviteState +
                ", inviteSentDate='" + inviteSentDate + '\'' +
                '}';
    }
}
