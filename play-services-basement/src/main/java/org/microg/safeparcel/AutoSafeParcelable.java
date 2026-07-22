/*
 * SPDX-FileCopyrightText: 2015, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package org.microg.safeparcel;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

public abstract class AutoSafeParcelable extends AbstractSafeParcelable {
    private static final String TAG = "SafeParcel";

    @SuppressWarnings("unchecked")
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        Creator<Parcelable> creator = SafeParcelReflectionUtil.getCreator(this.getClass());
        if (creator instanceof SafeParcelableCreatorAndWriter) {
            ((SafeParcelableCreatorAndWriter<AutoSafeParcelable>) (SafeParcelableCreatorAndWriter<?>) creator).writeToParcel(this, dest, flags);
        } else {
            Log.w(TAG, "AutoSafeParcelable is not using SafeParcelableCreatorAndWriter");
            SafeParcelReflectionUtil.writeObject(this, dest, flags);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends AbstractSafeParcelable> SafeParcelableCreatorAndWriter<T> findCreator(java.lang.Class<T> tClass) {
        try {
            return AbstractSafeParcelable.findCreator(tClass);
        } catch (Exception e) {
            if (AutoSafeParcelable.class.isAssignableFrom(tClass)) {
                return (SafeParcelableCreatorAndWriter<T>) new AutoCreator<>((java.lang.Class<AutoSafeParcelable>) tClass);
            } else {
                throw new RuntimeException("AutoSafeParcelable.findCreator() invoked with non-AutoSafeParcelable");
            }
        }
    }

    @Deprecated
    public static class AutoCreator<T extends AutoSafeParcelable> extends ReflectedSafeParcelableCreatorAndWriter<T> {
        public AutoCreator(java.lang.Class<T> tClass) {
            super(tClass);
        }
    }
}