/*
 * SPDX-FileCopyrightText: 2021, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay.issuer;

import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.utils.ToStringHelper;

@SafeParcelable.Class
public class TokenStatus extends AbstractSafeParcelable {
    @Field(2)
    @NonNull
    public final String issuerTokenId;
    @Field(value = 3, getterName = "getTokenState")
    private final int tokenState;
    @Field(value = 4, getterName = "isSelected")
    private final boolean selected;

    @Constructor
    public TokenStatus(@NonNull @Param(2) String issuerTokenId, @Param(3) int tokenState, @Param(4) boolean selected) {
        this.issuerTokenId = issuerTokenId;
        this.tokenState = tokenState;
        this.selected = selected;
    }

    public int getTokenState() {
        return tokenState;
    }

    public boolean isSelected() {
        return selected;
    }

    @NonNull
    @Override
    public String toString() {
        return ToStringHelper.name("TokenStatus").value(issuerTokenId)
                .field("tokenState", tokenState)
                .field("selected", selected)
                .end();
    }

    public static final SafeParcelableCreatorAndWriter<TokenStatus> CREATOR = findCreator(TokenStatus.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
