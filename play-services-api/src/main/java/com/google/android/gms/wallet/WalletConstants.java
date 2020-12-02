/*
 * SPDX-FileCopyrightText: 2020, microG Project Team
 * SPDX-License-Identifier: Apache-2.0 AND CC-BY-4.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.wallet;

import org.microg.gms.common.PublicApi;

/**
 * Collection of constant values used by the ClientLibrary.
 */
@PublicApi
public class WalletConstants {
    /**
     * Credit card networks. Different APIs may support only a subset of these.
     * <p>
     * Available options:
     * <ul>
     * <li>{@link #CARD_NETWORK_AMEX}</li>
     * <li>{@link #CARD_NETWORK_DISCOVER}</li>
     * <li>{@link #CARD_NETWORK_JCB}</li>
     * <li>{@link #CARD_NETWORK_MASTERCARD}</li>
     * <li>{@link #CARD_NETWORK_VISA}</li>
     * <li>{@link #CARD_NETWORK_INTERAC}</li>
     * <li>{@link #CARD_NETWORK_OTHER}</li>
     * </ul>
     * When used with Google Pay, the following networks use EMV cryptograms instead of 3DS cryptograms as part of the payment credentials:
     * <ul>
     * <li>{@link #CARD_NETWORK_INTERAC}</li>
     * </ul>
     */
    public @interface CardNetwork {
        /**
         * @deprecated Use {@link #CARD_NETWORK_AMEX} instead.
         */
        int AMEX = 1;
        /**
         * @deprecated Use {@link #CARD_NETWORK_DISCOVER} instead.
         */
        int DISCOVER = 2;
        /**
         * @deprecated Use {@link #CARD_NETWORK_JCB} instead.
         */
        int JCB = 3;
        /**
         * @deprecated Use {@link #CARD_NETWORK_MASTERCARD} instead.
         */
        int MASTERCARD = 4;
        /**
         * @deprecated Use {@link #CARD_NETWORK_VISA} instead.
         */
        int VISA = 5;
        /**
         * @deprecated Use {@link #CARD_NETWORK_INTERAC} instead.
         */
        int INTERAC = 6;
        /**
         * @deprecated Use {@link #CARD_NETWORK_OTHER} instead.
         */
        int OTHER = 1000;
    }

    /**
     * The payment methods you support.
     * <p>
     * Available options:
     * <ul>
     * <li>{@link #PAYMENT_METHOD_UNKNOWN}</li>
     * <li>{@link #PAYMENT_METHOD_CARD}</li>
     * <li>{@link #PAYMENT_METHOD_TOKENIZED_CARD}</li>
     * </ul>
     * Note {@link WalletConstants.PaymentMethod} is different from {@link PaymentMethodTokenizationType}, which identifies how you want to receive the returned payment credential.
     */
    public @interface PaymentMethod {
    }

    /**
     * Payment method tokenization types.
     * <p>
     * Available options:
     * <ul>
     * <li>{@link #PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY}</li>
     * <li>{@link #PAYMENT_METHOD_TOKENIZATION_TYPE_NETWORK_TOKEN}</li>
     * <li>{@link #PAYMENT_METHOD_TOKENIZATION_TYPE_DIRECT}</li>
     * </ul>
     * Integrator can configure {@link PaymentDataRequest} to tokenize the credit card selected by the buyer for a transaction using one of the tokenization types listed above. The token for the selected payment method can be retrieved by calling {@link PaymentData#getPaymentMethodToken()}.
     */
    public @interface PaymentMethodTokenizationType {
    }


    public static final int CARD_CLASS_CREDIT = 1;
    public static final int CARD_CLASS_DEBIT = 2;
    public static final int CARD_CLASS_PREPAID = 3;
    public static final int CARD_CLASS_UNKNOWN = 0;

    public static final int CARD_NETWORK_AMEX = 1;
    public static final int CARD_NETWORK_DISCOVER = 2;
    public static final int CARD_NETWORK_INTERAC = 6;
    public static final int CARD_NETWORK_JCB = 3;
    public static final int CARD_NETWORK_MASTERCARD = 4;
    public static final int CARD_NETWORK_OTHER = 1000;
    public static final int CARD_NETWORK_VISA = 5;

    public static final int PAYMENT_METHOD_CARD = 1;
    public static final int PAYMENT_METHOD_TOKENIZED_CARD = 2;
    public static final int PAYMENT_METHOD_UNKNOWN = 0;


    public static final int PAYMENT_METHOD_TOKENIZATION_TYPE_DIRECT = 3;
    public static final int PAYMENT_METHOD_TOKENIZATION_TYPE_NETWORK_TOKEN = 2;
    public static final int PAYMENT_METHOD_TOKENIZATION_TYPE_PAYMENT_GATEWAY = 1;
}
