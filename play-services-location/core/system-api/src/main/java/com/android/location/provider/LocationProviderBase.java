/*
 * SPDX-FileCopyrightText: 2010, The Android Open Source Project
 * SPDX-FileCopyrightText: 2014, microG Project Team
 * SPDX-License-Identifier: Apache-2.0
 */

package com.android.location.provider;

import android.content.Context;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.location.provider.ProviderProperties;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.IBinder;
import android.os.WorkSource;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.List;

/**
 * Base class for location providers implemented as unbundled services.
 *
 * <p>The network location provider must export a service with action
 * "com.android.location.service.v2.NetworkLocationProvider"
 * and a valid minor version in a meta-data field on the service, and
 * then return the result of {@link #getBinder()} on service binding.
 *
 * <p>The fused location provider must export a service with action
 * "com.android.location.service.FusedLocationProvider"
 * and a valid minor version in a meta-data field on the service, and
 * then return the result of {@link #getBinder()} on service binding.
 *
 * <p>IMPORTANT: This class is effectively a public API for unbundled
 * applications, and must remain API stable. See README.txt in the root
 * of this package for more information.
 *
 * @deprecated This class is not part of the standard API surface - use
 * {@link android.location.provider.LocationProviderBase} instead.
 */
@Deprecated
public abstract class LocationProviderBase {

    /**
     * Callback to be invoked when a flush operation is complete and all flushed locations have been
     * reported.
     */
    protected interface OnFlushCompleteCallback {

        /**
         * Should be invoked once the flush is complete.
         */
        void onFlushComplete();
    }

    /**
     * Bundle key for a version of the location containing no GPS data.
     * Allows location providers to flag locations as being safe to
     * feed to LocationFudger.
     *
     * @deprecated Do not use from Android R onwards.
     */
    @Deprecated
    public static final String EXTRA_NO_GPS_LOCATION = "noGPSLocation";

    /**
     * Name of the Fused location provider.
     *
     * <p>This provider combines inputs for all possible location sources
     * to provide the best possible Location fix.
     */
    public static final String FUSED_PROVIDER = LocationManager.FUSED_PROVIDER;

    final String mTag;
    @Nullable
    final String mAttributionTag;
    final IBinder mBinder;


    volatile ProviderProperties mProperties;
    volatile boolean mAllowed;

    /**
     * @deprecated Prefer
     * {@link #LocationProviderBase(Context, String, ProviderPropertiesUnbundled)}.
     */
    @Deprecated
    public LocationProviderBase(String tag, ProviderPropertiesUnbundled properties) {
        throw new UnsupportedOperationException();
    }

    /**
     * This constructor associates the feature id of the given context with this location provider.
     * The location service may afford special privileges to incoming calls identified as belonging
     * to this location provider.
     */
    @RequiresApi(VERSION_CODES.R)
    public LocationProviderBase(Context context, String tag,
                                ProviderPropertiesUnbundled properties) {
        throw new UnsupportedOperationException();
    }

    public IBinder getBinder() {
        return mBinder;
    }

    /**
     * @deprecated Use {@link #setAllowed(boolean)} instead.
     */
    @Deprecated
    @RequiresApi(VERSION_CODES.Q)
    public void setEnabled(boolean enabled) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets whether this provider is currently allowed or not. Note that this is specific to the
     * provider only, and is not related to global location settings. This is a hint to the Location
     * Manager that this provider will generally be unable to fulfill incoming requests. This
     * provider may still receive callbacks to onSetRequest while not allowed, and must decide
     * whether to attempt to satisfy those requests or not.
     *
     * <p>Some guidelines: providers should set their own allowed/disallowed status based only on
     * state "owned" by that provider. For instance, providers should not take into account the
     * state of the location master setting when setting themselves allowed or disallowed, as this
     * state is not owned by a particular provider. If a provider requires some additional user
     * consent that is particular to the provider, this should be use to set the allowed/disallowed
     * state. If the provider proxies to another provider, the child provider's allowed/disallowed
     * state should be taken into account in the parent's allowed state. For most providers, it is
     * expected that they will be always allowed.
     */
    @RequiresApi(VERSION_CODES.R)
    public void setAllowed(boolean allowed) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets the provider properties that may be queried by clients. Generally speaking, providers
     * should try to avoid changing their properties after construction.
     */
    @RequiresApi(VERSION_CODES.Q)
    public void setProperties(ProviderPropertiesUnbundled properties) {
        throw new UnsupportedOperationException();
    }

    /**
     * Sets a list of additional packages that should be considered as part of this location
     * provider for the purposes of generating locations. This should generally only be used when
     * another package may issue location requests on behalf of this package in the course of
     * providing location. This will inform location services to treat the other packages as
     * location providers as well.
     *
     * @deprecated On Android R and above this has no effect.
     */
    @Deprecated
    @RequiresApi(VERSION_CODES.Q)
    public void setAdditionalProviderPackages(List<String> packageNames) {}

    /**
     * @deprecated Use {@link #isAllowed()} instead.
     */
    @Deprecated
    @RequiresApi(VERSION_CODES.Q)
    public boolean isEnabled() {
        throw new UnsupportedOperationException();
    }

    /**
     * Returns true if this provider is allowed. Providers start as allowed on construction.
     */
    @RequiresApi(VERSION_CODES.R)
    public boolean isAllowed() {
        throw new UnsupportedOperationException();
    }

    /**
     * Reports a new location from this provider.
     */
    public void reportLocation(@NonNull Location location) {
        throw new UnsupportedOperationException();
    }

    /**
     * Reports a new batch of locations from this provider. Locations must be ordered in the list
     * from earliest first to latest last.
     */
    public void reportLocations(@NonNull List<Location> locations) {
        throw new UnsupportedOperationException();
    }

    protected void onInit() {
        // call once so that providers designed for APIs pre-Q are not broken
        throw new UnsupportedOperationException();
    }

    /**
     * @deprecated This callback will be invoked once when the provider is created to maintain
     * backwards compatibility with providers not designed for Android Q and above. This method
     * should only be implemented in location providers that need to support SDKs below Android Q.
     * Even in this case, it is usually unnecessary to implement this callback with the correct
     * design. This method may be removed in the future.
     */
    @Deprecated
    protected void onEnable() {}

    /**
     * @deprecated This callback will be never be invoked on Android Q and above. This method should
     * only be implemented in location providers that need to support SDKs below Android Q. Even in
     * this case, it is usually unnecessary to implement this callback with the correct design. This
     * method may be removed in the future.
     */
    @Deprecated
    protected void onDisable() {}

    /**
     * Set the {@link ProviderRequestUnbundled} requirements for this provider. Each call to this method
     * overrides all previous requests. This method might trigger the provider to start returning
     * locations, or to stop returning locations, depending on the parameters in the request.
     */
    protected abstract void onSetRequest(ProviderRequestUnbundled request, WorkSource source);

    /**
     * Requests a flush of any pending batched locations. The callback must always be invoked once
     * per invocation, and should be invoked after {@link #reportLocation(Location)} or
     * {@link #reportLocations(List)} has been invoked with any flushed locations. The callback may
     * be invoked immediately if no locations are flushed.
     */
    protected void onFlush(OnFlushCompleteCallback callback) {
        callback.onFlushComplete();
    }

    /**
     * @deprecated This callback will never be invoked on Android Q and above. This method may be
     * removed in the future. Prefer to dump provider state via the containing service instead.
     */
    @Deprecated
    protected void onDump(FileDescriptor fd, PrintWriter pw, String[] args) {}

    /**
     * This method will no longer be invoked.
     *
     * @deprecated This callback will be never be invoked on Android Q and above. This method should
     * only be implemented in location providers that need to support SDKs below Android Q. This
     * method may be removed in the future.
     */
    @Deprecated
    protected int onGetStatus(Bundle extras) {
        return LocationProvider.AVAILABLE;
    }

    /**
     * This method will no longer be invoked.
     *
     * @deprecated This callback will be never be invoked on Android Q and above. This method should
     * only be implemented in location providers that need to support SDKs below Android Q. This
     * method may be removed in the future.
     */
    @Deprecated
    protected long onGetStatusUpdateTime() {
        return 0;
    }

    /**
     * Implements location provider specific custom commands. The return value will be ignored on
     * Android Q and above.
     */
    protected boolean onSendExtraCommand(@Nullable String command, @Nullable Bundle extras) {
        return false;
    }
}