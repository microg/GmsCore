package com.google.android.gms.semanticlocation.internal;

import android.accounts.Account;
import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;

@SafeParcelable.Class
public class SemanticLocationParameters extends AbstractSafeParcelable {
    @Field(1)
    public Account account;
    @Field(2)
    public String clientIdentifier;
    @Field(3)
    public String packageName;

    public SemanticLocationParameters() {}

    public SemanticLocationParameters(Account account, String clientIdentifier, String packageName) {
        this.account = account;
        this.clientIdentifier = clientIdentifier;
        this.packageName = packageName;
    }

    public static final SafeParcelableCreatorAndWriter<SemanticLocationParameters> CREATOR = findCreator(SemanticLocationParameters.class);

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }
}
