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
import org.microg.gms.common.PublicApi;
import org.microg.gms.utils.ToStringHelper;

import java.util.Arrays;
import java.util.Objects;

/**
 * Represents extensions that can be passed into FIDO2 APIs. This container class corresponds to the additional
 * parameters requesting additional processing by authenticators.
 * <p>
 * Note that rather than accepting arbitrary objects as specified in WebAuthn, this class requires a structured entry
 * for each supported extension.
 */
@PublicApi
@SafeParcelable.Class
public class AuthenticationExtensions extends AbstractSafeParcelable {
    @Field(value = 2, getterName = "getFidoAppIdExtension")
    @Nullable
    private FidoAppIdExtension fidoAppIdExtension;
    @Field(value = 3, getterName = "getCableAuthenticationExtension")
    @Nullable
    private CableAuthenticationExtension cableAuthenticationExtension;
    @Field(value = 4, getterName = "getUserVerificationMethodExtension")
    @Nullable
    private UserVerificationMethodExtension userVerificationMethodExtension;
    @Field(value = 5, getterName = "getGoogleMultiAssertionExtension")
    @Nullable
    private GoogleMultiAssertionExtension googleMultiAssertionExtension;
    @Field(value = 6, getterName = "getGoogleSessionIdExtension")
    @Nullable
    private GoogleSessionIdExtension googleSessionIdExtension;
    @Field(value = 7, getterName = "getGoogleSilentVerificationExtension")
    @Nullable
    private GoogleSilentVerificationExtension googleSilentVerificationExtension;
    @Field(value = 8, getterName = "getDevicePublicKeyExtension")
    @Nullable
    private DevicePublicKeyExtension devicePublicKeyExtension;
    @Field(value = 9, getterName = "getGoogleTunnelServerIdExtension")
    @Nullable
    private GoogleTunnelServerIdExtension googleTunnelServerIdExtension;
    @Field(value = 10, getterName = "getGoogleThirdPartyPaymentExtension")
    @Nullable
    private GoogleThirdPartyPaymentExtension googleThirdPartyPaymentExtension;
    @Field(value = 11, getterName = "getPrfExtension")
    @Nullable
    private PrfExtension prfExtension;
    @Field(value = 12, getterName = "getSimpleTransactionAuthorizationExtension")
    @Nullable
    private SimpleTransactionAuthorizationExtension simpleTransactionAuthorizationExtension;
    @Field(value = 13, getterName = "getHmacSecretExtension")
    @Nullable
    private HmacSecretExtension hmacSecretExtension;
    @Field(value = 14, getterName = "getPaymentExtension")
    @Nullable
    private PaymentExtension paymentExtension;

    @Constructor
    public AuthenticationExtensions(@Param(2) @Nullable FidoAppIdExtension fidoAppIdExtension, @Param(3) @Nullable CableAuthenticationExtension cableAuthenticationExtension, @Param(4) @Nullable UserVerificationMethodExtension userVerificationMethodExtension, @Param(5) @Nullable GoogleMultiAssertionExtension googleMultiAssertionExtension, @Param(6) @Nullable GoogleSessionIdExtension googleSessionIdExtension, @Param(7) @Nullable GoogleSilentVerificationExtension googleSilentVerificationExtension, @Param(8) @Nullable DevicePublicKeyExtension devicePublicKeyExtension, @Param(9) @Nullable GoogleTunnelServerIdExtension googleTunnelServerIdExtension, @Param(10) @Nullable GoogleThirdPartyPaymentExtension googleThirdPartyPaymentExtension, @Param(11) @Nullable PrfExtension prfExtension, @Param(12) @Nullable SimpleTransactionAuthorizationExtension simpleTransactionAuthorizationExtension, @Param(13) @Nullable HmacSecretExtension hmacSecretExtension, @Param(14) @Nullable PaymentExtension paymentExtension) {
        this.fidoAppIdExtension = fidoAppIdExtension;
        this.cableAuthenticationExtension = cableAuthenticationExtension;
        this.userVerificationMethodExtension = userVerificationMethodExtension;
        this.googleMultiAssertionExtension = googleMultiAssertionExtension;
        this.googleSessionIdExtension = googleSessionIdExtension;
        this.googleSilentVerificationExtension = googleSilentVerificationExtension;
        this.devicePublicKeyExtension = devicePublicKeyExtension;
        this.googleTunnelServerIdExtension = googleTunnelServerIdExtension;
        this.googleThirdPartyPaymentExtension = googleThirdPartyPaymentExtension;
        this.prfExtension = prfExtension;
        this.simpleTransactionAuthorizationExtension = simpleTransactionAuthorizationExtension;
        this.hmacSecretExtension = hmacSecretExtension;
        this.paymentExtension = paymentExtension;
    }

    @Nullable
    public FidoAppIdExtension getFidoAppIdExtension() {
        return fidoAppIdExtension;
    }

    @Hide
    @Nullable
    public CableAuthenticationExtension getCableAuthenticationExtension() {
        return cableAuthenticationExtension;
    }

    @Nullable
    public UserVerificationMethodExtension getUserVerificationMethodExtension() {
        return userVerificationMethodExtension;
    }

    @Nullable
    public GoogleMultiAssertionExtension getGoogleMultiAssertionExtension() {
        return googleMultiAssertionExtension;
    }

    @Nullable
    public GoogleSessionIdExtension getGoogleSessionIdExtension() {
        return googleSessionIdExtension;
    }

    @Nullable
    public GoogleSilentVerificationExtension getGoogleSilentVerificationExtension() {
        return googleSilentVerificationExtension;
    }

    @Nullable
    public DevicePublicKeyExtension getDevicePublicKeyExtension() {
        return devicePublicKeyExtension;
    }

    @Nullable
    public GoogleTunnelServerIdExtension getGoogleTunnelServerIdExtension() {
        return googleTunnelServerIdExtension;
    }

    @Nullable
    public GoogleThirdPartyPaymentExtension getGoogleThirdPartyPaymentExtension() {
        return googleThirdPartyPaymentExtension;
    }

    @Nullable
    public PrfExtension getPrfExtension() {
        return prfExtension;
    }

    @Nullable
    public SimpleTransactionAuthorizationExtension getSimpleTransactionAuthorizationExtension() {
        return simpleTransactionAuthorizationExtension;
    }

    @Nullable
    public HmacSecretExtension getHmacSecretExtension() {
        return hmacSecretExtension;
    }

    @Nullable
    public PaymentExtension getPaymentExtension() {
        return paymentExtension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AuthenticationExtensions)) return false;

        AuthenticationExtensions that = (AuthenticationExtensions) o;

        if (!Objects.equals(fidoAppIdExtension, that.fidoAppIdExtension)) return false;
        if (!Objects.equals(cableAuthenticationExtension, that.cableAuthenticationExtension)) return false;
        if (!Objects.equals(userVerificationMethodExtension, that.userVerificationMethodExtension)) return false;
        if (!Objects.equals(googleMultiAssertionExtension, that.googleMultiAssertionExtension)) return false;
        if (!Objects.equals(googleSessionIdExtension, that.googleSessionIdExtension)) return false;
        if (!Objects.equals(googleSilentVerificationExtension, that.googleSilentVerificationExtension)) return false;
        if (!Objects.equals(devicePublicKeyExtension, that.devicePublicKeyExtension)) return false;
        if (!Objects.equals(googleTunnelServerIdExtension, that.googleTunnelServerIdExtension)) return false;
        if (!Objects.equals(googleThirdPartyPaymentExtension, that.googleThirdPartyPaymentExtension)) return false;
        if (!Objects.equals(prfExtension, that.prfExtension)) return false;
        if (!Objects.equals(simpleTransactionAuthorizationExtension, that.simpleTransactionAuthorizationExtension)) return false;
        if (!Objects.equals(hmacSecretExtension, that.hmacSecretExtension)) return false;
        return Objects.equals(paymentExtension, that.paymentExtension);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(new Object[]{fidoAppIdExtension, cableAuthenticationExtension, userVerificationMethodExtension, googleMultiAssertionExtension, googleSessionIdExtension, googleSilentVerificationExtension, devicePublicKeyExtension, googleTunnelServerIdExtension, googleThirdPartyPaymentExtension, prfExtension, simpleTransactionAuthorizationExtension, hmacSecretExtension, paymentExtension});
    }

    @Override
    public String toString() {
        return ToStringHelper.name("AuthenticationExtensions").field("fidoAppIdExtension", fidoAppIdExtension != null ? fidoAppIdExtension.getAppId() : null).field("cableAuthenticationExtension", cableAuthenticationExtension).field("userVerificationMethodExtension", userVerificationMethodExtension != null ? userVerificationMethodExtension.getUvm() : null).field("googleMultiAssertionExtension", googleMultiAssertionExtension).field("googleSessionIdExtension", googleSessionIdExtension).field("googleSilentVerificationExtension", googleSilentVerificationExtension).field("devicePublicKeyExtension", devicePublicKeyExtension).field("googleTunnelServerIdExtension", googleTunnelServerIdExtension).field("googleThirdPartyPaymentExtension", googleThirdPartyPaymentExtension).field("prfExtension", prfExtension).field("simpleTransactionAuthorizationExtension", simpleTransactionAuthorizationExtension).field("hmacSecretExtension", hmacSecretExtension).field("paymentExtension", paymentExtension).end();
    }

    /**
     * Builder for {@link AuthenticationExtensions}.
     */
    public static class Builder {
        @Nullable
        private FidoAppIdExtension fidoAppIdExtension;
        @Nullable
        private UserVerificationMethodExtension userVerificationMethodExtension;

        /**
         * The constructor of {@link AuthenticationExtensions.Builder}.
         */
        public Builder() {
        }

        /**
         * Sets the App ID extension, which allows for authentication of U2F authenticators previously registered
         * under the supplied App ID.
         */
        public Builder setFido2Extension(@Nullable FidoAppIdExtension appIdExtension) {
            this.fidoAppIdExtension = appIdExtension;
            return this;
        }

        /**
         * Sets the User Verification Method extension, which allows the relying party to ascertain up to three
         * authentication methods that were used.
         */
        public Builder setUserVerificationMethodExtension(@Nullable UserVerificationMethodExtension userVerificationMethodExtension) {
            this.userVerificationMethodExtension = userVerificationMethodExtension;
            return this;
        }

        /**
         * Builds the {@link AuthenticationExtensions} object.
         */
        @NonNull
        public AuthenticationExtensions build() {
            return new AuthenticationExtensions(fidoAppIdExtension, null, userVerificationMethodExtension, null, null, null, null, null, null, null, null, null, null);
        }
    }

    @Override
    public void writeToParcel(@NonNull Parcel dest, int flags) {
        CREATOR.writeToParcel(this, dest, flags);
    }

    public static final SafeParcelableCreatorAndWriter<AuthenticationExtensions> CREATOR = findCreator(AuthenticationExtensions.class);
}
