/*
 * SPDX-FileCopyrightText: 2026 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.smartdevice.directtransfer;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import com.google.android.gms.common.server.response.FastJsonResponse;
import com.google.android.gms.smartdevice.utils.BinarySerializableFastSafeParcelableJson;

import org.microg.gms.common.Hide;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@SafeParcelable.Class
@Hide
public class AccountPickerOptions extends BinarySerializableFastSafeParcelableJson {
    private static final Map<String, FastJsonResponse.Field<?, ?>> FIELD_MAPPINGS;
    static {
        Map<String, FastJsonResponse.Field<?, ?>> fields = new LinkedHashMap<>();
        fields.put("title", new FastJsonResponse.Field<>(7, false, 7, false, "title", 2, null));
        fields.put("description", new FastJsonResponse.Field<>(7, false, 7, false, "description", 3, null));
        fields.put("skipButtonVisible", new FastJsonResponse.Field<>(6, false, 6, false, "skipButtonVisible", 4, null));
        fields.put("multiSelectEnabled", new FastJsonResponse.Field<>(6, false, 6, false, "multiSelectEnabled", 5, null));
        fields.put("supervisedAccountsSupported", new FastJsonResponse.Field<>(6, false, 6, false, "supervisedAccountsSupported", 6, null));
        fields.put("corpAccountsSupported", new FastJsonResponse.Field<>(6, false, 6, false, "corpAccountsSupported", 7, null));
        fields.put("kidsOnboardingEnforced", new FastJsonResponse.Field<>(6, false, 6, false, "kidsOnboardingEnforced", 8, null));
        FIELD_MAPPINGS = Collections.unmodifiableMap(fields);
    }

    @SafeParcelable.Indicator
    final Set<Integer> presentFields;
    @SafeParcelable.Field(value = 2, mayNull = true)
    public String title;
    @SafeParcelable.Field(value = 3, mayNull = true)
    public String description;
    @SafeParcelable.Field(4)
    public boolean skipButtonVisible;
    @SafeParcelable.Field(5)
    public boolean multiSelectEnabled;
    @SafeParcelable.Field(6)
    public boolean supervisedAccountsSupported;
    @SafeParcelable.Field(7)
    public boolean corpAccountsSupported;
    @SafeParcelable.Field(8)
    public boolean kidsOnboardingEnforced;

    public AccountPickerOptions() {
        presentFields = new HashSet<>();
        skipButtonVisible = true;
    }

    public AccountPickerOptions(String title, String description, boolean skipButtonVisible,
                                boolean supervisedAccountsSupported, boolean corpAccountsSupported) {
        this();
        this.title = title;
        this.description = description;
        this.skipButtonVisible = skipButtonVisible;
        this.multiSelectEnabled = false;
        this.supervisedAccountsSupported = supervisedAccountsSupported;
        this.corpAccountsSupported = corpAccountsSupported;
        this.kidsOnboardingEnforced = false;
        for (int fieldId = 2; fieldId <= 8; fieldId++) presentFields.add(fieldId);
    }

    @SafeParcelable.Constructor
    AccountPickerOptions(@SafeParcelable.Indicator Set<Integer> presentFields,
                         @SafeParcelable.Param(2) String title,
                         @SafeParcelable.Param(3) String description,
                         @SafeParcelable.Param(4) boolean skipButtonVisible,
                         @SafeParcelable.Param(5) boolean multiSelectEnabled,
                         @SafeParcelable.Param(6) boolean supervisedAccountsSupported,
                         @SafeParcelable.Param(7) boolean corpAccountsSupported,
                         @SafeParcelable.Param(8) boolean kidsOnboardingEnforced) {
        this.presentFields = presentFields;
        this.title = title;
        this.description = description;
        this.skipButtonVisible = skipButtonVisible;
        this.multiSelectEnabled = multiSelectEnabled;
        this.supervisedAccountsSupported = supervisedAccountsSupported;
        this.corpAccountsSupported = corpAccountsSupported;
        this.kidsOnboardingEnforced = kidsOnboardingEnforced;
    }

    @Override
    public Map<String, FastJsonResponse.Field<?, ?>> getFieldMappings() { return FIELD_MAPPINGS; }

    @Override
    protected Object getFieldValue(FastJsonResponse.Field<?, ?> field) {
        switch (field.safeParcelFieldId) {
            case 2: return title;
            case 3: return description;
            case 4: return skipButtonVisible;
            case 5: return multiSelectEnabled;
            case 6: return supervisedAccountsSupported;
            case 7: return corpAccountsSupported;
            case 8: return kidsOnboardingEnforced;
            default: throw new IllegalStateException("Unknown field ID: " + field.safeParcelFieldId);
        }
    }

    @Override
    protected boolean isFieldSet(FastJsonResponse.Field<?, ?> field) { return presentFields.contains(field.safeParcelFieldId); }

    @Override
    protected void setString(FastJsonResponse.Field<?, ?> field, String value) {
        if (value == null) return;
        switch (field.safeParcelFieldId) {
            case 2: title = value; break;
            case 3: description = value; break;
            default: throw new IllegalArgumentException("Field is not a string: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    protected void setBoolean(FastJsonResponse.Field<?, ?> field, boolean value) {
        switch (field.safeParcelFieldId) {
            case 4: skipButtonVisible = value; break;
            case 5: multiSelectEnabled = value; break;
            case 6: supervisedAccountsSupported = value; break;
            case 7: corpAccountsSupported = value; break;
            case 8: kidsOnboardingEnforced = value; break;
            default: throw new IllegalArgumentException("Field is not a boolean: " + field.outputFieldName);
        }
        presentFields.add(field.safeParcelFieldId);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof AccountPickerOptions)) return false;
        AccountPickerOptions that = (AccountPickerOptions) other;
        return Objects.equals(title, that.title) && Objects.equals(description, that.description)
                && skipButtonVisible == that.skipButtonVisible
                && multiSelectEnabled == that.multiSelectEnabled
                && supervisedAccountsSupported == that.supervisedAccountsSupported
                && corpAccountsSupported == that.corpAccountsSupported
                && kidsOnboardingEnforced == that.kidsOnboardingEnforced;
    }

    @Override
    public int hashCode() { return Objects.hash(title, description, skipButtonVisible, kidsOnboardingEnforced); }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) { CREATOR.writeToParcel(this, dest, flags); }

    public static final SafeParcelableCreatorAndWriter<AccountPickerOptions> CREATOR = findCreator(AccountPickerOptions.class);
}
