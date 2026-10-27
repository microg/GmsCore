/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */
package com.google.android.gms.nearby.sharing;

import android.net.Uri;
import android.os.Parcel;
import androidx.annotation.NonNull;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class Attachment extends AbstractSafeParcelable {
    @Field(1)
    public final long id;
    @Field(2)
    public final int type;
    @Field(3)
    public final String name;
    @Field(4)
    public final long size;
    @Field(5)
    public final String mimeType;
    @Field(6)
    public final Uri uri;
    @Field(7)
    public final String textBody;

    @Constructor
    public Attachment(@Param(1) long id, @Param(2) int type, @Param(3) String name, @Param(4) long size, @Param(5) String mimeType, @Param(6) Uri uri, @Param(7) String textBody) {
        this.id = id;
        this.type = type;
        this.name = name;
        this.size = size;
        this.mimeType = mimeType;
        this.uri = uri;
        this.textBody = textBody;
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Attachment> CREATOR = findCreator(Attachment.class);
}
