/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.common.server.response;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;

import java.util.Objects;

public abstract class FastSafeParcelableJsonResponse extends FastJsonResponse implements SafeParcelable {
    @Override
    public final int describeContents() {
        return 0;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null) return false;
        if (this == other) return true;
        if (!getClass().isInstance(other)) return false;
        FastJsonResponse response = (FastJsonResponse) other;
        for (FastJsonResponse.Field<?, ?> field : getFieldMappings().values()) {
            if (isFieldSet(field) != response.isFieldSet(field)) return false;
            if (isFieldSet(field) && !Objects.equals(getFieldValue(field), response.getFieldValue(field))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 0;
        for (FastJsonResponse.Field<?, ?> field : getFieldMappings().values()) {
            if (isFieldSet(field)) result = 31 * result + Objects.hashCode(getFieldValue(field));
        }
        return result;
    }
}
