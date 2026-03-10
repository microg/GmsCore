/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import android.os.Parcel;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.internal.safeparcel.AbstractSafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelable;
import com.google.android.gms.common.internal.safeparcel.SafeParcelableCreatorAndWriter;
import org.microg.gms.common.Hide;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;

/**
 * Relying Parties may use {@link AuthenticatorSelectionCriteria} to specify their requirements regarding authenticator
 * attributes.
 */
@SafeParcelable.Class
public class AuthenticatorSelectionCriteria extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getAttachment")
    @Nullable
    private Attachment attachment;
    @Field(value = 3, getterName = "getRequireResidentKey")
    @Nullable
    private Boolean requireResidentKey;
    @Field(value = 4, getterName = "getRequireUserVerification")
    @Nullable
    private UserVerificationRequirement requireUserVerification;
    @Field(value = 5, getterName = "getResidentKeyRequirement")
    @Nullable
    private ResidentKeyRequirement residentKeyRequirement;

    @Constructor
    AuthenticatorSelectionCriteria(@Param(2) @Nullable Attachment attachment, @Param(3) @Nullable Boolean requireResidentKey, @Param(4) @Nullable UserVerificationRequirement requireUserVerification, @Param(5) @Nullable ResidentKeyRequirement residentKeyRequirement) {
        this.attachment = attachment;
        this.requireResidentKey = requireResidentKey;
        this.requireUserVerification = requireUserVerification;
        this.residentKeyRequirement = residentKeyRequirement;
    }

    @Nullable
    public Attachment getAttachment() {
        return attachment;
    }

    @Nullable
    public String getAttachmentAsString() {
        if (attachment == null) return null;
        return attachment.toString();
    }

    @Nullable
    public Boolean getRequireResidentKey() {
        return requireResidentKey;
    }

    @Nullable
    public ResidentKeyRequirement getResidentKeyRequirement() {
        return residentKeyRequirement;
    }

    @Nullable
    public String getResidentKeyRequirementAsString() {
        if (residentKeyRequirement == null) return null;
        return residentKeyRequirement.toString();
    }

    @Hide
    @Nullable
    public UserVerificationRequirement getRequireUserVerification() {
        return requireUserVerification;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticatorSelectionCriteria)) return false;

        AuthenticatorSelectionCriteria that = (AuthenticatorSelectionCriteria) o;

        if (attachment != that.attachment) return false;
        if (requireResidentKey != null ? !requireResidentKey.equals(that.requireResidentKey) : that.requireResidentKey != null) return false;
        if (requireUserVerification != that.requireUserVerification) return false;
        if (residentKeyRequirement != that.residentKeyRequirement) return false;
        return true;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{attachment, requireResidentKey, requireUserVerification});
    }

    @Override
    @NonNull
    public String toString() {
        return ToStringHelper.name("AuthenticatorSelectionCriteria")
                .field("attachment", attachment)
                .field("requireResidentKey", requireResidentKey)
                .field("requireUserVerification", requireUserVerification)
                .field("residentKeyRequirement", residentKeyRequirement)
                .end();
    }

    /**
     * Builder for {@link AuthenticatorSelectionCriteria}.
     */
    public static class Builder {
        @Nullable
        private Attachment attachment;
        @Nullable
        private Boolean requireResidentKey;
        @Nullable
        private ResidentKeyRequirement residentKeyRequirement;

        /**
         * Sets the attachment to use for this session.
         */
        public Builder setAttachment(@Nullable Attachment attachment) {
            this.attachment = attachment;
            return this;
        }

        /**
         * Sets whether the key created will be a resident key.
         */
        public Builder setRequireResidentKey(@Nullable Boolean requireResidentKey) {
            this.requireResidentKey = requireResidentKey;
            return this;
        }

        /**
         * Sets residentKeyRequirement
         */
        public Builder setResidentKeyRequirement(@Nullable ResidentKeyRequirement residentKeyRequirement) {
            this.residentKeyRequirement = residentKeyRequirement;
            return this;
        }

        @NonNull
        public AuthenticatorSelectionCriteria build() {
            return new AuthenticatorSelectionCriteria(attachment, requireResidentKey, null, residentKeyRequirement);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticatorSelectionCriteria> CREATOR = findCreator(AuthenticatorSelectionCriteria.class);
}
