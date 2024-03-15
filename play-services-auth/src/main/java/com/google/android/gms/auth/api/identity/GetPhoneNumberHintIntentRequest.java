/*
 * SPDX-FileCopyrightText: 2023 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;

import androidx.annotation.NonNull;

import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Request object used to get an Intent to start the Phone Number Hint flow.
 */
@SafeParcelable.Class
public class GetPhoneNumberHintIntentRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getTheme")
    private final int theme;

    @Constructor
    GetPhoneNumberHintIntentRequest(@Param(1) int theme) {
        this.theme = theme;
    }

    @NonNull
    public static Builder builder() {
        return new Builder();
    }

    @Hide
    public int getTheme() {
        return theme;
    }

    /**
     * Builder for {@link GetPhoneNumberHintIntentRequest}
     */
    public static class Builder {
        private int theme;

        @NonNull
        public GetPhoneNumberHintIntentRequest build() {
            return new GetPhoneNumberHintIntentRequest(theme);
        }

        @Hide
        @NonNull
        public Builder setTheme(int theme) {
            this.theme = theme;
            return this;
        }
    }


    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<GetPhoneNumberHintIntentRequest> CREATOR = findCreator(GetPhoneNumberHintIntentRequest.class);
}
