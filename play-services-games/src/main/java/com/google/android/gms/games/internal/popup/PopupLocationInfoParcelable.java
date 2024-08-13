/**
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.games.internal.popup;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

import android.os.Bundle;
import android.os.IBinder;

@SafeParcelable.Class
public class PopupLocationInfoParcelable extends AbstractSafeParcelable {

    @Field(1)
    public final Bundle popupLocationInfoBundle;

    @Field(2)
    public final IBinder gamesClientBinder;

    @Constructor
    public PopupLocationInfoParcelable(@Param(1) Bundle popupLocationInfoBundle, @Param(2) IBinder gamesClientBinder) {
        this.popupLocationInfoBundle = popupLocationInfoBundle;
        this.gamesClientBinder = gamesClientBinder;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<PopupLocationInfoParcelable> CREATOR = findCreator(PopupLocationInfoParcelable.class);
}
