/*
 * Copyright (C) 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.libraries.entitlement;

import android.net.Network;

import androidx.annotation.Nullable;

import com.android.libraries.entitlement.utils.UrlConnectionFactory;
import com.google.auto.value.AutoValue;

/**
 * Carrier specific customization to be used in the service entitlement queries and operations.
 *
 * @see #ServiceEntitlement
 */
@AutoValue
public abstract class CarrierConfig {
    /** Default value of {@link #timeoutInSec} if not set. */
    public static final int DEFAULT_TIMEOUT_IN_SEC = 30;

    public static final String CLIENT_TS_43_IMS_ENTITLEMENT = "client-IMS-Entitlement";
    public static final String CLIENT_TS_43_COMPANION_ODSA = "client-Companion-ODSA";
    public static final String CLIENT_TS_43_PRIMARY_ODSA = "client-Primary-ODSA";
    public static final String CLIENT_TS_43_SERVER_ODSA = "client-Server-ODSA";

    /** The carrier's entitlement server URL. See {@link Builder#setServerUrl}. */
    public abstract String serverUrl();

    /**
     * Client-ts43 attribute. Used to set the User-Agent header in HTTP requests as defined in TS.43
     * section 2.2.
     */
    public abstract String clientTs43();

    /** Returns {@code true} if HTTP POST, instead of GET, should be used for TS.43 requests. */
    public abstract boolean useHttpPost();

    /** Client side timeout for HTTP connection. See {@link Builder#setTimeoutInSec}. */
    public abstract int timeoutInSec();

    /** The {@link Network} used for HTTP connection. See {@link Builder#setNetwork}. */
    @Nullable
    public abstract Network network();

    /** The factory to create connections. See {@link Builder#setUrlConnectionFactory}. */
    @Nullable
    public abstract UrlConnectionFactory urlConnectionFactory();

    /** The EAP-AKA realm. See {@link Builder#setEapAkaRealm}. */
    public abstract String eapAkaRealm();

    /** Returns a new {@link Builder} object. */
    public static Builder builder() {
        return new AutoValue_CarrierConfig.Builder()
                .setServerUrl("")
                .setClientTs43("")
                .setUseHttpPost(false)
                .setTimeoutInSec(DEFAULT_TIMEOUT_IN_SEC)
                .setEapAkaRealm("nai.epc");
    }

    /** Builder. */
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract CarrierConfig build();

        /**
         * Sets the carrier's entitlement server URL. If not set, will use {@code
         * https://aes.mnc<MNC>.mcc<MCC>.pub.3gppnetwork.org} as defined in GSMA TS.43 section 2.1.
         */
        public abstract Builder setServerUrl(String url);

        /** Sets the Client-ts43 attribute. Used to set the User-Agent header in HTTP requests. */
        public abstract Builder setClientTs43(String clientTs43);

        /** Set to {@code true} to use HTTP POST instead of GET for TS.43 requests. */
        public abstract Builder setUseHttpPost(boolean useHttpPost);

        /**
         * Sets the client side timeout for HTTP connection. Default to
         * {@link DEFAULT_TIMEOUT_IN_SEC}.
         *
         * <p>This timeout is used by both {@link java.net.URLConnection#setConnectTimeout} and
         * {@link java.net.URLConnection#setReadTimeout}.
         */
        public abstract Builder setTimeoutInSec(int timeoutInSec);

        /**
         * Sets the {@link Network} used for HTTP connection. If not set, the device default network
         * is used.
         */
        public abstract Builder setNetwork(Network network);

        /**
         * If unset, the default Android API {@link java.net.URL#openConnection}
         * would be used. This allows callers of the lib to choose the HTTP stack.
         */
        public abstract Builder setUrlConnectionFactory(UrlConnectionFactory urlConnectionFactory);

        /**
         * Sets the realm for EAP-AKA. If unset, uses the standard "nai.epc" defined in 3GPP TS
         * 23.003 clause 19.3.2.
         */
        public abstract Builder setEapAkaRealm(String eapAkaRealm);
    }
}
