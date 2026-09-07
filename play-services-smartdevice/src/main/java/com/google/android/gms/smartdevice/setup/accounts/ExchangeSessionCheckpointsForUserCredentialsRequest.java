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
public class ExchangeSessionCheckpointsForUserCredentialsRequest extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> F;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> f = new LinkedHashMap<>();
        f.put("sessionCheckpoints", new FastJsonResponse.Field<>(11, true, 11, true, "sessionCheckpoints", 2, SessionCheckpoint.class));
        f.put("targetDeviceRiskSignals", new FastJsonResponse.Field<>(11, false, 11, false, "targetDeviceRiskSignals", 3, DeviceRiskSignals.class));
        F = Collections.unmodifiableMap(f);
    }

    @SafeParcelable.Indicator
    public final Set<Integer> p;
    @SafeParcelable.Field(value = 2, subClass = SessionCheckpoint.class, mayNull = true)
    public List<SessionCheckpoint> sessionCheckpoints;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public DeviceRiskSignals targetDeviceRiskSignals;

    public ExchangeSessionCheckpointsForUserCredentialsRequest() {
        p = new HashSet<>();
    }

    public ExchangeSessionCheckpointsForUserCredentialsRequest(ArrayList<SessionCheckpoint> v) {
        this();
        sessionCheckpoints = v;
        targetDeviceRiskSignals = null;
        p.add(2);
        p.add(3);
    }

    @SafeParcelable.Constructor
    ExchangeSessionCheckpointsForUserCredentialsRequest(@SafeParcelable.Indicator Set<Integer> p, @SafeParcelable.Param(2) List<SessionCheckpoint> s, @SafeParcelable.Param(3) DeviceRiskSignals d) {
        this.p = p;
        sessionCheckpoints = s;
        targetDeviceRiskSignals = d;
    }

    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return F;
    }

    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> f) {
        return p.contains(f.safeParcelFieldId);
    }

    protected Object getFieldValue(FastJsonResponse.Field<?, ?> f) {
        if (f.safeParcelFieldId == 2) return sessionCheckpoints;
        if (f.safeParcelFieldId == 3) return targetDeviceRiskSignals;
        throw new IllegalStateException();
    }

    @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> f, List<? extends FastJsonResponse> v) {
        if (f.safeParcelFieldId != 2) throw new IllegalArgumentException();
        sessionCheckpoints = (List<SessionCheckpoint>) v;
        p.add(2);
    }

    public void writeToParcel(@NonNull Parcel d, int flags) {
        CREATOR.writeToParcel(this, d, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExchangeSessionCheckpointsForUserCredentialsRequest> CREATOR = findCreator(ExchangeSessionCheckpointsForUserCredentialsRequest.class);
}
