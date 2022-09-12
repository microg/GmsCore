/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.wallet;

import org.microg.gms.common.PublicApi;
import org.microg.safeparcel.AutoSafeParcelable;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A Parcelable request that can optionally be passed to {@link PaymentsClient#isReadyToPay(IsReadyToPayRequest)} to specify additional filtering criteria for determining if a user is considered ready to pay.
 */
@PublicApi
public class IsReadyToPayRequest extends AutoSafeParcelable {
    @Field(value = 2, useDirectList = true)
    private ArrayList<Integer> allowedCardNetworks;
    @Field(4)
    private String unknown4;
    @Field(5)
    private String unknown5;
    @Field(value = 6, useDirectList = true)
    private ArrayList<Integer> allowedPaymentMethods;
    @Field(7)
    private boolean existingPaymentMethodRequired;
    @Field(8)
    private String json;

    private IsReadyToPayRequest() {
    }

    private IsReadyToPayRequest(String json) {
        this.json = json;
    }

    /**
     * Constructs {@link IsReadyToPayRequest} from a JSON object serialized as a string.
     * <p>
     * To convert back to a JSON object serialized as string use {@link #toJson()}.
     * <p>
     * Note that you shouldn't rely on the values returned by getters in {@link IsReadyToPayRequest} as they will not be populated with the data set in the given JSON.
     * <p>
     * For the expected format of this JSON, please see <a href="https://developers.google.com/pay/api/android/reference/object/IsReadyToPayRequest">IsReadyToPayRequest object reference documentation</a>.
     */
    public static IsReadyToPayRequest fromJson(String isReadyToPayRequestJson) {
        return new IsReadyToPayRequest(isReadyToPayRequestJson);
    }

    /**
     * @return a builder for constructing the {@link IsReadyToPayRequest} object.
     * @deprecated Use the JSON request format instead, see {@link #fromJson(String)}.
     */
    public static IsReadyToPayRequest.Builder newBuilder() {
        return new IsReadyToPayRequest().new Builder();
    }

    /**
     * Returns {@link IsReadyToPayRequest} in JSON format.
     * <p>
     * Note that this will be {@code null} if this request was not constructed using {@link #fromJson(String)}.
     * <p>
     * For the expected format of this JSON, please see <a href="https://developers.google.com/pay/api/android/reference/object/IsReadyToPayRequest">IsReadyToPayRequest object reference documentation</a>.
     */
    public String toJson() {
        return json;
    }

    /**
     * @return the {@link WalletConstants.CardNetwork} that will be used to filter the instruments deemed acceptable by {@link PaymentsClient#isReadyToPay(IsReadyToPayRequest)}. If not explicitly set, the default supported networks will be {@link WalletConstants#CARD_NETWORK_AMEX}, {@link WalletConstants#CARD_NETWORK_DISCOVER}, {@link WalletConstants#CARD_NETWORK_MASTERCARD}, and {@link WalletConstants#CARD_NETWORK_VISA}.
     * @deprecated Use the JSON request format instead, see {@link #fromJson(String)}.
     */
    public ArrayList<Integer> getAllowedCardNetworks() {
        return allowedCardNetworks;
    }

    /**
     * @return the supported payment credential types defined in {@link WalletConstants.PaymentMethod}, or {@code null} if no restrictions were specified.
     * @deprecated Use the JSON request format instead, see {@link #fromJson(String)}.
     */
    public ArrayList<Integer> getAllowedPaymentMethods() {
        return allowedPaymentMethods;
    }

    /**
     * @return whether or not IsReadyToPay will be determined by the user having an existing payment method that matches the other criteria specified in the IsReadyToPayRequest.
     * @deprecated Use the JSON request format instead, see {@link #fromJson(String)}.
     */
    public boolean isExistingPaymentMethodRequired() {
        return existingPaymentMethodRequired;
    }

    /**
     * Builder for creating an {@link IsReadyToPayRequest}.
     *
     * @deprecated Use the JSON request format instead, see {@link #fromJson(String)}.
     */
    public class Builder {
        /**
         * Adds a card network for cards allowed in the purchase. See {@link WalletConstants.CardNetwork} for available options. If not explicitly set via this or {@link #addAllowedCardNetworks(Collection)}, the default supported networks will be {@link WalletConstants#CARD_NETWORK_AMEX}, {@link WalletConstants#CARD_NETWORK_DISCOVER}, {@link WalletConstants#CARD_NETWORK_MASTERCARD}, and {@link WalletConstants#CARD_NETWORK_VISA}.
         */
        public Builder addAllowedCardNetwork(int allowedCardNetwork) {
            if (allowedCardNetworks == null) allowedCardNetworks = new ArrayList<>();
            allowedCardNetworks.add(allowedCardNetwork);
            return this;
        }

        /**
         * Adds a collection of card networks for cards allowed in the purchase. See {@link WalletConstants.CardNetwork} for available options. If not explicitly set via this or {@link #addAllowedCardNetwork(int)}, the default supported networks will be {@link WalletConstants#CARD_NETWORK_AMEX}, {@link WalletConstants#CARD_NETWORK_DISCOVER}, {@link WalletConstants#CARD_NETWORK_MASTERCARD}, and {@link WalletConstants#CARD_NETWORK_VISA}.
         */
        public Builder addAllowedCardNetworks(Collection<Integer> allowedCardNetworks) {
            if (IsReadyToPayRequest.this.allowedCardNetworks == null)
                IsReadyToPayRequest.this.allowedCardNetworks = new ArrayList<>();
            IsReadyToPayRequest.this.allowedCardNetworks.addAll(allowedCardNetworks);
            return this;
        }

        /**
         * Adds an allowed payment method. See {@link WalletConstants.PaymentMethod} for available options. If not explicitly set via this or {@link #addAllowedPaymentMethods(Collection)}, then the default allowed payment method will be {@link WalletConstants#PAYMENT_METHOD_TOKENIZED_CARD}.
         */
        public Builder addAllowedPaymentMethod(int allowedPaymentMethod) {
            if (allowedPaymentMethods == null) allowedPaymentMethods = new ArrayList<>();
            allowedPaymentMethods.add(allowedPaymentMethod);
            return this;
        }

        /**
         * Adds a collection of allowed payment methods. See {@link WalletConstants.PaymentMethod} for available options. If not explicitly set via this or {@link #addAllowedPaymentMethod(int)}, then the default allowed payment method will be {@link WalletConstants#PAYMENT_METHOD_TOKENIZED_CARD}.
         */
        public Builder addAllowedPaymentMethods(Collection<Integer> allowedPaymentMethods) {
            if (IsReadyToPayRequest.this.allowedPaymentMethods == null)
                IsReadyToPayRequest.this.allowedPaymentMethods = new ArrayList<>();
            IsReadyToPayRequest.this.allowedPaymentMethods.addAll(allowedPaymentMethods);
            return this;
        }

        /**
         * @return The actual {@link IsReadyToPayRequest} created using the data passed to the Builder object.
         */
        public IsReadyToPayRequest build() {
            return IsReadyToPayRequest.this;
        }

        /**
         * If set to true, then IsReadyToPay will only return true if the user has an existing payment method that matches the other criteria specified in the IsReadyToPayRequest.
         * <p>
         * If you set this to true, make sure to call {@link PaymentsClient#isReadyToPay(IsReadyToPayRequest)} as early as possible in your flow as the call may take longer to resolve due to the extra check for an existing payment method.
         */
        public Builder setExistingPaymentMethodRequired(boolean existingPaymentMethodRequired) {
            IsReadyToPayRequest.this.existingPaymentMethodRequired = existingPaymentMethodRequired;
            return this;
        }
    }

    public static final Creator<IsReadyToPayRequest> CREATOR = new AutoCreator<>(IsReadyToPayRequest.class);
}
