/* SPDX-License-Identifier: Apache-2.0 */
package com.google.android.gms.smartdevice.d2d.data;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.setup.accounts.*;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.*;

@SafeParcelable.Class
@Hide
public class AccountBootstrapPayload extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> F;

    static {
        Map<String, FastJsonResponse.Field<?, ?>> f = new LinkedHashMap<>();
        f.put("userBootstrapInfos", new FastJsonResponse.Field<>(11, true, 11, true, "userBootstrapInfos", 2, UserBootstrapInfo.class));
        f.put("challenges", new FastJsonResponse.Field<>(11, true, 11, true, "challenges", 3, Challenge.class));
        f.put("exchangeAssertionsForUserCredentialsRequest", new FastJsonResponse.Field<>(11, false, 11, false, "exchangeAssertionsForUserCredentialsRequest", 4, ExchangeAssertionsForUserCredentialsRequest.class));
        f.put("userCredentials", new FastJsonResponse.Field<>(11, true, 11, true, "userCredentials", 5, UserCredential.class));
        f.put("exchangeSessionCheckpointsForUserCredentialsRequest", new FastJsonResponse.Field<>(11, false, 11, false, "exchangeSessionCheckpointsForUserCredentialsRequest", 6, ExchangeSessionCheckpointsForUserCredentialsRequest.class));
        F = Collections.unmodifiableMap(f);
    }

    @SafeParcelable.Indicator
    public final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, subClass = UserBootstrapInfo.class, mayNull = true)
    public List<UserBootstrapInfo> userBootstrapInfos;
    @SafeParcelable.Field(value = 3, subClass = Challenge.class, mayNull = true)
    public List<Challenge> challenges;
    @SafeParcelable.Field(value = 4, mayNull = true)
    public ExchangeAssertionsForUserCredentialsRequest exchangeAssertionsForUserCredentialsRequest;
    @SafeParcelable.Field(value = 5, subClass = UserCredential.class, mayNull = true)
    public List<UserCredential> userCredentials;
    @SafeParcelable.Field(value = 6, mayNull = true)
    public ExchangeSessionCheckpointsForUserCredentialsRequest exchangeSessionCheckpointsForUserCredentialsRequest;

    public AccountBootstrapPayload() {
        presentFields = new HashSet<>();
    }

    @SafeParcelable.Constructor
    AccountBootstrapPayload(@SafeParcelable.Indicator Set<Integer> p, @SafeParcelable.Param(2) List<UserBootstrapInfo> a, @SafeParcelable.Param(3) List<Challenge> c, @SafeParcelable.Param(4) ExchangeAssertionsForUserCredentialsRequest e, @SafeParcelable.Param(5) List<UserCredential> u, @SafeParcelable.Param(6) ExchangeSessionCheckpointsForUserCredentialsRequest s) {
        presentFields = p;
        userBootstrapInfos = a;
        challenges = c;
        exchangeAssertionsForUserCredentialsRequest = e;
        userCredentials = u;
        exchangeSessionCheckpointsForUserCredentialsRequest = s;
    }

    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() {
        return F;
    }

    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> f) {
        return presentFields.contains(f.safeParcelFieldId);
    }

    protected Object getFieldValue(FastJsonResponse.Field<?, ?> f) {
        switch (f.safeParcelFieldId) {
            case 2:
                return userBootstrapInfos;
            case 3:
                return challenges;
            case 4:
                return exchangeAssertionsForUserCredentialsRequest;
            case 5:
                return userCredentials;
            case 6:
                return exchangeSessionCheckpointsForUserCredentialsRequest;
            default:
                throw new IllegalStateException();
        }
    }

    @SuppressWarnings("unchecked")
    protected void addConcreteTypeArray(FastJsonResponse.Field<?, ?> f, List<? extends FastJsonResponse> v) {
        switch (f.safeParcelFieldId) {
            case 2:
                userBootstrapInfos = (List<UserBootstrapInfo>) v;
                break;
            case 3:
                challenges = (List<Challenge>) v;
                break;
            case 5:
                userCredentials = (List<UserCredential>) v;
                break;
            default:
                throw new IllegalArgumentException();
        }
        presentFields.add(f.safeParcelFieldId);
    }

    protected void addConcreteType(FastJsonResponse.Field<?, ?> f, FastJsonResponse v) {
        if (f.safeParcelFieldId == 4)
            exchangeAssertionsForUserCredentialsRequest = (ExchangeAssertionsForUserCredentialsRequest) v;
        else if (f.safeParcelFieldId == 6)
            exchangeSessionCheckpointsForUserCredentialsRequest = (ExchangeSessionCheckpointsForUserCredentialsRequest) v;
        else throw new IllegalArgumentException();
        presentFields.add(f.safeParcelFieldId);
    }

    public void setUserBootstrapInfos(List<UserBootstrapInfo> v) {
        userBootstrapInfos = v;
        presentFields.add(2);
    }

    public void setExchangeAssertionsForUserCredentialsRequest(ExchangeAssertionsForUserCredentialsRequest v) {
        exchangeAssertionsForUserCredentialsRequest = v;
        presentFields.add(4);
    }

    public void setExchangeSessionCheckpointsForUserCredentialsRequest(ExchangeSessionCheckpointsForUserCredentialsRequest v) {
        exchangeSessionCheckpointsForUserCredentialsRequest = v;
        presentFields.add(6);
    }

    public void writeToParcel(@NonNull Parcel d, int flags) {
        CREATOR.writeToParcel(this, d, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AccountBootstrapPayload> CREATOR = findCreator(AccountBootstrapPayload.class);
}
