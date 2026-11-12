/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.smartdevice.setup.accounts;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.*;

@SafeParcelable.Class
@Hide
public class SessionCheckpoint extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> F;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> f = new LinkedHashMap<>();
        f.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false, "accountIdentifier", 2, null));
        f.put("sessionCheckpoint", new FastJsonResponse.Field<>(7, false, 7, false, "sessionCheckpoint", 3, null));
        F = Collections.unmodifiableMap(f);
    }

    @SafeParcelable.Indicator
    final Set<Integer> p;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String accountIdentifier;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public String sessionCheckpoint;

    public SessionCheckpoint() {
        p = new HashSet<>();
    }

    public SessionCheckpoint(String a, String s) {
        this();
        accountIdentifier = a;
        sessionCheckpoint = s;
        p.add(2);
        p.add(3);
    }

    @SafeParcelable.Constructor
    SessionCheckpoint(@SafeParcelable.Indicator Set<Integer> p, @SafeParcelable.Param(2) String a, @SafeParcelable.Param(3) String s) {
        this.p = p;
        accountIdentifier = a;
        sessionCheckpoint = s;
    }

    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return F;
    }

    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> f) {
        return p.contains(f.safeParcelFieldId);
    }

    protected Object getFieldValue(FastJsonResponse.Field<?, ?> f) {
        if (f.safeParcelFieldId == 2) return accountIdentifier;
        if (f.safeParcelFieldId == 3) return sessionCheckpoint;
        throw new IllegalStateException();
    }

    protected void setString(FastJsonResponse.Field<?, ?> f, String v) {
        if (v == null) return;
        if (f.safeParcelFieldId == 2) accountIdentifier = v;
        else if (f.safeParcelFieldId == 3) sessionCheckpoint = v;
        else throw new IllegalArgumentException();
        p.add(f.safeParcelFieldId);
    }

    public void writeToParcel(@NonNull Parcel d, int flags) {
        CREATOR.writeToParcel(this, d, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SessionCheckpoint> CREATOR = findCreator(SessionCheckpoint.class);
}
