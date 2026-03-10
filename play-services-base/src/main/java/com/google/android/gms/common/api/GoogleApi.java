/*
 * SPDX-FileCopyrightText: 2020 microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 * Notice: Portions of this file are reproduced from work created and shared by Google and used
 *         according to terms described in the Creative Commons 4.0 Attribution License.
 *         See https://developers.google.com/readme/policies for details.
 */

package com.google.android.gms.common.api;

import android.app.Activity;
import android.content.Context;

import android.os.Looper;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import com.google.android.gms.common.api.internal.ApiExceptionMapper;
import com.google.android.gms.common.api.internal.ApiKey;
import com.google.android.gms.common.api.internal.StatusExceptionMapper;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import org.microg.gms.common.Hide;
import org.microg.gms.common.PublicApi;
import org.microg.gms.common.api.GoogleApiManager;
import org.microg.gms.common.api.PendingGoogleApiCall;

import static android.os.Build.VERSION.SDK_INT;

/**
 * Base class for Google API entry points. API clients based on this class manage the connection between your app and Google Play services
 * (as opposed to {@link GoogleApiClient}-based calls).
 * <p>
 * GoogleApi-based instances enqueue calls until a connection is made to Google Play services, and then execute them in order. The instances
 * are:
 * <ul>
 *     <li>"Cheap" to create</li>
 *     <li>Thread-safe</li>
 *     <li>Automatically deduplicated</li>
 *     <li>Automatically timed-out and reconnected when necessary</li>
 * </ul>
 * {@code GoogleApi} abstracts the connection to Play services, so callers do not need to implement {@link GoogleApiClient.ConnectionCallbacks}.
 * <p>
 * If the user needs to install or update Google Play services, {@code GoogleApi} will prompt the user to do so and enqueue API calls until the issue is
 * resolved. If {@code GoogleApi} was initialized with an {@code Activity} it will create a foreground prompt, otherwise it will display a system notification. If
 * the user cancels the resolution or some other issue arises, pending API calls will be fail with an {@link ApiException} and status code
 * {@code CommonStatusCodes.API_NOT_CONNECTED}.
 * <p>
 * If isGooglePlayServicesAvailable returns {@code true}, {@code GoogleApi} instances will not show any UI to resolve connection failures.
 */
@PublicApi
public abstract class GoogleApi<O extends Api.ApiOptions> implements HasApiKey<O> {
    @NonNull
    private final GoogleApiManager manager;
    @Hide
    @NonNull
    public final Api<O> api;
    @Hide
    @NonNull
    public final O options;
    @NonNull
    private final Context context;
    @Nullable
    private final String contextAttributionTag;
    @NonNull
    private final Looper looper;
    @NonNull
    private final GoogleApiClient apiClient;
    @NonNull
    private final StatusExceptionMapper exceptionMapper;
    private final ApiKey<O> key;

    @Hide
    public GoogleApi(@NonNull Context context, @NonNull Api<O> api, @NonNull O options, @NonNull Settings settings) {
        this(context, null, api, options, settings);
    }

    @Hide
    @Deprecated
    public GoogleApi(@NonNull Context context, @NonNull Api<O> api, @NonNull O options, @NonNull StatusExceptionMapper exceptionMapper) {
        this(context, api, options, new Settings.Builder().setMapper(exceptionMapper).build());
    }

    @Deprecated
    public GoogleApi(@NonNull Activity activity, @NonNull Api<O> api, @NonNull O options, @NonNull StatusExceptionMapper exceptionMapper) {
        this(activity, api, options, new Settings.Builder().setMapper(exceptionMapper).setLooper(activity.getMainLooper()).build());
    }

    @Hide
    @MainThread
    public GoogleApi(@NonNull Activity activity, @NonNull Api<O> api, @NonNull O options, @NonNull Settings settings) {
        this(activity, activity, api, options, settings);
    }

    private GoogleApi(@NonNull Context context, @Nullable Activity activity, @NonNull Api<O> api, @NonNull O options, @NonNull Settings settings) {
        this.context = context.getApplicationContext();
        if (SDK_INT < 30) {
            this.contextAttributionTag = getApiFallbackAttributionTag(context);
        } else {
            this.contextAttributionTag = ContextCompat.getAttributionTag(context);
        }
        this.api = api;
        this.options = options;
        this.looper = settings.looper;
        this.key = ApiKey.getSharedApiKey(api, options, contextAttributionTag);
        this.apiClient = null; //new ConnectionlessGoogleApiClient(this);
        this.manager = GoogleApiManager.getInstance(context);
        this.exceptionMapper = settings.exceptionMapper;
    }

    @Hide
    @Deprecated
    protected GoogleApi(Context context, Api<O> api, O options) {
        this(context, null, api, options, Settings.DEFAULT_SETTINGS);
    }

    @NonNull
    @Hide
    public GoogleApiClient asGoogleApiClient() {
        return this.apiClient;
    }

    /**
     * This method is only invoked on versions below Android R, where no attribution tag is available. If APIs have some other type of client
     * identifier they wish to use, they may implement this method in order to provide their own fallback which will be used in place of the
     * attribution tag (and thus passed through client/module implementations as the attribution tag).
     * <p>
     * This method is invoked from the constructor, and should be careful not to reference uninitialized members or allow references to escape.
     */
    protected @Nullable String getApiFallbackAttributionTag(Context context) {
        return null;
    }

    @Override
    @NonNull
    @Hide
    public ApiKey<O> getApiKey() {
        return this.key;
    }

    @NonNull
    @Hide
    public O getApiOptions() {
        return this.options;
    }

    @NonNull
    @Hide
    public Context getApplicationContext() {
        return this.context;
    }

    @Nullable
    @Hide
    protected String getContextAttributionTag() {
        return this.contextAttributionTag;
    }

    @Nullable
    @Hide
    @Deprecated
    protected String getContextFeatureId() {
        return this.contextAttributionTag;
    }

    @NonNull
    @Hide
    public Looper getLooper() {
        return this.looper;
    }

    @Hide
    protected <R, A extends Api.Client> Task<R> scheduleTask(PendingGoogleApiCall<R, A> apiCall) {
        TaskCompletionSource<R> completionSource = new TaskCompletionSource<>();
        manager.scheduleTask(this, apiCall, completionSource);
        return completionSource.getTask();
    }

    @Hide
    public static class Settings {
        @NonNull
        public static final Settings DEFAULT_SETTINGS = new Builder().build();

        @NonNull
        public final StatusExceptionMapper exceptionMapper;

        @NonNull
        public final Looper looper;

        @Hide
        public static class Builder {
            private StatusExceptionMapper exceptionMapper;
            private Looper looper;

            @NonNull
            public Settings build() {
                if (exceptionMapper == null) exceptionMapper = new ApiExceptionMapper();
                if (looper == null) looper = Looper.getMainLooper();
                return new Settings(exceptionMapper, looper);
            }

            @NonNull
            public Builder setLooper(@NonNull Looper looper) {
                this.looper = looper;
                return this;
            }

            @NonNull
            public Builder setMapper(@NonNull StatusExceptionMapper exceptionMapper) {
                this.exceptionMapper = exceptionMapper;
                return this;
            }
        }

        private Settings(@NonNull StatusExceptionMapper exceptionMapper, @NonNull Looper looper) {
            this.exceptionMapper = exceptionMapper;
            this.looper = looper;
        }
    }
}
