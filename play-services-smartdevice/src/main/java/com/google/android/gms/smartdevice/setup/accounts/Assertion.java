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
public class Assertion extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> F;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> f = new LinkedHashMap<>();
        f.put("accountIdentifier", new FastJsonResponse.Field<>(7, false, 7, false, "accountIdentifier", 2, null));
        f.put("clientData", new FastJsonResponse.Field<>(8, false, 8, false, "clientData", 3, null));
        f.put("encryptedUserAssertion", new FastJsonResponse.Field<>(8, false, 8, false, "encryptedUserAssertion", 4, null));
        f.put("challengeSessionState", new FastJsonResponse.Field<>(8, false, 8, false, "challengeSessionState", 5, null));
        f.put("challenge", new FastJsonResponse.Field<>(8, false, 8, false, "challenge", 6, null));
        f.put("assertionType", new FastJsonResponse.Field<>(0, false, 0, false, "assertionType", 7, null));
        F = Collections.unmodifiableMap(f);
    }

    @SafeParcelable.Indicator
    final Set<Integer> p;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String accountIdentifier;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public byte[] clientData;
    @SafeParcelable.Field(value = 4, mayNull = true)
    public byte[] encryptedUserAssertion;
    @SafeParcelable.Field(value = 5, mayNull = true)
    public byte[] challengeSessionState;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public byte[] challenge;
    @SafeParcelable.Field(7)
    public int assertionType;

    public Assertion() {
        p = new HashSet<>();
    }

    public Assertion(String a, byte[] b, byte[] c, byte[] d, byte[] e) {
        this();
        accountIdentifier = a;
        clientData = b;
        encryptedUserAssertion = c;
        challengeSessionState = d;
        challenge = e;
        assertionType = 1;
        for (int i = 2; i <= 7; i++) p.add(i);
    }

    @SafeParcelable.Constructor
    Assertion(@SafeParcelable.Indicator Set<Integer> p, @SafeParcelable.Param(2) String a, @SafeParcelable.Param(3) byte[] b, @SafeParcelable.Param(4) byte[] c, @SafeParcelable.Param(5) byte[] d, @SafeParcelable.Param(6) byte[] e, @SafeParcelable.Param(7) int t) {
        this.p = p;
        accountIdentifier = a;
        clientData = b;
        encryptedUserAssertion = c;
        challengeSessionState = d;
        challenge = e;
        assertionType = t;
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
                return accountIdentifier;
            case 3:
                return clientData;
            case 4:
                return encryptedUserAssertion;
            case 5:
                return challengeSessionState;
            case 6:
                return challenge;
            case 7:
                return assertionType;
            default:
                throw new IllegalStateException();
        }
    }

    protected void setString(FastJsonResponse.Field<?, ?> f, String v) {
        if (v != null && f.safeParcelFieldId == 2) {
            accountIdentifier = v;
            p.add(2);
        } else if (f.safeParcelFieldId != 2) throw new IllegalArgumentException();
    }

    protected void setInteger(FastJsonResponse.Field<?, ?> f, int v) {
        if (f.safeParcelFieldId != 7) throw new IllegalArgumentException();
        assertionType = v;
        p.add(7);
    }

    protected void setDecodedBytes(FastJsonResponse.Field<?, ?> f, byte[] v) {
        if (v == null) return;
        switch (f.safeParcelFieldId) {
            case 3:
                clientData = v;
                break;
            case 4:
                encryptedUserAssertion = v;
                break;
            case 5:
                challengeSessionState = v;
                break;
            case 6:
                challenge = v;
                break;
            default:
                throw new IllegalArgumentException();
        }
        p.add(f.safeParcelFieldId);
    }

    public void writeToParcel(@NonNull Parcel d, int flags) {
        CREATOR.writeToParcel(this, d, flags);
    }

    public static final SafeParcelableCreatorAndWriter<Assertion> CREATOR = findCreator(Assertion.class);
}
