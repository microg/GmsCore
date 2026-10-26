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
public class ExchangeAssertionsForUserCredentialsRequest extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> F;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> f = new LinkedHashMap<>();
        f.put("assertions", new FastJsonResponse.Field<>(11, true, 11, true, "assertions", 2, Assertion.class));
        f.put("targetDeviceSignals", new FastJsonResponse.Field<>(11, false, 11, false, "targetDeviceSignals", 3, DeviceSignals.class));
        f.put("credentialType", new FastJsonResponse.Field<>(0, false, 0, false, "credentialType", 4, null));
        f.put("clientId", new FastJsonResponse.Field<>(7, false, 7, false, "clientId", 5, null));
        f.put("locale", new FastJsonResponse.Field<>(7, false, 7, false, "locale", 6, null));
        f.put("sourceBackupAccountId", new FastJsonResponse.Field<>(7, false, 7, false, "sourceBackupAccountId", 7, null));
        f.put("sourceAndroidDeviceId", new FastJsonResponse.Field<>(7, false, 7, false, "sourceAndroidDeviceId", 8, null));
        f.put("deferCredentialsAfterFallback", new FastJsonResponse.Field<>(6, false, 6, false, "deferCredentialsAfterFallback", 9, null));
        f.put("platformVariant", new FastJsonResponse.Field<>(0, false, 0, false, "platformVariant", 10, null));
        F = Collections.unmodifiableMap(f);
    }

    @SafeParcelable.Indicator
    public final Set<Integer> p;
    @SafeParcelable.Field(value = 2, subClass = Assertion.class, mayNull = true)
    public List<Assertion> assertions;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public DeviceSignals targetDeviceSignals;
    @SafeParcelable.Field(4)
    public int credentialType;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public String clientId;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public String locale;
    @SafeParcelable.Field(value = 7, mayNull = true)
    public String sourceBackupAccountId;
    @SafeParcelable.Field(value = 8, mayNull = true)
    public String sourceAndroidDeviceId;
    @SafeParcelable.Field(9)
    public boolean deferCredentialsAfterFallback;
    @SafeParcelable.Field(10)
    public int platformVariant;

    public ExchangeAssertionsForUserCredentialsRequest() {
        p = new HashSet<>();
    }

    public ExchangeAssertionsForUserCredentialsRequest(List<Assertion> a, DeviceSignals d, int c, String id, String l, String b, String androidId, boolean defer, int variant) {
        this();
        assertions = a;
        targetDeviceSignals = d;
        credentialType = c;
        clientId = id;
        locale = l;
        sourceBackupAccountId = b;
        sourceAndroidDeviceId = androidId;
        deferCredentialsAfterFallback = defer;
        platformVariant = variant;
        for (int i = 2; i <= 10; i++) p.add(i);
    }

    @SafeParcelable.Constructor
    ExchangeAssertionsForUserCredentialsRequest(@SafeParcelable.Indicator Set<Integer> p, @SafeParcelable.Param(2) List<Assertion> a, @SafeParcelable.Param(3) DeviceSignals d, @SafeParcelable.Param(4) int c, @SafeParcelable.Param(5) String id, @SafeParcelable.Param(6) String l, @SafeParcelable.Param(7) String b, @SafeParcelable.Param(8) String androidId, @SafeParcelable.Param(9) boolean defer, @SafeParcelable.Param(10) int variant) {
        this.p = p;
        assertions = a;
        targetDeviceSignals = d;
        credentialType = c;
        clientId = id;
        locale = l;
        sourceBackupAccountId = b;
        sourceAndroidDeviceId = androidId;
        deferCredentialsAfterFallback = defer;
        platformVariant = variant;
    }

    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return F;
    }

    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> f) {
        return p.contains(f.safeParcelFieldId);
    }

    protected Object getFieldValue(FastJsonResponse.Field<?, ?> f) {
        switch (f.safeParcelFieldId) {
            case 2:
                return assertions;
            case 3:
                return targetDeviceSignals;
            case 4:
                return credentialType;
            case 5:
                return clientId;
            case 6:
                return locale;
            case 7:
                return sourceBackupAccountId;
            case 8:
                return sourceAndroidDeviceId;
            case 9:
                return deferCredentialsAfterFallback;
            case 10:
                return platformVariant;
            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> f, List<? extends FastJsonResponse> v) {
        if (f.safeParcelFieldId != 2) throw new IllegalArgumentException();
        assertions = (List<Assertion>) v;
        p.add(2);
    }

    protected void setInteger(FastJsonResponse.Field<?, ?> f, int v) {
        if (f.safeParcelFieldId == 4) credentialType = v;
        else if (f.safeParcelFieldId == 10) platformVariant = v;
        else throw new IllegalArgumentException();
        p.add(f.safeParcelFieldId);
    }

    protected void setBoolean(FastJsonResponse.Field<?, ?> f, boolean v) {
        if (f.safeParcelFieldId != 9) throw new IllegalArgumentException();
        deferCredentialsAfterFallback = v;
        p.add(9);
    }

    protected void setString(FastJsonResponse.Field<?, ?> f, String v) {
        if (f.safeParcelFieldId == 5 && v == null) return;
        switch (f.safeParcelFieldId) {
            case 5:
                clientId = v;
                break;
            case 6:
                locale = v;
                break;
            case 7:
                sourceBackupAccountId = v;
                break;
            case 8:
                sourceAndroidDeviceId = v;
                break;
            default:
                throw new IllegalArgumentException();
        }
        p.add(f.safeParcelFieldId);
    }

    public void writeToParcel(@NonNull Parcel d, int flags) {
        CREATOR.writeToParcel(this, d, flags);
    }

    public static final SafeParcelableCreatorAndWriter<ExchangeAssertionsForUserCredentialsRequest> CREATOR = findCreator(ExchangeAssertionsForUserCredentialsRequest.class);
}
