/*
 * SPDX-FileCopyrightText: 2013 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.accounts.Account;

import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;

import com.google.android.gms.auth.api.signin.GoogleSignInOptionsExtension;
import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.internal.ConnectionCallbacks;
import com.google.android.gms.common.api.internal.OnConnectionFailedListener;
import com.google.android.gms.common.internal.BaseGmsClient;
import com.google.android.gms.common.internal.ClientSettings;
import com.google.android.gms.common.internal.IAccountAccessor;
import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.ApiClientBuilder;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.List;
import java.util.Set;

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
public final class Api<O extends Api.ApiOptions> {
    @NonNull
    private final String name;
    @NonNull
    private final AbstractClientBuilder<? extends Client, O> clientBuilder;
    @NonNull
    private final ClientKey<? extends Client> clientKey;

    @Hide
    @Deprecated
    public <C extends Client> Api(ApiClientBuilder<O> builder) {
        this("Deprecated "+builder.getClass().getName(), new AbstractClientBuilder<C, O>() {
            @NonNull
            @Override
            public C buildClient(@NonNull Context context, @NonNull Looper looper, @NonNull ClientSettings clientSettings, @NonNull O options, @NonNull ConnectionCallbacks connectionCallbacks, @NonNull OnConnectionFailedListener onConnectionFailedListener) {
                return (C) builder.build(options, context, looper, clientSettings, connectionCallbacks, onConnectionFailedListener);
            }
        }, new ClientKey<>());
    }

    public <C extends Client> Api(String name, AbstractClientBuilder<C, O> clientBuilder, ClientKey<C> clientKey) {
        this.name = name;
        this.clientBuilder = clientBuilder;
        this.clientKey = clientKey;
    }

    @Hide
    @Deprecated
    public ApiClientBuilder<O> getBuilder() {
        return (options, context, looper, clientSettings, callbacks, connectionFailedListener) -> clientBuilder.buildClient(context, looper, clientSettings, options, callbacks, connectionFailedListener);
    }

    @Hide
    public static abstract class AbstractClientBuilder<T extends Client, O> extends BaseClientBuilder<T, O> {
        @NonNull
        @Deprecated
        public T buildClient(@NonNull Context context, @NonNull Looper looper, @NonNull ClientSettings clientSettings, @NonNull O options, @NonNull GoogleApiClient.ConnectionCallbacks connectionCallbacks, @NonNull GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener) {
            return buildClient(context, looper, clientSettings, options, (ConnectionCallbacks) connectionCallbacks, (OnConnectionFailedListener) onConnectionFailedListener);
        }

        @NonNull
        public T buildClient(@NonNull Context context, @NonNull Looper looper, @NonNull ClientSettings clientSettings, @NonNull O options, @NonNull ConnectionCallbacks connectionCallbacks, @NonNull OnConnectionFailedListener onConnectionFailedListener) {
            throw new UnsupportedOperationException();
        }
    }

    @Hide
    public interface AnyClient {
    }

    @Hide
    public static class AnyClientKey<C extends AnyClient> {
    }

    /**
     * Base interface for API options. These are used to configure specific parameters for
     * individual API surfaces. The default implementation has no parameters.
     */
    public interface ApiOptions {
        /**
         * Base interface for {@link ApiOptions} in {@link Api}s that have options.
         */
        interface HasOptions extends ApiOptions {
        }

        /**
         * Base interface for {@link ApiOptions} that are not required, don't exist.
         */
        interface NotRequiredOptions extends ApiOptions {
        }

        /**
         * {@link ApiOptions} implementation for {@link Api}s that do not take any options.
         */
        final class NoOptions implements NotRequiredOptions {
        }

        /**
         * Base interface for {@link ApiOptions} that are optional.
         */
        interface Optional extends HasOptions, NotRequiredOptions {
        }

        /**
         * An interface for {@link ApiOptions} that include an account.
         */
        interface HasAccountOptions extends HasOptions, NotRequiredOptions {
            @NonNull
            Account getAccount();
        }

        /**
         * An interface for {@link ApiOptions} that includes a {@link GoogleSignInAccount}
         */
        interface HasGoogleSignInAccountOptions extends HasOptions {
            GoogleSignInAccount getGoogleSignInAccount();
        }

        @NonNull
        NoOptions NO_OPTIONS = new NoOptions();
    }

    @Hide
    public static abstract class BaseClientBuilder<T extends AnyClient, O> {
        public static final int API_PRIORITY_GAMES = 1;
        public static final int API_PRIORITY_PLUS = 2;
        public static final int API_PRIORITY_OTHER = Integer.MAX_VALUE;

        @NonNull
        public List<Scope> getImpliedScopes(@Nullable O options) {
            return Collections.emptyList();
        }

        public int getPriority() {
            return API_PRIORITY_OTHER;
        }
    }

    @Hide
    public interface Client extends AnyClient {
        @Deprecated
        void connect();

        void connect(@NonNull BaseGmsClient.ConnectionProgressReportCallbacks connectionProgressReportCallbacks);

        void disconnect();

        void disconnect(@NonNull String reason);

        void dump(@NonNull String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args);

        @NonNull
        Feature[] getAvailableFeatures();

        @NonNull
        String getEndpointPackageName();

        @Nullable
        String getLastDisconnectMessage();

        int getMinApkVersion();

        void getRemoteService(@Nullable IAccountAccessor iAccountAccessor, @Nullable Set<Scope> scopes);

        @NonNull
        Feature[] getRequiredFeatures();

        @NonNull
        Set<Scope> getScopesForConnectionlessNonSignIn();

        @Nullable
        IBinder getServiceBrokerBinder();

        @NonNull
        Intent getSignInIntent();

        boolean isConnected();

        boolean isConnecting();

        void onUserSignOut(@NonNull BaseGmsClient.SignOutCallbacks signOutCallbacks);

        boolean providesSignIn();

        boolean requiresAccount();

        boolean requiresGooglePlayServices();

        boolean requiresSignIn();
    }

    @Hide
    public static final class ClientKey<C extends Client> extends AnyClientKey<C> {
    }

    @Hide
    @NonNull
    @Deprecated
    public BaseClientBuilder<? extends Client, O> getBaseClientBuilder() {
        return clientBuilder;
    }

    @Hide
    @NonNull
    public AbstractClientBuilder<? extends Client, O> getClientBuilder() {
        return clientBuilder;
    }

    @Hide
    @NonNull
    public ClientKey<? extends Client> getClientKey() {
        return clientKey;
    }

    @Hide
    @NonNull
    public String getName() {
        return name;
    }
}
