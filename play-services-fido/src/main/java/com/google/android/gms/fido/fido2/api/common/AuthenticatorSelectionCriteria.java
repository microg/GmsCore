/*
 * SPDX-FileCopyrightText: 2022 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.fido.fido2.api.common;

import androidx.annotation.Nullable;
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.Arrays;

/**
 * Relying Parties may use {@link AuthenticatorSelectionCriteria} to specify their requirements regarding authenticator
 * attributes.
 */
public class AuthenticatorSelectionCriteria extends AutoSafeParcelable {
    @Field(2)
    private Attachment attachment;
    @Field(3)
    private Boolean requireResidentKey;
    @Field(4)
    private UserVerificationRequirement requireUserVerification;
    @Field(5)
    private ResidentKeyRequirement residentKeyRequirement;

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

    @PublicApi(exclude = true)
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
        private Attachment attachment;
        private Boolean requireResidentKey;
        private ResidentKeyRequirement residentKeyRequirement;

        /**
         * Sets the attachment to use for this session.
         */
        public Builder setAttachment(Attachment attachment) {
            this.attachment = attachment;
            return this;
        }

        /**
         * Sets whether the key created will be a resident key.
         */
        public Builder setRequireResidentKey(Boolean requireResidentKey) {
            this.requireResidentKey = requireResidentKey;
            return this;
        }

        /**
         * Sets residentKeyRequirement
         */
        public Builder setResidentKeyRequirement(ResidentKeyRequirement residentKeyRequirement) {
            this.residentKeyRequirement = residentKeyRequirement;
            return this;
        }

        public AuthenticatorSelectionCriteria build() {
            AuthenticatorSelectionCriteria criteria = new AuthenticatorSelectionCriteria();
            criteria.attachment = attachment;
            criteria.requireResidentKey = requireResidentKey;
            criteria.residentKeyRequirement = residentKeyRequirement;
            return criteria;
        }
    }

    public static final Creator<AuthenticatorSelectionCriteria> CREATOR = new AutoCreator<>(AuthenticatorSelectionCriteria.class);
}
