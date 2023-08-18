/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.internal.safeparcel;

import android.os.Parcel;
import android.os.Parcelable;

public interface SafeParcelableCreatorAndWriter<T extends SafeParcelable> extends Parcelable.Creator<T> {
    void writeToParcel(T object, Parcel parcel, int flags);
}
