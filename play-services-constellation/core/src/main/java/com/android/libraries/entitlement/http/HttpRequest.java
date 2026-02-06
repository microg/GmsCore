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

package com.android.libraries.entitlement.http;

import android.content.res.Resources;
import android.net.Network;

import androidx.annotation.Nullable;

import com.android.libraries.entitlement.utils.UrlConnectionFactory;
import com.android.libraries.entitlement.CarrierConfig;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.net.HttpHeaders;
import com.google.errorprone.annotations.CanIgnoreReturnValue;

import org.json.JSONObject;

import java.util.List;

/** The parameters of an http request. */
@AutoValue
public abstract class HttpRequest {
    /** The URL. */
    public abstract String url();

    /** The HTTP request method, like "GET" or "POST". */
    public abstract String requestMethod();

    /** For "POST" request method, the body of the request in JSON format. */
    public abstract JSONObject postData();

    /** HTTP header fields. */
    public abstract ImmutableListMultimap<String, String> requestProperties();

    /** The client side timeout, in seconds. See {@link Builder#setTimeoutInSec}. */
    public abstract int timeoutInSec();

    /** The network used for this HTTP connection. See {@link Builder#setNetwork}. */
    @Nullable
    public abstract Network network();

    /**
     * The {@link UrlConnectionFactory} used for this HTTP connection.
     * See {@link Builder#setUrlConnectionFactory}.
     */
    @Nullable
    public abstract UrlConnectionFactory urlConnectionFactory();

    /** Builder of {@link HttpRequest}. */
    @AutoValue.Builder
    public abstract static class Builder {
        public abstract HttpRequest build();

        /** Sets the URL. */
        public abstract Builder setUrl(String url);

        /**
         * Sets the HTTP request method, like "GET" or "POST".
         *
         * @see HttpConstants.RequestMethod
         */
        public abstract Builder setRequestMethod(String requestMethod);

        /** For "POST" request method, sets the body of the request in JSON format. */
        public abstract Builder setPostData(JSONObject postData);

        abstract ImmutableListMultimap.Builder<String, String> requestPropertiesBuilder();

        /** Adds an HTTP header field. */
        @CanIgnoreReturnValue
        public Builder addRequestProperty(String key, String value) {
            requestPropertiesBuilder().put(key, value);
            return this;
        }

        /**
          * Adds an HTTP header field with multiple values. Equivalent to calling
          * {@link #addRequestProperty(String, String)} multiple times with the same key and
          * one value at a time.
          */
        @CanIgnoreReturnValue
        public Builder addRequestProperty(String key, List<String> value) {
            requestPropertiesBuilder().putAll(key, value);
            return this;
        }

        /**
         * Sets the client side timeout for HTTP connection. Default to
         * {@link com.android.libraries.entitlement.CarrierConfig#DEFAULT_TIMEOUT_IN_SEC}.
         *
         * <p>This timeout is used by both {@link java.net.URLConnection#setConnectTimeout} and
         * {@link java.net.URLConnection#setReadTimeout}.
         */
        public abstract Builder setTimeoutInSec(int timeoutInSec);

        /**
         * Sets the network used for this HTTP connection. If not set, the device default network
         * is used.
         */
        public abstract Builder setNetwork(@Nullable Network network);

        /**
         * If unset, the default Android API {@link java.net.URL#openConnection}
         * would be used. This allows callers of the lib to choose the HTTP stack.
         */
        public abstract Builder setUrlConnectionFactory(
                @Nullable UrlConnectionFactory urlConnectionFactory);
    }

    public static Builder builder() {
        return new AutoValue_HttpRequest.Builder()
                .setUrl("")
                .setRequestMethod("")
                .setPostData(new JSONObject())
                .setTimeoutInSec(CarrierConfig.DEFAULT_TIMEOUT_IN_SEC)
                .addRequestProperty(
                        HttpHeaders.ACCEPT_LANGUAGE,
                        Resources.getSystem()
                                .getConfiguration()
                                .getLocales()
                                .get(0)
                                .toLanguageTag());
    }
}
