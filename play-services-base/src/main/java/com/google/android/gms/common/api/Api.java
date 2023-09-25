/*
 * Copyright (C) 2013-2017 microG Project Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.common.api;

import android.accounts.Account;

import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.ApiClientBuilder;

/**
 * Describes a section of the Google Play Services API that should be made available. Instances of
 * this should be passed into {@link GoogleApiClient.Builder#addApi(Api)} to enable the appropriate
 * parts of Google Play Services.
 * <p/>
 * Google APIs are partitioned into sections which allow your application to configure only the
 * services it requires. Each Google API provides an API object which can be passed to
 * {@link GoogleApiClient.Builder#addApi(Api)} in order to configure and enable that functionality
 * in your {@link GoogleApiClient} instance.
 * <p/>
 * See {@link GoogleApiClient.Builder} for usage examples.
 */
@PublicApi
public final class Api<O extends Api.ApiOptions> {
    private final ApiClientBuilder<O> builder;

    @PublicApi(exclude = true)
    public Api(ApiClientBuilder<O> builder) {
        this.builder = builder;
    }

    @PublicApi(exclude = true)
    public ApiClientBuilder<O> getBuilder() {
        return builder;
    }

    /**
     * Base interface for API options. These are used to configure specific parameters for
     * individual API surfaces. The default implementation has no parameters.
     */
    @PublicApi
    public interface ApiOptions {
        /**
         * Base interface for {@link ApiOptions} in {@link Api}s that have options.
         */
        @PublicApi
        interface HasOptions extends ApiOptions {
        }

        /**
         * Base interface for {@link ApiOptions} that are not required, don't exist.
         */
        @PublicApi
        interface NotRequiredOptions extends ApiOptions {
        }

        /**
         * {@link ApiOptions} implementation for {@link Api}s that do not take any options.
         */
        @PublicApi
        final class NoOptions implements NotRequiredOptions {
        }

        /**
         * Base interface for {@link ApiOptions} that are optional.
         */
        @PublicApi
        interface Optional extends HasOptions, NotRequiredOptions {
        }

        /**
         * An interface for {@link ApiOptions} that include an account.
         */
        @PublicApi
        interface HasAccountOptions extends HasOptions, NotRequiredOptions {
            Account getAccount();
        }

        /**
         * An interface for {@link ApiOptions} that includes a {@link GoogleSignInAccount}
         */
        @PublicApi
        interface HasGoogleSignInAccountOptions extends HasOptions {
            GoogleSignInAccount getGoogleSignInAccount();
        }
    }

    @Hide
    public interface Client {
        void connect();

        void disconnect();

        boolean isConnected();

        boolean isConnecting();
    }
}
