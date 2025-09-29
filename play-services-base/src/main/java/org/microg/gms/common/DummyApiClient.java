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

package org.microg.gms.common;

import android.content.Intent;
import android.os.IBinder;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.gms.common.Feature;
import com.google.android.gms.common.api.Api;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.common.internal.BaseGmsClient;
import com.google.android.gms.common.internal.IAccountAccessor;

import java.io.FileDescriptor;
import java.io.PrintWriter;
import java.util.Collections;
import java.util.Set;

@Deprecated
public class DummyApiClient implements Api.Client {
    private boolean connected = false;

    @Override
    public void connect() {
        connected = true;
    }

    @Override
    public void connect(@NonNull BaseGmsClient.ConnectionProgressReportCallbacks connectionProgressReportCallbacks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void disconnect() {
        connected = false;
    }

    @Override
    public void disconnect(@NonNull String reason) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void dump(@NonNull String prefix, @Nullable FileDescriptor fd, @NonNull PrintWriter writer, @Nullable String[] args) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Feature[] getAvailableFeatures() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public String getEndpointPackageName() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public String getLastDisconnectMessage() {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getMinApkVersion() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void getRemoteService(@Nullable IAccountAccessor iAccountAccessor, @Nullable Set<Scope> scopes) {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Feature[] getRequiredFeatures() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Set<Scope> getScopesForConnectionlessNonSignIn() {
        throw new UnsupportedOperationException();
    }

    @Nullable
    @Override
    public IBinder getServiceBrokerBinder() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    public Intent getSignInIntent() {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isConnecting() {
        return false;
    }

    @Override
    public void onUserSignOut(@NonNull BaseGmsClient.SignOutCallbacks signOutCallbacks) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean providesSignIn() {
        return false;
    }

    @Override
    public boolean requiresAccount() {
        return false;
    }

    @Override
    public boolean requiresGooglePlayServices() {
        return false;
    }

    @Override
    public boolean requiresSignIn() {
        return false;
    }
}
