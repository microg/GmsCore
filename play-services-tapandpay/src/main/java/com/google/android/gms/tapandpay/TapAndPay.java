/*
 * SPDX-FileCopyrightText: 2025 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.google.android.gms.tapandpay;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public abstract class TapAndPay {
    public static final int CARD_NETWORK_AMEX = 1;
    public static final int CARD_NETWORK_DISCOVER = 2;
    public static final int CARD_NETWORK_MASTERCARD = 3;
    public static final int CARD_NETWORK_VISA = 4;
    public static final int CARD_NETWORK_INTERAC = 5;
    public static final int CARD_NETWORK_PRIVATE_LABEL = 6;
    public static final int CARD_NETWORK_EFTPOS = 7;
    public static final int CARD_NETWORK_MAESTRO = 8;
    public static final int CARD_NETWORK_ID = 9;
    public static final int CARD_NETWORK_QUICPAY = 10;
    public static final int CARD_NETWORK_JCB = 11;
    public static final int CARD_NETWORK_ELO = 12;
    public static final int CARD_NETWORK_MIR = 13;

    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({CARD_NETWORK_AMEX, CARD_NETWORK_DISCOVER, CARD_NETWORK_MASTERCARD, CARD_NETWORK_VISA, CARD_NETWORK_INTERAC, CARD_NETWORK_PRIVATE_LABEL, CARD_NETWORK_EFTPOS, CARD_NETWORK_MAESTRO, CARD_NETWORK_ID, CARD_NETWORK_QUICPAY, CARD_NETWORK_JCB, CARD_NETWORK_ELO, CARD_NETWORK_MIR})
    public @interface CardNetwork {
    }

    public static final int TOKEN_PROVIDER_AMEX = 2;
    public static final int TOKEN_PROVIDER_MASTERCARD = 3;
    public static final int TOKEN_PROVIDER_VISA = 4;
    public static final int TOKEN_PROVIDER_DISCOVER = 5;
    public static final int TOKEN_PROVIDER_EFTPOS = 6;
    public static final int TOKEN_PROVIDER_INTERAC = 7;
    public static final int TOKEN_PROVIDER_OBERTHUR = 8;
    public static final int TOKEN_PROVIDER_PAYPAL = 9;
    public static final int TOKEN_PROVIDER_JCB = 13;
    public static final int TOKEN_PROVIDER_ELO = 14;
    public static final int TOKEN_PROVIDER_GEMALTO = 15;
    public static final int TOKEN_PROVIDER_MIR = 16;

    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOKEN_PROVIDER_AMEX, TOKEN_PROVIDER_MASTERCARD, TOKEN_PROVIDER_VISA, TOKEN_PROVIDER_DISCOVER, TOKEN_PROVIDER_EFTPOS, TOKEN_PROVIDER_INTERAC, TOKEN_PROVIDER_OBERTHUR, TOKEN_PROVIDER_PAYPAL, TOKEN_PROVIDER_JCB, TOKEN_PROVIDER_ELO, TOKEN_PROVIDER_GEMALTO, TOKEN_PROVIDER_MIR})
    public @interface TokenServiceProvider {
    }

    public static final int TOKEN_STATE_UNTOKENIZED = 1;
    public static final int TOKEN_STATE_PENDING = 2;
    public static final int TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION = 3;
    public static final int TOKEN_STATE_SUSPENDED = 4;
    public static final int TOKEN_STATE_ACTIVE = 5;
    public static final int TOKEN_STATE_FELICA_PENDING_PROVISIONING = 6;

    @Target({ElementType.TYPE_USE})
    @Retention(RetentionPolicy.SOURCE)
    @IntDef({TOKEN_STATE_UNTOKENIZED, TOKEN_STATE_PENDING, TOKEN_STATE_NEEDS_IDENTITY_VERIFICATION, TOKEN_STATE_SUSPENDED, TOKEN_STATE_ACTIVE, TOKEN_STATE_FELICA_PENDING_PROVISIONING})
    public @interface TokenState {
    }


    @NonNull
    public static final String EXTRA_CARD_RESULT = "extra_card_result";

    @NonNull
    public static final String EXTRA_ISSUER_TOKEN_ID = "extra_issuer_token_id";

    @NonNull
    public static final String EXTRA_STATUS_LIST = "extra_status_list";

    @NonNull
    public static final String EXTRA_TOKENIZATION_SESSION_ID = "extra_tokenization_session_id";

    @NonNull
    public static final String EXTRA_TOKEN_RESULT = "extra_token_result";

    @NonNull
    public static final String EXTRA_VIRTUAL_CARDS_RESULT = "extra_virtual_cards_result";

    @NonNull
    public static final String TOKEN_REQUESTOR_ID_WALLET = "google_wallet";

    private TapAndPay() {
    }

    public interface DataChangedListener {
        void onDataChanged();
    }
}
