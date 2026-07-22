/*
 * SPDX-FileCopyrightText: 2024 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.auth.api.identity;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;

/**
 * Configurations that allow saving a {@link SignInPassword} to Google.
 */
@SafeParcelable.Class
public class SavePasswordRequest extends AbstractSafeParcelable {
    @Field(value = 1, getterName = "getSignInPassword")
    private final SignInPassword signInPassword;
    @Field(value = 2, getterName = "getSessionId")
    @Nullable
    private final String sessionId;
    @Field(value = 3, getterName = "getTheme")
    private final int theme;

    @Constructor
    SavePasswordRequest(@Param(1) SignInPassword signInPassword, @Param(2) @Nullable String sessionId, @Param(3) int theme) {
        this.signInPassword = signInPassword;
        this.sessionId = sessionId;
        this.theme = theme;
    }

    /**
     * Returns a new instance of the {@link SavePasswordRequest.Builder}.
     */
    public static Builder builder() {
        return new Builder();
    }

    @Hide
    @Nullable
    public String getSessionId() {
        return sessionId;
    }

    public SignInPassword getSignInPassword() {
        return signInPassword;
    }

    @Hide
    public int getTheme() {
        return theme;
    }

    /**
     * Builder for {@link SavePasswordRequest}.
     */
    public static class Builder {
        private SignInPassword signInPassword;
        @Nullable
        private String sessionId;
        private int theme;

        /**
         * Sets the {@link SignInPassword}
         */
        @NonNull
        public Builder setSignInPassword(@NonNull SignInPassword signInPassword) {
            this.signInPassword = signInPassword;
            return this;
        }

        @Hide
        @NonNull
        public Builder setSessionId(@Nullable String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        @Hide
        @NonNull
        public Builder setTheme(int theme) {
            this.theme = theme;
            return this;
        }

        /**
         * Builds and returns a new instance of {@link SavePasswordRequest}.
         */
        @NonNull
        public SavePasswordRequest build() {
            return new SavePasswordRequest(signInPassword, sessionId, theme);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<SavePasswordRequest> CREATOR = findCreator(SavePasswordRequest.class);
}
